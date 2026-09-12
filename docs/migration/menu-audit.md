# Menu 滞留清收审计轮（第 20 轮）

## 目标

registry-catalog 中 `registry=menu` 的 22 条 pending 是历史滚动迁移的滞留项：MenuType
大多已随 ModMachines 自动注册，但每条的真实状态（专属布局是否等价、缺哪些 GUI 特性）
从未逐条核对过。本轮把这 22 条逐条与 legacy `Ic2ScreenHandlers` / `Container*` 语义
比对，用一台全 kind 菜单审计测试固化结论，并当轮翻转 catalog。

## 审计方法

1. 逐条语义比对：legacy `Ic2ScreenHandlers.registerManagedBe/registerManagedItem` +
   `ContainerFullInv` / `ContainerElectricMachine` / `ContainerChunkLoader` /
   `ContainerMagnetizer` / `ContainerSortingMachine` / `ContainerPatternStorage` 等
   专属 Container 类 vs port `MachineMenu` 的按 kind 分支布局。
2. 全 kind 审计测试
   `MenuAuditTests.everyMachineMenuBindsInventorySlotsExactlyOnce`：遍历
   `MachineKind.values()` 全部 89 个 kind，用服务端路径构造 `MachineMenu`，断言
   ①机器槽数等于该 kind 的期望布局（`auditedMachineSlots` 期望表），②每个槽的
   绑定（handler 身份 + handler 槽位）全局唯一，不允许两槽指到同一底层槽位。
3. BE 路径抽查 `MenuAuditTests.chunkLoaderMenuMatchesSingleDischargeLayout`：
   放置真实 chunk loader BE 构造菜单，断言 1 discharge + 4 upgrade 布局。

### 坑位：NeoForge 槽身份判定

`StackCopySlot`/`ResourceHandlerSlot` 的所有槽共享一个静态 `emptyInventory`
（`SimpleContainer(0)`），所以 `slot.container` 没有区分度，`getContainerSlot()`
虽然返回 handler 槽位索引但同一 index 会跨 handler 撞 key。槽身份必须反射读
`ResourceHandlerSlot` 私有 `handler` 字段，用
`(System.identityHashCode(handler) << 32) | containerSlot` 做 64 位唯一键。
32 位组合键（hash*1000+index 之类）会碰撞，不要用。

## 审计发现并修复的 7 处真实菜单缺陷

全 kind 审计逐轮迭代抓出以下缺陷，全部修复：

1. **chunk_loader 槽位重叠越界**：discharge 槽绑定到了升级槽段（legacy
   ContainerChunkLoader 是 1 discharge(8,143) + 4 upgrade）。补专属分支：
   `addBatterySlot(inventory, 0, 8, 143)`。
2. **magnetizer 定义错误**：`MachineKind.MAGNETIZER` slots=4 但菜单走通用 3 槽。
   legacy ContainerMagnetizer 是 1 discharge(8,44) + 4 upgrade(152,8+18i) + 1 护甲槽。
   改 `MAGNETIZER("magnetizer", 100, 1, 0, 0)` + `upgradeSlots()` 特判 +4 +
   菜单专属分支（护甲槽见缺口清单）。
3. **sorting_machine 客户端 NPE（线上 bug）**：菜单分支强转
   `SortingMachineBlockEntity` 并直接调 `filters()`，客户端构造菜单时
   `machine=null`，右键分拣机直接断连。修复：`instanceof` 判断，客户端绑占位
   `MachineInventory(42)` 过滤网格。另记档：legacy 3 个升级槽 port BE 无升级
   消费逻辑，补 GUI 槽会造"放了无效果"的误导槽，审计期望表按 53 记档缺口。
4. **4 个反应堆代理 kind 落错分支**：REACTOR_CHAMBER / REACTOR_FLUID_PORT /
   REACTOR_REDSTONE_PORT / REACTOR_ACCESS_HATCH（slots=0，菜单代理到
   NuclearReactorBlockEntity）原占位 if 在分支链外，没拦住通用 else 的 3 槽
   分支。移入 else 链占位分支。
5. **RCI 槽位虚标**：RCI_RSH/RCI_LZH slots=13 把 4 个升级段重复计入（菜单
   实际 9+4=13 槽而 inventory 17 槽、槽 9-16 闲置）。改 slots=9；BE 只用
   slot<9 语义不变。
6. **pattern_storage 布局错误**：1 磁盘槽(18,20) 走了通用 3 槽分支。补专属
   分支（legacy ContainerPatternStorage）。
7. **teleporter / creative_generator**：legacy 无 GUI 无 inventory，却被通用
   分支给了 3 槽。补占位分支。

## catalog 翻转（22 条 → 3 implemented + 8 partial + 11 pending 补 note）

### → implemented（3）

- `dynamic_be`：通用受管 BE 菜单工厂由 MachineMenu 按 kind 分支全量覆盖，
  全 kind 审计通过。
- `solar_generator` / `rt_heat_generator`：legacy 即走通用受管 BE 布局
  （无专属 Container 类），port 通用分支等价，审计通过。

### → partial（8）

- `dynamic_item`：手持物品菜单族 5/7（meter/tool_box/containment_box/
  crop_analyzer/mining_filter 已实现；scanner 与 advanced_upgrade 三屏未
  移植，各自单列条目）。
- `energy_storage`：槽位绑定正确，legacy ContainerElectricBlock 4 个护甲
  充电槽未移植。
- `transformer`：流量/传输模式调节展示未移植。
- `chargepad`：物品槽与 redstoneMode 控件未移植。
- `energy_o_mat_closed` / `energy_o_mat_open`：升级槽与 closed/open 两态
  切换未移植。
- `trade_o_mat_closed` / `trade_o_mat_open`：∞（无限库存）按钮 UI 不可达、
  owner 校验缺失。

### 留 pending + 补 note（11）

chunk_loader（槽位缺陷本轮已修，9x9 画布未做）、scanner、fluid_bottler、
fluid_distributor、weighted_fluid_distributor、weighted_item_distributor、
industrial_workbench、batch_crafter、advanced_upgrade、
advanced_upgrade/edit_ore、advanced_upgrade/value_config。

## 记档的缺口清单（未修，超出审计轮范围）

- **护甲槽整族**：legacy ContainerElectricBlock 4 护甲槽、ContainerMagnetizer
  1 护甲槽，port 无护甲槽槽型。整族 GUI 特性，需单独切片。
- **sorting_machine 升级消费**：legacy 3 升级槽，port BE 无升级消费逻辑，
  补 GUI 槽会误导（审计期望表记 53 = 42 过滤 + 11 buffer）。
- **chunk_loader 9x9 区块选择画布**。
- **scanner 手持 GUI**、**advanced_upgrade 三屏**、**batch_crafter**、
  **industrial_workbench**、**trade_o_mat ∞ 按钮 + owner 校验**、
  **transformer 流量展示**、**chargepad 物品槽/redstoneMode**、
  **energy_o_mat 升级槽 + 两态**。

## 验证

- `MenuAuditTests` 双测试隔离：IC2、GT 双模式全绿。
- 全量回归：IC2 486/486；GT 首跑 485/486（唯一失败
  `sheet_resin_cushion`——既有相位型 flaky 本轮在 GT 模式复发），隔离复跑绿、
  GT 全量复跑 486/486。基线 484 + 本轮新增 2（menu_audit_slots、
  menu_audit_chunk_loader）。
- `core:test` 绿。
- registry-catalog menu pending 22→11，registry 总 pending 60→49。

## 新增测试 id

- `ic2_tests:menu_audit_slots`（全 kind 纯构造审计，无 tick，不 wrap）
- `ic2_tests:menu_audit_chunk_loader`（需 BE，`EntityTickingTests.wrap`）
