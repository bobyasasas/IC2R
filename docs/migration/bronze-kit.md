# Bronze 工具/护甲切片:9 物品注册与 9 条配方解锁

2026-09-11 切片(配方清收后续,pending 清收路线)。注册 legacy
`Ic2Items` 的青铜工具 ×5(镐/斧/锹/锄/剑)与青铜护甲 ×4(盔/胸/腿/靴),
解锁 shaped/bronze_* 9 条配方。

## 旧行为依据(legacy)

- `Ic2ToolMaterials.BRONZE`:等级 2、耐久 350、速度 6.0、伤害加成 2.0、
  附魔 affinity 14(构造体内写死)、修复物 bronze ingot。
- 工具注册参数(与原版同类同参):
  `Ic2Pickaxe(BRONZE, 1, -2.8F)`、`Ic2Axe(BRONZE, 6.0F, -3.1F)`、
  `ShovelItem(BRONZE, 1.5F, -3.0F)`、`Ic2Hoe(BRONZE, -2, -1.0F)`、
  `SwordItem(BRONZE, 3, -2.4F)`。Ic2Pickaxe/Ic2Hoe/Ic2Axe 均为原版类
  纯直通,无附加行为。
- `Ic2ArmorMaterials.BRONZE`:耐久 ×15(11/16/15/13 基数)、防御
  头 2/胸 6/腿 5/靴 2、附魔 9、铁装音效、韧性 0、击退 0、修复物
  bronze ingot。
- `ItemArmorIC2` = 原版 ArmorItem + `IMetalArmor#isMetalArmor` 恒 true。

## 新移植(26.1.2 范式)

- 工具:`ToolMaterial` record
  (`BlockTags.INCORRECT_FOR_IRON_TOOL`, 350, 6.0F, 2.0F, 14,
  `#ic2:repairs_bronze_tool`)+ 原版 Properties 助手
  (`p.pickaxe(...)`/`p.sword(...)`)与 `AxeItem`/`ShovelItem`/`HoeItem`
  构造,数字逐字对齐 legacy。
- 护甲:`ArmorMaterial(15, {BOOTS:2,LEGGINGS:5,CHESTPLATE:6,HELMET:2},
  9, ARMOR_EQUIP_IRON, 0, 0, #ic2:repairs_bronze_armor, ic2:ic2_bronze)`
  + `Properties.humanoidArmor(material, ArmorType)`(自动承载
  耐久/属性/ EQUIPPABLE/音效);`BronzeArmorItem extends Item implements
  MetalArmorLike`——legacy `IMetalArmor` 标记的 port 等价物,现有消费方
  为吞食植物状态效果豁免(EatingPlantCropCard)。
- 修复标签:`data/ic2/tags/item/repairs_bronze_tool.json` 与
  `repairs_bronze_armor.json`,values 均为 `ic2:bronze_ingot`。
- 资源:items/*.json 客户端定义 ×9、models/item ×9(工具 parent
  `ic2:item/tool/default`、护甲 parent `ic2:item/default`)、
  equipment/ic2_bronze.json(humanoid+humanoid_leggings 两层)、
  legacy 贴图照搬(工具 5 + 护甲 4 + 层贴图 2 映射到
  textures/entity/equipment/humanoid[/_leggings]/ic2_bronze.png)。
- lang 早已预存;创造页:工具 ×4 → TOOLS_AND_UTILITIES、剑 → COMBAT
  (ModTools)、护甲 ×4 → COMBAT(ModArmor)。

## 配方解锁

recipes.py 重跑:registry-catalog 9 条翻 implemented 后 registered 集合
收编,shaped/bronze_{pickaxe,axe,shovel,hoe,sword,helmet,chestplate,
leggings,boots} 9 条 converted(732→741),成分 `#c:ingots/bronze`+
`minecraft:stick`,加载清单同步 741。registry-catalog 附 per-item
evidence 与 legacy 材质注记。

## GameTest(双模式 411=409+2 全绿)

- bronze_tools:bronze_pickaxe 配方 matches/assemble;`DataComponents.TOOL`
  非空 + maxDamage 350(镐/剑)。
- bronze_armor:bronze_helmet 配方 matches;MetalArmorLike 标记 ×4;
  maxDamage 165/240/225/195(helmet/chestplate/leggings/boots,= 基数
  ×15)。

## 待人工/未验收

- 附魔 affinity 14、修复物在铁砧/砂轮上的实机表现未测(GT 断言组件与
  耐久);
- 护甲穿戴实机观感(层贴图渲染)与配方书分组属实机人工验收项。
