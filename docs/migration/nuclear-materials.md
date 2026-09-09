# 核材料(P09)

更新日期:2026-09-08。

## 旧行为依据

- `legacy/.../item/ItemNuclearResource.java`(可堆叠核资源;携带辐射依赖防化服)
- `legacy/.../ref/Ic2Items.java` 的 `uranium`/`uranium_235`/`uranium_238`/`plutonium`/
  `small_plutonium`/`small_uranium_235`/`small_uranium_238`/`mox`/`uranium_pellet`
- 离心配方 `uranium_to_uranium_238`(minHeat 4000)、`crushed/purified_uranium_to_small_uranium_235`
  (minHeat 3000)、`rtg_pellet_to_plutonium`(minHeat 5000)
- 压缩配方 `small_plutonium_to_plutonium`、`small_uranium_235_to_uranium_235`(9→1)
- 组装/分解 `uranium`、`uranium_2`、`plutonium`、`shapeless/small_plutonium`、`mox_1..4`

旧语义:核材料为矿石精炼与反应堆副产物;离心机需达到配方热量门槛(预热耗 EU)才能加工;
小撮与整锭按 9:1 互转;MOX 由铀 238/铀与钚/小撮铀 235 组合。

## 新实现

- 九个材料物品注册于 `registration/ModReactorItems.java`(可堆叠;辐射伤害行为依赖 P16
  防化服,暂不实现,代码注释与本文记录;燃料棒装罐链随 P12)。
- 配方随物品注册自动转换(550/796),含全部热量门槛离心配方;`rtg_pellet` 配方因此解锁,
  `centrifuge/rtg_pellet_to_plutonium` 回收链闭环。
- 转换器新增**:输出数量超过物品模板上限 99 时拆分为多条模板**(如 112 → 99+13),
  消费端同槽堆叠(槽上限 64 时自然分槽),机器行为与旧版总量守恒一致。

## 测试证据

- GameTest `NuclearTests`:
  - `nuclear_uranium_centrifuge`:20 铀 → 铀 238×112(跨两槽,单槽上限 64)+ 铀 235×7,
    预热至 4000 后加工;
  - `nuclear_rtg_pellet_centrifuge`:废 RTG 燃料丸 → 3 钚 + 54 铁粉(minHeat 5000)。
- 配方加载清单测试(`loaded_recipes`)覆盖全部已转换核配方。
- IC2 与 GT 两种能量模式 155 项 GameTest 全部通过(2026-09-08)。

## 未验收范围

- 辐射行为(P16)、燃料棒与装罐(P12)、铁栅栏与磁化机(随 P08)、多人验收(M16)。
