# 2026-09-14 单客户端核心可玩性实测

- 分支：`neoforge26.1.2`
- 测试基线：`b93593bd` 加本记录同批的配方同步修复
- 启动方式：NeoForge 开发客户端，单人世界 `IC2 Manual Core 20260914`
- 结论：**核心可玩链通过**。启动、交互、能源加工、产物取出、存档重载和方块放置／破坏均可用。

## 实测结果

| 场景 | 实际操作 | 结果 | 证据 |
|---|---|---|---|
| 登录与配方同步 | 启动客户端并进入现有世界 | 首次发现 `JetpackAttachmentRecipe` 的 unit codec 使用两个不同实例，导致 `neoforge:recipe_content` 编码失败；统一为单例后完整重启，玩家稳定进入世界 | `latest.log` 中修复后有 `Dev joined the game`，无后续 `Failed to encode`／异常断线 |
| 原版交互基线 | 准星瞄准箱子，真实右键开关 | Chest 菜单正常打开和关闭 | 同次实测观察 |
| IC2 菜单基线 | 空手准星瞄准 MFE 和 Iron Furnace，真实右键 | 两个菜单均正常打开 | 同次实测观察 |
| 能源加工闭环 | MFE 邻接 Iron Furnace；通过 GUI 放入 1 个 Raw Iron，等待机器 tick，再真实点击输出槽并放入玩家热栏 | 原料消耗，输出 1 个 Iron Ingot；产物可以正常取出 | [产物取出](../images/core-loop-iron-taken.png) |
| 正常保存 | 从世界暂停菜单真实点击 `Save and Quit to Title` | 集成服务器保存玩家与三个维度后正常停止并返回标题页 | [保存后标题页](../images/core-loop-saved-title.png)；`latest.log` 05:24:33–05:24:34 保存记录 |
| 重进持久化 | 从标题页重新进入同一个世界 | 热栏第 1 格的 1 个 Iron Ingot、测试平台、MFE 和 Iron Furnace 均保留 | [重进世界](../images/core-loop-rejoined.png) |
| IC2 方块放置 | 命令只发放 Iron Furnace 并准备目标石块；玩家准星命中侧面后真实右键 | `(3,101,1)` 出现 `ic2:iron_furnace`，随后由 `/data get block` 只读确认 | [真实放置](../images/real-place-iron-furnace.png) |
| IC2 方块破坏 | 空手准星命中刚放置的 Iron Furnace，创造模式真实左键 | 方块消失，`execute if block ... air` 返回 `REAL_BREAK_PASS` | [真实破坏](../images/real-break-iron-furnace.png) |

## 本轮修复

`ModCraftingRecipes` 原先分别为 `MapCodec.unit(...)` 和 `StreamCodec.unit(...)` 创建 `JetpackAttachmentRecipe`。unit stream codec 只接受与解码端相同的对象实例，因此登录时配方同步抛出 `IllegalStateException` 并断开玩家。现在两个 codec 共享 `JetpackAttachmentRecipe.INSTANCE`；完整重启后的真实登录和上述核心链直接验证了修复。

## 已知非阻断项

- `ic2:reinforced_glass` 报缺少方块模型变体。
- 创造标签发现重复 `ic2:blank_tfbp`，并使 JEI 的 Tools & Utilities 分组缺失。
- narrator/flite 和 X11 cursor 警告属于当前无头测试环境。

这些问题没有阻止本轮核心玩法。后续按影响单独处理，不阻塞当前“可启动、可操作、可加工、可保存”的可玩性结论。本轮是单客户端创造测试世界，不代表全部机器、生存进度或多人场景已完成实测。
