# 地形改造机（Terraformer）

Legacy `TileEntityTerra`（注册名 TERRAFORMER）移植：装入手持的 TFBP（terraforming
blueprint）程序卡后按程序改造周围地形，**每次成功地形编辑付 consume EU，失败尝试付
consume/10**——机器没有任何 GUI：右键先弹出已装蓝图，空手装蓝图时插入主手物品。

## 机器行为（对照 legacy updateEntityServer）

- **能量门控**：`stored >= consume` 才行动（consume=0 的空蓝图恒过门）。
- **随机游走**：上次成功点 `lastPos` 邻域 ±range/10；失败后 `lastPos=null`，从机器自身
  位置扩散 ±range×(failedAttempts+1)/5（failedAttempts 封顶 4，legacy 同）。目标 y 恒为
  机器所在高度，程序自行向下扫描地表。
- **付账**：`terraform(...)==true` → 提取 consume、`failedAttempts=0`、`lastPos=目标`；
  false → 提取 consume/10、`failedAttempts++`、`lastPos=null`。
- **活性**：有蓝图且付得起电即 active；停机经 30 tick 迟滞（legacy 同）。
- **legacy 怪癖忠实保留**：blank TFBP（consume=0、program=null）机器永久 active 且零耗
  ——`terraform` 恒 false 但每次"失败"提取 0/10=0。
- **勘误（如实记录）**：①legacy 向下扫描以 `getY() >= 0` 为界是 1.12 时代硬编码，
  1.20.1 的负 Y 世界里已是潜在 bug——port 修复为 `world.getMinY()`（"扫描到世界底"
  意图等价）；②扫描工具（getFirstSolidBlockFrom/getFirstBlockFrom/switchGround 向下行）
  跳过 `minecraft:barrier`/`minecraft:structure_void`：不可获得的技术方块、GameTest 骨架
  非地形，不应阻挡扫描或被当成地表；③legacy 每tick 动态 metered tier，port 静态 tier-4
  sink（所有 TFBP 消耗均适用）。

## TFBP 程序（neoforge item/tfbp 包）

| 卡 | consume | range | 程序要点 |
| --- | --- | --- | --- |
| blank | 0 | 0 | 无程序（见上怪癖） |
| chilling | 2000 | 50 | 水→冰、冰下水连冻；雪层渐进、四邻全雪升级雪块 |
| cultivation | 4000 | 40 | 沙/末地石→泥土、泥土→草方块并播种（草/花/树苗/作物/蘑菇/瓜，含橡胶树苗） |
| desertification | 2500 | 40 | 泥/草/耕地→沙；植物/树叶/雪/水清除；可燃木 1/15 起火；冰/雪→水 |
| flatification | 4000 | 40 | 低于基准填泥土、高于基准削除可移除方块（含原木/树苗 tag） |
| irrigation | 3000 | 60 | 沙→土；骨粉催熟；原木向上复制+邻位长叶；灭火；草蔓延；1/48000 触发降雨 |
| mushroom | 8000 | 25 | 菌丝播种→小蘑菇→巨型伞盖（MushroomBlock.growMushroom）+清理 2×2 小蘑菇 |

- **Irrigation 降雨**：26.1.2 移除了 `LevelData.setRaining`——改经
  `server.setWeatherParameters(0, RAIN_DURATION.sample(rng), true, false)`。
- **Mushroom 勘误（如实记录）**：①`growBlockWithDependancy` 只扫 NW 条带
  （`xm < x+1` 从 x−1 起、`zm ∈ {z−1, z}`）是 legacy off-by-one，逐行忠实转写（labeled
  block）；②legacy `BiomeUtil.setBiome` 运行时群系写入在 NeoForge 26.1.2 无对应 API，
  跳过（菌丝方块与蘑菇照常出现）；③`Blocks.TALL_GRASS`→`SHORT_GRASS`、`Blocks.GRASS`→
  `GRASS_BLOCK` 等 26.1.2 改名逐一适配。
- 程序内部 `Cultivation.plants()`/`Flatification.removable()` 懒初始化（注册期 tag 未
  加载），TerraformerBase `isVanilla`（namespace 判定）随程序沿用。

## 接线

- `MachineKind.TERRAFORMER("terraformer", 100000, 0, 0, 0)`（无槽位）+ `ModMachines.
  createEntity`；`MachineMenu` slotless 分支（与 TESLA_COIL 同组，顺带修掉 tesla 走
  泛型 3 槽分支的潜在 GUI 崩溃）；`MachineSounds` MACHINE_TERRAFORMER_LOOP。
- `TerraformerBlockEntity extends PoweredBlockEntity`：tfbp 单物品手装卸（save/load 经
  ItemStack.CODEC，空 stack 不落盘）、无自动化口（ResourcePort 全 false）。
- 资产：legacy 贴图 11 张（机器 4 + tfbp 7）、blockstate facing×active 12 变体
  （active→独立模型 cube_bottom_top）、7 个 item 模型/items JSON、创造页
  TOOLS_AND_UTILITIES。
- 数据：配方 8 条——blank（电路+高级电路+红stone）、6 程序（中心 blank_tfbp 的图案
  照搬 legacy）、机器本体 `GTG/DMD/GDG`（blank_tfbp+glowstone_dust×4+dirt×4+
  advanced_machine）；战利品表 alternatives：扳手保机器/否则掉 advanced_machine
  （legacy DefaultDrop.AdvMachine）。

## 验证（GameTest 5 项，双模式 390 项全绿）

两块专用泥土场地结构：`ic2_tests:terraformer_yard`（25×8×25，供无游走/短程测试）与
`ic2_tests:terraformer_yard_large`（85×8×85，供游走类测试——chilling 首散布 ±10 加
每 tick ±5 续走，6 tick 最坏漂移 35 < 半宽 42，游走被数学锁进场内，不依赖 GT 网格间距
的运气）。chilling 程序在任何实体顶面自持成功（水→冰→雪层→雪块→雪块上可继续承雪是
原版 canSurvive 特性），使逐 tick 能耗可断言；GameTest 骨架在箱子外围放置 barrier，
扫描工具按上文勘误跳过。

- `terraformer_chilling_ledger`：大场地装 chilling 灌满电跑 6 tick——**精确断言**
  消耗 = 6×2000 = 12000（每 tick 恰一次整价成功编辑）、active、场地出现雪/雪块。
  （此前 25×25 场地为 30 tick 界限断言 [2000, 60000]，因游走可能探出场外骨架列只烧
  1/10 价；大场地把游走锁进场内后升级为精确整价账目，是收紧而非放宽。）
- `terraformer_energy_gate`：大场地；1999 EU 五 tick 原地不动且 inactive；+1 EU 后
  一 tick 恰好 0（一次整价 2000 编辑）且 active。
- `terraformer_blank_blueprint`：空蓝图 10 tick 电量分文不动且 active（legacy 怪癖）。
- `terraformer_hand_insert_eject`：主手插入→再右键弹出成掉落物实体→非蓝图拒收。
- `terraformer_program_transforms`：六程序直接驱动——cultivation 泥→草、desertification
  草→沙、irrigation 沙→泥、chilling 水→冰、flatification 填坑到基准+削草、mushroom
  NW 条带 (18,0,18) 菌丝（含条带怪癖的确定性锚定）。

## 状态

M11/P08 辅助机器链新增地形改造机+TFBP 家族（7 卡）。勘误：legacy y>=0 扫描下界（1.20.1
潜在 bug）修复为 getMinY；barrier/structure_void 作为技术方块对扫描透明；Mushroom 群系
写入跳过与 NW 条带保留；blank 零耗怪癖保留。待人工：实机听觉（loop 音效）与手装蓝图的
手感；M14 人工验收保留人工，P20 待清单全部验收后再启动。

### CI 34620146197 失败事件（如实记录）

8feee725 推送后 CI run 34620146197 失败于 **solar_distiller_containers**（既有测试，
非本切片新测试；本地双模式当时全绿）。根因与本切片无因果，但由本切片首暴露：该测试断言
的精确罐量会被蒸馏相位 tick 蚀 1 mB——`updateTicker` 以机器世界坐标 hash 为种子
（`floorMod(pos.hashCode(), 72)`），而 GameTest 网格出生点每次运行随机 → 相位每次重掷
（69/70/71 落进断言窗口即两种失败文案，与实测逐值吻合）；是否真正蒸馏还取决于世界时刻，
并发测试经 `setTime` 全局改写昼夜 → run 间方差。这是该测试自带的 ~4%/run 偶发率，此前
CI 全绿属抽样侥幸。修复（测试侧，不动生产代码）：经 NBT 往返钉 `updateTicker=0`
（sustainability 测试同款先例），首个相位被推到全部断言之后 69 tick。附带加固与勘误：
①游走类测试移入 85×8×85 大场地 + ledger 升级 6 tick 精确账目（隔离收紧；"游走污染
邻箱"假设后被证伪，如实记录）；②大场地 nbt 生成脚本两处 bug（blocks 列表误嵌套
palette[1] 内 + 列表元素多写类型字节）曾致 vanilla 报 EOF/"Failed to place test
structure"——以逐字节严格解析器对照可加载的小场地修正；经验：手写 gzip NBT 的校验器
必须独立于生成器实现，且列表元素不带类型前缀。
