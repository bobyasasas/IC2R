# 辐射链与防化服(P16)

2026-09-10 切片。迁移 legacy 辐射伤害链:`Ic2Potion.radiation`(辐射
病药水效果)、`ic2:radiation` 伤害类型、`IHazmatLike`/`ItemArmorHazmat`
防化套装与 `EventHandler` 的全套豁免钩子;并把核爆辐射环
(`Ic2Explosion` 核型尾部)与反应堆 70% 热辐射分支接入新端——这两处
此前因"辐射随 P16"而留空(新 `Ic2Explosion` 中的 stub 注释)。

## 旧行为依据(legacy)

- **辐射病药水**(`Ic2Potion.radiation`,HARMFUL,色 5149489):
  `isDurationEffectTick` 每 `25 >> amplifier` tick(amplifier ≥ 5 时
  rate 0 → 每 tick);`applyEffectTick` 以 `ic2:radiation` 伤害类型造成
  `amplifier/100 + 0.5` 伤害。
- **防化判定**(`IHazmatLike.hasCompleteHazmat`):四个护甲槽都持有
  `addsProtection()=true` 的 HazmatLike 物件,且任一槽
  `fullyProtects()`=true 即算完整;全套无 fullyProtects 件时四件齐备
  即完整。legacy `ItemArmorHazmat` 三件(头盔/胸甲/护腿)均为
  addsProtection=true,第四件是 **rubber_boots**——legacy 同样以
  `ItemArmorHazmat(FEET)` 注册,四件可达,无缺陷。
- **核爆辐射环**(`Ic2Explosion`,isNuclear 且 radiationRange ≥ 1):
  以爆心 ±radiationRange 的 AABB 收集 `Mob`(不含玩家,玩家由防化钩
  子覆盖),无完整防化的生物按距离施加:饥饿
  `(int)(120×(range−dist))` tick(amplifier 0)与辐射病
  `(int)(80×(range/3−dist))` tick;负值跳过。
- **反应堆热辐射**(`calculateHeatEffects` ≥ 70%):对堆芯 −3…+4 AABB
  内全部 LivingEntity(无防化豁免)直接造成
  `(int)(nextInt(4)×hem)` 点 ic2:radiation 伤害。
- **玩家全套豁免**(`EventHandler.onEntityAttacked`):玩家在完整防化
  下,火/电(ic2:electricity)/辐射(ic2:radiation)伤害归零,火 case
  同时灭火。

## 新设计

- `effect/RadiationEffect`(MobEffect 子类)+ `registration/ModEffects`
  (DeferredRegister,Registries.MOB_EFFECT);26.1.2 API:
  `applyEffectTick(ServerLevel, LivingEntity, int)`、
  `shouldApplyEffectTickThisTick(duration, amplifier)`;伤害源经
  `level.damageSources().damageTypes.getOrThrow(ic2:radiation)`。
- `item/HazmatLike`(接口 + 静态 hasCompleteHazmat;26.1.2 槽类型为
  `EquipmentSlot.Type.HUMANOID_ARMOR`,非旧名 ARMOR)、
  `item/HazmatArmorItem`(addsProtection=true)、
  `item/HazmatHelper`(hazmatAbsorbs + LivingIncomingDamageEvent 玩家
  豁免钩子,`setAmount(0)`+灭火)。
- `registration/ModArmor`:四件套(hazmat_helmet/chestplate/leggings +
  rubber_boots),创造页 COMBAT;材质 legacy `Ic2ArmorMaterials.HAZMAT`
  (防御 3/6/8/3、韧性 2.0、耐久 0、附魔 0、皮革装备音)——耐久 0 与
  附魔 0 沿用 cf_pack 的手工组件装配(26.1.2 拒绝零值组件);装备资产
  `assets/ic2/equipment/ic2_hazmat.json`(humanoid + humanoid_leggings
  双层,纹理取 legacy ic2_hazmat_layer_1/2)。含 slotType 说明:每件
  的 Equippable slot 与 legacy EquipmentSlot 一致。
- `Ic2Explosion.applyNuclearRadiation(ServerLevel, Vec3, int)`:核型爆
  炸尾部调用,GameTest 可直接驱动;玩家由 HazmatHelper 钩子覆盖
  (legacy Mob 限定保持)。
- `NuclearReactorBlockEntity.meltDown` 补 ≥ 0.7 分支(legacy AABB
  −3…+4 的非对称保持)。
- 资源:`data/ic2/damage_type/radiation.json`(legacy 原样)、
  `textures/mob_effect/radiation.png`、物品纹理/模型/items 定义 ×4、
  装备资产;语言键(item.ic2.hazmat_* / rubber_boots /
  effect.ic2.radiation)已随 legacy 语言文件存在,零改动。
- 配方:hazmat_helmet(橡胶×4+玻璃+铁栏杆+橙色染料)、chestplate、
  leggings、rubber_boots(橡胶×5+羊毛标签)共 4 条 shaped 转换,
  683 → 687/796。

## 测试证据

- `radiation_hazmat_set`:四件齐→完整;脱靴→不完整;裸猪→不完整。
- `radiation_effect_damage`:amplifier 0 的 tick 节律(25 tick 一次、
  24 跳过);施加后 30 tick 内猪掉血。
- `radiation_explosion_ring`:爆心 1.1 格裸僵尸同时中饥饿+辐射病,
  3 格外只有饥饿(超出 range/3),着完整防化的僵尸完全豁免
  (中心用 absolutePos 绝对坐标——结构放置在数百万格外,局部坐标
  判定恒失,踩坑记录)。
- `reactor_heat_radiation`:堆芯 7,000 热(70%)+ 邻格猪 → 一轮内
  受辐射伤害或进入受击无敌帧(legacy `nextInt(4)` 可为 0 伤害,断言
  取或)。
- IC2 与 GT 双模式 `runGameTestServer` 303 项全绿。

## 未验收 / 后续

- 核材料物品(原铀/钚)手持背包辐射(ItemNuclearResource)、燃料棒
  拿取辐射(ItemReactorUranium)的物品交互分支。
- 特斯拉(Tesla)电击的防化豁免随特斯拉切片。
- 防化套装实机穿戴外观/吸收表现(待测试.md §50)。
- 作物农业(P15)独立切片;量子套装(quantum suit)未迁移。
