# 切片：EU-Reader（meter）与 per-node 能量统计

本轮交付 legacy `ItemToolMeter`/`ContainerMeter`/`HandHeldMeter`/`GuiToolMeter`（EU-Reader，4 类 398 行）的移植，以及它依赖的 per-node 能量统计基建。catalog 连带翻转：registry item `ic2:meter`、menu `ic2:meter`，recipe `shaped/meter.json`。

## legacy 行为依据

- `ItemToolMeter.onItemUseFirst`（server 侧）：目标须为 `IEnergySource`/`IEnergyConductor`/`IEnergySink` 三类能量 tile，否则消息 "Not an energy net tile"；命中则打开手持菜单并把目标 tile `setUut`（只设一次）。
- `ContainerMeter.broadcastChanges`：每 tick `EnergyNet.getNodeStats(uut)` 采样；stats==null（tile 离网）关 GUI。5 模式：EnergyIn/EnergyOut/EnergyGain(=in−out)/Voltage/Amperage(working>0?:average)。读数 ×1000 缩放取整，滑动累计 avg/min/max/count（`avg=(avg*count+scaled)/(count+1)`），首次采样三者相同。
- `setMode` 归零累计；`reset` 归零；GUI 上 5 个模式按钮 + Reset 区域。
- `onDroppedByPlayer`：丢出持有 GUI 的电表时关 GUI。
- `GuiToolMeter`：guitooleumeter.png 217 宽，5 模式按钮 (112,55)/(132,55)/(112,75)/(132,75)/(152,65)，Reset 区域 (26,111)-(83,123)，avg/max/min/cycle 文本行，SI 缩写格式 `Util.toSiString(value, 6)`。
- 配方：glowstone_dust + circuit + insulated_copper_cable×4。

## 移植实现

- **core `PacketDistributor`**：`Result` 第 7 组件由第 18 轮的 `Map<GridPosition, Double> conductorEnergyIn` 升级为 `Map<GridPosition, NodeStats>`（`NodeStats(energyIn, energyOut, voltage, amperage)`），单次分发的每节点统计。记录点：经典模式 source(out=drawn)/conductor(in=out=drawn)/sink(in=accepted、voltage=到达包功率 drawn)；GT 模式 source(out=voltage)/每个 conductor(进入包功率逐段递减)/sink(in=accepted、voltage=扣除全部损耗后的到达包)。多包同 tick 累加，voltage 取峰值，amperage 计包数。
- **`WorldEnergyNetworks`**：`lastConductorEnergyIn` 字段升级为 `lastNodeStats`；每 tick 分发前重置为空、分发后以 `NodeStats.ZERO` 播种全部图节点再加载数据（无流量节点读零流，对应 legacy 已注册 tile 的零统计）。`conductorEnergyIn(level,pos)` 访问器签名不变（返回 `nodeStats().energyIn()`），detector cable 零改动回归；新增 `nodeStats(level,pos)`（可空）供 meter 使用。
- **`MeterMenu`**（ic2.neoforge.menu）：手持菜单范式（同 ContainmentBox/CropAnalyzer）。目标 BlockPos 只设一次（构造固定）；`broadcastChanges` 服务端采样 `nodeStats`，null 时 `closeContainer()`；5 模式 + `clickMenuButton`（id 0-4 切模式、id 5 Reset），切模式/Reset 归零累计。同步走 `ContainerData`，每个读数拆 2 个 16 位字（MachineMenuData 同款，×1000 缩放值超 short 范围）；EnergyGain 为负数时 hi 字算术拆分可还原符号。`stillValid` 与其他手持菜单一致（持有堆栈引用校验）。
- **`MeterItem`**：`onItemUseFirst` server 侧判定 `PoweredBlockEntity`（源/汇终端）或 `CableBlock`/`FoamCableBlock`（导体）为能量节点，非节点发 `ic2.meter.info.not_energy_net` 消息；命中则 `openMenu` 写入目标 BlockPos。`onDroppedByPlayer`（NeoForge IItemExtension）关 GUI。`stacksTo(1)` 同 legacy。
- **`MeterScreen`**：port 扁平面板范式（无专属贴图），217×137，5 模式按钮 + Reset 按钮（`handleInventoryButtonClick`），avg/max/min/cycle 文本行；`Util.toSiString(value,6)` 的 k/M/G/T 工程缩写逻辑完整移植进客户端。
- **资产**：`items/meter.json` + `models/item/meter.json`（parent ic2:item/tool/default）+ legacy `textures/item/tool/meter.png` 拷贝；lang 全套 `ic2.meter.*`/`container.ic2.meter`/`item.ic2.meter` 已在前置轮就位，本轮仅补 `ic2.meter.info.not_energy_net`（en+zh）。
- **配方**：`data/ic2/recipe/shaped/meter.json`（shaped 格式同 detector_cable 轮）。

## 取舍记档（4 条）

1. **Amperage 语义**：legacy 读 `IElectricalNode.getWorkingCurrent()/getAverageCurrent()`；port 无该 API，Amperage 模式=该节点当 tick 包计数（NodeStats.amperage）。电压/能量读数语义不变。
2. **conductorEnergyIn 升级为 nodeStats**：conductor 的 energyIn 即第 18 轮 conductorEnergyIn（数值一致，detector 测试守护回归）；conductor 的 energyOut 镜像其 inflow（导体只过流）。
3. **sink 读数**：energyIn=实际存入 accepted（端口 delivered 口径），voltage=到达包功率（经典=drawn、GT=扣损后包大小）；线路损耗体现在两者之差。
4. **丢弃关 GUI**：以 NeoForge `onDroppedByPlayer` + 菜单记录的手持槽位堆栈引用判定；legacy `saveAsThrown` 的掷出持久化不适用 port（手持容器均以堆栈组件存数据，meter 无数据）。

## 验证

- core 单测：`EnergyNetworkTest` 新增 nodeStats 断言（source out=32 / conductor in=out=32、amperage=1 / sink in=31、voltage=31 到达包），117 例全绿。
- GameTest `MeterTests` 5 例（test_instance JSON 同步新增，max_ticks 60-140）：
  - `meter_node_stats_flow`：发电机→电缆→MFE 全链统计（source 纯 out、conductor in=out 镜像、sink 纯 in、voltage/amperage>0、detector 访问器同源）。
  - `meter_idle_nodes_zero`：抽干电源后空闲节点仍存在且读零流。
  - `meter_menu_samples`：菜单首采样钉住 avg=min=max、EnergyIn 读数=节点 inflow×1000、clickMenuButton 切模式归零、Amperage≥1、Reset 全归零。
  - `meter_menu_closes_on_lost_target`：目标电缆被拆后菜单自动关闭（同 legacy stats==null），`closeContainer` 回背包菜单口径断言。
  - `meter_item_targets`：物品对机器 SUCCESS、电缆是能量节点、石头不是、对非节点 SUCCESS+消息。
  - IC2/GT 双模式各 5/5 隔离绿。
- 全量：IC2 模式 484 例（基线 479+5），首跑唯一失败为既有相位型 flaky `sheet_resin_cushion`（cushioned 13.0 vs bare 12.0，第 7 轮起记档），隔离复跑 1/1 绿；复跑全量见待测试.md §105。GT 模式 484 例同法验证。
- 连带：配方 `shaped/meter.json` 转换（数据包加载期解析校验，任一 GameTest 跑通即证）。
