# Mining Laser（mining_laser + laser_bullet）移植记录

切片：2026-09-11，legacy `ItemToolMiningLaser`（375 行）+ `LaserBulletEntity`（307 行）→
`ic2.neoforge.item.MiningLaserItem` + `ic2.neoforge.entity.LaserBulletEntity`。
解锁 3 条旧配方（mining_laser 本体、centrifuge、pattern_storage——后两条此前因
ingredient 未注册而 pending），recipes.py 745→748 converted（pending 51→48）。

## legacy 语义（逐条对齐）

- 规格：maxCharge 300000 / transferLimit 512 / tier 3，Rarity.UNCOMMON，stacksTo(1)。
- 8 模式，每发 EU：mining 1250 / low-focus 100 / long-range 5000 / horizontal 0（空中）/ super-heat 2500 / scatter 10000 / explosive 5000 / 3x3 7500；对方块瞄准的水平/3x3 齐射改为每轮 3000。
- LaserBulletEntity：零重力、速度 3.0、每 tick 功率 −0.5（射程由功率驱动）；穿透玻璃（含染色/玻璃板）；基岩（硬度<0）挡停；击中方块扣 功率−硬度/1.5，功率耗尽光束消散不破坏。
- 命中实体：伤害=(int)功率，Boss（末影龙/凋灵）封顶 5，先点燃 伤害×(熔炼模式?2:1) 秒再 mobProjectile 伤害。
- 熔炼模式：可燃方块不留掉落直接烧掉；否则按掉落表计算，若掉落物是"别的方块的 BlockItem"则以被挖方块本体为熔炼输入（石头→平滑石），配方产物原位放置、功率归零（一束只置换一块）。
- 爆炸模式：命中即在弹位触发 legacy Ic2Explosion（功率 5.0、掉落率 0.85、Type.Normal）。
- 挖到 TNT：内联原版 TntBlock.wasExploded 等价实现——引出 PrimedTnt 并随机缩短引信。
- 破坏可燃方块后 10% 概率原地起火。
- 散射模式：5×5=25 束、功率 12；3x3 瞄准：浅角 13 束（朝向相关）、陡角 9 束竖直矩阵。
- mode_switch NBT（laser_setting）→ 数据组件 `ic2:laser_mode`（Codec.intRange(0,7)）。

## 平台分歧（已文档化）

1. **模式切换**：legacy 客户端 Alt+ModeSwitch 键 → 潜行+使用（port 无键位系统，与
   night vision/NanoSuit 既定范式一致）；切换只换组件不耗电，服务端发送模式提示。
2. **onItemUseFirst → useOn**：legacy Forge 钩子被 port 的 `useOn` 覆写取代（同为
   先于方块交互；非 3/7 模式持激光右键方块一律 FAIL，与 legacy 返回一致）。
3. **legacy use() case 2 落空穿（fall-through）**：long-range 分支缺 break，落入
   3/4/7 分支——远射程模式实际射出两条光束（20 功率 + 8 功率熔炼）。按原样移植并在
   代码注释与 catalog 注记标记，未"修复"。
4. **isFlammable → getFlammability>0**：26.1.2 移除了 BlockState.isFlammable，改用
   NeoForge IBlockStateExtension.getFlammability（含 FireBlock 燃烧概率表）。
5. **Explosion 接口化**：26.1.2 `Explosion` 变为接口，legacy 激光挖 TNT 时构造的临时
   原版 Explosion 无法复刻，改为内联 TntBlock.wasExploded 的等价五行实现。
6. **Ic2Player 假玩家分支**：legacy 弹丸 owner 非玩家时退到共享假玩家继续挖；port 中
   激光只会由玩家发射，无玩家 owner 时光束继续飞行（功率照常衰减）。
7. **渲染器**：legacy 手工拼箭形贴片；26.1.2 自带同构 ArrowModel（bake
   ModelLayers.ARROW）+ submit 制渲染，复用原模型 + `ic2:textures/models/laser.png`
   （贴图从 legacy 拷贝）。视觉人工验收项。
8. legacy 附能耗电量条的 tab_icon 隐藏怪癖未移植（port 电量条由 ElectricItem 基类
   统一绘制，物品模型无 tab icon 用途）。

## GameTest（LaserTests 4 例，双模式 418=414+4 全绿）

隔离设计（关键）：StructureGridSpawner 源码证实地块间距仅 结构尺寸+5（empty 结构
1×1×1 → 横向 6 格、行距 7 格），且所有测试并行跑在同一世界。早期横向 3 层土墙版
（墙在 x=15..17，已越界进邻居地块）失败在 4 例间轮换、还污染 boat_lava——结构性
跨测试污染，非实现缺陷。终版原则：弹道只允许竖直、爆炸必须自封闭——

- mock shooter 悬在自家地块上空 (3,16,2)、xRot=90° 俯视，4 例全部竖直开火，
  光束永不横穿邻居地块；
- 采掘垫低置（y=1 石地板 + y=2 垫块，与其它测试的结构同高带）：远处高空爆炸的
  射线下坠 13 格空气即被 0.5/格吸收耗尽，够不到低结构（这也是 itnt 高空引爆
  不伤邻居的几何原因）；石地板兼作接drop盘，圆石掉落物不会坠出断言房间；
- 爆炸例：5×5 石壳（侧壁+顶圈 y=2..5）内填 3×3×3 泥土芯，y=5 玻璃盖在射手列正上
  **留 1 格空洞**——弹丸直落洞中，爆点恒定在泥土表面（曾用"穿透玻璃盖"设计：命中
  tick 落点在盖平面还是泥土面取决于逐 tick 位移与盖面的相对位置，本地 IC2×5 全绿
  但 CI GT step 两次超时失败，开洞后确定性起爆本地 IC2×3+GT×8 全绿）；每条爆炸射线
  都在石壳内耗尽（玻璃 ~1.8/块 + 石头 3.5/块 吸收 > 5 功率），12.5 格半径
  Ic2Explosion 全部封死在地块内。

- `laser_mining_shot`：2000 EU 开采模式俯射石垫——恰破 1 块、恰掉 1 圆石、
  恰扣 1250 EU、光束用尽（无存活弹丸）。
- `laser_superheat_smelts_sand_to_glass`：4000 EU 超热模式俯射沙垫（石地板防沙落）——
  恰 1 块 sand→glass 原位置换、无掉落、恰扣 2500 EU。
- `laser_mode_switch`：潜行+使用循环 0→1→…→7→0，全程不耗电。
- `laser_explosive_shot`：10000 EU 爆炸模式俯射封爆箱——箱内炸出弹坑 ≥3 块、
  恰扣 5000 EU、光束用尽（断言房间覆盖整条弹道 y=0..19）。

调试记录：GameTest 里 `makeMockPlayer` 不入世界不 tick，但 pos/yaw/xRot 直接可用
——`Item.use(level, player, hand)` 可在测试中直接调用复刻玩家开火；空结构 1×1×1
意味着地块尺寸不约束方块摆放（相对坐标可超界），但邻居就在 6 格外，摆放越界即
等于把测试断言押在别人的地块上。

## 验证

- 完整链：build → GT IC2 模式 418（×3）→ GT 默认模式 418（×2）→ :core:test →
  verify_artifact → progress 跑+--check → git diff --check 全绿。
- 既有 flaky（与本切片无关，复跑即绿）：luminator_ignite（火焰随机 tick）、
  boat_lava（骑手着火计时偶发）。

## 待人工验收（视觉/UX，保留人工）

- 激光光束实体渲染（箭形模型+laser.png）与开火音效（5 种 legacy 音效已注册）。
- 8 模式实机手感：模式切换提示、瞄准陡角提示（"Mining laser aiming angle too steep"
  原文直发，legacy 即非本地化字面量）。
- 散射/3x3 齐射的实机覆盖形状。
