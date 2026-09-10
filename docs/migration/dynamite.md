# P13 爆炸链:dynamite(炸药棒、投掷/粘性实体、遥控器)

2026-09-09 切片一至四。迁移 legacy `BlockDynamite`、`ItemRemote`、
`DynamiteEntity`/`StickyDynamiteEntity`、`ItemDynamite`、`PointExplosion`、
`BehaviorDynamiteDispense`。

## 旧行为依据

### 方块(BlockDynamite)

- 双状态:`FACING`(UP/南北东西,无 DOWN)+ `LINKED` 布尔(遥控配对标记)。
- 五向 VoxelShape;`canSurvive` 贴面必须 sturdy;支撑丢失弹出物品。
- 引信链(`checkPlacement`/`explode`):
  - 红石信号(`hasNeighborSignal`)→ 移除方块,生成 **粘性炸药实体**
    (sticky 类型,fuse **40**),`TNT_PRIMED` 音效;
  - 玩家任何模式挖掘(`onDestroyedByPlayer`,创造不豁免)→ 同上 fuse 40;
  - 被爆炸波及(`onBlockExploded`)→ 实体 fuse **5**(连锁短引信);
  - `detonate`(遥控器调用)→ fuse 40。
- 材质:0 强度、草声、无碰撞、noLootTable、PushReaction.DESTROY。

### 投掷实体(DynamiteEntity / StickyDynamiteEntity)

- `ItemDynamite.use`(空气右键):消耗 1,`ARROW_SHOOT` 音效,从眼部
  前方 0.16 格抛出实体,fuse **100**,速度 1.0、散布 1.0;普通炸药
  对方块右键是放置(useOn),粘性炸药 useOn 为 PASS(不可放置,一律投掷);
  双方均 stacksTo(16)。
- 物理:自研 clip 射线(不是实体碰撞)——命中方块收缩 0.05 贴面并以
  `(0.75-rand, -0.3, 0.75-rand)` 反弹,朝向 0.2 插值;拖拽 0.98、重力 0.04。
- **粘性特有**:首次命中后锚定 `stickPos`,每 tick 额外烧 3 引信且速度清零;
  支撑消失后恢复下落。任意实体卡地 200 tick 后静默消失(哑火清理)。
- **水中哑火(quirk)**:`isInWater` 时每 tick `fuse += 2000`——入水炸药
  实际成为哑弹,只会冒气泡(阻力 0.75)。legacy 如此,原样保留。
- 爆炸 = `PointExplosion`:以爆心为中心的 **固定 3×3×3** 方块破坏
  (抗性 < power×10=10 即破坏),掉落率 1.0,±2 格实体一律 **20 点伤害**
  (不含投掷实体本身),原生爆炸音效与 EXPLOSION 粒子;不是 vanilla
  衰减射线爆炸。连锁:被破坏的 dynamite 方块经 onBlockExploded 再变
  fuse 5 实体,ITNT 经 TileEntityExplosive 链。
- 发射器:`BehaviorDynamiteDispense` 以 1.1 速度、6.0 散布朝发射器朝向
  (+0.1Y)投出对应实体。
- 配方:`shaped/dynamite_sticky.json` — 8 dynamite + 1 resin → 8
  dynamite_sticky。

## 新设计

- `DynamiteBlock`(neoforge.world):切片一至三已有状态机/贴面生存/
  LINKED/支撑弹出;本切片补齐引信链——`neighborChanged`+`setPlacedBy`
  → `checkPlacement`(红石 → `prime` fuse 40,附 `is(this)` 守卫的支撑
  弹出兜底)、`playerWillDestroy` → `prime` fuse 40(26.1.2 钩子,创造
  同样引爆,与 legacy 一致)、`wasExploded` → `prime` fuse 5、
  `dropFromExplosion` false。`prime(Level,Pos,LivingEntity,int)` 移除
  方块并生成 sticky 实体 + `TNT_PRIMED`;遥控器调用点由旧直爆
  `explode(2.5F)` 改为 `detonate`(fuse 40,owner=玩家)。
- `DynamiteEntity`(neoforge.entity):普通/粘性共用一个类,由实体类型
  区分(`ic2:dynamite`/`ic2:sticky_dynamite`,均 0.5×0.5、tracking 8、
  interval 5,对齐 legacy);fuse 走 SynchedEntityData;owner 用
  `EntityReference<LivingEntity>` 持久化;tick 逐行对照 legacy(含
  `super.tick()`=baseTick 更新水状态、200 tick 哑火清理、入水 +2000);
  爆炸遵守 `GameRules.TNT_EXPLODES`(legacy 无此 gamerule,属有意
  现代化,与 ITnt 切片一致)。
- `PointExplosion`(neoforge.world):26.1.2 的 `Explosion` 已是接口,
  直接实现它;方块破坏复用原生 `state.onExplosionHit` 掉落与
  `onBlockExploded`→`wasExploded` 钩子链(连锁与 ITNT/dynamite 都走
  同一入口),保留 legacy 的抗性门控与 ±2 格 20 点伤害
  (`damageSources().explosion(this)` + `hurtServer`);dropRate 参数
  省略——唯一调用点恒为 1.0,与原生无条件掉落等价。
- `DynamiteItem`(extends BlockItem,stacksTo 16):放置沿用原生
  BlockItem,`use` 投掷(比 legacy 自写 useOn 更贴合新版本);`useOn`
  FAIL 不投掷的 legacy 死角不复刻。
- `StickyDynamiteItem`:`useOn` PASS + `use` 投掷。投掷路径共享
  `ThrownDynamite.throwCharge`。
- 发射器:`DynamiteDispenseBehavior` ×2 在 `FMLCommonSetupEvent`.
  `enqueueWork` 注册(legacy 为静态注册期)。
- 渲染:复用 vanilla `ThrownItemRenderer`(实体实现 `ItemSupplier`,
  `getItem()` 按类型返回对应物品,GROUND 显示形态)。
- 资产:items/models/纹理 `dynamite_sticky` 移植;lang 沿用批量生成
  的既有条目;配方 `shaped/dynamite_sticky.json` 按 port 格式转换。

## 缺陷修复与差异记录

- 上一切片遗留:玩家挖掘 dynamite 原为直接消失(无钩子),本切片对齐
  legacy 改为引爆(fuse 40,创造不豁免)。
- 旧 `DynamiteBlock.explode(2.5F 原生爆炸)`(切片二遥控群爆简化)替换为
  legacy 精确语义(40 tick 引信实体);遥控群爆因此有可见嘶嘶引信。
- `RemoteTests.remoteDetonatesLinkedDynamite` 修复两处空转:断言误用
  世界坐标 (8,8,8)(结构外空气,恒真);mock player 空手拿不到
  REMOTE_LINKS 组件,`use` 实际从未触发引爆。现改为手持遥控器 +
  `absolutePos` 绝对坐标配对,并断言实体生成与 45 tick 后爆炸。
- 爆炸连锁测试使用黑曜石支撑:legacy 中支撑与炸药同被普通爆炸破坏时,
  破坏顺序随机,炸药可能走 updateShape 弹出而非 wasExploded 引爆
  (legacy 固有竞争,未改);测试用不塌的支撑锁定确定性路径。

## 测试证据

- 既有:`DynamiteTests.placesWithFacingAndSupport`、
  `linkedStateToggles`;`RemoteTests.remoteDetonatesLinkedDynamite`
  (修复后:清格 + 实体 fuse≤40 + 45 tick 后爆炸)。
- 新增 `DynamiteTests`:`redstonePrimesFuse`(红石 → 清格 + fuse≤40 +
  支撑被炸)、`playerBreakPrimesFuse`(FakePlayer 破坏 → 引爆)、
  `explosionChainsFuse`(邻爆 → fuse≤5 → 即刻爆炸)。
- 新增 `ThrownDynamiteTests`:`thrownDynamiteDetonates`(fuse 100 →
  引信耗尽 → PointExplosion 破坏石台)、`stickyDynamiteFuseAccelerates`
  (粘性实体每 tick 额外烧 3,早于普通实体爆炸;普通实体仍按 100)、
  `waterDisarmsFuse`(入水 150 tick 后仍是哑弹且 fuse 净增)。
- IC2 与 GT 双模式 `runGameTestServer` 249 项全绿。

## 未验收 / 后续

- 实机测试(投掷手感、发射器、粘性攀附视觉)见 待测试.md 第 41 节。
- legacy `Ic2Explosion` 全尺寸语义(核弹/NUKE)未迁移。
- 遥控器配对上限随后续切片;投掷实体被箭/攻击命中无交互(与 legacy 一致)。
