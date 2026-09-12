# EU-Detector / EU-Splitter 电缆家族（第 18 轮切片：4 block + 2 item）

legacy 基线:`ic2/core/block/wiring/AbstractDetectorCableBlock.java`、
`AbstractSplitterCableBlock.java`、`DetectorCableBlock.java`、
`DetectorFoamCableBlock.java`、`SplitterCableBlock.java`、
`SplitterFoamCableBlock.java`、`ic2/core/ref/Ic2Blocks.java`(DETECTOR/SPLITTER
规格预留 + 四方块注册)、`ic2/core/ref/Ic2Items.java`(两个 ItemCable)。

## legacy 行为依据

- **规格**:`CableStats.DETECTOR/SPLITTER = (8192, 64, 0.5, 0, 0, 0.5)`
  (voltage 8192/电流 64/classic 损耗 0.5/GT 损耗 0/绝缘 0/直径 0.5);
  detector/splitter 无绝缘层级、无 foam 色染。foam 兄弟方块经
  `DetectorCableBlock.create(settings, DETECTOR_FOAM_CABLE)` 配对注册,
  掉落对应普通电缆物品。
- **detector**:ACTIVE 属性 + tickRate 32。onPlace
  `scheduleTick(nextInt(32))` 相位打散;tick = tickFoamHardening(foam 变体)
  + 重挂 32 + `energyIn > 0` 探测:能量流入→active=true 并
  setBlockAndUpdate(触发邻居更新),已 active 且值不变时仅
  updateNeighbourForOutputSignal(比较器重估);无流量→active=false。
  红石输出 active?15:0;比较器 `(int)(energyIn / 8192 × 15)` 饱和到 15
  (32 EU→0、2048 EU→3、8192 EU→15)。energyIn 是**该电缆导体上的流入
  汇总**(ENet 每秒 dissector 按 conductor 统计)。
- **splitter**:getStateForPlacement 读 `hasNeighborSignal(clickedPos)`
  初始相位;neighborChanged 里 active≠hasNeighborSignal 时翻转 ACTIVE
  并入网/出网(addToEnet 门控 active)。**未激活的 splitter 不入
  ENet = 网络在此断开**,激活即并入——用红石做电缆开关。
- **foam 变体**:与普通 foam cable 同壳(喷涂/硬化/破坏转回),detector
  foam 软期探针仍每 tick(硬化链 1-tick 驱动顺带探测),硬化后转 32 链。

## 移植实现

- **per-conductor 能量统计(core)**:`energy/grid/PacketDistributor.Result`
  新增第 7 组件 `Map<GridPosition, Double> conductorEnergyIn`:
  emitClassic 在 conductors 循环 merge drawn、emitGt 成功路径对 traversed
  merge voltage;`WorldEnergyNetworks.Network.lastConductorEnergyIn` 每
  tick 先 `Map.of()` 重置(skipped distribution 读 0 不读陈旧值)再赋
  result 值,静态访问器 `conductorEnergyIn(ServerLevel, BlockPos)`。
- **四个新块类**(`neoforge/.../energy/`):
  - `DetectorCableBlock extends CableBlock`(CableBlock/FoamCableBlock 由
    final 放宽为可继承,material/insulation 字段包私有):ACTIVE 属性、
    onPlace scheduleProbe(nextInt(32),`hasScheduledTick` 防重)、tick=
    energyProbe(重挂 32+probeNow)、红石四件套(isSignalSource/getSignal/
    hasAnalogOutputSignal/getAnalogOutputSignal)、比较器
    `(int) Mth.clamp(energyIn / spec.voltageLimit() × 15, 0, 15)`。
  - `SplitterCableBlock extends CableBlock`:自有 ACTIVE 属性;
    getStateForPlacement 采纳放置时信号;neighborChanged 翻转 ACTIVE+
    `WorldEnergyNetworks.invalidate`(3 flag setBlock 成功才翻转)。
  - `DetectorFoamCableBlock`/`SplitterFoamCableBlock extends
    FoamCableBlock`:detector foam tick 双分支——硬 foam 直接走
    DetectorCableBlock.energyProbe(32 链);软 foam 先 tickFoamHardening
    再 probeNow(1-tick 硬化链顺带探测),转硬时移交 32 链。vanilla
    `LevelChunkTicks.ticksPerPosition` 对同 (pos,block) 重复 scheduleTick
    去重(保留先入者),双链不可能——legacy 宽松重挂语义同源,记档。
  - **property 实例共享**:foam 变体的 ACTIVE 引用 plain 类常量
    (`public static final BooleanProperty ACTIVE =
    DetectorCableBlock.ACTIVE`),静态 signalFor/analogFor/probeNow 对
    foam state 读同一 property 实例(跨块共享 property 与 vanilla
    PipeBlock.PROPERTY_BY_DIRECTION 同理,合法)。
- **splitter 隔离语义**:`WorldEnergyNetworks.rebuild` BFS 对
  SplitterCableBlock(及 foam 变体)`isConducting(state)==false` 的邻居
  **不放 Conductor 节点**——BFS `to == null → continue` 不再前进,
  等价 legacy removeFromEnet 隔离;翻转激活后 invalidate 重建即并入。
  detector/splitter 泡沫变体经 `conducts` 重载同门控。
- **注册**:`ModMachines` 新 helper `specialCable/specialFoamCable`
  (Function<Properties, CableBlock> 工厂)——detector_cable/splitter_cable
  直接进 CABLES 表(有 BlockItem),detector_foam_cable/
  splitter_foam_cable 进 FOAM_CABLES 表(无 BlockItem,randomTicks);
  cableCounterpart/foamCounterpart/CutterItem/FoamSprayerItem 零改动
  (CutterItem 既有 `insulation() <= 0 → return` 使 0 绝缘的 detector
  foam 剪刀安全 PASS)。
- **资产**:2 blockstate(multipart:ACTIVE×6 向+center,legacy 14 条目
  同构;active 贴图换 `_0_active`)、2 foam blockstate(foam=soft→
  `ic2:block/cf/foam`、hard→`ic2:block/cf/wall_light_gray`,无 active
  条件与 legacy 同)、8 模型(8 像素直径盒 [4,4,4]→[12,12,12]/[4,4,0]→
  [12,12,4])、2 物品模型、2 loot(掉自身)、2 foam loot(掉对应普通
  物品)、6 贴图自 legacy 拷贝。

## 已知取舍(如实记档)

1. **探测器探测源为调度 tick 采样**而非 legacy ENet 秒级 dissector:
   port 能量网按 tick 分发,probeNow 读当 tick `conductorEnergyIn`,
   反应延迟≤1 tick(legacy 1s);比较器随 tick 实时刷新(legacy 每秒)。
   验收以"有流量亮/无流量灭/比例刻度"为准,与 legacy 行为方向一致。
2. **comparator 满刻度基准用规格电压 8192**(legacy 同公式同基准)。
3. **splitter 未激活时 rebuild 不放节点**=legacy 不入 ENet 语义;区别在
   port 重建以方块图为准(翻转后 invalidate),legacy 直接挂/摘 ENet
   对象——外部表现(断流/复通)一致。
4. **foam 变体 2 态复用**(软/硬浅灰)沿第 17 轮 foam cable 取舍。

## 验证

- GameTest `neoforge/src/gameTest/java/ic2/neoforge/test/DetectorCableTests.java`
  7 例:detector_activates(GEN→MFE 流量→ACTIVE+红石 15+到池)、
  comparator(MFSU→MFSU 2048 EU 包→analog 3)、deactivates(抽干源→
  ACTIVE false+红石 0+analog 0)、splitter_gates(未激活断流→红石块
  激活→复通)、splitter_placement(带信号放置即 ACTIVE、撤信号回
  inactive)、foam_detector(喷涂后探测+输电)、family(6 注册+双向
  counterpart 对称+规格 8192/0 绝缘+foam 无物品+剪刀 PASS 不变)。
- 隔离 IC2/GT 各 5+2 绿;全量 479×2(IC2 首轮既有相位型 flaky
  sheet_resin_cushion 失败、隔离复证绿,GT 一次全绿);
  `:core:test` 绿(PacketDistributor 新组件)。
