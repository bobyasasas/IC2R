# 建筑泡沫(P13 爆炸链切片七)

legacy 基线:`ic2/core/block/misc/FoamBlock.java`(含 FoamType)、
`ic2/core/block/misc/WallBlock.java`、`ic2/core/block/misc/ConstructionFoamBlock.java`、
`ic2/core/item/tool/ItemSprayer.java`、`ic2/core/item/armor/ItemArmorCFPack.java`
(继承 `ItemArmorFluidTank`)、`ic2/core/ref/Ic2ArmorMaterials.java`(CF_PACK)。

## legacy 行为依据

- **泡沫方块**(FoamBlock,`ic2:foam`):双态 `type=normal(300)/reinforced(600)`,
  属性 noOcclusion、强度 0.01/抗爆 10、randomTicks、羊毛音效,无碰撞箱(可穿过),
  任何方式破坏均不掉落(legacy 无战利品表)。
- **硬化**:randomTick 概率 `1/(hardenTime×(16-光照)×20) × 4096/随机刻速度`,
  光照取自身与六邻域最大局部亮度(自身不挡光时)。normal 硬化为
  **浅灰色泡沫墙**(`WallBlock.DEFAULT_COLOR=LIGHT_GRAY`),reinforced 硬化为
  **强化石头**。手持沙子右键立即硬化并消耗 1 个沙子(仅消耗点击的手,创造不消耗)。
- **泡沫墙**(WallBlock ×16 色):完整方块,强度 3/抗爆 30,需正确工具挖掘,
  石音效,掉落自身;legacy 附带 Stainable/Retexturable(染料染色、遮蔽墙重纹理)
  ——属涂色器(涂色切片)范围。无合成配方(创造栏/泡沫产物获取)。
- **喷枪**(ItemSprayer,`ic2:foam_sprayer`,不可堆叠):容量 8,000 mB,仅装建筑泡沫
  流体;100 mB 喷 1 块。普通模式单次最多 **10 块**(NBT mode=0),单块模式 1 块
  (mode=1,legacy 用 M 键模式键切换并提示 `ic2.tooltip.mode`)。
  目标判定:命中的是原版脚手架 → 就地覆盖(破坏脚手架掉落 + 放泡沫)并沿
  脚手架网络蔓延;其余目标 → 在点击面相邻位置 BFS 泛洪(四邻,排除玩家朝向的
  反方向,防止泡沫流回玩家脚下),只经过可替换位置。液体优先从**胸甲槽 CF 背包**
  抽取,不足再从喷枪自身抽取;无液体返回 FAIL。tooltip 显示余量
  (`item.ic2.foam_sprayer.tooltip.content`)。
- **CF 背包**(ItemArmorCFPack,`ic2:cf_pack`):胸甲槽护甲,材质 CF_PACK
  (胸甲 8 甲、韧性 2.0、耐久乘数 0=不磨损、铁装备音、不可修复),流体罐
  80,000 mB 建筑泡沫,耐久条显示液位,HSV 色相随液位,创造栏给满/空两态,
  tooltip 显示内容物。
- **湿泡沫流体方块**(ConstructionFoamBlock,`ic2:fluid_block_construction_foam`):
  生物进入获得缓慢 IV(280 tick,等级 2)。
- **配方**(legacy JSON):
  - `shaped/foam_sprayer.json`:iron_casing×2 + facade_cell(斜线排列)。
  - `shaped/cf_pack.json`:foam_sprayer + circuit + iron_casing + facade_cell×3
    (喷枪作为材料被消耗,legacy 原行为)。

## 移植实现

- `ic2.neoforge.world.FoamBlock`:双态 EnumProperty,randomTick 公式逐项对照
  (26.1.2 `GameRules.RANDOM_TICK_SPEED` / `state.getLightDampening()`),
  `useWithoutItem` 消耗沙子(26.1.2 该钩子无手部上下文,按主手→副手顺序,
  创造跳过消耗),无碰撞箱,`.noLootTable()`。
- `ic2.neoforge.registration.ModFoam`:泡沫方块 + 16 色墙(plain Block,
  强度/音效对照)+ 喷枪 + CF 背包;创造栏分配:墙+泡沫→BUILDING_BLOCKS
  (legacy GENERAL)、喷枪→TOOLS_AND_UTILITIES、背包→COMBAT。
- `ic2.neoforge.item.FoamSprayerItem`:模式存 `spray_mode` 数据组件(0/1);
  泡沫区域 BFS 带 `Predicate<BlockPos>`(脚手架目标按 `is(SCAFFOLDING)` 蔓延,
  普通目标按 `canBeReplaced()` 蔓延)——legacy canPlaceFoam 的 target 分支语义;
  液体量存 `fluid` 数据组件(FluidStackTemplate,仅湿泡沫)。注:26.1.2 的
  `Item.use` 返回 `InteractionResult` 密封接口,无 InteractionResultHolder。
- `ic2.neoforge.item.CFPackItem`:26.1.2 护甲为纯 `Item` +
  `Properties.humanoidArmor(ArmorMaterial, ArmorType.CHESTPLATE)`;ArmorMaterial
  record 内联(0 耐久乘数/胸甲 8/0 附魔/铁装备音/2.0 韧性);装备资产
  `assets/ic2/equipment/cf_pack.json` + `textures/entity/equipment/humanoid/cf_pack.png`
  (legacy `models/armor/ic2_cf_pack_layer_1.png` 复制);耐久条与 tooltip 对照。
- `ic2.neoforge.fluid.FoamTankHandler`:ItemAccess 流体罐(容量参数 8,000/80,000),
  仅接受湿泡沫;为喷枪与背包注册 `Capabilities.Fluid.ITEM`,桶/单元交互走原生
  FluidUtil。
- `ic2.neoforge.world.ConstructionFoamBlock extends LiquidBlock`:
  ModFluids 仅对 CONSTRUCTION_FOAM 家族换用该方块;entityInside(新签名含
  `InsideBlockEffectApplier` 与 `isPrecise`)给生物缓慢 IV;26.1.2 实体入块
  默认 `Shapes.block()` 命中,行为与 legacy 一致。

## 有意差异

1. **模式切换**:legacy 用自建 M 键位(客户端→服务端同步),移植端尚无键位系统,
   改为**潜行 + 空手右键**切换模式(潜行时 useOn 直接 PASS,与 legacy 键下行为
   对应);提示改走 `sendSystemMessage`。
2. **电缆覆盖分支**:legacy 喷枪可覆盖 IC2 电缆(toFoamState 泡沫电缆变体),
   电缆泡沫属电缆系统切片;当前命中电缆按普通目标处理(泡沫放在相邻面)。
3. **遮蔽墙/染色**:WallBlock 的 Stainable/Retexturable、TileEntityWall、
   obscured_wall 随涂色器切片迁移;本次交付 16 色墙本体。
4. **沙子消耗手部**:legacy 只消耗点击手;26.1.2 `useWithoutItem` 无手部参数,
   按主手→副手顺序检查。
5. **randomTick 硬化无确定性 GameTest**:光照依赖概率在 GameTest 时长内不可靠,
   以沙子速凝(同一条硬化路径的确定性入口)覆盖两种 FoamType 的硬化产物。

## 配方解锁

- `shaped/foam_sprayer.json`、`shaped/cf_pack.json` 随本体交付(转换脚本自动翻转)。
- 附带解锁 `shaped/remote.json` 与 `shaped/remote_from_frequency_transmitter.json`
  ——遥控器本体已于早前切片实现,其配方此前因物品缺失挂起,本次转换脚本重新
  评估后翻转(共 2 条,663→665/796)。

## 测试证据

`neoforge/src/gameTest/java/ic2/neoforge/test/FoamTests.java` ×7(heat_room 隔离结构):
十块泛洪与液体扣除、单块模式(潜行切换)、脚手架就地覆盖、沙子速凝两种产物
(含消耗)、背包优先供液(80,000→79,000)、湿泡沫流体缓慢效果、喷枪流体过滤。

## 遗留(明确未验收)

- 真实客户端:喷枪/背包 GUI 内外观、喷涂动画/音效手感、护甲穿戴渲染
  (待测试.md §44)。
- 电缆泡沫变体(6 材质 + 绝缘系)随电缆切片;涂色器与遮蔽墙随涂色切片;
  辐射效果随 P16。
