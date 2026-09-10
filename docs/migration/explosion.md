# Ic2Explosion 射线爆炸引擎(P13 爆炸链切片五)

## legacy 行为依据

`ic2/core/Ic2Explosion.java`(以及消费它的 `ExplosiveEntity`/`ITntEntity`/`NukeEntity`):
IC2 全部大型爆炸(工业TNT、核弹、反应堆熔毁、蒸汽机过压、激光)共用一台射线爆炸引擎,
与 vanilla 的球形衰减爆炸完全不同。

- **射线网格**:`maxDistance = power / 0.4`;方位角 `phi` 取 `2*steps` 份、
  极角 `theta` 取 `steps` 份(`steps = ceil(π / atan(1/maxDistance))`),从爆心射出
  2·steps × steps 条射线。
- **吸收**:`absorption = 0.5 + (爆炸抗性 + 4) × 0.3`(空气 0.5;Heat 型 ×6;
  非.Normal 型的水改为 +1 即 1.5)。吸收 < 0 → 射线终止;**吸收 > 1000 的超抗方块
  (基岩等)降级为 0.5 吸收但不被破坏**——射线直接穿过(`ExplosionWhitelist` 可豁免
  某方块保留完整吸收并被破坏);吸收 > 剩余功率 → 射线终止;否则记录破坏该方块
  (剩余功率 > 8 时标记 noDrop)。
- **散射**:吸收 > 10 的方块(黑曜石 361.7、红石块 10.7 等)触发 5 条随机方向散射
  射线,功率为该方块吸收的 0.4 倍——legacy 固有行为,威力极大(黑曜石散射 ≈144)。
- **实体伤害**:收集 AABB(±ceil(maxDistance))内的 LivingEntity 与 ItemEntity,
  按 (int) 距离平方排序;每 8 条射线(phi_n%8==0 且 theta_n%8==0)在
  `(step+4)%8==0` 且剩余功率 ≥ 0.25 的步进上对 5 格内实体累加
  `damage += 4 × power1`(ItemEntity 生命上限 5,累加超限即提前受击并可能移除;
  LivingEntity 为 ∞,只在最终统一受击一次);击退沿爆心方向 `0.0875 × power1`,
  合速度平方 > 3600 时按比例截断。
- **掉落**:全部射线结束后按 y→z→x 顺序统一破坏;战利品上下文 ORIGIN 为
  **爆心**(非方块位置);受 `mobGriefing`/`doBlockDrops` gamerule 与
  `dropFromExplosion` 约束;每份掉落以 `dropRate` 掷骰(`nextFloat() <= dropRate`
  才掉),再按 **2×2 列**(x/2、z/2 截断除法——负坐标有 legacy 已知怪癖)与
  物品+NBT 合并,在列中心 `(x/2+rand)*2`、最高命中层 `maxY+0.5` 处成堆生成。
- **类型**:Normal / Heat(吸收×6)/ Electrical / Nuclear / ReactorMeltdown
  (后两者走 `ic2:nuke`/`ic2:reactor_explosion` 伤害类型,igniter 作为施害实体)。
- **特效**:按类型播音效(Normal GENERIC_EXPLODE 4.0 音量、Heat FIRE_EXTINGUISH、
  Electrical MACHINE_OVERLOAD、Nuclear/ReactorMeltdown nuke 爆炸音)+
  EXPLOSION_EMITTER 粒子。
- **辐射**:Nuclear 且 radiationRange ≥ 1 时对范围内无防化服生物施加饥饿与辐射
  效果——依赖 P16 的辐射药水与防化服,随该包迁移。
- **进度**:被 `Ic2Explosion` 击杀的核弹点燃者进度(`die_from_own_nuke`)与
  `explode_machine` 进度依赖成就基础设施,未随本切片。

## 移植实现

- `neoforge/src/main/java/ic2/neoforge/world/Ic2Explosion.java`:实现 26.1.2
  `Explosion` 接口(接口化是与 1.20.1 的最大 API 差异),完整复刻射线网格、
  吸收分支(含 >1000 降级穿越与 <0 终止)、noDrop 标记(2-bit OR 语义改为
  `Boolean::logicalAnd` 合并)、实体累积伤害与二分窗口、击退 3600 截断、
  dropRate 掷骰与 2×2 列聚合掉落(战利品上下文 ORIGIN 同样取爆心)。
- 破坏统一走 `state.onBlockExploded` → NeoForge 默认 `setBlock(air)` +
  `block.wasExploded`,因此 ItntBlock 链爆短引信、DynamiteBlock 5 tick 链、
  `canDropFromExplosion=false` 全部自然生效。
- 与 legacy 的两处有意差异:①legacy 用 `PathNavigationRegion` 快照读方块,移植
  版直接读世界(射线阶段不破坏方块,读数等价;破坏阶段连锁引信移除方块后,
  直接读可避免 legacy 快照读到陈旧方块重复引爆);②伤害类型 JSON
  (`data/ic2/damage_type/nuke.json`、`reactor_explosion.json`)自 legacy 资产原样
  迁移,`DamageSource` 经 `damageSources().damageTypes.getOrThrow(...)` 解析。
- `neoforge/src/main/java/ic2/neoforge/world/ExplosionWhitelist.java`:legacy API
  类原样移植(静态 IdentityHashSet)。
- `neoforge/src/main/java/ic2/neoforge/entity/ItntEntity.java`:爆炸从 vanilla
  `Level.explode`(切片三的临时替代)改接 `Ic2Explosion`,对齐 legacy
  `ITntEntity` 参数(威力 5.5、dropRate 0.9、实体伤害 0.3、Type.Normal、
  radiationRange 0);TNT_EXPLODES gamerule 守卫保留。
- 现有 4 项 ItntTests 全部兼容(坑形断言 destroyed ≥ 4 在射线坑形下依然成立;
  链爆触发路径不受引擎替换影响)。

## 测试证据

`neoforge/src/gameTest/java/ic2/neoforge/test/Ic2ExplosionTests.java`(heat_room):
- `ic2_explosion_ray_crater`:4.5 功率射线炸穿首层石台、无力切割第二层
  (石头吸收 0.5+(6+4)×0.3=3.5,空气每格 0.5)。
- `ic2_explosion_bedrock_passthrough`:基岩完好但其正下方石头被摧毁——
  >1000 吸收降级 0.5、射线穿越不破坏。
- `ic2_explosion_shield_stops_rays`:双层石盾吸收殆尽,第三层完好——
  普通吸收终止分支。用石头而非黑曜石做盾:黑曜石吸收 361.7 > 10 会触发 5 条
  功率 ~144 的随机散射射线飞出隔离结构,GameTest 中属于不可控随机破坏
  (legacy 固有行为,实机验收时注意)。
- `ic2_explosion_drop_rate_zero` / `full`:dropRate 0 无任何掉落物、
  dropRate 1 全部掉落。
- `ic2_explosion_ray_damage`:2 格外的鸡被累积射线伤害击杀。
- `ic2_explosion_nuclear_source`:Type.Nuclear 运行时解析 `ic2:nuke` 伤害类型
  并击杀目标。
- `itnt_blast_drops_debris`:真实施放的工业TNT 起爆后 dropRate 0.9 产生成堆
  掉落物(≥2),证明实体已改接引擎。

GameTest 257×2(IC2/GT 能量模式)全绿;`progress.py --check` 通过;
verify_artifact:425 类(+6:Ic2Explosion/ExplosionWhitelist 及内部类)。

## 遗留与勘误

- 核弹(NukeEntity 300 tick 引信、TileEntityNuke 铀/工业TNT 配方威力公式、
  GUI 槽位、扳手拆除)依赖 GUI/容器迁移,独立切片。
- 辐射效果(饥饿+辐射药水+防化服豁免)、`ic2:electricity`/`ic2:radiation`
  伤害类型随 P12 反应堆余量与 P16 护甲包。
- **配对上限勘误**:早期分析把「遥控器配对上限」列入 P13 待办;核对 legacy
  基线 `ItemRemote.java` 与原始反编译版,coords 列表均为无界 ListTag,
  **legacy 本身没有配对上限**,该项从队列移除。
- `explosionDropRate` 的 2×2 列负坐标截断除法怪癖按 legacy 原样保留
  (正坐标世界内无感知)。
