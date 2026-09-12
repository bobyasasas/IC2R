# Nano Saber（nano_saber）移植记录

切片：2026-09-12，legacy `AbstractItemNanoSaber`（324 行）+ `ItemNanoSaber`（属性定义）+
`NanoSaberStateImpl`（NBT/瞬态）→ `ic2.neoforge.item.NanoSaberItem`。
解锁 1 条旧配方（nano_saber 本体），recipes.py 748→749 converted（pending 48→47）。

## legacy 语义（逐条对齐）

- 规格：maxCharge 160000 / transferLimit 500 / tier 3，Rarity.UNCOMMON，stacksTo(1)。
- 右键切换（`use`）：激活需库存 ≥16 EU，切换本身免费；激活播 POWER_UP、熄灭播 IDLE。
- 计费（inventoryTick）：active 状态下手持每 16 tick 耗 64 EU，背包每 64 tick 耗
  16 EU（legacy energyTick 瞬态计数 + `slot < 9` 判热栏）；耗不起账 → 自动熄灭
  （legacy consumeEnergy 覆写：耗尽即灭）。
- 攻击属性：active 且 ≥400 EU → 20 伤 / 0 攻速；否则 4 伤 / −3 攻速
  （legacy ItemNanoSaber.getAttributeModifiers）。
- 命中（postHurtEnemy 承接 legacy hurt+postHurt 拆分）：active 时先扣 400 EU，
  扣不动 → 熄灭；随后反装甲——每个甲槽在 ≥2000 EU 时，把 NanoSuit 槽位抽走
  48000 EU、QuantumSuit 槽位抽走 300000 EU（走盔甲自身的 consumeEnergy：
  剑的 tier 3 + 无视传输上限），抽干（<1 EU）即整件移除；每槽再扣剑 2000 EU。
  服务端玩家对玩家出手仍过 canHarmPlayer 门控。
- 挖掘：active 蛛网 50、其余 4；熄灭 1（legacy getDestroySpeed）。
- 创造模式持剑拒绝破坏方块（legacy canAttackBlock → `!isCreative()`，无 active 门控）。
- 状态：legacy NBT `"active"`（持久）+ energyTick（瞬态）→ 组件
  `ic2:saber_active`（仅激活时 set，熄灭即 remove，等价 NBT 回读语义）+
  `ic2:saber_tick`（不持久，重载故意重置计费相位）。

## 平台分歧（已文档化）

1. **canAttackBlock 移除**：26.1.2 物品级"能否破坏方块"钩子已删，NeoForge 改由
   `BreakBlockEvent` 承载（对旧门控以预取消状态触发）。移植为静态监听：创造玩家
   主手是纳米剑即取消破坏（双方各跑一次，无需 notifyClient）。
2. **inventoryTick 新签名**：26.1.2 无 int slot，只有 `@Nullable EquipmentSlot`。
   legacy `slot < 9` 热栏判据 → MAINHAND/OFFHAND 视为手持（64 EU/16t）、其余槽位
   视为背包（16 EU/64t）。盔甲槽的 legacy 计费路径（物品不在背包 tick 列表）不可
   达，行为面不变。
3. **攻击属性动态化**：legacy 每 tick 换 ItemAttributeModifiers；port 用 NeoForge
   `IItemExtension.getDefaultAttributeModifiers(ItemStack)` 按 stack 切换（ ElectricArmorItem
   同范式），切换在下次属性刷新时生效（装备变化/tick），无需事件。
4. **idle 循环音效 → 单次**：legacy ItemElectricTool 客户端每 tick 起 stop/start
   循环音（isFoil 式 onActiveChanged）；port 无循环音通道，改为切换瞬间的单次
   POWER_UP / IDLE 音效（与 chainsaw 先例一致）。
5. **use() 补电分支不适用**：legacy ItemElectricTool.use 在没电时尝试从背包取电
   （manager.use），nano_saber 的 use 被覆写为纯切换、不补电，移植同样不补电。
6. 反装甲移除盔甲**无掉落**（legacy 直接 setItemSlot(EMPTY)，物品消失）——按原样保留。

## GameTest（NanoSaberTests 4 例，双模式 422=418+4 全绿）

- `saber_use_toggle`：1000 EU 熄灭态右键 → 激活且电量不变（切换免费）；
  再右键 → 熄灭；15 EU（<16 门槛）→ 拒绝激活。
- `saber_held_billing`：96 EU 激活态连跑 16 次 inventoryTick（交替 MAINHAND/null）
  → 恰剩 32（两次 64 EU 账单）；再跑 16 次 → 电量不变且自动熄灭（付不起账）。
- `saber_attributes`：熄灭 4/−3；置 active → 20/0；active 但 100 EU（<400）→
  回落 4/−3。
- `saber_strike_armor_drain`：20000 EU 激活态 postHurtEnemy 打在 48000 EU 纳米
  胸甲的僵尸上 → 剑恰扣 2400（400 命中 + 2000 disable）、胸甲槽被整件移除。
  僵尸 `setNoAi` 锁死在自家地块（打击是直接方法调用，不需要 AI）。

隔离收敛（本轮教训）：4 个 saber 测试最初注册在 laser 套件中间，GT 模式下
laser_superheat 连挂 2 次（批次邻居构成被插入打乱，叠加 Itnt 引信随机化的既有
串扰面；GT/IC2 模式本身不影响激光）。把 saber 注册移到 FUNCTIONS 末尾（既有测试
批次构成完全回到基线）+ 僵尸 NoAi 后，本地 IC2×1 + GT×3 连续全绿。

## 验证

- 完整链：build → GT IC2 模式 422 → GT 模式 422（收敛后共 ×3 全绿）→ :core:test →
  verify_artifact → progress 跑+--check → git diff --check 全绿。
- 既有 flaky（与本切片无关，复跑即绿，已记录在 mining-laser.md）：luminator_ignite、
  boat_lava；本轮观测到 laser_superheat 在批次构成被扰动的 GT 跑中可被邻位串扰
  拖挂，收敛注册顺序后未复现。

## 待人工验收（视觉/UX，保留人工）

- 光刃外观切换：`ic2:saber_active` 组件驱动的条件模型（inactive/active 双贴图 +
  动画 mcmeta）实机显示与切换即时性。
- POWER_UP / IDLE 单次音效听感（legacy 为循环音，听感必有差异）。
- 实机手感：激活/熄灭节奏、反装甲命中反馈（盔甲整件消失）、创造模式拆方块被拒。
