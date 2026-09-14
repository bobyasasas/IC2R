# 收尾验收矩阵（final acceptance）

主 Agent 唯一维护；基线审查：`/home/codex/minecraft/迁移审查-2026-09-14.md`（对象 ae3e0508）。
本矩阵链接现有清单（registry-catalog / recipe-catalog / plan.json / work-packages.json / STATUS.md），不取代它们。
验证维度：**实现**（代码/资源落地）、**自动化**（build/GameTest/verify/转换自测）、**实机**（真实客户端/专服观察）、**兼容**（旧档/集成/多人）。
状态词：done / partial / todo / blocked(原因)。每条证据注明提交 SHA 与运行环境。

## 已知问题来源（审查报告 6 项 → 分配）

| # | 审查问题 | 分配 | 状态 |
|---|---|---|---|
| 1 | 四罐缺 assets/ic2/items 定义；检查器只遍历已有 JSON | R01 | todo→进行中 |
| 2 | LaserBulletRenderer @OnlyIn 复发 | R01 | todo |
| 3 | 配方台账 7 条 pending 漂移；converted-recipes 缺 5 条；9 条 empty ingredients 警告 | R02 | todo |
| 4 | 旧档转换缺 InvSlots→inventory 结构迁移与 canner 映射；无端到端 | R07 | todo |
| 5 | 实机/集成/多人证据边界（留人工项、AE2 测试替身、证据非最终 HEAD） | R05/R06/R08 | todo |
| 6 | 状态文档双向过期（矿机/传送机/M14/UU 扫描持久化等） | R00/R10 | 进行中 |

## R00 基线盘点

- 状态：**进行中**（本文件建立即起点；持续填充）
- 基线核对：本地 HEAD = origin/neoforge26.1.2 = ae3e0508（2026-09-14 核对，工作区干净，CI 34812748489 success）
- 盘点方法：审查报告 6 项逐条对照当前源码核实（已核实 #1/#2/#3/#4/#6 的关键事实）；R03 逐族审计进行中
- 双向过期修正记录：
  - UU 扫描持久化：代码已存 progress/currentStack/pattern/state（UuScannerBlockEntity load/save）——历史文档「未保存」不再作为现存缺陷；R10 同步文档
  - 矿机/传送机/M14 代码已存在——work-packages 描述滞后；R10 按真实验收范围更新

## R01 客户端资源与 OnlyIn

| 任务 | 维度状态 | 证据 |
|---|---|---|
| 四罐 items 定义 | todo | 生成器 machines 列表接管（items/blockstate/loot/mineable/lang 全链） |
| 生成器同步 | todo | tools/migration/resources/machines.py |
| verify_artifact 反向完整性（注册物品→定义存在；定义→注册物品） | todo | RegistrationTests 运行时 dump + verify_artifact 反查 |
| 负向测试（删定义必须失败） | todo | /tmp 临时 fixture，不留存 |
| @OnlyIn 清理+扫描 | todo | 全仓 grep 仅 LaserBulletRenderer 一处；verify 已有 common 无客户端引用检查 |
| 最新构建客户端启动无警告+四罐外观实测 | todo | 与 R05 实机场合并执行 |

## R02 配方/标签/生成器一致性

| 任务 | 维度状态 | 证据 |
|---|---|---|
| 5 条罐配方：生成器/catalog/manifest 同步+真实 matches/assemble | done | recipes.py 重新转换（registered=定义集）；recipe-catalog converted；manifest 794 条；CraftingTests tankFamilyCrafting 逐槽+matches+assemble（GameTest 576×2） |
| fluid_bottler/solid_canner 旧配方显式处置 | done | recipe-catalog 2 条 status=pending + disposition=replaced + replaced_by=ic2:canner（不生成假配方，不留未迁移理由） |
| 796 条旧配方全量核对 | done（本轮口径） | 794 converted + 2 replaced；无未解释 pending |
| 材料标签与生存获取链 | partial→本轮修复关键断链 | `#c:ingots/plutonium` legacy 起即为空标签→MOX/RTG 弹丸无法合成（legacy 行为 bug）；补 ic2:plutonium 值（recipes.py tag_value_overrides 防 regen 回退）；全配方引用标签扫描器确认仅此 1 个空标签（其余由 NeoForge 默认标签提供）；回归测试 plutonium_tag_crafting |
| 9 条 empty ingredients placement 警告分析 | done | 4 条=空钚标签（上述修复）；3 条 gradual+2 条 matter_fabricator=机器交互配方（NOT_PLACEABLE 且非 special 触发 vanilla RecipeManager 警告）；两类型补 isSpecial=true（配方书本就无法放置，机器语义不变）；修复后 IC2/GT 两种模式日志警告数=0 |
| 生成器可复现 | partial | machines.py/recipes.py 主工作区两次运行幂等（变更集不变）；worktree 隔离复验归入 R09 |
| 配方书/JEI 实际可发现性 | todo | 依赖 R05/R06 实机 |

## R03 逐族实现审计

- 状态：**进行中**（P01–P16、P21 逐族对照最新源码；发现真实缺口→新增任务修复）
- 记录格式：族｜审计结论｜缺口｜处置

## R04 状态持久化与服务端边界

- 状态：todo（各族实现后推进；已有升级/罐/扫描持久化 GameTest 覆盖部分）

## R05 实机基础验收（实用版）

- 状态：todo（先四罐+激光渲染器，再补待测试.md 缺实机证据项；按指令第 R05 节表格执行）
- 证据目录：docs/migration/live-evidence/（截图+日志；每行一测：设备/模式｜提交｜操作｜预期｜实测｜结论｜证据路径）

## R06 JEI/Jade/AE2 真实集成

- 状态：todo（固定 26.1.2 兼容版本；AE2 用真实反射 API；无依赖生产 JAR 启动验证）

## R07 旧存档兼容

| 任务 | 维度状态 | 证据 |
|---|---|---|
| 序列化映射矩阵（InvSlots→inventory 等） | todo | save-conversion.md §5.3 已承认未完成 |
| fluid_bottler/solid_canner→canner 的 block/item/BE 迁移；crowbar/itnt 处置 | todo | save_id_map.json 现仅 itnt BE 删除 |
| 真实 1.20.1 样例世界→转换→26.1.2 打开（端到端） | todo | |
| 转换只写新副本、保留未知数据 | partial | save_convert.py 现有实现按此设计，待端到端复核 |

## R08 联机基础检查

- 状态：todo（候选 JAR 专服+1 客户端：开机器、操作、断线重连、保存重启一次；与 R05 共用测试场）

## R09 CI 与真实产物

- 状态：partial（CI 全绿但检查器有漏项——R01 补反向完整性；最终 HEAD 全量验证按指令 R09 命令序列）

## R10 台账与文档同步

- 状态：进行中（随每切片同步；最终按 M10–M17 真实验收收口，日期更新）

## R11 候选发布准备

- 状态：todo（独立 NeoForge 候选工作流/固定版本/SHA-256/说明/依赖/安装指南/旧档支持矩阵/已知限制；不触旧 Forge release.yml 与旧标签）

## 切片日志

| 日期 | 切片 | 提交 | 结论 |
|---|---|---|---|
| 2026-09-14 | R00 矩阵建立+审查 6 项核实分配 | （进行中） | 基线 ae3e0508 与远程一致 |
