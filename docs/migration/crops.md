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

## 未验收 / 后续切片

- 杂草自然出现(空杆 1/100 掷骰)的实机观察;WeedEX 水罐/肥料/
  水化罐的右键交互。踩踏判定已接线(`entityInside`),待实机观察。
- 其余约 17 种作物卡(红小麦、食人植物、GenericCropCard ×15)、
  杂交/crossing base、作物分析器、Cropmatron、收割机、群系加成。
