# 核反应堆本体(EU 模式)

2026-09-09 切片一与切片二。迁移 legacy `TileEntityNuclearReactorElectric`
(1114 行)与 `TileEntityReactorChamberElectric`。切片一覆盖 EU 模式核心
循环、铀棒脉冲/耗损、散热片热受体、热量持久化与熔毁爆炸;切片二加入
chamber 扩列(3→最高 9 列)、无效列弹出与 MOX 堆内脉冲。流体冷却模式、
接口件(访问仓/流体口/红石口/RCI)、反射与开关件交互属后续切片。

## 旧行为依据(legacy 420–650 行)

- 每 20 tick 一轮:两遍遍历组件网格(pass 0 脉冲/heatRun=false,pass 1 热
  /heatRun=true),`maxHeat`/`hem` 每轮重置为 10000/1.0。
- 铀棒脉冲(legacy `ItemReactorUranium.processChamber`):仅在红石信号下
  (`produceEnergy`);`basePulses = 1 + cells/2`,每 cell 自脉冲 basePulses
  次(每次脉冲 `addOutput(1)`),再叠加四邻可脉冲组件数;热遍历按脉冲计数
  生成 `tri(pulses)×4` 热,平均分配给四邻热受体,余热入堆;每轮 `use+1`,
  `use ≥ duration-1` 时换耗尽棒。
- 热受体:`canStoreHeat` 组件(散热片)经 `alterHeat` 吸收,余量返回。
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
  (散热片)以 `exchangeHeat` 作热受体;`MoxFuelRodItem` 堆内行为**本切片
  未实现**(processChamber 惰性,明确未实现项,防误导)。
- 热公式入 core:`ic2.core.reactor.ReactorMath`(三角数、铀棒热)。
- **存栈语义差异**:legacy 组件持有活 ItemStack 引用;新 transfer 栈的
  `inventory.stack()` 是副本——主机在每组件处理后按 `ItemStack.matches`
  判定并写回(耗尽换棒除外),热受体吸收后同样写回。
- 热效果本切片实现熔毁(≥100%:格内汽化+移除方块+`HeatExplosion` 威力
  10)与运行状态;火/岩浆/辐射/蒸水/点燃的地表效果与爆炸威力组件修正随
  后续切片。
- EU 输出:每轮 `energy.insert(output × 20)`,经 `EnergyNode.Terminal.source`
  (LV 32 EU × 30 包)向电缆输出。
- 资源:`machines.py` 加入 `nuclear_reactor`(十二态/Generator 掉落/
  挖掘标签/双语名);语言键补 `ic2.tooltip.reactor_heat`。

## 切片二:chamber 扩列与 MOX 堆内脉冲

- `ReactorChamberBlockEntity`:无自身物品的多方块代理;`findReactor` 六向
  寻址相邻堆芯;右键经 `createMenu` 转发打开反应堆 GUI;被移除后堆芯列数
  收缩。
- `NuclearReactorBlockEntity.columns()` = 3 + 相邻且指向本堆的 chamber 数
  (上限 9);`getItemAt/setItemAt` 按 active 列门控;每轮开始把无效列的
  物品弹出(legacy `dropAllUnfittingStuff`;弹出必须直写原始槽位——
  `getItemAt` 对无效列恒返回 null,是本切片踩过的坑)。
- 反应堆原始库存按 9 列布局存放 54 槽(legacy `InvSlotReactor` 同构),
  GUI 固定展示 9×6,menu 高度 232。
- `MoxFuelRodItem` 实现堆内脉冲:每脉冲 `4 × (heat/maxHeat) + 1` EU
  (legacy `ReaktorOutput`);`getFinalHeat` 的流体模式 ×2 分支留待流体
  冷却切片(当前恒为 EU 模式);耗尽棒对应 depleted MOX 物品。

## 切片三:反射器/热开关/元件散热/plating

- `HeatStorageComponent`(抽象基类):冷却单元/散热片/热开关共用的存热
  基座(REACTOR_HEAT 组件、`alterHeat` 返回未吸收余量、耐久条/tooltip);
  保留 legacy `exchangeHeat`/`heat` API 供既有测试与冷凝器兼容。
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
- **堆栈语义**:组件对邻居的修改(反射器耗损、散热片吸热、扩散冷却)
  统一在工作副本上修改后写回;耗尽自毁的反射器不写回。
- 16 个组件物品注册(创造页 INGREDIENTS),reactor.py 资源管线;
  配方转换 624 → 650(组件合成 26 条解锁,含 iridium_neutron_reflector)。

## 测试证据

- `ReactorComponentTests.reflectorBouncesPulse`:反射器使棒二次脉冲
  (输出 2、热 ≥ 三角形 8),薄片耗损 +1。
- `platingRaisesCoreLimits`:plating 将 maxHeat 提至 11000、hem 乘 0.95。
- `heatSwitchBalancesCoreHeat`:热交换器从 5,000 热的堆芯抽出热量。
- `ventSpreadCoolsNeighbours`:扩散散热给相邻冷却单元降 4 热。
- `ReactorChamberTests`/`NuclearReactorTests` 既有断言(扩列/收缩弹出/
  MOX 脉冲/单棒脉冲/散热吸收/熔毁)全部保持通过。
- IC2 与 GT 双模式 `runGameTestServer` 220 项全绿。

## 测试证据(切片一/二)
## 切片四a:地表热效果与热包

- `meltDown` 中的地表热效果(legacy `calculateHeatEffects` 地表分支):
  热 ≥ 85% 时随机格起火/岩浆化,≥ 50% 时蒸发相邻水(显式置空气——
  `Level.removeBlock` 对流体会重建 legacy block,蒸发会"假装无效"),
  ≥ 40% 时点燃可燃邻居;每轮按 hem 概率掷骰,随机坐标半径 2(不含自身)。
- `HeatpackItem`(heatpack 1000/1):堆芯热量低于 `1000 × 堆叠数` 时,给
  四邻热受体充 `1 × 堆叠数` 热(堆内"热包"供暖件)。
- 配方 +1(heatpack),转换 650 → 651。

## 测试证据(切片四a)

- `ReactorHeatEffectTests.heatpackWarmsVentStorage`:heatpack(堆叠 60)把
  相邻冷却单元从 0 充到 60,热量来自堆芯预算。
- 地表火/岩浆/蒸水/点燃为随机命中,自动断言不稳定——实现经代码审查对齐
  legacy,实际效果列入 `待测试.md` §27 实机验收。

## 切片四b:流体冷却模式与流体口

- 反应堆内部新增冷却液罐(10,000 mB)与热冷却液罐(10,000 mB);冷却液
  只认 `ic2:coolant`,热罐只认 `ic2:hot_coolant`。
- 流体模式:相邻存在 `reactor_fluid_port` 时启用(legacy 以 9 列满尺寸 +
  fluid chamber 为门控——`getReactorSize` 上限 3+4=7 使其不可达,判定为
  legacy 缺陷;新实现以流体口邻接启用,达成可见意图)。流体模式下棒产出
  不再进入 EU 缓冲。
- `convertEmitHeatToHotCoolant`:散热片自散热排出的热(`addEmitHeat`)按
  40 热/1 mB(legacy huOutputModifier 40 × config 1)换成热冷却液;罐满
  无法吸收的部分转为堆芯热量。
- `ReactorFluidPortBlockEntity`:六向寻址堆芯;`fluidAutomation` 暴露
  冷却液进/热冷却液出双罐视图;注册 `Capabilities.Fluid.BLOCK`。
- 配方:reactor_chamber/reactor_fluid_port 方块资源与合成已转换。

## 测试证据

- `ReactorFluidModeTests.fluidModeConvertsHeatToHotCoolant`:流体口邻接 +
  6,000 mB 冷却液 + 三铀棒/三散热片 → 热冷却液产出、冷却液消耗、堆芯低温。
- `hotCoolantExtractsThroughPort`:热冷却液经流体口能力抽出。
- 双模式 223 项全绿(切片三反射/开关/plating 保持)。

## 切片五:接口件(access hatch / redstone port / RCI)与爆炸威力修正

- `ReactorAccessHatchBlockEntity`:六向寻址堆芯;右键经 `createMenu` 转发
  打开反应堆网格界面;`automation` 暴露堆芯 54 槽物品进出。
- `ReactorRedstonePortBlockEntity`:红石口自身的红石输入视同堆芯输入
  (`poweredPortNear`),堆芯 `produceEnergy` 直连信号或红石口信号皆可。
- `ReactorRciBlockEntity`(rci_rsh/rci_lzh,48,000 EU、13 槽 = 9 冷却块格
  + 4 升级):对相邻堆芯网格中存热 > 85% 的 RSH/LZH 冷凝器,消耗 1 个
  红石/青金石块 + 1,000 EU 将其存热清零(legacy `TileEntityAbstractRCI`)。
- 熔毁爆炸威力按 legacy 公式:基础 10,+组件加成,× hem × 组件乘数
  (反射器 -1、plating 乘数),下限 1。
- 4 个接口方块资源与合成配方转换(machines.py)。

## 测试证据

- `ReactorAccessHatchTests.redstonePortPowersCore`:红石口的输入驱动堆芯
  脉冲周期。
- `hatchExposesGrid`:访问仓把棒插入堆芯网格。
- `rciRechargesCondensator`:RCI 消耗 1 红石块 + 1,000 EU,把 >85% 的
  RSH 冷凝器清零。
- 双模式 223 项全绿(既有反射/plating/热开关/流体测试保持)。

## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片五:接口件(access hatch / redstone port / RCI)与爆炸威力修正

- `ReactorAccessHatchBlockEntity`:六向寻址堆芯;右键经 `createMenu` 转发
  打开反应堆网格界面;`automation` 暴露堆芯 54 槽物品进出。
- `ReactorRedstonePortBlockEntity`:红石口自身的红石输入视同堆芯输入
  (`poweredPortNear`),堆芯 `produceEnergy` 直连信号或红石口信号皆可。
- `ReactorRciBlockEntity`(rci_rsh/rci_lzh,48,000 EU、13 槽 = 9 冷却块格
  + 4 升级):对相邻堆芯网格中存热 > 85% 的 RSH/LZH 冷凝器,消耗 1 个
  红石/青金石块 + 1,000 EU 将其存热清零(legacy `TileEntityAbstractRCI`)。
- 熔毁爆炸威力按 legacy 公式:基础 10,+组件加成,× hem × 组件乘数
  (反射器 -1、plating 乘数),下限 1。
- 4 个接口方块资源与合成配方转换(machines.py)。

## 测试证据

- `ReactorAccessHatchTests.redstonePortPowersCore`:红石口的输入驱动堆芯
  脉冲周期。
- `hatchExposesGrid`:访问仓把棒插入堆芯网格。
- `rciRechargesCondensator`:RCI 消耗 1 红石块 + 1,000 EU,把 >85% 的
  RSH 冷凝器清零。
- 双模式 223 项全绿(既有反射/plating/热开关/流体测试保持)。

## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片四b:流体冷却模式与流体口

- 反应堆内部新增冷却液罐(10,000 mB)与热冷却液罐(10,000 mB);冷却液
  只认 `ic2:coolant`,热罐只认 `ic2:hot_coolant`。
- 流体模式:相邻存在 `reactor_fluid_port` 时启用(legacy 以 9 列满尺寸 +
  fluid chamber 为门控——`getReactorSize` 上限 3+4=7 使其不可达,判定为
  legacy 缺陷;新实现以流体口邻接启用,达成可见意图)。流体模式下棒产出
  不再进入 EU 缓冲。
- `convertEmitHeatToHotCoolant`:散热片自散热排出的热(`addEmitHeat`)按
  40 热/1 mB(legacy huOutputModifier 40 × config 1)换成热冷却液;罐满
  无法吸收的部分转为堆芯热量。
- `ReactorFluidPortBlockEntity`:六向寻址堆芯;`fluidAutomation` 暴露
  冷却液进/热冷却液出双罐视图;注册 `Capabilities.Fluid.BLOCK`。
- 配方:reactor_chamber/reactor_fluid_port 方块资源与合成已转换。

## 测试证据

- `ReactorFluidModeTests.fluidModeConvertsHeatToHotCoolant`:流体口邻接 +
  6,000 mB 冷却液 + 三铀棒/三散热片 → 热冷却液产出、冷却液消耗、堆芯低温。
- `hotCoolantExtractsThroughPort`:热冷却液经流体口能力抽出。
- 双模式 223 项全绿(切片三反射/开关/plating 保持)。

## 切片五:接口件(access hatch / redstone port / RCI)与爆炸威力修正

- `ReactorAccessHatchBlockEntity`:六向寻址堆芯;右键经 `createMenu` 转发
  打开反应堆网格界面;`automation` 暴露堆芯 54 槽物品进出。
- `ReactorRedstonePortBlockEntity`:红石口自身的红石输入视同堆芯输入
  (`poweredPortNear`),堆芯 `produceEnergy` 直连信号或红石口信号皆可。
- `ReactorRciBlockEntity`(rci_rsh/rci_lzh,48,000 EU、13 槽 = 9 冷却块格
  + 4 升级):对相邻堆芯网格中存热 > 85% 的 RSH/LZH 冷凝器,消耗 1 个
  红石/青金石块 + 1,000 EU 将其存热清零(legacy `TileEntityAbstractRCI`)。
- 熔毁爆炸威力按 legacy 公式:基础 10,+组件加成,× hem × 组件乘数
  (反射器 -1、plating 乘数),下限 1。
- 4 个接口方块资源与合成配方转换(machines.py)。

## 测试证据

- `ReactorAccessHatchTests.redstonePortPowersCore`:红石口的输入驱动堆芯
  脉冲周期。
- `hatchExposesGrid`:访问仓把棒插入堆芯网格。
- `rciRechargesCondensator`:RCI 消耗 1 红石块 + 1,000 EU,把 >85% 的
  RSH 冷凝器清零。
- 双模式 223 项全绿(既有反射/plating/热开关/流体测试保持)。

## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片五:接口件(access hatch / redstone port / RCI)与爆炸威力修正

- `ReactorAccessHatchBlockEntity`:六向寻址堆芯;右键经 `createMenu` 转发
  打开反应堆网格界面;`automation` 暴露堆芯 54 槽物品进出。
- `ReactorRedstonePortBlockEntity`:红石口自身的红石输入视同堆芯输入
  (`poweredPortNear`),堆芯 `produceEnergy` 直连信号或红石口信号皆可。
- `ReactorRciBlockEntity`(rci_rsh/rci_lzh,48,000 EU、13 槽 = 9 冷却块格
  + 4 升级):对相邻堆芯网格中存热 > 85% 的 RSH/LZH 冷凝器,消耗 1 个
  红石/青金石块 + 1,000 EU 将其存热清零(legacy `TileEntityAbstractRCI`)。
- 熔毁爆炸威力按 legacy 公式:基础 10,+组件加成,× hem × 组件乘数
  (反射器 -1、plating 乘数),下限 1。
- 4 个接口方块资源与合成配方转换(machines.py)。

## 测试证据

- `ReactorAccessHatchTests.redstonePortPowersCore`:红石口的输入驱动堆芯
  脉冲周期。
- `hatchExposesGrid`:访问仓把棒插入堆芯网格。
- `rciRechargesCondensator`:RCI 消耗 1 红石块 + 1,000 EU,把 >85% 的
  RSH 冷凝器清零。
- 双模式 223 项全绿(既有反射/plating/热开关/流体测试保持)。

## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`
## 切片四c:chamber 链式扩列至 9 列

- `columns()` 改为 BFS:直接邻接 chamber 各计 1 列,chamber 相邻 chamber
  的链式延伸同样计数,上限 9 列(legacy 6×9 满尺寸)。
- **发现 legacy 缺陷**:`isFluidReactor` 要求 `isFullSize()==9`,而
  `getReactorSize()` 上限为 3+4=7——legacy 流体模式实际不可达。新实现以
  BFS 计数 + 流体口邻接启用流体模式,使其真正可用(文档化偏差)。
- 无效列弹出、`getItemAt/setItemAt` 门控均按动态列数工作。

## 测试证据(切片四c)

- `reactor_chamber_chain`:6 个 chamber(含两段链)扩列至 9;无效列物品
  弹出逻辑保持。

## 测试证据

- `ReactorChamberTests.chamberWidensGrid
## 测试证据

- `ReactorChamberTests.chamberWidensGrid`:1/2 个 chamber 分别扩列到 4/5,
  扩展列中的棒正常运行耗损。
- `brokenChamberEjectsColumn`:拆 chamber 列收缩,无效列的双联棒被弹出为
  掉落物。
- `moxPulseScalesWithHeat`:热量 50% 时 MOX 单棒脉冲产出 3 EU
  (4 × 0.5 + 1),热遍历按铀公式加 4 热。
- `uraniumRodPulsesAndDepletes`:单铀棒三轮——每轮 1 脉冲、3 轮累计 12 热、
  use=3。
- `adjacentRodMultipliesHeat`:相邻双棒互相脉冲,输出 > 2、热 > 8
  (三角数放大)。
- `ventAbsorbsRodHeat`:散热片吸收棒热且不超容。
- `meltDownExplodesCore`:五根四联棒连续过热 → 方块移除、格内汽化无掉落、
  热量清零。
- IC2 与 GT 双模式 `runGameTestServer` 213 项全绿。

## 未验收 / 后续(反应堆后续切片)

- chamber 扩列(3→6→9 列)与 `reactor_vessel` 方块。
- 反射器/增反器、热开关、元件散热片散热行为、plating(hem/maxHeat 修正)。
- MOX 堆内脉冲(热 scaled 输出与 `getFinalHeat` ×2)。
- 流体冷却模式 + 访问仓/流体口/红石口/RCI 接口件。
- 地表热效果(火/岩浆/辐射/蒸水/点燃)与爆炸威力组件修正、EU 输出的
  真实电缆/电压验收(自然 tick)。
