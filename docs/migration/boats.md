# Boat 家族切片:3 实体+3 物品注册与 4 条配方解锁

2026-09-11 切片(Bronze 工具/护甲后续,pending 清收路线)。移植 legacy
`Ic2Entities`/`Ic2Items` 的船家族——橡胶船/碳船/电力船三实体与三放置物品,
解锁 shaped/rubber_boat、shapeless/rubber_boat、shaped/carbon_boat、
shaped/electric_boat 共 4 条配方。tank 家族(bronze/iron/steel/iridium 四档
储罐及其 4 条配方)不在本切片:port 已有单一 `ic2:tank`(TankBlockEntity,
MachineKind.TANK,24,000 mB 通用储罐,见 docs/migration/tank.md,P08 已验收
切片)为文档化的有意分歧,重新实现 legacy 四档会与既有决策冲突。

## 旧行为依据(legacy)

- `RubberBoatEntity`(extends 1.20.1 AbstractBoatEntity 复制品):掉落
  broken_rubber_boat;`getBlockSpeedFactor` ×1.05;`brokenByFalling()=true`
  (1.20.1 原版船 >3 格落地摔毁语义);岩浆中 `hurt(lava, Float.MAX_VALUE)`
  即死。
- `CarbonBoatEntity`:掉落自身;×1.15;`brokenByFalling()=false`(摔落
  不毁);岩浆即死。
- `ElectricBoatEntity`:掉落自身;`fireImmune()=true`、`isOnFire()=false`;
  骑手(玩家)每 tick 从 `Inventory.armor` 四件电力盔甲抽 4 EU——先
  `discharge(stack, 4, MAX, true, true, simulate=true)` 试探,成功再真实
  放电,取到即 break;`getBlockSpeedFactor` ×(hasPower?1.5:0.25);岩浆中
  船无损但骑手 `setSecondsOnFire(5)`。
- 三实体 builder 一致:`MobCategory.MISC`、`sized(1.375F, 0.5625F)`、
  `clientTrackingRange(10)`。

## 新移植(26.1.2 范式)

- 实体基类变化:26.1.2 `Boat extends AbstractBoat`,ctor 为
  `(EntityType<? extends Boat>, Level, Supplier<Item>)`,掉落物由
  `VehicleEntity.destroy(ServerLevel, Item)` 统一(ENTITY_DROPS gamerule
  门控);实体类以 `EntityType<? extends Boat>` 通配参数透传 builder 的
  factory。
- `RubberBoatEntity`/`CarbonBoatEntity`/`ElectricBoatEntity`
  (ic2.neoforge.entity):逐条保留上列 legacy 语义。平台分歧两处:
  - 摔毁钩子:26.1.2 原版船已无摔落破坏机制(checkFallDamage 仅重置
    fallDistance),rubber 船的 `brokenByFalling=true` 在
    `checkFallDamage` 自实现(onGround && fallDistance>3 → destroy 掉
    broken_rubber_boat);
  - 岩浆检测:legacy 用 `getFluidState(blockPosition())`,而 26.1.2 船
    会浮在流体面上(浮出时 blockPosition 已不在流体块),故改用
    `isInLava()`(基于 AABB 与流体重叠,与原版 lavaHurt 同基准),行为
    语义"船身处岩浆"不变。
- `ModEntities`:RUBBER_BOAT/CARBON_BOAT/ELECTRIC_BOAT 三注册,尺寸与
  trackingRange 逐字 legacy;electric 船 fireImmune 走实体类覆写
  (26.1.2 builder 无 fireImmune 标志)。实体注册先于物品注册
  (BuiltInRegistries 顺序 ENTITY_TYPE 先于 ITEM),物品侧
  `DeferredHolder.get()` 安全。
- `ModItems`:三 `BoatItem(type.get(), p.stacksTo(1))`,泛型助手
  `boat(String, Supplier<EntityType<T extends AbstractBoat>>)` 把
  `.get()` 推迟到注册期;创造页 TOOLS_AND_UTILITIES(与原版船同页)。
- 渲染器:26.1.2 `AbstractBoatRenderer(EntityRendererProvider.Context,
  Identifier)` 抽象类,texture 为 protected final;原版 `BoatRenderer`
  从 ModelLayerLocation 派生纹理路径无法携带 ic2 命名空间,故
  `Ic2BoatRenderer` 拷贝 BoatRenderer 主体(waterPatch =
  `ModelLayers.BOAT_WATER_PATCH` + `RenderTypes.waterMask()`,
  model = `BoatModel(bakeLayer(ModelLayers.OAK_BOAT))`,
  submitTypeAdditions 提交水面补丁),构造直传
  `ic2:textures/entity/boat/boat_{rubber,carbon,electric}.png`;
  IndustrialCraftClient 经 `EntityRenderersEvent.RegisterRenderers`
  注册三个实例。
- 资源:legacy 贴图照搬——entity/boat/boat_{rubber,carbon,electric}.png
  + item/boat/{rubber_boat,carbon_boat,electric_boat}.png(broken
  rubber boat 物品贴图既有);items/*.json 客户端定义 ×3 +
  models/item ×3(parent `ic2:item/default`,样板 broken_rubber_boat)。

## 配方解锁

recipes.py 重跑:registry-catalog 6 条(物品 3+实体 3)翻 implemented
后 registered 集合收编,rubber_boat(shaped RBR/RRR,R=ic2:rubber,
B=#minecraft:boats)、rubber_boat(shapeless,broken_rubber_boat+rubber
×2)、carbon_boat('CRC','CCC',C=ic2:carbon_plate)、electric_boat
('AAA','RBR','RMR',A=insulated_copper_cable,R=iron_plate,
B=electric_motor,M=iron_rotor)4 条 converted(741→745),加载清单同步
745。catalog 附 per-entry evidence 与 legacy 语义注记(含两处平台分歧
说明)。

## GameTest(双模式 414=411+3 全绿)

- boat_drops:三船 `hurtServer`(rubber/carbon 用 lava MAX、electric 用
  generic 10 触发 destroy 阈值),轮询房间 ItemEntity——每船恰掉自身
  legacy 物品(rubber→broken_rubber_boat 证明 Supplier 掉落链),三船
  均销毁。
- boat_lava:两层岩浆池(船会浮,单层源块可能让船壳完全浮出流体致
  isInLava 翻转,深池保持 legacy"岩浆浴"),骑手
  `makeMockPlayer` 上船;5 tick 后 rubber/carbon 死、electric 火免存活、
  骑手仍为 controlling passenger 且 isOnFire(骑手着火 5 秒)。
- boat_power_draw:满电 batpack(5000)穿胸口,mock player 上电船;轮询
  断言 (5000-余量) 为 4 的正倍数(每 tick 恰 4 EU,不依赖轮询到第几个
  tick)且船存活。
- 测试环境注意:NeoForge `FakePlayer.startRiding` 无条件返回 false
  (fake player 不可骑乘),GameTest 骑乘必须用
  `GameTestHelper.makeMockPlayer`(纯 Player,骑乘走原版
  Entity.startRiding 路径)。

## 验证与已知波动

- build → runGameTestServer IC2 模式 → 默认模式 → :core:test →
  verify_artifact.py → progress.py(跑+--check)→ git diff --check 全绿;
  双模式各连跑 2+ 次 414 全绿。
- 既有 `luminator_ignite` 测试偶发 flaky(火焰蔓延随机 tick,与船无关,
  验证期间出现过 1 次,复跑即绿);船测试本身无波动。
- 速度因子 ×1.05/×1.15/×(1.5|0.25) 为 protected
  getBlockSpeedFactor 覆写,GameTest 侧无公开断言入口(无反射先例),
  语义由覆写存在性与 legacy 数字注释保证。

## 待人工/未验收

- 三船实机放置/骑乘/水面观感与实体贴图渲染(客户端渲染器)属实机人工
  验收项;
- electric 船有电/无电时的实机速度差(×1.5/×0.25)未在 GT 断言(同上
  protected 入口);
- rubber 船 >3 格摔毁(brokenByFalling 自实现)的实机表现未在 GT 断言
  (需要高空跌落结构,属实机可验)。
