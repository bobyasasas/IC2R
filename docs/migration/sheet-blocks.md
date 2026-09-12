# Sheets（树脂/橡胶/羊毛薄片家族）

切片：pending 清收路线（detector/splitter 电缆需能量网络手术、coke_kiln 多方块体量大、
流体/成分转换方案设计重，对比后取最小无决策依赖族）。移植 legacy `Ic2SheetBlock`
（三类共享单类），解锁 3 条配方，registry-catalog 翻转 3 方块 + 3 物品。

## Legacy 本体（legacy/forge-1.20.1/src/main/java/ic2/core/block/inherit/Ic2SheetBlock.java）

单类 `Ic2SheetBlock extends Block`，按 `state.getBlock()` 身份区分三件（均无 blockstate
属性），薄片碰撞盒 `Shapes.box(0,0,0,1,0.125,1)`：

- **resin_sheet** strength(1.6, 0.5)：无碰撞；放置需下方满块；邻居变更失效即掉落破坏；
  entityInside 缓冲：fallDistance×0.75、速度 ×(0.6, 0.85, 0.6)。
- **rubber_sheet** strength(0.8, 2.0)：有碰撞；放置需水平邻块为满块/橡胶薄片或下方满块；
  entityInside 蹦床：块下方非空直接 return；**LivingEntity 且 canSupportWeight=false 时**
  播放 2001 破坏粒子+移除方块；vy ≤ -0.4 → fallDistance 清零、水平 ×1.1、竖直
  （玩家潜行 ×-0.1；其余 ×-0.8；玩家跳跃键 ×-1.3）。
  **canSupportWeight**：沿 EAST/SOUTH 两轴 ±1 方向各扫描 ≤16 格——满块即锚定；橡胶薄片
  继续沿 -Y 降阶梯（薄片下满块也算锚定）。**怪异控制流 1:1 保留**：`!supported → break`
  直接跳出方向循环转下一轴；`if (dir == 1) return true` 只在 +1 方向完成后触发——
  语义等价于"单轴双侧均有 16 格内锚点"。
- **wool_sheet** strength(0.8, 0.8)：放置恒有效（isValidPosition 恒 true）→ 永不因失效
  破坏；碰撞形状对"潜行玩家"或"脚低于 顶面 − maxUpStep 的玩家"为 empty（潜行穿透 +
  从下方跳穿）；entityInside 的 `fallDistance ×0.95` 分支**嵌套在橡胶 else-if 内
  不可达**（legacy quirk，按不可达原样保留并注记）。

## 配方（3 条，全部当轮解锁）

1. `shaped/resin_sheet`：两行×3 ic2:resin → ×3（count 3 保留）。
2. `shaped/rubber_sheet`：两行×3 ic2:rubber → ×3。
3. `shaped/wool_sheet`：`WRW`——W=#minecraft:wool_carpets 标签、R=ic2:resin_sheet
   （中间夹树脂薄片）。

## 移植（neoforge/src/main/java/ic2/neoforge/block/SheetBlock.java）

1:1 逐参移植。平台差异如实记录：

- **跳跃键分歧**：legacy 蹦床跳跃 ×-1.3 分支读 `IC2.keyboard.isJumpKeyDown`（客户端键位
  同步网络层），port 未建该层（Jetpack 降级同范式）→ 跳跃玩家落入默认 ×-0.8。
- **26.1.2 API**：`neighborChanged` 增 `@Nullable Orientation`；`entityInside` 增
  `InsideBlockEffectApplier`/`isPrecise`（实现不使用）；`fallDistance` 由 float 字段变
  double；`isCollisionShapeFullBlock` 走 `BlockStateBase` 公开缓存入口（等效委托）。
- legacy isValidPosition 中满块判定传薄片自身 pos 而非邻块 pos（对满块不可区分），
  1:1 保留并注记。
- 方块属性 1:1：仅 `strength(...)`，不加 mapColor/sound（legacy 亦未设置）。
- 战利品表 1:1 复制：树脂掉 `ic2:resin`（random_chance 0.8 + survives_explosion），
  橡胶/羊毛掉自身（survives_explosion）。
- 资产：legacy resin/rubber 贴图复制；羊毛复用 `minecraft:block/white_wool`；
  `sheet/base` 元素模型（16×2×16 六面 cullface）+ 三 blockstate + 三 item 模型 +
  items/ 定义；创造页 BUILDING_BLOCKS。

## GameTest（neoforge/src/gameTest/java/ic2/neoforge/test/SheetTests.java，6 例）

| 测试 | 覆盖 |
|---|---|
| sheet_placement_rules | 树脂无支撑拒绝/满块可放；橡胶悬浮无邻拒绝/邻满块可放/满块上方可放；羊毛恒可放 |
| sheet_support_break | 移除下方满块→树脂破坏；羊毛同场不破坏；悬浮橡胶移除唯一水平锚→破坏 |
| sheet_resin_cushion | 双井 10 格坠落对照：树脂井僵尸血量 > 裸石井 +1（缓降减伤） |
| sheet_rubber_trampoline | 物品坠落 vy≤-0.4 反弹为正、薄片不破（非生物不走承重检查） |
| sheet_rubber_weight_break | 单侧锚悬浮橡胶 + NoAi 僵尸踩入 → canSupportWeight=false 破坏 |
| sheet_wool_collision | 站立玩家有碰撞/潜行 empty/脚低于顶面−步高 empty/非玩家实体有碰撞/树脂恒 empty/外形薄片保留 |

确定性要点：实体坠落测试 `EntityTickingTests.wrap` 包装；树脂缓降用真实僵尸双井对照
（伤害差 ≥2 保证 tick 采样鲁棒）；蹦床/断裂用 `startSequence().thenWaitUntil` 轮询
（26.1.2 `succeedOnTickWhen` 要求恰 tick 命中，不适用随机落点时序）。

## 验证

- build 全绿；GameTest 双模式 IC2×434 + GT×434 全绿（428+6）；core test 绿。
- recipes.py 758→761 converted、pending 38→35；registry-catalog 翻转 6 条
  （block 3 + item 3），item 级 313/528、pending 36。
- verify_artifact：645 类 / 490 item 定义 / 1089 模型依赖。

## 待人工验收

- 薄片模型实机观感（2 像素厚、六面 cullface 与邻块衔接）。
- 树脂/橡胶贴图、羊毛白毛复用观感。
- 蹦床手感（坠落反弹幅度、潜行压弹 ×-0.1）；跳跃强弹 ×-1.3 未移植（键位分歧）。
- 潜行穿羊毛/树脂缓降/橡胶承重断裂的实机体验。
