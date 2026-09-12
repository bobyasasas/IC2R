# 升级家族收尾切片：redstone_inverter_upgrade + remote_interface_upgrade

legacy `ItemUpgradeModule`（`ic2/core/item/upgrade/ItemUpgradeModule.java`，588 行）11 种 UpgradeType 中
port 已有 7 种（`UpgradeItem.Kind`），本切片补齐剩余两种非方向性升级，升级家族至此全量交付。

## redstone_inverter_upgrade（1.5k EU 语义红石反转器）

**legacy 行为**（IRedstoneSensitiveUpgrade + InvSlotUpgrade.UpgradeRedstoneModifier + Redstone 组件）：

- `Redstone.update()` 读 `getBestNeighborSignal`，逐个过机器升级槽里的 modifier：
  inverter 返回 `15 - external`；所有消费者再对结果做 `> 0` 判断。
- 因此反转语义是**模拟量**的：只有满强度信号（15）反转为"无信号"，任何不足满信号（含 0）反转为"有信号"。

**port 实现**：

- `UpgradeItem.invertedSignal(machine, inventory, level, pos)`：读 `getBestNeighborSignal`，
  扫描 `kind().upgradeStart()..inventory.size()` 升级槽（与 `UpgradeableBlockEntity.refreshUpgrades`
  同一扫描范围），命中 inverter 即返回 `15 - external > 0`，否则 `external > 0`——与 legacy 链条逐位等价。
- 5 台带红石门控且持有升级槽的机器接线（各机器门控方向沿用既有切片已验收行为，反转统一发生在信号输入处）：

| 机器 | legacy 消费方向 | port 门控位置 |
| --- | --- | --- |
| induction_furnace | `(canOperate \|\| hasRedstoneInput)` 保温（`TileEntityInduction:93/145`） | `InductionFurnaceBlockEntity` cycle.tick |
| centrifuge | `!hasRedstoneInput()` 出料 + 低热保温（`TileEntityCentrifuge:74/82`） | `CentrifugeBlockEntity` heat.tick |
| blast_furnace | `hasRedstoneInput()` 加热（`TileEntityBlastFurnace:155`） | `BlastFurnaceBlockEntity` heatUp |
| adv_miner | `hasRedstoneInput()` 暂停（`TileEntityAdvMiner:155`） | `AdvMinerBlockEntity` work |
| matter_generator | `subscribe(newLevel == 0)` 能量启用 + 暂停（`TileEntityMatter:89/154`） | `MatterGeneratorBlockEntity` serverTick |

- **suitable 边界如实记档**：legacy `RedstoneSensitive` 共 7 台。port 接受其中 6 台；
  `magnetizer` 排除——port 磁化机无升级槽（待机器对齐切片补齐），物品物理不可插入；
  `replicator` 接受但惰性——legacy `TileEntityReplicator` 仅声明 RedstoneSensitive 属性、
  从未挂载 Redstone 组件，inverter 在 legacy 里同样是"可插入、无效果"，port 1:1 保留该惰性。
- `suitable()` 检查置于方法最前（各机器既有 return 均只枚举方向性类型，放后会漏入默认分支）。
- 客户端 tooltip：`IndustrialCraftClient.addTooltip` 补 `REDSTONE_INVERTER`/`REMOTE_INTERFACE`
  case——不加会落入 default 分支被错标为 ejector 提示。

## remote_interface_upgrade（legacy 死路径，1:1 惰性移植）

- `getRangeAmplification`（`existingRange << 1`）在 legacy **零调用方**；
  `UpgradableProperty.RemotelyAccessible` 无任何机器声明——该升级在 IC2 2.10.39 中
  无法装入任何机器，纯存在性物品。
- port 如实注册：Kind 枚举 + `ModUpgrades` 自动注册（物品/创造性栏/模型/贴图/items 定义）+
  tooltip（`ic2.tooltip.upgrade.remote_interface`，legacy 文案含 stack size 参数）；
  `suitable()` 恒 false。与 crowbar 注记同类：行为面为空属 legacy 事实，非 port 缺口。

## 资源管道

- `tools/migration/resources/upgrades.py` 清单追加 `redstone_inverter`、`remote_interface`
  （均无 overrides，走 `base.item()` 单模型分支，复制 legacy 贴图）；重跑幂等，既有 7 升级资源无扰动。
- 生成物：items 定义 ×2、item 模型 ×2、贴图 ×2。
- 配方解锁（`recipes.py` 重跑）：`shaped/redstone_inverter_upgrade.json`（锡板×4+拉杆，1 件）与
  `_9`（致密锡板+拉杆，9 件）；converted 774→776、pending 22→20。remote_interface 无 legacy 配方。

## GameTest（UpgradeInverterTests 3 例，454 = 451 + 3）

- `inverter_keeps_induction_warm`：感应炉保温方向三段实证——无 inverter 无信号不保温不耗电；
  插入后无信号即保温（heat+1、1 EU/tick，15−0>0）；满强度红石块即停保温冷却（15−15=0）；
  撤信号恢复保温。
- `inverter_suitability_matches_legacy`：suitable 矩阵（6 台 true；electric_furnace/magnetizer false；
  remote_interface 全 false）+ matter generator（暂停方向）在满信号下经 inverter 读为无信号。
- `inverter_menu_insertion_gates`：走真实插入门控（MachineMenu quickMoveStack）——inverter 落入
  升级槽 5；remote_interface 留在玩家背包、不入机器任何槽。

## 勘误与坑位记档

- GameTest 直调 `invertedSignal` 必须传 `helper.absolutePos(POSITION)`——serverTick 内部用绝对
  `worldPosition`，测试直调传相对坐标会静默读到世界原点（首跑失败根因）。
- 首版 helper 采用 boolean 抽象（`!external`），与 legacy 模拟量 `15 - external > 0` 在非满强度
  信号下分歧；重读 legacy `Redstone.update` 后改为模拟量链 1:1。

## 待人工验收

- 两种升级贴图观感；redstone inverter 悬停 tooltip 中英文案；满强度红石反转的实机体验
  （非满强度信号反转为"有信号"属 legacy 模拟量语义，实机验证需比较器信号源）。
