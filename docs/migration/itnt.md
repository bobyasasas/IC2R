# P13 爆炸链切片三:工业TNT(方块+实体+渲染器)

2026-09-10 切片。迁移 legacy `TileEntityITnt`/`TileEntityExplosive`/
`ExplosiveEntity`/`ITntEntity`/`ExplosiveBlockRenderer` 与 `Ic2Items.ITNT`
方块物品。同切片修复切片一 dynamite 支撑弹出未落地与测试坐标 bug。

## 旧行为依据

- `TileEntityExplosive`:红石信号>0 引爆;打火石/火药弹点燃;玩家拆除
  即爆(`explodeOnRemoval`,创造同样);被爆炸波及以短引信链爆
  (`fuse/8 + rng(fuse/4)`);燃烧弹射物引爆。
- `ITntEntity`:引信 60、威力 5.5、renderBlockState=itnt、辐射范围 0。
- `ExplosiveBlockRenderer`:块状渲染+末 10 tick 白闪放大(vanilla TNT 同款)。
- 配方:`itnt.json`/`itnt_vertical.json`(flint+tnt→4);
  `shapeless/dynamite.json`(itnt+string→8 dynamite)。

## 新设计

- `ItntBlock`(neoforge.world):逐钩子对照 vanilla `TntBlock` 的 26.1.2
  签名(onPlace/neighborChanged(Orientation)/playerWillDestroy/wasExploded
  (ServerLevel)/useItemOn/onProjectileHit/dropFromExplosion)。爆炸经原生
  `level.explode(..., 5.5F, ExplosionInteraction.TNT)`,受 `TNT_EXPLODES`
  游戏规则约束;legacy `Ic2Explosion` 的 dropRate/伤害语义未随原生爆炸
  迁移(与 dynamite、热爆炸同款偏差,记入工作包)。
- `ItntEntity`(neoforge.entity,新包):fuse/blockState 同步
  (`SynchedEntityData`)、`TraceableEntity` 溯源、ValueInput/Output 持久化、
  引力/落点/烟粒与 vanilla `PrimedTnt` 对齐;`MobCategory.MISC` 注册于新
  `ModEntities`(0.98³、fireImmune、tracking 10)。
- `ItntRenderer`(client):复用 `TntRenderState`+
  `TntMinecartRenderer.submitWhiteSolidBlock`,白闪/放大参数与 legacy 一致。
- legacy `TileEntityITnt`/`TileEntityExplosive` 不设独立 BE,行为由
  `ItntBlock` 方块级钩子等价实现(注册清单 `block_entity ic2:itnt` 记
  replace/implemented)。
- dynamite 支撑弹出修复:`updateShape`(26.1.2 新签名,ScheduledTickAccess)
  在支撑面朝向失效时显式 `popResource` 并返回空气;noLootTable 方块不能
  依赖 `Block.updateOrDestroy→destroyBlock` 的原生掉落,故保留 legacy
  popResource 语义。同时补注册 `DYNAMITE` BlockItem(simple block item,
  覆盖 legacy `ItemBlockIc2` 放置语义;legacy `ItemDynamite.use` 的投掷
  实体随后续切片)。
- 配方经 `ic2:shaped`/`ic2:shapeless` 管线转换 3 条,目录翻转后重跑
  `progress.py`(660/796)。

## 测试证据

- `ItntTests.redstonePrimesFusedCharge`:红石即刻引燃,实体引信≤60。
- `ItntTests.fuseDetonates`:heat_room 内石台被 5.5 威力炸毁≥4 块,实体引信
  耗尽后消失(隔离结构内验证爆炸威力)。
- `ItntTests.playerBreakPrimes`:FakePlayer 拆除即引燃(explodeOnRemoval)。
- `ItntTests.chainReaction`:邻爆后实体以短引信(<60)链爆。
- `DynamiteTests.placesWithFacingAndSupport`(修复):先置支撑再放置,
  撤支撑后弹 item 实体;原断言误用相对坐标(相对 (8,8,8) 恰为空气格)且
  移除的是朝向侧而非支撑侧,空转通过,已一并修正。
- IC2/GT 双模式 `runGameTestServer` 243 项全绿。

## 未验收 / 后续

- 投掷炸药(`DynamiteEntity` fuse 100)、粘性炸药(`StickyDynamiteEntity`
  攀附+dynamite_sticky 配方)、dynamite 红石引爆与 40 tick 引信链、
  legacy `Ic2Explosion` 自定义爆炸(dropRate/实体伤害)语义。
- 遥控器配对上限、核弹(NUKE)随 P13 后续切片。
- 实机测试项见 `待测试.md` 第 40 节。
