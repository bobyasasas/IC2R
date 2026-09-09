# 采矿机(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityMiner.java`(727 行):内部缓冲 1,000 EU,
  放电等级默认 1(配置 1–4);槽位:钻头/管道/扫描器各 1 + 15 格缓冲 + 1 升级格。
- `work()` 状态机:`getOperationPos` 沿管道列向下找第一个非管道格;
  - 无钻头 → `withDrawPipe`:每管道 3 EU/t × 20t 收回,管道回到缓冲;
    管道格中的非管道方块(垫块)会被消耗并放置以回填孔洞;
  - 该格非尖端 → `digDown`:消耗 1 管道,`mineBlock` 完成后放置管道尖端;
  - 该格是尖端 → `mineLevel(y)`:无扫描器直挖下一层;有扫描器先
    `startLayerScan`(OD 50 EU / OV 250 EU,半径 3 / 6)再向同层矿石目标
    Bresenham 掘进(`mineTowards`,每次调用处理一格,已挖空气跳过);
  - `mineBlock` 耗速:空气 3 EU/t × 20t;钻头 6×200 / 钻石 20×50 / 铱 200×20;
    液体:源/泵可达时标记 `liquidPos` 交泵处理并返回临时失败;
    不可采(黑曜石等 `canMine` 拒绝)返回永久失败;
  - `harvestBlock`:额外 2 ×(机器 Y − 目标 Y) EU + 钻头磨损 50/80/800;
    铱钻头按 fortune 3 取掉落;掉落入缓冲,满则丢在地上;
  - `canMine`:管道/尖端/箱子不可采;液体需相邻泵;`requiresCorrectToolForDrops`
    方块按钻头掉落正确性判定。
- `chargeTools` 每刻先给扫描器(2 级)再给钻头(3 级)从缓冲充电——
  供电盈余时钻头磨损被即时补满,是旧版原行为。

## 新实现

- `machine/MinerBlockEntity.java`:完整移植上述状态机(Work/Withdraw/MineAir/
  三种钻头模式);`acceptsInventorySlot` 限制钻头格(`DrillItem`)、管道格
  (管道物品或任意方块物品作垫块)、扫描器格(`ScannerItem`)。
- 升级格:矿机无可超频的固定工序,超频器无效果(与旧版一致);储能升级
  +10,000 EU 缓冲、变压器提升接入等级(1 级起,上限 5),每刻重算。
- 泵联动:`isPumpConnected` 检查六向相邻 `PumpBlockEntity.canDrain`
  (罐体余量 ≥1,000 mB 且朝向搜索仍能找到源);矿机标记 `liquidPos` 后经
  `requestDrain` 移交泵,泵下次作业优先抽取该位置(对应旧版
  `pump(target, sim, miner)` 契约)。
- 矿石目标:`c:ores` 标签(新增 `data/c/tags/block/ores.json` 把 IC2 铅/锡/铀矿
  并入通用标签)+ 远古残骸;旧版 `OreValues` 物品价值表不再复刻,以标签为准。
- 掉落:`Block.getDrops` 以钻头物品栈为工具;铱钻头临时附加 fortune 3
  (`ItemEnchantments`)。26.1.2 原版矿石掉落表不含工具标签谓词,
  组件化 Tool 规则决定正确采集。
- 界面:`MachineMenu` MINER 分支(钻头/管道/扫描器 8,22/40/58 + 5×3 缓冲 +
  升级侧栏);`client/MinerScreen` 泵模式按钮(`ic2.Miner.gui.pumpMode.*`,
  既有语言键);`MachineSounds` 矿机循环音效暂同其余机器为空。
- 资源:`machines.py` 生成 blockstate/十二态模型/纹理/掉落表/物品定义;
  `recipes.py` 解锁矿机配方(572/796);标签随管线补齐(wrench/pickaxe/
  needs_iron_tool)。

## 测试证据

- GameTest `MinerTests`(IC2/GT 双模式 182 项):
  - `miner_digs_down`:3 管道 + 钻石钻头挖穿 2 石 1 铁矿;管道-尖端列状态、
    原铁入缓冲、管道逐层消耗、缓冲充电(等效 legacy chargeTools)逐项断言;
  - `miner_scanner_tunnel`:OD 扫描器向 3 格外煤矿掘进(路径方块被采、
    煤入缓冲),每层一次 50 EU 扫描脉冲由缓冲补满;
  - `miner_withdraw`:取出钻头后整列管道收回且缓冲回收 2 管道、世界无残留;
  - `miner_pump_mode`:泵抽模式经 `menuAction` 切换;竖井水源被相邻泵抽走
    (罐体 ≥1,000 mB),矿机穿水继续掘进。

## 未验收范围

- 区块边界(`getMinY` 以下停止)已按移植实现,但无专门 GameTest;
- 真实客户端界面操作、泵模式按钮外观、十二态外观随实机测试;
- 高级矿机(`TileEntityAdvMiner`)独立切片;多人表现随 M16。
