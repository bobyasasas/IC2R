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

## 未验收 / 后续切片

- 杂草自然出现(空杆 1/100 掷骰)的实机观察;WeedEX 水罐/肥料/
  水化罐的右键交互;踩踏破坏。
- 其余 50 种作物卡、杂交/crossing base、作物分析器、Cropmatron、
  收割机、群系加成。
