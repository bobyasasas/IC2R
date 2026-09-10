# 核反应堆(EU 模式与流体冷却模式)

2026-09-09 切片一至切片五,2026-09-10 切片六。迁移 legacy
`TileEntityNuclearReactorElectric`(1114 行)、`TileEntityReactorChamberElectric`
与 vessel 接口件(`TileEntityReactorVessel` 族:访问仓/红石口/流体口)。
覆盖 EU 模式核心循环、chamber 扩列、组件脉冲/散热/plating、地表热效果、
流体冷却模式、接口件与 6×9 满尺寸多方块校验。

## 旧行为依据(legacy)

- 每 20 tick 一轮:两遍遍历组件网格(pass 0 脉冲,pass 1 热),
  `maxHeat`/`hem` 每轮重置为 10000/1.0。
- 铀棒脉冲(legacy `ItemReactorUranium.processChamber`):仅在红石信号下
  (`produceEnergy`);`basePulses = 1 + cells/2`,每 cell 自脉冲 basePulses
  次(每次脉冲 `addOutput(1)`),再叠加四邻可脉冲组件数;热遍历按脉冲计数
  生成 `tri(pulses)×4` 热,平均分配给四邻热受体,余热入堆;每轮 `use+1`,
  `use ≥ duration-1` 时换耗尽棒。
- 热受体:`canStoreHeat` 组件(散热片)经 `alterHeat` 吸收,余量返回。
- EU 模式扩列:`getReactorSize = 3 + 六向直接邻接的 chamber 数`(上限 9,
  六面各一个 chamber 即满尺寸 6×9);chamber 自身只认六向直接邻接的堆芯
  (链式 chamber 不计列、不转发 GUI)。`dropAllUnfittingStuff` 每轮把
  无效列物品弹出。
- 流体模式门控(`isFluidReactor`,每轮评估):`isFullSize()`(9 列)+
  `hasFluidChamber()`(Chebyshev 半径 2 的 98 格外壳全部为 reactor_vessel
  方块或 `isWall()` 的 vessel 件——访问仓/红石口/流体口算墙,燃料棒
  chamber 的 `isWall()=false` 不算)+ 半径 4 立方内无其他满尺寸且外壳
  完整的堆芯。模式切换伴随能量网络/红石链接/罐口开闭。
- 流体热换:`huOutput = 40 × EmitHeat × outputModifier`;按热交换属性
  coolant→hot_coolant **20 HU/mB**(`round(20 × heatExchangerHotCoolant)`,
  配置默认 1.0)换算,只转换实际排出的冷却液,余量 `addHeat(huOutput/40)`
  回堆芯。
- 流体口寻址(`FluidReactorLookup`):在 Chebyshev 半径 2 内寻找**正在
  流体模式**的堆芯,附着其冷却液双罐。
- 热效果:`heat/maxHeat ≥ 1` 熔毁(爆炸前格内物品全部汽化,不掉落)、
  ≥ 0.85 火/岩浆、≥ 0.7 辐射伤害、≥ 0.5 蒸水、≥ 0.4 点燃;熔毁威力
  10 × 组件修正,受 `reactorExplosionPowerLimit` 限制。
- EU 输出:`output` EU/t,经 LV 电气节点向电缆输出。

## 新设计

- `MachineKind.NUCLEAR_REACTOR("nuclear_reactor", 100000, 18, 0, 0)`,
  `NuclearReactorBlockEntity extends PoweredBlockEntity` 并实现
  `ReactorHost`(legacy IReactor 子集:heat/maxHeat/hem/itemAt/output/
  produceEnergy)。
- `ReactorComponent` 接口(default 空实现,与 AbstractDamageableReactorComponent
  的非抽象方法对应);`FuelRodItem` 实现完整铀棒算法;`ReactorHeatItem`
  (散热片)以 `exchangeHeat` 作热受体。
- 热公式入 core:`ic2.core.reactor.ReactorMath`(三角数、铀棒热)。
- **存栈语义差异**:legacy 组件持有活 ItemStack 引用;新 transfer 栈的
  `inventory.stack()` 是副本——主机在每组件处理后按 `ItemStack.matches`
  判定并写回(耗尽换棒除外),热受体吸收后同样写回。
- 反应堆原始库存按 9 列布局存放 54 槽(legacy `InvSlotReactor` 同构),
  GUI 固定展示 9×6;无效列弹出必须直写原始槽位(`getItemAt` 对无效列
  恒返回 null)。
- EU 输出:每轮 `energy.insert(output × 20)`,经 `EnergyNode.Terminal.source`
  (LV 32 EU × 30 包)向电缆输出。

## 切片二:chamber 扩列与 MOX 堆内脉冲

- `ReactorChamberBlockEntity`:无自身物品的多方块代理;`findReactor` 六向
  寻址相邻堆芯(legacy chamber 同为六向直接邻接);右键经 `createMenu`
  转发打开反应堆 GUI;被移除后堆芯列数收缩。
- `columns()` = 3 + 直接邻接 chamber 数(上限 9);`getItemAt/setItemAt`
  按 active 列门控;每轮开始弹出无效列物品(legacy
  `dropAllUnfittingStuff`)。
- `MoxFuelRodItem` 实现堆内脉冲:每脉冲 `4 × (heat/maxHeat) + 1` EU
  (legacy `ReaktorOutput`);耗尽棒对应 depleted MOX 物品。

## 切片三:反射器/热开关/元件散热/plating

- `HeatStorageComponent`(抽象基类):冷却单元/散热片/热开关共用的存热
  基座(REACTOR_HEAT 组件、`alterHeat` 返回未吸收余量、耐久条/tooltip)。
- `ReactorVentItem`(heat_vent 1000/6/0、reactor_heat_vent 1000/5/5、
  overclocked 1000/20/36、advanced 1000/12/0):先从堆芯抽 `reactorVent`
  热入自身存储,再自放 `selfVent` 热入 EmitHeat。
- `VentSpreadItem`(component_heat_vent 4):给四邻各降 4 热,余量计入
  EmitHeat;冷却写回邻居堆栈。
- `HeatSwitchItem`(heat_exchanger 2500/12/4、reactor_heat_exchanger
  5000/0/72、component_heat_exchanger 5000/36/0、advanced 10000/24/8):
  legacy 介质百分比配速的双向热平衡(自身↔邻件↔堆芯)。
- `ReactorPlatingItem`(reactor_plating 1000/0.95、heat 2000/0.99、
  containment 500/0.9):热遍历提升 maxHeat 并乘 hem;爆炸影响系数。
- `ReflectorItem`(neutron_reflector 30000、thick 120000、iridium 永久):
  脉冲弹回给棒(棒再产出 1 EU 并计一次脉冲);薄片每弹一次耗损 1,
  耗尽即从网格消失;爆炸影响 -1。
- 16 个组件物品注册(创造页 INGREDIENTS),reactor.py 资源管线;
  配方转换 624 → 650。

## 切片四a:地表热效果与热包

- 地表热效果(legacy `calculateHeatEffects` 地表分支):热 ≥ 85% 时随机格
  起火/岩浆化,≥ 50% 时蒸发相邻水(显式置空气——`Level.removeBlock` 对
  流体会重建 legacy block),≥ 40% 时点燃可燃邻居;每轮按 hem 概率掷骰,
  随机坐标半径 2(不含自身)。
- `HeatpackItem`(heatpack 1000/1):堆芯热量低于 `1000 × 堆叠数` 时给
  四邻热受体充 `1 × 堆叠数` 热。配方 +1(650 → 651)。

## 切片四b:流体冷却模式与流体口

- 内部冷却液罐/热冷却液罐各 10,000 mB;冷却液只认 `ic2:coolant`。
- `convertEmitHeatToHotCoolant`:散热片自散热(`addEmitHeat`)经热交换
  属性换成热冷却液;罐满无法吸收的部分转为堆芯热量。
- `ReactorFluidPortBlockEntity`:附着堆芯,`fluidAutomation` 暴露冷却液
  进/热冷却液出双罐视图。
- 本切片曾以"流体口邻接"启用流体模式,理由是认为 legacy 门控不可达
  ——该判断有误,见切片六勘误;本切片的罐、换算与流体口能力保持有效。

## 切片四c(已被切片六取代):chamber 扩列

- 本切片曾把 `columns()` 改为 BFS 链式计数(链上 chamber 亦计列),理由
  同样是误认为 legacy 上限 3+4=7;切片六已恢复 legacy 的六向直接邻接
  语义。链式 chamber 从不扩列,`reactor_chamber_chain` 测试改为断言该
  legacy 行为。

## 切片五:接口件(access hatch / redstone port / RCI)与爆炸威力修正

- `ReactorAccessHatchBlockEntity`:转发打开反应堆网格界面,`automation`
  暴露堆芯 54 槽物品进出。
- `ReactorRedstonePortBlockEntity`:红石口自身的红石输入视同堆芯输入
  (`poweredPortNear`)。
- `ReactorRciBlockEntity`(rci_rsh/rci_lzh,48,000 EU、13 槽):对堆芯网格
  中存热 > 85% 的 RSH/LZH 冷凝器,消耗 1 个红石/青金石块 + 1,000 EU 将
  其存热清零(legacy `TileEntityAbstractRCI`)。
- 熔毁爆炸威力按 legacy 公式:基础 10,+组件加成,× hem × 组件乘数
  (反射器 -1、plating 乘数),下限 1。
- 4 个接口方块资源与合成配方转换。

## 切片六:6×9 满尺寸多方块校验(2026-09-10)

- `isFluidReactor = isFullSize() && hasVesselRing() && 无冲突堆`,每轮
  评估,替代此前"流体口邻接即启用"的近似。
- `hasVesselRing`:Chebyshev 半径 2 的 98 格外壳,reactor_vessel 方块或
  wall 件(访问仓/红石口/流体口)皆可;燃料棒 chamber 不算墙
  (legacy `isWall()=false`)。
- 冲突堆:半径 4 立方内存在其他满尺寸且外壳完整的堆芯则拒绝流体模式;
  对方外壳破损即解除。
- **勘误(推翻切片四b/四c 的记录)**:legacy `Util.ALL_DIRS` 是全部 6 个
  方向,`getReactorSize` 上限为 3+6=9——满尺寸与流体模式在 legacy
  **可达**,此前"上限 3+4=7、legacy 缺陷"的论断系与 `HORIZONTAL_DIRS`
  混淆。据此 `columns()` 恢复 legacy 六向直接邻接语义(移除 BFS 链式
  计数),流体模式恢复 legacy 完整门控。
- `convertEmitHeatToHotCoolant` 修正两处偏差:换热比从 1 HU/mB 改为
  legacy 20 HU/mB(1 热点 → 2 mB 热冷却液);修复冷却液不足/为空时仍
  按请求量凭空产出热冷却液的守恒缺陷——现在只转换实际排出的冷却液,
  未转换余量按 `huOutput/40` 回堆芯(与 legacy 一致)。
- 流体口 `findReactor` 改为 legacy `FluidReactorLookup` 语义:Chebyshev
  半径 2 内寻找正在流体模式的堆芯;红石口与访问仓的扫描同样扩到半径 2
  (访问仓保留 EU 模式邻接可用的新增行为,较 legacy 略宽,文档化偏差)。
- RCI 寻址支持穿一层 chamber(legacy RCI 面向堆芯或 chamber),
  `rciOutputBonus` 对贴着本堆 chamber 的注入器同样计数——满尺寸结构的
  六个基面全是 chamber,不穿墙则注入器无处安放。

## 测试证据

- 切片一/二:`uraniumRodPulsesAndDepletes`、`adjacentRodMultipliesHeat`、
  `ventAbsorbsRodHeat`、`meltDownExplodesCore`、`chamberWidensGrid`、
  `brokenChamberEjectsColumn`、`moxPulseScalesWithHeat`。
- 切片三:`reflectorBouncesPulse`、`platingRaisesCoreLimits`、
  `heatSwitchBalancesCoreHeat`、`ventSpreadCoolsNeighbours`。
- 切片四a:`heatpackWarmsVentStorage`(地表火/岩浆/蒸水/点燃为随机命中,
  列入 `待测试.md` 实机验收)。
- 切片五:`redstonePortPowersCore`、`hatchExposesGrid`、
  `rciRechargesCondensator`、`rciBonusRaisesConversion`。
- 切片六:
  - `reactor_full_size`:六面 chamber → 9 列,`isFullSize()` 成立;
  - `reactor_chamber_chain`:4 直接 + 2 链式 chamber = 7 列(链式不扩列,
    legacy 语义);
  - `reactor_fluid_mode_gating`:仅流体口邻接(无结构)→ 流体模式关闭,
    EU 路径照常,无热冷却液产出;
  - `reactor_fluid_mode`:满结构 + 三四联棒 + 三散热片 + 6,000 mB 冷却液
    → 流体模式启用,热冷却液产出且不超过排出的冷却液(守恒),堆芯低温;
  - `reactor_fluid_port_extract`:外壳内流体口 radius-2 附着流体堆,
    经端口抽出热冷却液;
  - `reactor_vessel_conflict`:两座完整结构相距 4 → 流体模式被阻断,
    打穿对方外壳后启用;
  - `vessel_ring_ports`:外壳含流体口仍完整、换成 chamber 则不完整;
  - `vessel_ring_detection` / `vessel_places` 保持。
- IC2 与 GT 双模式 `runGameTestServer` 297 项全绿(切片六)。

## 未验收 / 后续

- 地表热效果随机命中的实机验收(待测试.md §27)。
- 辐射伤害随 P16(防化服/辐射药水)。
- 反应堆 EU 输出经真实电缆/电压的整链验收与多人回归(M16)。
- 燃料棒装罐配方依赖核材料链已解锁;堆内脉冲/辐射为 P12/P16 后续验收。
