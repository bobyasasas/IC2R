# 核弹(P13 爆炸链切片六)

legacy 基线:`ic2/core/block/machine/tileentity/TileEntityNuke.java`(经
`TileEntityBridgeNuke` 继承 `TileEntityExplosive`)、`ic2/core/entity/block/NukeEntity.java`
(继承 `api/entity/block/ExplosiveEntity.java`)、GUI `assets/ic2/guidef/nuke.xml`。

## legacy 行为依据

- **威力公式**(TileEntityNuke.getNukeExplosivePower):外槽装工业 TNT(整组消耗),
  `power = 5 × itntCount^(1/3)`;内槽按燃料种类给辐射范围与 Boost——
  铀 238:范围=itnt 数;铀块:范围=itnt×6;小脉铀 235:范围=itnt×2,itnt≥64 时
  `+0.0556×inside^1.6`;铀 235:范围=itnt×2,itnt≥32 时 `+0.5×inside^1.4`;
  小脉钚:范围=itnt×3,itnt≥32 时 `+0.0556×inside^2`;钚:范围=itnt×4,
  itnt≥16 时 `+0.5×inside^1.8`。结果被 `IC2Config.protection.nukeExplosionPowerLimit`
  (默认 60)截断;外槽为空返回 -1(拒绝引爆)。
- **实体**(NukeEntity → ExplosiveEntity):引信 300 tick、dropRate 0.05、
  实体伤害系数 1.5、渲染为核弹方块;`radiationRange > 0` 时爆炸走
  `Ic2Explosion.Type.Nuclear`(ic2:nuke 伤害源),否则 Normal。辐射圈效果
  (Ic2Potion.radiation)legacy 在爆炸内触发——随 P16 辐射切片接入。
- **引信链**(TileEntityExplosive):红石上升沿、火焰块引燃(火令/打火石,含客户端
  预测)、燃烧箭命中、邻近爆炸(短引信:`nextInt(fuse/4)+fuse/8`)。挖掘**不会**引爆
  (explodeOnRemoval=false),正常掉落自身;引爆时清空两槽、移除方块、播放
  TNT_PRIMED。
- **扳手**:对引信实体使用扳手(canTakeDamage)消耗 1 点耐久并拆除实体。
- **GUI**(guidef/nuke.xml,176×219):单个 insideSlot (78,61) + 单个 outsideSlot 的
  8 个视图位(51,7)(105,7)(25,34)(132,34)(25,88)(132,88)(51,115)(105,115)
  + 玩家栏 (7,136);同一槽多视图为 legacy 动态 GUI 惯例。
- **方块**:即刻破坏、自掉落(DefaultDrop.Self)。
- **配方**:4×thick_neutron_reflector + advanced_circuit + advanced_machine
  (legacy JSON 带 `hidden:true`,为指南书内部标记,未迁移)。
- **日志**:legacy 在放置/引爆时打 PlayerActivity 日志——移植侧省略。

## 移植实现

- `ic2.neoforge.machine.NukeBlockEntity`:独立 BlockEntity(不走 MachineKind 体系),
  `MachineInventory` 双槽(0=外槽仅 itnt,1=内槽铀系/钚系/铀块),威力公式逐分支
  对照 legacy,`BalanceConfig.ENABLE_NUKE` / `NUKE_EXPLOSION_POWER_LIMIT`
  (protection 段,默认 true/60)承接 IC2Config。
- `ic2.neoforge.world.NukeBlock`:onPlace/neighborChanged/useItemOn(火令/打火石)/
  onProjectileHit(燃烧箭)/useWithoutItem(开 GUI)。**覆写 NeoForge
  `Block.onBlockExploded`:先引爆后卸装**——26.1.2 的 `Containers.dropItemStack`
  用 `split()` 清空传入的活引用栈,若先移方块再引爆,preRemoveSideEffects 掉落内容
  会清空库存导致威力公式拿到 -1;先引爆也与 legacy 顺序一致(引爆本身会清槽+移方块)。
  拒绝引爆(未装载/配置关闭)时回退 removeBlock+正常掉落。
- `ic2.neoforge.entity.NukeEntity extends ItntEntity`(对应 legacy
  ExplosiveEntity 基类语义):基类抽出 `dropRate`/`damageVsEntities`/`explode()`
  protected 与 `maxSavedPower()` 钩子;核弹覆盖引信 300/dropRate 0.05/伤害 1.5、
  Nuclear/Normal 类型选择、扳手 `interact`、`radiation_range` 持久化、
  存档威力不再截断 128(由配置在计算端截断)。
- **引擎缺口修复**:Ic2Explosion 此前把实体伤害硬编码为 `4×power`,legacy 实为
  `4×power×damageVsEntities`——新增 11 参构造承接(旧 10 参委托 1.0),工业 TNT
  0.3 系数由此真正生效(切片五遗留缺口)。
- `ic2.neoforge.menu.NukeMenu` + `client.NukeScreen`(176×219,ContainerScreenBase
  纯绘制边框,端口惯例):1 内槽 + 8 个外槽视图 + 玩家栏;菜单注册
  `ModNuke.NUKE_MENU`(IMenuTypeExtension 读 BlockPos)。
- 注册 `ic2.neoforge.registration.ModNuke`(方块/物品 UNCOMMON/BE/菜单)+ 实体在
  `ModEntities.NUKE`;客户端渲染复用 `ItntRenderer`(TntRenderState 块闪烁渲染),
  Screen 在 IndustrialCraftClient.registerScreens 注册。
- 资源:blockstate/方块模型(cube_bottom_top)/物品模型/三贴图(自 legacy 资源复制)/
  战利品表(自掉落)/配方(port `ic2:shaped` 格式,去 hidden 标记)/
  语言键复用现有 `block.ic2.nuke`。

## 有意差异

1. 直接读世界(同切片五):不复制 legacy PathNavigationRegion 快照。
2. 省略 PlayerActivity 放置/引爆日志(调试日志,非玩法)。
3. 配方 JSON 去 `hidden` 标记(指南书内部标记)。
4. 引火物消耗改为仅成功引爆时消耗(legacy 先消耗后引爆,失败也损耗;与切片三
   itnt 的玩家友好处理一致)。
5. 实体侧存档威力不再被 128 截断(配置截断在计算端,legacy 语义)。

## 测试证据

`neoforge/src/gameTest/java/ic2/neoforge/test/NukeTests.java`(7 项,heat_room 隔离
结构,双模式 264×2 全绿):

- nuke_power_formula:空外槽 -1;1×itnt=5.0;8×itnt=10.0(立方根)。
- nuke_radioactive_payload:32×itnt+1×铀 235 → 5×32^(1/3)+0.5=16.374,辐射范围 64。
- nuke_redstone_primes:红石引燃生成 300 tick 实体、方块移除、两槽清空、
  铀 238 → 范围=itnt 数。
- nuke_unloaded_refuses:未装载拒绝引爆且方块保留。
- nuke_chain_reaction:邻近爆炸短引信连锁(引信 <300)。
- nuke_wrench_defuses:扳手 CONSUME + 实体移除。
- nuke_menu_slots:菜单 9+36 槽;内槽仅收放射性 payload,外槽视图仅收 itnt。

## 遗留与勘误

- 辐射圈效果(饥饿/辐射药水/防化服豁免)随 P16;`radiationRange` 参数已全线贯通。
- 核爆专用音效/特效(ic2:block.nuke.explode 已注册)随特效切片接入引擎
  playExplosionEffect。
- `die_from_own_nuke` 进度(advancement)随 P16 进度批量迁移。
- GameTest 陷阱:Heat 结构内放置 loaded 核弹测试连锁时,爆炸会波及相邻测试——
  连锁断言用 3.0 威力 vanilla 爆炸在 ORIGIN 旁 1.5 格触发,坑洞被 heat_room 全
  bedrock 吸收,未外溢。
