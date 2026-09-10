# M15／P18：旧存档转换实验（副本工具 + 可复现流程 + 不支持项清单）

状态：工具与流程已交付并验证（2026-09-10）。本文是 P18 验收的三要素落点：跨版本数据迁移工具（`tools/migration/save_convert.py`，纯 stdlib）、可复现流程（下文第 4 节）、不支持项清单（第 6 节）。

## 1. 取证依据

1. **ID 面极小**：`docs/migration/registry-catalog.json` 共 1199 条目，1198 条 decision=preserve（新旧同 ID），仅 1 条 replace（`ic2:itnt` 方块实体——新端由 `ItntBlock` 方块级钩子等价实现，无独立 BE，见 docs/migration/itnt.md）。因此"转换"的工作量不在改名，而在**副本改写管线本身**与 BE/物品 NBT 的边界处理。
2. **能量字段同名**：legacy `TileEntityElectricBlock` 写 `nbt.putDouble("energy",…)`，新端 `PoweredBlockEntity` 持久化同名 `"energy"`（double），BE ID 又是 preserve——存电池类方块旧 NBT 直读即可用。
3. **Anvil 格式实测**（以真实存档探针取证，非仅按规范）：压缩字节 **2=Zlib、1=Gzip**；文件头 8KB（1024 槽 locations + 1024 槽 timestamps，4 字节/槽），扇区 4KB；26.1.2 chunk 键 `sections[].block_states.palette[].Name/Properties`、`block_entities`、`entities`（chunk 内）、`Entities`、`block_ticks`、`fluid_ticks`、`PostProcessing`、`DataVersion`、`Status`；目录布局 `saves/<name>/dimensions/<命名空间>/<维度>/{region,entities,poi}/r.*.mca`。1.20.1 DataVersion=3465，26.1.2 DataVersion=4790。
4. **外部 chunk 存在**：真实存档里出现 location offset==0 且 timestamp!=0 的槽（payload 在兄弟文件 `c.<x>.<z>.mca`）——工具必须显式处理而不是当损坏。

## 2. 工具：tools/migration/save_convert.py

纯 Python 3.11+ stdlib（无第三方依赖），三个子命令：

| 子命令 | 作用 |
|---|---|
| `scan --src <副本> [--out r.json]` | 只读盘点：全部 ic2 命名空间 ID 按 palette/block_entity/item/entity 分类计数、方块状态属性键清单、DataVersion 分布、无法解析文件（含外部 chunk 槽）清单 |
| `convert --src <副本> --dst <新路径> --map save_id_map.json [--write]` | 副本改写：改 palette/BE/entity/item ID；`--write` 才落盘（默认 dry run）；拒绝 in-place（src==dst）与覆盖已存在 dst；其余文件（data/、serverconfig/、poi/、icon.png 等）原样复制带走 |
| `self-test` | 内置合成 1.20.1 fixture 断言两条代码路径（见第 5 节） |

关键保真设计：

- **NBT 层带标签标量**：`NbtByte/Short/Int/Long/Float/Double` 子类记住各自 TAG 类型——纯 Python int 无法区分 TAG_Int/TAG_Long（level.dat 的 RandomSeed/LastPlayed 是 long），不区分会写坏真实存档（真实 convert 首跑即暴露此问题后修复）。
- **Anvil 层**：`RegionData(chunks, timestamps)` 保留原 timestamp 扇区；未修改 chunk 逐字节保留原压缩 payload（`raw_payload`），被修改 chunk 以原压缩格式重新序列化；外部 chunk 槽（offset==0, timestamp!=0）标记为 None 并在扫描报告列 Unsupported，改写时位置条目原样保留。
- **安全**：永不接触原始存档；convert 只写 dst 且拒绝覆盖。

ID 映射 `tools/migration/save_id_map.json` 由 registry-catalog 生成（可复现）：未列出的 ID = 逐字保留（1198/1199 的 catalog 默认），映射表只携带偏差——当前唯一条目 `block_entity.ic2:itnt = ""`（drop：新端无此 BE，方块级实现不读它）。转换统计区分 renamed / dropped / preserved（preserved 是信息性计数，供与 scan 报告对照，不是错误）。

## 3. 为什么"先 scan 后 convert"是流程的一部分

scan 报告即转换前的人工审阅点：所有将被触碰的 ic2 ID 都在报告里，preserved 清单与 scan 报告不一致就停下来查，而不是改完才发现。

## 4. 可复现流程（对任意存档副本执行）

```bash
# 0. 永远先做副本
cp -r "saves/<存档>" "/tmp/p18/<存档>.copy"

# 1. 盘点（只读）
python3 tools/migration/save_convert.py scan --src "/tmp/p18/<存档>.copy" --out scan.json

# 2. 试转换（dry run 看统计，确认 renamed/dropped 符合预期、无意外 preserved）
python3 tools/migration/save_convert.py convert --src "/tmp/p18/<存档>.copy" \
    --dst /tmp/p18/converted --map tools/migration/save_id_map.json

# 3. 真正写出副本
python3 tools/migration/save_convert.py convert --src "/tmp/p18/<存档>.copy" \
    --dst /tmp/p18/converted --map tools/migration/save_id_map.json --write --out convert.json

# 4. 用 26.1.2 客户端/服务端打开 converted，人工确认机器/物品/能量
#    （原版侧 1.20.1→26.1.2 结构升级由游戏内 DataFixer 按 DataVersion 自动完成，
#     与本工具正交、幂等：先跑 DataFixer 再跑本工具结果一致）
```

## 5. 验证证据（2026-09-10）

### 5.1 self-test（合成 legacy 1.20.1 路径）

`python3 tools/migration/save_convert.py self-test` → **PASS**。合成 DataVersion=3465 fixture 断言：palette 改名、物品改名 ×2（机器 Items + 玩家 Inventory）、实体改名、`ic2:itnt` BE drop、in-place 拒绝、identity 映射转换后 region **字节级一致**、playerdata 转换。

### 5.2 真实 26.1.2 冒烟存档往返（字节级）

对 `neoforge/run/saves/IC2 Migration Smoke`（28MB，29 文件，6962 chunk，DataVersion 4790）：

- scan 报告：`docs/migration/evidence/p18-save-scan-2612-smoke.json`——palette 10 种 ic2 ID（generator/lv_transformer/rubber_log/insulated_copper_cable/rubber_sign 等，177182 个 palette 项）、BE 3 种（generator/lv_transformer/sign）、物品 4 种（含 wrench）、2 个外部 chunk 槽如实列入 unparsed。
- convert（真实映射）统计：`docs/migration/evidence/p18-save-convert-2612-smoke.json`——renamed=0、dropped=0（identity 后 posture 正确）、preserved 清单与 scan 一致、chunks_kept=6962/rewritten=0。
- 独立逐槽校验脚本复核：26 个 region/entities 文件、6962 个在文件 chunk 槽 **payload 字节级 100% 一致**；2 个外部槽双侧保留；timestamp 全部一致；6951 槽物理扇区布局被归一化（文件级字节不同、chunk 内容等价——writer 按槽序重排，这是已知且无害的行为）。

### 5.3 尚未执行的完整 E2E

环境内没有真实 legacy IC2 1.20.1 存档，"真实 1.20.1 存档 → convert → 26.1.2 打开"的完整端到端未跑过；现有证据是两段合成/真实证据（5.1 legacy 路径 + 5.2 真实字节级往返）。拿到真实旧存档后按第 4 节流程执行并在本文补记。

## 6. 不支持项清单（P18 验收要求，逐项）

| # | 不支持项 | 现状与后果 |
|---|---|---|
| 1 | 外部 chunk（`c.<x>.<z>.mca`，offset==0 且 timestamp!=0） | payload 不在本 region 文件内，工具不读不写其内容；位置条目与 timestamp 原样保留，scan 报告列 Unsupported。实体区文件实测存在此类槽 |
| 2 | BE 内部字段级迁移 | 只重命名 BE ID（含 drop），不改写 BE 内部 NBT 字段。字段名漂移（如 legacy TileEntityTransformer 的模式字段语义变化、单位换算）未逐字段清点；已取证的 `"energy"` 双端同名直读可用 |
| 3 | 物品旧 NBT → 26.1 数据组件 | 1.20.1 `tag` → 组件由原版 DataFixer 承担；IC2 自有 tag 键名/结构在组件化后的漂移未逐项清点，含复杂 NBT 的物品（如已配置的工具）转换后需人工抽查 |
| 4 | 方块状态属性漂移仅报告不修正 | scan 列出 `block_properties`；若同一方块 ID 的新端属性集有增删，工具不增删属性（catalog preserve 条目注册时已对照，但工具不做强制对齐） |
| 5 | legacy IC2 2.10.39 原版（非 IC2R fork 基线）的 ID 集 | catalog 以仓库内 `legacy/forge-1.20.1` 源码为准；IC2 2.10.39 上游与该基线的 ID 差异未清点。第三方旧存档里若出现 catalog 外的 ic2 ID，会落入 preserved 计数，需人工对照 scan 报告 |
| 6 | `data/`（世界级 `*.dat`）内的 ic2 自有数据 | 原样复制，不转换。legacy IC2 若在世界级 data 写过自有键（如爆炸抑制、产量统计）未清点 |
| 7 | `poi/` region | 原样复制不扫描（村民/POI 无 ic2 内容） |
| 8 | legacy 专用实体字段（Ic2Explosion 溯源、旧任务数据等） | 随实体 preserved 保留原样，新端读取语义按实体迁移各自的对照实现，未逐字段验证 |

## 7. 后续（非本切片）

- 真实 legacy 1.20.1 IC2 存档到位后跑完整 E2E（第 4 节流程）并补记结果。
- 若第 6 节 #2/#3 的字段漂移清点发现实际差异，扩展映射表 schema（BE 字段级改名/物品 tag→组件规则）再开切片。
