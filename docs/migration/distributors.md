# 分配器家族切片（fluid_distributor / weighted_fluid_distributor / weighted_item_distributor）

## 范围

- `ic2:fluid_distributor`（流体分配机）：registry item/block/block_entity/menu 4 条 + 配方 1 条
- `ic2:weighted_fluid_distributor`（高级流体分配机）：registry 4 条 + 配方 1 条
- `ic2:weighted_item_distributor`（高级物品分配机）：registry 4 条 + 配方 1 条

合计 registry-catalog 12 条 pending→implemented、recipe-catalog 3 条 pending→converted。

## 实现

| 类 | 说明 |
| --- | --- |
| `neoforge/src/main/java/ic2/neoforge/machine/FluidDistributorBlockEntity.java` | plain/加权流体共用基类：1000mB 内罐、桶填充输出、模式化端口与推送 |
| `neoforge/src/main/java/ic2/neoforge/machine/WeightedFluidDistributorBlockEntity.java` | ≤5 方向有序优先级，按序推送直至罐空 |
| `neoforge/src/main/java/ic2/neoforge/machine/WeightedItemDistributorBlockEntity.java` | 9 格缓冲 + 同构优先级列表，逐方向 `moveStacking` 搬空缓冲 |
| `neoforge/src/main/java/ic2/neoforge/client/FluidDistributorScreen.java` | 模式 toggle 按钮 + 罐量读数 |
| `neoforge/src/main/java/ic2/neoforge/client/WeightedDistributorScreen.java` | 6 向 toggle 按钮矩阵（weighted 两件共用） |

注册走 `ModMachines` 自动链（MachineKind→block/item/entity/menu），`MachineMenu` 增加三件槽位布局，`MachineSounds` null 声明组。资产：10 张 legacy 贴图、7 个模型、3 个 blockstate（12 变体×3）、3 个 loot（wrench alternatives→machine 模式）。

## legacy 语义对照

- **plain 流体分配机**：`active` 是持久模式而非工作指示（legacy `TileEntityFluidDistributor.onNetworkEvent` 翻转）。
  - active：推正面单出；除正面外全部接受流入。
  - idle：仅正面接受流入；向其余所有面**均衡分配**（先对每个非正面液体邻居 simulate fill 收集 accepted 总量，循环 `share=min(acceptedTotal, tankAmount)/剩余邻居数` 分配，`moved<share` 的邻居剔除；share==0 时逐个给 `min(acceptedTotal, tank)` 一轮后结束）。
- **加权流体/物品分配机**（legacy `TileEntityWeightedFluidDistributor`/`TileEntityWeightedItemDistributor`）：
  - priority 为有序 `List<Direction>`（≤5，不含 facing），持久化为 int 数组（`get3DDataValue`：down=0…east=5），load 时 `remove(facing())` 保持 legacy `updateConnectivity` 不变式；
  - 恒正面进；流体按序推送直到罐空；物品逐方向把缓冲全部 `moveStacking`，某方向清空缓冲即 break。

## 验证

- `DistributorTests` 六用例，IC2/GT 双模式隔离+全量 494+494 全绿：
  1. `distributor_fluid_push`：menuAction(0) 切 active 后整罐 1000mB 推正面 + 空桶填充成水桶进输出槽
  2. `distributor_fluid_balance`：idle 下 800mB 均衡东西两罐各 400 且罐排空
  3. `distributor_fluid_ports`：idle 仅正面接受、active 翻转、weighted 恒正面
  4. `distributor_weighted_fluid`：优先级顺序（EAST→WEST）、拒绝 facing、移除后次序顶上
  5. `distributor_weighted_item`：首优先搬空 64 缓冲、移除后次优先接手、被移除方不再取物
  6. `distributor_priority_reload`：saveWithFullMetadata/loadStatic 往返保序、toggle 关闭缺口
- `MenuAuditTests` 槽位审计自动覆盖（三件均非 upgradable，kind.slots() 2/2/9）。
- `python3 tools/migration/progress.py --check` 通过。

## 现代化取舍（记档）

1. **加权 GUI**：legacy 为 5×6 权重调节按钮矩阵；移植为 6 向 toggle 按钮（按下加入/移出优先级列表，屏幕按序显示）。语义等价（≤5 个非正面方向的有序集合），**视觉/UX 属人工验收项，留用户签署**。
2. **模式切换**：legacy plain 分配器经红石网络事件 `onNetworkEvent` 翻转 active；移植为菜单按钮（`menuAction(0)`）。红石翻转不再提供。
3. **均衡算法**：逐行移植 legacy 双阶段（simulate 收集+share 分配），行为经用例 2 验证与 legacy 一致。
