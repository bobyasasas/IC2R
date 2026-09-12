# 强化玻璃（Reinforced Glass）与泛光灯（Luminator）

Legacy `Ic2GlassBlock`（REINFORCED_GLASS）与 `TileEntityLuminator`（LUMINATOR_FLAT）移植。
两者是 P16 收口的最后前置：解锁夜视仪、量子头盔、UU 物质扫描器、复制器与两条强化玻璃
配方，共 **7 条 recipe-catalog pending → converted**。

## 强化玻璃（neoforge block/ReinforcedGlassBlock）

- Legacy `Ic2GlassBlock extends AbstractGlassBlock`，26.1.2 无 AbstractGlassBlock（vanilla
  已合并为 `HalfTransparentBlock`），移植类改为继承之；`getBlockSupportShape → empty`
  **逐字保留**——强化玻璃上不能放置任何依赖支撑的方块（泛光灯挂上去会掉落）。
- 属性逐项对照 legacy 注册：`noOcclusion + strength(5, 180) + GLASS 声音 +
  isValidSpawn(false)`；无 `requiresCorrectToolForDrops`（legacy 同，任何工具可挖）。
- 渲染：模型照抄 vanilla 26.1.2 `glass.json` 的 texture 对象写法
  （`force_translucent + sprite`），贴图为 legacy `textures/block/reinforced_glass.png`；
  战利品表 = 自身掉落（legacy 默认掉落）。
- 注册于 ModMaterialBlocks（BUILDING_BLOCKS 创造栏），items/item 模型走 port 常规布局。

## 泛光灯（MachineKind.LUMINATOR + LuminatorBlockEntity）

以 MachineKind 体系接入（`("luminator", 容量 5, 0 槽)`，electricalTier 默认 tier-1），
`LuminatorBlockEntity extends PoweredBlockEntity`（TeslaCoil 同款 0 槽无 GUI 路线）：

- **点亮逻辑（对照 legacy updateEntityServer）**：`lit = 红石输入 XOR invertRedstone 且
  consume(0.25)`；active 即 lit（方块 lightLevel 15，状态驱动，无需 legacy 的
  手动 light engine checkBlock——26.1.2 属性光自动重算）。
- **能量**：tier-1 sink、容量 5 EU、0.25 EU/t；电网经 `insert` 只能维持到 5。
- **右键（legacy onActivated）**：手持电物品 → 先 simulate 后实 discharge
  （tier=1、ignoreTransferLimit=true、externally=true，与 legacy 两段调用逐参一致），
  `forceAddEnergy(amount)` 灌入；**10000 EU 上限怪癖忠实保留**——容量 5 的存储可被手
  放电顶到 10000（满电跑约 33 分钟不接网）。tier 门与 legacy 一致：tier-1 请求收不下
  tier≥2 的物品（高级电池点泛光灯 = 切换反转而非充电），`ignoreLimit` 只放行传输限制
  不放行 tier（port ElectricTransfers 与 legacy manager 逐行同义）。否则空手/普通物品
  → 切换 `invertRedstone`。
- **扳手（legacy setFacingWrench/wrenchCanRemove）**：WrenchTool.rotate 对 LUMINATOR
  分支——不旋转、只切换反转（两个扳手均经 WrenchTool）；**勘误（如实记录）**：
  legacy `wrenchCanRemove=false` 禁止扳手拆除，port 无扳手拆除机制（全机器统一走普通
  挖掘+战利品表），扳手挖掘泛光灯掉自身，行为上是放宽项，记录于此。
- **点燃怪物（legacy igniteTouchingMonsters）**：lit 时对 igniteBounds 内 Monster
  `setRemainingFireTicks(undead ? 20 : 10)`。undead 判定勘误：26.1.2 已移除 MobType，
  用 `LivingEntity.isInvertedHealAndHarm()`（同一亡灵语义）。**igniteBounds 公式逐字
  转写 legacy aabbMap**：~0.53³ 盒锚定支撑面且偏往 +x/+y 角——部分朝向的盒会伸进支撑
  方块，legacy 怪癖原样保留（floor-mount 朝上时 = 方块下半，确能点燃走进灯里的怪）。
- **放置校验（legacy isValidPosition/checkPlacement）**：支撑位 = `facing.getOpposite()`
  侧，`isFaceSturdy(level, supportPos, facing)`（26.1.2 去掉了 SupportType 重载，等价
  FULL）通过即合法；否则若支撑是电缆（legacy 电缆即 IEnergyEmitter）或其 BE 的
  energyNode 有 output 端（发电机/变压器族）同样合法。**勘误（如实记录）**：legacy 走
  `EnergyNet.getSubTile → IEnergyEmitter` 图级查询，port 无公开图查询 API，改为方块级
  判定（电缆方块 / 带源终端的 PoweredBlockEntity），语义覆盖实际可挂的所有载体。
  首个 serverTick + 每次邻域变化各校验一次，失撑 `destroyBlock(true)` 掉落自身。
- **形状/放置**：面板 outline 16×16×1 贴支撑面（facing 背侧）；facing = 离开墙面方向，
  沿 port verticalFacing 惯例（nearestLooking 反向，与 legacy clickedFace 在全部常见
  放置姿态等价）；blockstate/模型/贴图/物品模型从 legacy assets 逐字拷贝（12 变体 +
  luminator_shape_flat 元素模型 + handheld 物品模型）。
- **比较器（legacy ComparatorEmitter → energy.getComparatorValue）**：
  `min(stored×15/capacity, 15)`；过充后饱和 15。MachineBlock hasAnalogOutputSignal 加
  LUMINATOR 分支。
- **持久化**：invertRedstone 存 `invert`；`energy` 由 PoweredBlockEntity 写入。过充的
  5 EU 上限问题：load 时 super 先按容量 clamp，再按读到的原始值把超容量部分
  `forceAdd` 补回——存读往返保留怪癖（GameTest 钉死 3000 EU 往返）。
- **声音**：无 loop 音（legacy 同），MachineSounds switch 补 LUMINATOR→null。
- MachineMenu 槽位分支补 LUMINATOR（0 槽组，防 GUI 走泛型分支凭空出槽）。

## 配方（7 条，全部 legacy 图案逐字转写）

| 目标 | 图案 | 要点 |
| --- | --- | --- |
| reinforced_glass ×2 | GAG/GGG/GAG、GGG/AGA/GGG | legacy `forge:glass` tag → port `#c:glass`（crop_analyzer 先例），A=合金 |
| luminator | ICI/GTG/GGG | 铁外壳+绝缘铜缆+锡缆+玻璃 ×8 |
| night_vision_goggles | ABA/CDC/EFE | 本切片解锁 |
| quantum_helmet | GnG/ILI/CNC | P16 收口 |
| uu_scanner | ABA/CDC/EFE | 本切片解锁 |
| replicator | SGS/TTT/VFV | 本切片解锁 |

## GameTest 6 项（ic2_tests:empty 场地，双模式 396 项全绿）

1. **luminator_ledger**：满 5 EU 无信号 20 tick 分文不动且 inactive；红石后 20 tick 恰
   耗 20×0.25=5 EU、active；再 1 tick 清空后熄灭。
2. **luminator_invert**：右键空手切反转→供电不亮不耗电；再切恢复→5 tick 恰耗 1.25
   （4.75→3.5 逐段精确账目）。
3. **luminator_discharge**：充 3000 的 RE 电池（tier-1）右键→灯内 3000（>5 容量怪
   癖）、电池清空；save/load 往返保留 3000；比较器饱和 15，半容量恰为 7。
4. **luminator_ignite**：未亮 3 tick 零点燃；点亮后同一盒内僵尸（亡灵 20t）与爬行者
   （10t）均着火且僵尸火 ticks 严格更大。
5. **luminator_support**：悬空/强化玻璃上 → 失撑掉落自身（ItemEntity 经 AABB 扫描断
   言——empty 场地仅 1×1，框架 assertEntityPresent 只搜结构盒）；石头上/绝缘铜缆上
   → 存活。
6. **luminator_craft**：强化玻璃（tag 匹配 + 7 产出）与泛光灯（8 产出）配方 matches +
   assemble。

## 历史交付补记

`ic2:reactor_chamber`（block/block_entity/item 三条 registry-catalog pending）对应的
MachineKind.REACTOR_CHAMBER + ReactorChamberBlockEntity 随 P12 反应堆切片早已交付并
在全量 GameTest/CI 中验证，本轮顺带把三条条目翻转为 implemented（补记，非新交付）。

## 待人工验收（视觉/UX，保留人工）

- 泛光灯 12 变体朝向/active 贴图切换与手持贴图渲染。
- 强化玻璃半透明渲染层级（cutout/translucent 实机观感）与防爆贴图。
