# 分拣机(P08)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntitySortingMachine.java`
- 15,000 EU、tier 2;11 格缓冲、6 面×7 过滤格、3 升级槽、默认路由面(初始 DOWN)。
- 路由:非默认面仅在过滤格匹配且缓冲数量≥过滤数量时整份输出(每件 20 EU);
  默认面接收无任何过滤声明的物品,一次一件(20 EU)。

## 新实现

- `machine/SortingMachineBlockEntity.java`:过滤格为独立 42 格 `MachineInventory`
  (每格 1 件模板),路由经原生物品能力投递到相邻容器,预检/提交/回滚与物品扣减
  在同一事务;默认面回退语义保持。
- 面向编辑的过滤端口 `filterAutomation()` 不对自动化开放;菜单 6×7 过滤网格 +
  11 格缓冲;方向切换经 `menuAction`(0..5 对应六面)。
- 资源由 `machines.py` 生成。

## 测试证据

- GameTest `SortingMachineTests`:
  - `sorting_machine_filter`:EAST 面过滤木棍后整份路由(5 件 / 100 EU);
  - `sorting_machine_default`:无过滤物品经默认面(UP)逐件输出(20 EU/件)。
- IC2 与 GT 两种能量模式 164 项 GameTest 全部通过(2026-09-09)。

## 未验收范围

- 升级(过载/变压器)与红石敏感行为;真实 GUI 方向按钮交互(M16)。
