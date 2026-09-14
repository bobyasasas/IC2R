# 收尾验收矩阵（final acceptance）

主 Agent 唯一维护；基线审查：`/home/codex/minecraft/迁移审查-2026-09-14.md`（对象 ae3e0508）。
本矩阵链接现有清单（registry-catalog / recipe-catalog / plan.json / work-packages.json / STATUS.md），不取代它们。
验证维度：**实现**（代码/资源落地）、**自动化**（build/GameTest/verify/转换自测）、**实机**（真实客户端/专服观察）、**兼容**（旧档/集成/多人）。
状态词：done / partial / todo / blocked(原因)。每条证据注明提交 SHA 与运行环境。

## 已知问题来源（审查报告 6 项 → 分配）

| # | 审查问题 | 分配 | 状态 |
|---|---|---|---|
| 1 | 四罐缺 assets/ic2/items 定义；检查器只遍历已有 JSON | R01 | done（7b04af50；反向完整性检查+13 缺定义补齐+死定义清理） |
| 2 | LaserBulletRenderer @OnlyIn 复发 | R01 | done（7b04af50；全仓 grep=0，CI 警告消失） |
| 3 | 配方台账 7 条 pending 漂移；converted-recipes 缺 5 条；9 条 empty ingredients 警告 | R02 | done（ec4354ea，CI 34818810933 success；台账 794/2 显式处置；警告清零） |
| 4 | 旧档转换缺 InvSlots→inventory 结构迁移与 canner 映射；无端到端 | R07 | partial（实现+自测端到端 done，本切片；真实旧世界样本端到端 todo） |
| 5 | 实机/集成/多人证据边界（留人工项、AE2 测试替身、证据非最终 HEAD） | R05/R06/R08 | partial（单客户端核心可玩链已通过；集成与多人仍待测） |
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
| 四罐 items 定义 | done（实现+自动化） | 生成器 machines 列表接管（items/blockstate/loot/mineable/lang 全链）；提交 7b04af50，CI 34817123339 success |
| 生成器同步 | done（实现+自动化） | tools/migration/resources/machines.py（regen 幂等）；9 个迟到机器定义+8 机器 pickaxe 标签同源修复 |
| verify_artifact 反向完整性（注册物品→定义存在；定义→注册物品） | done（自动化） | RegistrationTests 运行时 dump（neoforge/run/ic2_item_registry.json，含 mod_version 防陈旧）+ verify_artifact 双向差集断言；本 HEAD 复跑通过（524 定义=524 注册） |
| 负向测试（删定义必须失败） | done（自动化） | /tmp fixture 删定义→检查器失败（不留存）；漏 13 个定义即由该体系发现 |
| @OnlyIn 清理+扫描 | done（实现+自动化） | 全仓 grep 仅 LaserBulletRenderer 一处已删（dist=CLIENT 构造器唯一引用）；CI 日志 OnlyIn 警告消失 |
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

- 状态：**审计完成；缺口修复随切片推进**（P01–P16、P21 逐族对照最新源码，2026-09-14）
- 结论概要：全 17 包敏感族基本覆盖+大量超集（超集裁决已记录归档）；硬缺口收敛为 3 项+1 项观感差异：
  1. 传送机 targetX/Y/Z int→target BlockPos compound 字段迁移缺失 → **已修**（R07 切片 convert_legacy_fields）
  2. 矿机 lastMode legacy int 序数→新端枚举名字符串迁移缺失（不迁移则 getStringOr 回落 NONE 静默重置） → **已修**（同上）
  3. 传送机 Boss 权重差异（legacy WitherBoss=5000/EnderDragon=10000；新代码只判 Monster→凋灵 500/龙 0） → **已修**（TeleporterBlockEntity.weightOf，本切片）
  4. Matter lastEnergy 不持久化（纯观感，非行为缺陷） → 记录，不修
- 记录格式：族｜审计结论｜缺口｜处置（明细见审计报告，修复处置见上）

## R04 状态持久化与服务端边界

- 状态：todo（各族实现后推进；已有升级/罐/扫描持久化 GameTest 覆盖部分）

## R05 实机基础验收（实用版）

- 状态：partial（2026-09-14 单客户端核心可玩链通过：稳定登录、创造物品栏、Chest/MFE/Iron Furnace 菜单、MFE 供电加工、产物取出、保存重进、IC2 方块真实放置与破坏；全功能视觉与集成项继续按需补测）
- 证据：[单客户端核心可玩性实测](live-evidence/2026-09-14-core-playability.md)、[创造物品栏崩溃修复](live-evidence/2026-09-14-creative-inventory.md)；证据目录 `docs/migration/live-evidence/`

## R06 JEI/Jade/AE2 真实集成

- 状态：todo（固定 26.1.2 兼容版本；AE2 用真实反射 API；无依赖生产 JAR 启动验证）

## R07 旧存档兼容

| 任务 | 维度状态 | 证据 |
|---|---|---|
| 序列化映射矩阵（InvSlots→inventory 等） | done（实现+自动化） | save_convert.py：convert_inventory 按 inventory_map 布局展开 InvSlots.<slot>.Contents（Index 字节偏移）→inventory.stacks（OPTIONAL_CODEC 形状）；tag.charge→ic2:charge、tag.Damage→minecraft:damage 组件映射；未映射 tag 键计数报告；原 InvSlots 冻结为 ic2_legacy_InvSlots 取证副本（不二次改名、不进扫描计数） |
| fluid_bottler/solid_canner→canner 的 block/item/BE 迁移；crowbar/itnt 处置 | done（实现+自动化） | save_id_map.json：BE+block_entity 映射 fluid_bottler/solid_canner→canner；item ic2:crowbar=""→air/0 计入 dropped；inventory_map 第一批 15 台机器布局（6 标准电机+离心机+洗矿机+铁炉+四罐+两罐装机，槽名逐一对 legacy 源码核实；矿机/block_cutter/发酵机等显式延期并注明） |
| 真实 1.20.1 样例世界→转换→26.1.2 打开（端到端） | partial | 合成 fixture 端到端 done（self-test：扫描→转换→字节级回读断言→复扫描，覆盖库存迁移/溢出/未映射槽/改名路径/传送机+矿机字段迁移/丢弃/取证冻结/字节级恒等往返）；**真实旧存档样本端到端 todo**（需样例世界，归实机场） |
| 传送机/矿机 NBT 字段迁移 | done（实现+自动化） | convert_legacy_fields：targetX/Y/Z→target{X,Y,Z}（三键齐全，缺失计数上报；幂等）；lastMode 序数→MINER_MODES 枚举名（越界回落 NONE 并计数）；空映射下仍执行（schema 迁移与 id 映射解耦） |
| 转换只写新副本、保留未知数据 | done（实现+自动化） | 拒绝原地转换；取证副本保留未迁移数据；字节级恒等往返测试（纯原版 region 逐字节相等） |

## R08 联机基础检查

- 状态：todo（候选 JAR 专服+1 客户端：开机器、操作、断线重连、保存重启一次；与 R05 共用测试场）

## R09 CI 与真实产物

- 状态：partial（CI 全绿：ae3e0508/7b04af50/ec4354ea 三连 success；verify_artifact 反向完整性已由 R01 补齐并在本 HEAD 复跑通过；最终 HEAD 全量验证按指令 R09 命令序列，归 R09 收口）

## R10 台账与文档同步

- 状态：进行中（随每切片同步；最终按 M10–M17 真实验收收口，日期更新）

## R11 候选发布准备

- 状态：todo（独立 NeoForge 候选工作流/固定版本/SHA-256/说明/依赖/安装指南/旧档支持矩阵/已知限制；不触旧 Forge release.yml 与旧标签）

## 切片日志

| 日期 | 切片 | 提交 | 结论 |
|---|---|---|---|
| 2026-09-14 | R00 矩阵建立+审查 6 项核实分配 | （进行中） | 基线 ae3e0508 与远程一致 |
| 2026-09-14 | R01 资源反向完整性+13 定义+OnlyIn | 7b04af50（CI 34817123339 success） | 检查器抓出 9 个额外缺定义+1 死定义+8 漏标签；OnlyIn 警告清零 |
| 2026-09-14 | R02 配方链 794/2+钚标签+警告清零 | ec4354ea（CI 34818810933 success） | legacy 空#c:ingots/plutonium 判定为 legacy bug，补值修复；GameTest 576×2、双模式警告=0 |
| 2026-09-14 | R07 存档转换：库存结构+字段迁移+canner 映射+传送机权重（R03 缺口 1/2/3） | （本切片） | self-test 端到端 PASS；GameTest 576×2；verify_artifact 通过；真实旧世界样本端到端遗留 |
| 2026-09-14 | R05 单客户端核心可玩性实测+配方同步登录修复 | （与本记录同提交） | 实际登录、菜单、MFE→铁炉→铁锭、保存重进、真实放置/破坏全部通过；非阻断资源与创造标签问题留后续 |
| 2026-09-14 | R05 创造物品栏崩溃修复 | （与本记录同提交） | 删除七种 TFBP 的第二组重复注册；真实按 E、搜索 TFBP、关闭界面及正常退出通过 |
| 2026-09-14 | R05 IC2 专属创造标签恢复 | （与本记录同提交） | 新增自动收录全部 IC2 注册物品的 IC2 General；实机打开、搜索 MFE、取入热栏通过 |
