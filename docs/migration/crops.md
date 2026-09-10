# 作物农业(P15)切片一:作物杆与生长内核

2026-09-10 切片。开始迁移 legacy IC2 作物系统(`TileEntityCrop` 1411 行、
`CropCard` API 与 ~24 张作物卡、`Ic2Crops` 注册表、`ItemCropSeed` 种子
袋)。本切片交付可玩的单作物闭环:作物杆→播种小麦→生长→收获→拾取
种子,外加杂草的自然侵占;分析器/Cropmatron/收割机/杂交育种随后续
切片。

## 旧行为依据(legacy)

- **结构**:每种作物一个方块(`crop_stick` 空杆 + 52 种作物方块),全部
  挂 `TileEntityCrop`;作物类型与年龄在方块状态,统计与资源在 BE。
- **tick 节律**:`tickRate = 256` 游戏 tick 一轮;地形三项(湿度/养分/
  空气)在 1024 tick 周期内错开(0/256/512 偏移)刷新。
- **生长公式**(`performGrowthTick`):`baseGrowth = 3+nextInt(7)+Gr`;
  `minimumQuality = max((tier-1)*4+Gr+Ga+Re, 0)`;
  `providedQuality = weight(湿度+养分+空气)*5`;达标时
  `points += base*(100+盈余)/100`,不足时 `aux=差*4`,`aux>100` 且
  `nextInt(32) > Re` 则作物死亡重置,否则 `points += max(base*(100-aux)/100, 0)`;
  `points ≥ tier*200` 进一个年龄。
- **地形**:湿度 = 生物群群系加成 + 湿润耕地 +2 + 水罐存储
  (≥5 再 +2,每 25 mB +1);养分 = 群系加成 + 下方连续泥土数(1..4)
  + 每 20 存储养 +1;空气 = 高度加成(clamp 0..2)+ 2×2 遮挡扫描
  (预算 9,fresh/2)+ 露天 +4。
- **收获/拾取**:右键成熟作物 → 高斯掉落 `round(N×chance*0.6827+chance)`,
  `chance = 0.95^tier × 1.03^Ga`,年龄回 `getAgeAfterHarvest`(小麦 2);
  左键/破坏 → 拾取种子(概率含 Re/Gr 加权,小麦统计 ≤1 掉原版小麦
  种子,否则掉带统计的种子袋),植株重置为空杆。
- **杂草**:空杆 1/100 概率自然长草(WeedEX 存储 ≥1 阻止并衰减);
  杂草 `isWeed` 时按 `nextInt(50)-Gr ≤ 2` 向邻杆扩散并抬升对方 Gr;
  踩踏(疾跑碰撞)1/100×抗性判定踩坏。种子袋不能种杂草
  (`tryPlantIn` 拒绝)。
- **种子袋**(`crop_seed_bag`,stacksTo 1):NBT {owner,id,growth,gain,
  resistance,scan};对作物杆右键播种;scan ≥ 4 tooltip 显示 Gr/Ga/Re。
- **作物杆物品**:只能放在 CropSoilType(耕地/菌丝体/沙/灵魂沙)。

## 新设计

- **core(纯 Java)**:`ic2.core.crop.CropMath`(生长与三项地形公式,
  自定义 `CropRandom` 最小随机接口隔离 Minecraft 类型)、
  `CropProperties` record(tier/chemistry/consumable/defensive/
  colorful/weed)。
- **neoforge**:`crop/CropCard` 接口 + `WheatCropCard`/`WeedCropCard`;
  `CropBlock`(EntityBlock,age 在方块状态;左键拾取、右键收获、
  细杆造型、无碰撞)+ `WheatCropBlock`(age 0..7)/`WeedCropBlock`
  (0..4)子类静态属性——**构造器虚调用陷阱**:状态定义在 Block 父类
  构造器中构建,实例字段尚未赋值,属性必须走子类静态字段;
  `CropBlockEntity` 生长内核(tick 门控/地形/生长/存储衰减/杂草扩散/
  播种/收获/拾取/重置,ValueInput/ValueOutput 持久化);
  `CropSeedItem`(`CROP_SEED` 数据组件替代 NBT)+ `CropStickItem`
  (CropSoilType 限定);`registration/ModCrops`。
- **简化(文档化)**:群系湿度/养分加成取 0(legacy 经 env-proxy 群系
  分类,未迁移);湿度由湿润耕地与水罐存储驱动。stat 随机为
  `level.getRandom()`,测试以显式 ticker 驱动 `performTick(long)`。
- **资源**:crop_stick/wheat_crop/weed_crop 方块状态与横切模型、纹理
  自 legacy 逐字拷贝;物品定义/模型(crop_stick、crop_seed_bag);
  语言键全部已随 legacy 语言文件存在(`ic2.crop.seeds` 作为种子袋
  描述 id),零改动。创造页 NATURAL_BLOCKS。

## 测试证据

- core JUnit `CropMathTest`(6 项):盈余生长不重置、严重亏损能杀死
  低抗作物、高抗性免死、湿度/养分/空气公式逐值(含 legacy 整除语义
  (20+19)/20=1、fresh 预算 9 起步)。
- `crop_seed_bag_roundtrip`:组件编解码 + 卡片解析。
- `crop_plant_harvest`:播种→方块切换→七级成熟→多轮收获(单次高斯
  可合法滚零,断言跨 6 轮累积)→年龄回 2→拾取种子袋→回空杆。
- `crop_weed_growth`:种子袋拒种杂草;空杆转杂草后生长且 `isWeed`。
- IC2 与 GT 双模式 `runGameTestServer` 306 项全绿;core JUnit 110 项。

## 切片二:五张 IC2 作物卡与 base seed 播种(2026-09-10)

- **作物卡**:reed(芦苇,age 0..2,湿度加权 `h*1.2+n+0.8a`,age>0 即
  可收获,产物甘蔗×当前年龄)、flax(亚麻,线,humidity+nutrients+air,
  光照 ≥9)、hops(啤酒花,age 0..6,时长 600,收获后回 age 2,产物
  `ic2:hops`)、coffee(咖啡,age 0..4,权重 `0.4h+1.4n+1.2a`,分阶段
  时长(age2 ×0.5、age1 ×1.5)、age ≥3 开收获窗但 age 3 无产物、最优
  收获 age 2,产物 `ic2:coffee_beans`)、cocoa(可可,`canGrow` 要求
  存储养分 ≥3,时长 900/400 分段,age 2 收获,产物可可豆)。
- **base seed 播种**(legacy `registerBaseSeed`/`getBaseSeed`):
  `BaseSeed(card, size, growth, gain, resistance)`——甘蔗→芦苇
  (0,3,0,2)、可可豆→可可(0,0,0,0)、咖啡豆→咖啡(0,1,1,1);
  size 同时是播种年龄与消耗数量(size 0 不消耗,legacy quirk);
  手持产物对空杆右键播种(`CropBlock.useItemOn` 手持路径 +
  `rightClick(player, held)`)。
- **关键行为**:可可的养分门同样挡播种(legacy 须先对杆施肥——
  肥料交互随后续切片,测试以直接设置存储养分等价);
  年龄切换产生的新 BE 地形未初始化,负质量会触发 legacy 死亡掷骰——
  提供 `refreshTerrain`(对应 legacy `setCrop` 立即刷新语义)供
  生长循环与测试使用。
- 资源:5 个方块状态/横切模型/纹理逐字拷贝;无新增物品。

## 测试证据(切片二)

- `crop_base_seed_planting`:甘蔗播种芦苇(size 0 不消耗)、未注册
  产物拒绝、可可无养分拒绝播种且保持空杆。
- `crop_reed_age_gains`:多轮收获累计产出甘蔗(age 比例产物)。
- `crop_coffee_harvest_window`:分阶段时长下长到收获窗,窗口期收获
  无产物,成熟后多轮累计产出咖啡豆。
- `crop_cocoa_nutrient_gate`:无养分不生长,设置存储养分后生长。
- IC2 与 GT 双模式 `runGameTestServer` 310 项全绿;core JUnit 110 项。

## 切片三:12 张原版产物作物卡与 base seed(2026-09-10)

- **作物卡**:nether_wart(地狱疣,props (5,4,2,0,2,1),dropGainChance
  ×2.0,rootsLength 5,产物地狱疣;card tick 中灵魂沙在根区则
  `canGrow` 时 +100 生长点;雪下转化为 terra wart 因该卡未迁移而
  跳过,见「未验收」)、red/brown_mushroom(`CropBaseMushroom`,props
  (2,0,4,0,0,4),时长 200,产物自身蘑菇)、carrots/beetroots
  (`CropVanilla`,props (2,0,4,0,0,2)/(1,0,4,0,1,2),光照 ≥9,产物
  胡萝卜/甜菜,低统计拾取掉原版种子——甜菜掉甜菜种子而非产物)、
  potato(`CropPotato`,props (2,0,4,0,0,2),光照 ≥9,age ≥2 开收获带,
  满龄 5% 有毒土豆否则土豆,未满龄无产物)、6 种树苗
  (`CropBaseSapling`,props (3,1,0,4,4,0),maxAge 4,光照 ≥9,时长
  600/末段 150,收获回 age 3,产物原木 +25% 树苗,橡树另 25% 苹果)。
- **方块**:12 个 `CropBlock` 子类(age 上限 2/2/2/3/3/3/4×6)。
- **base seed**(legacy `registerBaseSeed` 第三参 size 均为 0,不消耗
  ——legacy 蘑菇注册的 `new ItemStack(BLOCK, 4)` 中 4 是物品堆数量
  而非 size):地狱疣/胡萝卜/土豆/甜菜种子/红菇/棕菇/6 树苗 →
  (0,1,1,1);可可等既有项不变。
- **BE/接口**:`CropCard.getRootsLength`(默认 1)、
  `CropBlockEntity.isBlockBelow`(沿根区向下扫描,遇空气停止)、
  `setGrowthPoints`(地狱疣卡片 tick 加点)。
- 资源:12 作物方块状态/横切模型/纹理自 legacy 逐字拷贝;无新增物品。

## 测试证据(切片三)

- `crop_nether_wart_soul_sand`:无灵魂沙 5 tick 生长点 <500,放置
  灵魂沙后 5 tick 增量 ≥500(每 tick +100),并长到满龄 2。
- `crop_potato_harvest_band`:age 2 进入收获带可收获、长到满龄 3
  可收获,多轮累计掉落土豆。
- `crop_mushroom_base_seed`:棕菇播种(size 0 不消耗 legacy quirk)、
  时长 200 下长到满龄 2、收获回蘑菇。
- `crop_sapling_gains`:橡树苗长到满龄 4、收获后回 age 3
  (`getAgeAfterHarvest`),多轮累计掉落原木。
- IC2 与 GT 双模式 `runGameTestServer` 314 项全绿;core JUnit 110 项。

## 切片四:十张花/瓜果/杂交作物卡与 terra wart(2026-09-10)

卡片:南瓜/西瓜(CropVanillaStem 基类:湿度加权 1.1/0.9、收获回
maxAge-1)、五色花(CropColorFlower 参数化:光 ≥12、收获掉对应染料、
回 age 2、age 2→3 时长 600)、Venomilia(满龄 5、age 4 碰撞施毒并回
age 3、收获掉 grin powder、age 4 且 Gr≥8 视为杂草、右/左键非潜行也
触发毒刺)、粘性芦苇(满龄 3 掉树脂、幼龄掉甘蔗×age、收获后 2/0 随机
回退、免疫踩踏、湿度 1.2/空气 0.8 加权)、terra wart(雪下每 tick
+100 生长点、灵魂沙下 1/300 转化回 nether wart、dropGainChance 0.8)。
nether wart 补上雪→terra wart 的 1/300 转化(legacy 对偶语义)。

- 方块 ×10(pumpkin/melon/dandelion/poppy/blackthorn/tulip/cyazint/
  venomilia/sticky_reed/terra_wart_crop),纹理自 legacy 逐字拷贝。
- base seed:南瓜籽/西瓜籽(size 0)、罂粟/蒲公英(**size 3**:种在
  age 3 并消耗 3 朵,legacy quirk)、terra wart 物品(size 0)。
- 新物品 `ic2:terra_wart`(ItemTerraWart:食用清除失明/反胃/饥饿/
  中毒/凋零/虚弱/缓慢/挖掘疲劳;辐射 ≤600t 直接清除,更长剂量减
  600t;食物属性 nutrition 0 / alwaysEat / Rarity.RARE)。
- 交互钩子补全(共享缺陷修复):此前卡片 `onEntityCollision` 定义了
  但从未被调用——本切片接线 `CropBlock.entityInside` → BE 踩踏判定
  (1/100 且 nextInt(40)>Re → 植株重置且下方变泥土),并补 legacy
  `onRightClick`/`onLeftClick` 卡片钩子(use=收获/attack=拾取,卡片可
  附加毒刺等副作用);`getAgeAfterHarvest`/`onEntityCollision` 签名
  补 crop 参数以支持粘性芦苇的随机回退与 Venomilia 的毒刺。
- `CropBlockEntity.setCrop`(legacy 单参 transformCropBlock 语义:保留
  年龄换卡并刷新地形)供双向转化使用。

## 测试证据(切片四)

- `crop_flower_dye_harvest`:4 朵罂粟播种消耗 3 朵、种下即 age 3、
  收获回 age 2,多轮累计掉落红色染料。
- `crop_pumpkin_stem` / `crop_melon_stem`:播种生长到满龄 3,收获掉
  南瓜/西瓜或西瓜片,收获回 age 2。
- `crop_venomilia_poison`:直接种在 age 4,收获掉 grin powder;猪碰撞
  后获得中毒效果且植株回 age 3。
- `crop_sticky_reed_resin`:age 1 收获累计掉甘蔗,满龄收获累计掉树脂。
- `crop_terra_wart_snow`:无雪 5 tick 生长点 <500,放雪后 5 tick 增量
  ≥500,长到满龄 2 并收获 terra wart。
- `crop_wart_snow_transmutation`:nether wart 雪下 9000 次掷骰内转化为
  terra wart 卡(1/300 分支)。
- `crop_terra_wart_cure`:食用清除中毒,900t 辐射剂量食用后缩短为
  ≤600t 而非清除。
- IC2 与 GT 双模式 `runGameTestServer` 322 项全绿;core JUnit 110 项。

## 切片五:六张矿质作物卡(2026-09-10)

legacy CropBaseMetalCommon(ferru/cyprium/stagnium/plumbiscus)与
CropBaseMetalUncommon(aurelia/shining)合并为参数化 `MetalCropCard`:
tier 6、满龄 3、根区 5 格、收获后回 age 1、收获即产物本身
(`cropDrop.copy()`,无种子产出)。共同语义:age < 2 自由生长,age == 2
(末段)必须根区扫描命中任一需求 tag 才能继续(`isBlockBelow(TagKey)`
新增重载,与方块版同样的"空气即停"扫描);差异:common 的
dropGainChance 减半(0.95^6/2),uncommon 保持 0.95^6;末段时长
common 2000 / uncommon 2200,早段 800 / 750。

- 需求 tag 组合:ferru=iron_ores+iron_blocks、cyprium=copper_ores+
  copper_blocks、stagnium=tin_ores+tin_blocks、plumbiscus=
  lead_ores+lead_blocks、aurelia=gold_ores+gold_blocks、shining=
  silver_ores+silver_blocks(原版三矿用 vanilla BlockTags,其余为
  新增 `Ic2BlockTags` c: 常量)。
- 新增 c: tag 数据:`c:lead_ores`/`c:tin_ores`(ic2 铅/锡矿及其
  deepslate 变体)、`c:iron_blocks`/`c:copper_blocks`/`c:gold_blocks`
  (原版矿块)、`c:tin_blocks`/`c:lead_blocks`/`c:silver_blocks`
  (ic2 矿块)。`c:silver_ores` 刻意不建:移植侧与 legacy 一样没有
  银矿方块(legacy 原版环境下该 tag 同样为空,银作物的矿石路径依赖
  其他模组;本移植中 shining 可经 `c:silver_blocks` 的 ic2 银块成熟)。
- 无 base seed(legacy 杂交产物,不可用物品播种);卡实例按
  ColorFlower 先例在 ModCrops 直接参数化,六个方块子类 AGE 0..3,
  纹理自 legacy 拷贝 age 0..3(aurelia/shining 的 `_4` 贴图是 legacy
  未使用的遗留,不迁移)。

## 测试证据(切片五)

- `crop_metal_ore_root_gate`:ferru 自由长到 age 2,无矿 300 tick
  停滞;下方放铁矿石后长到 age 3,多轮收获累计小撮铁粉,收获后回
  age 1。
- `crop_shining_uncommon_roots`:shining 播种,断言 uncommon 保持
  dropGainChance(>ferru 的减半值)、早段时长 750 对 800、末段 2200;
  下方放 ic2 银块(走 `c:silver_blocks`)后成熟,多轮收获累计小撮
  银粉,卡片按方块解析正确。
- IC2 与 GT 双模式 `runGameTestServer` 324 项全绿;core JUnit 110 项。

## 切片六:红小麦与食人植物(2026-09-10)

- **红小麦**(legacy CropRedWheat,maxAge 6,tier 6):只在光照
  5–10 的暗处生长(`lightLevel` 方块属性让满龄杆发出的 7 级光不会
  阻断自身收获后的再生长,因满龄不可再生长);满龄发出红石信号 15
  (`RedWheatCropBlock.isSignalSource/getSignal`,对应 legacy
  isRedstoneSignalEmitter/getEmittedRedstoneSignal)与 7 级光;
  dropGainChance 0.5;收获产物按 legacy 语义:邻块信号 ≤0 时小麦/
  红石各 50%,任一邻居供电则必掉红石;时长 600,收获回 age 1。
- **食人植物**(legacy CropEating,maxAge 5,tier 6):光照 >10;
  age ≥2 后末段生长需根区 5 格内熔岩(`isBlockBelow(LAVA)`);
  收获带为 age 3–4(`optimalHarvestAge = maxAge-2`),满龄 5 不可
  收获,带内收获掉仙人掌;根区 5。
- **食客行为**(age ≥1 的 tick):legacy customData "eaten" 标志
  持久化为 BE `eaten` 字段——喂食后的下一 tick 掉腐肉;AABB ±1 格、
  高 2 格内选随机活体(排除创造/旁观,创造玩家跳过),拖向中心
  (Δv×0.5、vy ≤ −0.05),`ic2:crop_eating` 伤害源(新 damage_type
  JSON 自 legacy 原样复制)按 (age+1)×2 扣血;玩家无 IC2 金属甲时
  附加缓慢 64t/amp 50、隐身 64t、失明 64t(新 `MetalArmorLike`
  标记接口对应 legacy IMetalArmor——防化服是橡胶不算金属甲,当前
  移植尚无金属甲物品,标记就位待青铜甲切片);进食且可生长时
  +100 生长点,播放 GENERIC_EAT 音效,每 tick 只咬一个。
- 群系加成(swamp/mountain 时长 ÷1.5)仍随 env-proxy 缺席取 0
  (文档化偏差);空气品质 ÷(1+air/10) 已移植。
- base seed:仙人掌 → 食人植物(size 0);红小麦无 base seed。
- 纹理 red_wheat 0..6、eating_plant 0..5 自 legacy 逐字拷贝。

## 测试证据(切片六)

- `crop_red_wheat_dim_light`:露天房间内石顶封出暗袋 + 6 格高处
  萤石得到带内光照,断言光 >10 抑制生长、5–10 允许生长;满龄红石
  信号 15(`Level.getSignal`,`absolutePos` 换算)、满龄方块
  `getLightEmission()==7` 而收获后归 0;多轮收获累计出小麦与红石,
  收获回 age 1;邻置红石块后必掉红石不掉小麦。
- `crop_eating_plant_lava`:仙人掌右键播种;亮光长到 age 2,无熔岩
  300 tick 停滞,放熔岩后长到 5;满龄不可收获,age 4 收获累计掉
  仙人掌;age 1 时咬猪(扣血、缓慢/失明/隐身),下一 tick 掉腐肉。
- IC2 与 GT 双模式 `runGameTestServer` 326 项全绿;core JUnit 110 项。

## 未验收 / 后续切片

- 杂草自然出现(空杆 1/100 掷骰)的实机观察;WeedEX 水罐/肥料/
  水化罐的右键交互。踩踏判定已接线(`entityInside`),待实机观察。
- 其余约 15 种作物卡(GenericCropCard ×15:blazereed/bobs/corium/
  corpse_plant/creeper_weed/diareed/egg_plant/ender_blossom/
  meat_rose/milk_wart/oil_berries/slime_plant/spidernip/tearstalks/
  withereed)、杂交/crossing base、作物分析器、Cropmatron、收割机、
  群系加成。

## 切片七:GenericCropCard 数据作物 ×15(2026-09-10)

- **参数化基类**(legacy GenericCropCard 对应移植):字段齐备的
  数据卡——drops 固定掉落 + specialDrops 特殊掉落掷轮(每次收获
  `nextInt(length*2+2)`,每个特殊项占一个槽位,单一特殊项即 1/4
  概率)、时长公式 `growthSpeed<200 ? tier*200 : tier*growthSpeed`、
  收获窗 `harvestSize>=2?harvestSize:maxSize-1`、收获后回
  `afterHarvestSize`(register() 的归一化语义保留);根区 tag 门
  (15 张卡均未用)与 `canCross`(age+2>maxSize)一并就位待杂交
  切片;掉落物品全部惰性 supplier(材料注册表后绑定)。
- **15 张卡逐字参数**:blazereed(火药+掷轮棒/硫粉)、bobs 浆果
  (绿宝石特殊)、corium(皮革,无特殊)、corpse_plant(腐肉+骨/
  骨粉×2)、creeper_weed(火药)、diareed(小撮钻石粉+钻石特殊,
  tier 12)、egg_plant(蛋+鸡肉/羽毛×3,时长 6×900,收获回 2)、
  ender_blossom(末影珍珠粉+珍珠×2/末影之眼)、meat_rose(粉红染
  料+四肉,时长 7×1500)、milk_wart(奶疣,时长 6×900,唯一
  base seed)、oil_berries(油莓,tier 9)、slime_plant(粘液球,
  收获回 2)、spidernip(线+蜘蛛眼/蛛网,时长 4×600)、tearstalks
  (恶魂之泪,tier 8)、withereed(煤炭粉尘+煤炭×2,tier 8)。
- **maxSize 与方块 age 上界的错位**:legacy Ic2CropType 的
  blazereed 族方块 age 属性上界为 3,而卡 setMaxSize(4)(egg_plant/
  milk_wart/oil_berries 为 3 对 2);legacy `setCurrentAge` 把超出
  钳到方块上界。新端 `CropBlockEntity.withCropAge` 按此补上钳制
  (从 age property 的 possible values 取上界),越界生长静默停在
  满龄;收获窗 maxSize-1 与方块满龄重合,行为与 legacy 等价。
- 15 个方块子类(11 个 AGE 0..3、3 个 AGE 0..2)、ModCrops 全量
  接线(方块注册、共享 CROP_ENTITY、cardFor/card(String)、
  milk_wart base seed size 0);资源 15 套 blockstate/model/纹理
  (legacy 0-based 纹理逐字拷贝)。

## 测试证据(切片七)

- `crop_generic_corium_drops`:corium 播种,getGains 断言恰为皮革;
  age 2 拒绝收获,满龄 3 收获掉皮革(按 26% 空手率循环至多 16 轮),
  回 age 1。
- `crop_generic_special_drops`:blazereed 长到方块满龄 3 后再多跑
  2500 tick 触发越界生长,断言钳制保持在 3 不崩;getGains 直接
  掷轮 200 次断言火焰粉始终在场且棒/硫粉都出现,egg_plant 150 次
  断言鸡肉与羽毛;egg_plant/slime_plant 收获回 2;spidernip/
  milk_wart/meat_rose/oil_berries/diareed 时长公式抽查;真实收获
  回 age 1。
- `crop_generic_milk_wart_base_seed`:奶疣物品 base seed 注册断言,
  右键播种,长到方块满龄 2(卡 maxSize 3 的钳制路径),多轮收获
  累计奶疣,回 age 1。
- IC2 与 GT 双模式 `runGameTestServer` 329 项全绿;core JUnit 110 项。

## 切片八:杂交与 crossing base(2026-09-10)

- **crossing base 状态**(legacy tile 字段改 blockstate 承载):空
  作物杆方块新增 `crossing_base` 属性(false→原杆模型,true→
  legacy `stick_upgraded` 模型/纹理逐字拷贝);种植作物时方块被
  换掉,标志随之消失,与 legacy tile 字段语义等价。右键持作物杆
  升级空杆(消耗 1,创造 instabuild 不耗);左键空 crossing base
  降级并掉落作物杆(创造顶层 PASS);base seed 与 `tryPlantIn`
  在 crossing base 上一律拒绝(legacy 消耗前先挡,物品不掉)。
- **attemptCrossing 全语义**:每作物 tick 1/3 概率门;四向邻株
  逐一过 `canGrow(本tile)`/`canCross(邻tile)`(legacy 默认
  age≥2,GenericCropCard 为 age+2>maxSize)与 d16 stat 门(基础
  4,Gr≥16/≥30 加成,Re≥28 加 27−Re);候选遍历全部 52 张注册卡
  (ModCrops.allCards 注册序,决定加权二分查找的平局),比率表
  `calculateRatioFor`:同卡 500,五项非 tier 属性差 Σ(2−|Δ|),
  共享 attribute(忽略大小写)+5,tier 差 >1 罚 2×diff、<−3 罚
  −diff,下限 0;total≤0 视为无候选(legacy 在此 nextInt(0) 会
  抛异常,新端守卫并注释)。stat 继承:邻株求和取均 + 每项
  `nextInt(1+2n)−n` 抖动,钳 0..31;成功后 transformCropBlock
  (新卡,age 0)并在同一 tick 内走生长。
- **attemptSpreading**:恰好 1 个水平 TileEntityCrop 邻居(空杆
  也计入)且邻卡可种可交、stat 门通过时,邻卡直接蔓延到 crossing
  base,三项 stat 精确复制无抖动;checkSpreadingAvailability 是
  legacy 死代码未移植。performTick 顺序保持 legacy 短路:有作物
  不杂交,crossing 失败才 spreading,成功当 tick 新作物即生长。
- **52 张卡 attributes 全量**:接口默认 canCross(age≥2)/
  getAttributes(空);GenericCropCard 15 卡逐字、专用卡 17 张、
  参数化类(花/蘑菇/原版产物/金属)构造透传;core CropProperties
  补 `getAllProperties()`(五项非 tier 值)供比率表使用。

## 测试证据(切片八)

- `crop_crossing_base_interactions`:右键 4 根作物杆断言消耗 1 且
  blockstate 置位;crossing base 上右键奶疣 base seed 拒绝且物品
  保留;左键降级掉落作物杆(计数 1);空杆左键无动作。
- `crop_crossing_breed`:四簇 crossing base 各围 4 株 wheat
  (10/10/10,age 3),循环 attemptCrossing(每簇至多 300 次,消掉
  1/3 门与 stat 门的随机性)至成功;断言每簇成卡、age 0、三 stat
  落在均值 10±抖动 4 的 [6,14] 区间,且至少一簇为 wheat(同卡
  比率 4×500 压倒性主导)。
- `crop_crossing_spread`:恰 1 邻 wheat(5/6/7,age 3)循环
  attemptSpreading 至成功,断言变 wheat、age 0、stat 精确复制
  5/6/7;负例三连:两邻居拒绝、邻株 age 1(canCross 不过)拒绝、
  唯一邻居为空杆拒绝。
- IC2 与 GT 双模式 `runGameTestServer` 332 项全绿;core JUnit
  110 项。CI run 34514309416(headSha d995bd3d)success。

## 切片九:作物分析器(2026-09-10)

- **物品**(legacy ItemCropAnalyzer):手持 10 万 EU/tier 2 缓冲
  (转移 128),UNCOMMON 稀有度,能量条显示沿用 ElectricItem。
  空手 `use` 打开手持 GUI(服务端 `openMenu`,槽位序号走
  varint 附加数据);`onItemUseFirst` 右键作物 tile(非潜行、
  服务端、tile 有卡)付 900 EU(`energyForLevel(2)`)播报 7 行
  系统消息:名称(内嵌 `ic2.crop.<id>`)、发现者、年龄、养分、
  水分、WeedEX、生长点数/周期;电量不足静默 PASS(legacy 语义:
  use 失败不提示)。报告以 `List<Component>` 返回供测试断言。
- **CropCard 展示面**:接口补 `getDiscoveredBy()`(本迁移无卡覆
  写,默认 "unknown")与 `desc(int)`(legacy 属性表折叠成两行:
  desc(0)=att[0]+", "+att[1],desc(1)=att[2]+", "+att[3],越界
  跳过),分析器 GUI 与未来图鉴共用。
- **手持菜单**(legacy ContainerAnalyzer/HandHeldCropAnalyzer):
  三真实槽——输入(8,7)仅收种子袋、输出(41,7)只出不进、电池
  (152,7)为 `SlotDischarge(tier)` 等价的放电门(需能按分析器
  tier 放出电量,空电池拒收,与 legacy simulate 判定一致)+玩家
  背包三排与快捷栏(176×223)。槽内容持久化到分析器栈
  `analyzer_contents` 数据组件(≤3 槽校验);`broadcastChanges`
  每 tick `tryScan`,槽位变更即回写;`stillValid` 校验手持同一
  栈,数字键换位被挡(MiningFilterMenu 同款)。关 GUI 物件留栈
  (26.1.2 removed() 不清槽容器,即 legacy 手持库存行为)。
- **tryScan 全语义**:输出占用/输入空/非种子→不动;scan≥4 免费
  挪到输出(不扣电不升级);否则按 `energyForLevel`(1→90/
  2→900/3→9000/default→10)从分析器自身扣费(legacy 电池槽只
  是放置门,不喂扫描),扣后 scan+1 并移到输出。
- **GUI**(legacy GuiCropAnalyzer,程序化背景无纹理):按扫描
  等级逐层揭示——0→UNKNOWN(8,37);≥1→作物名;≥2→Tier 罗马字
  (I..XVI,默认 0)+Discovered by(8,50/73/86);≥3→desc 两行
  (8,109/122);≥4→Growth/Gain/Resistance 标签与数值
  (118 列,色 0xAE26E6/0xEEC900/0x00CED1)。客户端直接读同步
  槽栈上的 CROP_SEED 组件与卡查询,无自建网络同步。
- **配方**:legacy shaped(AA /BCB/BDB:绝缘铜缆+红石×4+#c:glass
  +电路)转 `ic2:shaped`;新端无 c:glass tag,按 forge:glass
  1.20.1 内容新建(玻璃+tinted+16 染色玻璃);补上切片一遗漏的
  crop_stick 配方(S S/S S,4 棍出 2)——该行自切片一起一直
  pending 而物品早已实现,本次一并转正。

## 测试证据(切片九)

- `crop_analyzer_scan_ladder`:规格断言(10 万/tier 2/128);
  无电不动;10/90/900/9000 四级精确扣费逐级升 scan;满级免费挪
  且不扣电;输出占用拒绝;非种子袋拒绝。
- `crop_analyzer_report`:种 wheat 后(tile 已换成作物方块,重新
  取 BE)未电无报告;充电 1000 报告 7 行,余 100(900 EU 费)。
- `crop_analyzer_menu`:mock 玩家手持充电分析器开菜单;输入槽
  仅收种子袋/输出槽拒放/电池槽收充过 RE 电池拒泥土;塞入种子袋
  一次 broadcastChanges 即完成 10 EU 扫描并移到输出;组件回写
  (尾空槽裁剪后 2 槽,槽 1 为 scan 1 的袋)。
- IC2 与 GT 双模式 `runGameTestServer` 335 项全绿;registry/
  recipe catalog 翻正后 `progress.py` 重新生成并 `--check` 通过。
## 切片十:作物护理物品(2026-09-10)

- **水化罐**(legacy ItemHydrationCell→`HydrationCellItem`):10000
  次使用(`hydration_uses` 数据组件,早已注册待用);`useOn` 作物
  tile 走 `applyToCrop`:每次使用先记 1 次「开罐费」再灌,机器喂
  每次上限 180、手持无上限;全部用尽时罐消失(`shrink`),耐久条
  uses>0 显示、宽度 chargeLevel×13、色 `hsvToRgb(chargeLevel/3)`,
  高级 tooltip 显示 `item.durability`(仅 count==1)。legacy 无配方
  (registry 条目注明,仅创造/机器供给)。
- **除草铲**(legacy ItemWeedingTrowel→`WeedingTrowelItem`):
  stacksTo(1),`onItemUseFirst` 仅服务端:对 weed 卡 tile 掉落
  age+1 个 weed 材料并 `reset` 回作物杆(resetData 不清 storage
  字段,即 legacy 原义)。配方 shaped(铁锭 A A/ A /BAB+橡胶)转
  `ic2:shaped`。
- **TileEntityCrop.rightClick 重排为 legacy 顺序**:空杆+作物杆→
  crossing base;作物+肥料→`applyFertilizer(true)` 且无论成败都
  消耗 1(创造不耗)恒 true;水化罐→applyToCrop;水容器→
  applyFluidFromHand(WATER);WeedEx 流体容器→applyFluidFromHand
  (weed_ex);base seed;空手→crop.onRightClick。
- **tile 三 apply 方法**:`applyHydration`(上限 200)、
  `applyWeedEx`(手动 100/机器 150,fixedAmount 要求 space>amount)、
  `applyFertilizer`(≥100 拒;手动 +100、机器 +90),均带 simulate。
- **流体容器分支语义**:legacy 经典罐排液是整罐(真实排液恒返
  1000 并清罐)——新端先 simulate 排全量定大小,真实排液请求
  全部存量(非钳制后的 applied),再把结果喂给 tile,复现
  legacy 两步顶满;weedEx 分支同样二次入账。
- **26.1.2 关键坑**:`ItemAccess.forStack` 的 `StackItemAccess`
  拒绝交换底层物品(水罐→空罐正是物品交换),extract 恒 0;
  手持容器必须解析为玩家背包槽 access(`forPlayerSlot`,主手/
  副手判定),仅栈不在手中时(直调/测试)回退 forStack。
- **注册**:两物品入 ModCrops(TOOLS_AND_UTILITIES 创造栏),
  items/model 定义、legacy 纹理拷贝、中英 lang 键齐备。

## 测试证据(切片十)

- `crop_care_fertilizer`:种 wheat 后肥料一次顶满 100 并消耗;
  满仓再施肥仍消耗但不再加(legacy 无论成败都扣)。
- `crop_care_hydration`:水罐灌至 200 且罐变空罐(经手持槽
  交换);满 tile 拒水化罐且罐不动;干 tile 水化罐一次灌 200、
  uses=1+200。
- `crop_care_weed_ex_trowel`:空杆收 weed-ex 罐→storage 100(手动
  上限)罐变空罐;trowel 对 age 3 的 weed 掉 4 个 weed(UseOnContext
  直构)且回到作物杆。
- IC2 与 GT 双模式 `runGameTestServer` 338 项全绿;registry
  (hydration_cell/weeding_trowel)与 recipe(weeding_trowel)
  catalog 翻正后 `progress.py` 重新生成并 `--check` 通过。

## 切片十一:Cropmatron 作物监管机(2026-09-10)

- **CropmatronBlockEntity**(legacy TileEntityCropmatron):
  extends PoweredBlockEntity(10000 EU、tier 1 sink,不经
  UpgradeableBlockEntity——其处理循环假定 slot 2 是电池槽)。
  11 内容槽:7 肥料(0-6)+Weed-Ex 入/出(7/8)+水入/出(9/10),
  外加 4 升级槽(`kind().slots()`=15,与 UpgradeableBlockEntity
  同约定);双 MachineFluidTank 各 2000 mB(水/weed-ex 严格过滤),
  CombinedResourceHandler 对外 insert-only(extract 恒拒)。
- **扫描语义逐字迁移**:每 10 tick 且储能 ≥31 EU 时 scan():
  光标从 (-4,-1,-4) 起 x→z→y 推进,每步耗 1 EU;命中 CropBlock
  tile 时肥料(applyFertilizer(false) 机器剂量 +90、耗 1 肥料)、
  水(applyHydration 真实施加,按返回量排罐)、weed-ex(手动=false
  上限 150)各收 10 EU;非作物且湿度 <7 的耕地直接从水罐补水
  (min(罐量,7-湿度))并收 10 EU。
- **升级手动实现**(legacy 无 Overclocking 属性):refreshUpgrades
  统计升级槽 ENERGY_STORAGE(+10000 EU/个,energy.resize)与
  TRANSFORMER(+1 输入 tier,钳 1..4),仅
  suitable() 放行(Suitable 除 OVERCLOCKER 全收)。
- **菜单/界面**:MachineMenu CROPMATRON 分支(ex 罐位 49/67、
  水罐位 57/75、肥料 7 槽 8+i×18,shift-click 肥料→槽 0、流体
  容器→水入);CropmatronScreen extends MachineScreen,双
  FluidTankDisplay(menuValue 0-2 水、3-5 ex)+通用能量条。
- **注册与资源**:MachineKind 枚举驱动自动注册
  block/item/BE/menu;blockstate 6 朝向×active 双态、
  sides/bottom/top 纹理自 legacy 拷贝、loot(扳手掉自身)、
  shaped 配方(cBc/UMU/CCC:作物杆×4+电路+木箱+cell)转换;
  lang 键预存在(block.ic2.cropmatron)。
- **GameTest 陷阱**:BE 库存尺寸必须 `kind().slots()`(11 内容
  +4 升级=15),只传 11 会让 upgradeStart()==size 越界;机器施肥
  剂量是 +90 不是手动的 +100。

## 测试证据(切片十一)

- `cropmatron_serves_crop`:一次 scan 顶满杆 tile(养分 90/水
  200/weed-ex 150),耗 1 肥料、水罐 2000→1800、ex 罐
  1000→850、EU 400→369;第二次 scan 仅耗步进 1 EU。
- `cropmatron_hydrates_farmland`:湿度 0 的耕地一次 scan 补到
  7、罐 50→43、EU 100→89;已湿耕地再 scan 仅耗 1 EU。
- `cropmatron_containers_upgrades`:水桶→水罐 1000+空桶入输出
  槽;weed-ex 罐→ex 罐 1000+空罐入输出槽;对外 insert 错流体
  双向拒绝;ENERGY_STORAGE×2 → capacity 30000。
- IC2 与 GT 双模式 `runGameTestServer` 341 项全绿;registry
  (ic2:cropmatron 四条)与 recipe(shaped/cropmatron)catalog
  翻正后 `progress.py` 重新生成并 `--check` 通过。

## 未验收 / 后续切片

- 杂草自然出现(空杆 1/100 掷骰)与踩踏复位(`entityInside` 已
  接线)的实机观察。
- 肥料/WeedEX/水化罐右键与 Cropmatron 已交付(切片十/十一);
  余下作物收割机、群系加成(红小麦 swamp/mountain 与
  env-proxy),以及分析器/图鉴之外的卡信息展示面。
