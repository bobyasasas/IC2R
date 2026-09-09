# 核反应堆本体切片一(EU 模式 3×6 网格)

2026-09-09 切片。开始迁移 legacy `TileEntityNuclearReactorElectric`(1114 行):
本切片覆盖 EU 模式核心循环、铀棒脉冲/耗损、散热片热受体、热量持久化与熔毁
爆炸。chamber 扩列、流体冷却模式、接口件(访问仓/流体口/红石口/RCI)、反射
与开关件交互属后续切片。

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

## 测试证据

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
