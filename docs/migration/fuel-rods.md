# 核燃料棒物品链(P12 燃料棒簇)

2026-09-09 切片。迁移 legacy `ItemReactorUranium`/`ItemReactorMOX` 的物品层与
`ItemNuclearResource` 耗尽棒:铀/钚 × 单/双/四联共 6 种燃料棒 + 6 种耗尽棒,
解锁 14 条 pending 配方(canner 装罐 2、shaped 双/四联 6、centrifuge 耗尽处理 6)。
配方转换 610 → 624,pending 186 → 172。

## 旧行为依据

- 燃料棒:`ItemReactorUranium(properties, cells)`(铀,持续 20,000)与
  `ItemReactorMOX(properties, cells)`(钚,持续 10,000),cells 1/2/4;
  `AbstractDamageableReactorComponent` 的 NBT `use` = 已消耗量(新鲜为 0),
  tooltip "Durability: 剩余/上限"、耐久条按 use 比例。
- 双/四联合成要求**新鲜棒**:`"data": {"use": 0}` 配料键(用过的不可再组装)。
- 耗尽棒为普通物品(`ItemNuclearResource`,辐射行为随 P16),离心处理为
  small_plutonium 产出链。
- 装罐:空燃料棒(`ic2:fuel_rod`)+ 铀 → 铀燃料棒(canner_bottle);MOX 同理
  (fill 为 mox 物品)。

## 新设计

- `ModDataComponents.REACTOR_USE`(int ≥ 0):反应堆组件的损耗计数,缺省 0
  (新鲜)。`FuelRodItem(properties, cells, duration)` 注册缺省组件值 0,
  tooltip/耐久条与 legacy 一致;`cells()`/`duration()` 供 P12 反应堆脉冲
  逻辑使用。
- 配料转换:recipes.py 的 `ingredient()` 接受 legacy `"data": {"use": 0}`,
  转成 `neoforge:components` 配料(`ic2:reactor_use: 0`)——因物品注册了
  缺省组件值,新鲜棒可匹配、已用棒(use>0)不匹配;非零 use 保持 pending
  (legacy 数据集中不存在该形态)。
- `ModReactorItems` 注册 12 个物品(创造页 INGREDIENTS);reactor.py 资源
  管线(items 定义/模型/纹理/双语名)。
- 堆内脉冲/辐射(`processChamber`/`acceptUraniumPulse`)随 P12 反应堆本体
  迁移,不在本切片。

## 测试证据

- `FuelRodTests.freshRodsCraftDualRod`:经真实数据包配方
  (`ic2:shaped/dual_uranium_fuel_rod`)匹配两根新鲜铀棒+铁板并合成出双联棒。
- `usedRodsAreRejected`:use=100 的棒使配方不匹配(组件配料生效)。
- `depletedRodsChainToCentrifuge`:耗尽四联钚棒已注册且其离心配方已加载。
- IC2 与 GT 双模式 `runGameTestServer` 209 项全绿。

## 未验收 / 后续

- 堆内行为(脉冲、发热、耗损递增、钚增益)随 P12 反应堆本体;辐射伤害随
  P16。客户端外观随实机测试(待测试.md 第 23 节)。
- lithium_fuel_rod/tritium/depleted_isotope(锂氚循环)随反应堆切片迁移。
