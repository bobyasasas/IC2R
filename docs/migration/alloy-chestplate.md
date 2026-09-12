# alloy_chestplate 切片

legacy 蓝本：`ic2/core/item/armor/ItemArmorIC2.java` + `Ic2ArmorMaterials.ALLOY`（单胸甲件，
`Ic2Items.ALLOY_CHESTPLATE`，创造组 COMBAT）。解锁 `shaped/alloy_chestplate` 与
`shaped/alloy_chestplate_2` 两条同 result 不同排布的配方（3 alloy + 皮革胸甲 + 铁胸甲），
配方计数 771 → 773、pending 25 → 23。

## 材质（Ic2ArmorMaterials.ALLOY 1:1）

- 耐久乘数 50（26.1.2 `humanoidArmor` 按件型基数换算 → 胸甲 50×16=800）；
- 防御 boots 4 / legs 7 / chest 9 / head 4；
- 附魔能力 12、韧性 2.0、击退 0.0、铁装备音效；
- 修复材料 ic2:alloy（tag `ic2:repairs_alloy_armor`）。

## 实现

- `ic2.neoforge.item.AlloyArmorItem`：与 `BronzeArmorItem` 同范式——纯原版护甲行为，
  legacy `IMetalArmor#isMetalArmor` 恒真以 `MetalArmorLike` 标记复现。
- `ModArmor`：`ALLOY` ArmorMaterial（equipment asset `ic2:ic2_alloy`）+ `ALLOY_CHESTPLATE`
  注册（`p.humanoidArmor(ALLOY, ArmorType.CHESTPLATE)`）+ 创造页 COMBAT（legacy 一致）。
- 资源：`equipment/ic2_alloy.json`（humanoid/humanoid_leggings 双层均指向
  `ic2:ic2_alloy`）、贴图 `textures/entity/equipment/humanoid{,_leggings}/ic2_alloy.png`
  （legacy 仅 `ic2_alloy_layer_1.png`——alloy 无腿甲件，leggings 层资产复用同贴图只为
  引用完整）、items 定义 + item 模型 + 物品贴图 `item/armor/alloy_chestplate.png`。

## GameTest（AlloyChestplateTests 2 例）

| id | 覆盖 |
| --- | --- |
| alloy_chestplate_stats | 属性 ARMOR=9 / TOUGHNESS=2.0、maxDamage=800、REPAIRABLE 含 ic2:alloy、MetalArmorLike 标记 |
| alloy_chestplate_wear | mock player 穿戴后 ARMOR 属性 +9、护甲留驻胸槽（手动 transient 同步，mock 不 tick） |

IC2 模式隔离 2/2 绿（全量双模式见 STATUS）。
