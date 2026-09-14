# 2026-09-14 创造物品栏崩溃修复

- 触发条件：在创造模式世界 HUD 中按 `E`。
- 修复前结果：`CreativeModeInventoryScreen` 构建标签时抛出 `IllegalArgumentException: Itemstack 1 ic2:blank_tfbp already exists in the tab's list`，物品栏没有显示，客户端随即崩溃退出。
- 根因：`ModTools.creativeContents` 将同一组七种 TFBP 向 `Tools & Utilities` 连续注册了两次。
- 修复：保留每种 TFBP 的一次注册，删除第二组重复的 `event.accept(...)`。

## 实机验收

1. 对修改后的工作树执行 `clean build`，完整重启 NeoForge 开发客户端。
2. 进入 `IC2 Manual Core 20260914`，确认创造模式 HUD 后真实按 `E`。
3. Creative Inventory 正常打开并持续显示超过 3 秒，客户端保持运行。
4. Search Items 可以搜索 `TFBP`，结果物品显示为 `Tools & Utilities` 分类。
5. 关闭物品栏，随后通过菜单正常保存并退出客户端。

证据：[修复后的 Creative Inventory](../images/creative-inventory-fixed.png)。新客户端日志中 `blank_tfbp already exists`、`Item Group crashed`、`CreativeModeInventoryScreen` 异常和 FATAL 均为 0。

JEI 仍报告 Combat 标签存在一项重复物品警告；它没有中断标签构建，也没有影响本轮物品栏打开与搜索，作为非阻断项保留。

## IC2 专属标签恢复

崩溃修复后，IC2 物品仍只分散在原版标签中：创造物品栏没有 IC2／IndustrialCraft 标签，Search Items 的 `@ic2` 查询结果也为空。旧版注册了九个 IC2 标签，迁移端保留了对应翻译，但没有迁移 `CREATIVE_MODE_TAB` 注册。

新增 `ModCreativeTabs` 注册统一的 `ic2:general` 标签：

- 标题复用 `itemGroup.ic2.general`，图标使用 IC2 Generator。
- 内容从物品注册表筛选 `ic2` 命名空间并按注册 ID 排序；新增物品自动出现，无需维护第二份手工清单。
- 标签自带搜索框，同时保留现有原版标签分类。
- 当前产物检查确认有 524 个 IC2 物品定义。

最终源码完整重启后的实机结果：创造物品栏第 2 页出现 `IC2 General`；标签可以打开并显示多行 IC2 机器、装备与材料；标签内搜索 `MFE` 可以取出 MFE 放入玩家热栏。证据：[搜索并取出 MFE](../images/ic2-general-mfe-taken.png)、[最终源码复核](../images/ic2-general-final.png)。
