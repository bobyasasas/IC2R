# 逐步回充配方(gradual)与冷凝器(RSH/LZH)

2026-09-09 切片。迁移 legacy `GradualRecipe`(自定义合成配方类型)与
`ItemReactorCondensator`(RSH 20,000 / LZH 100,000 热量存储反应堆组件),
3 条 `ic2:gradual` 配方。

## 旧行为依据

- 冷凝器 `use` 字段 = **已吸收的热量**(新鲜为 0);在反应堆中 `alterHeat`
  吸热直至上限(P12 反应堆本体迁移后生效)。
- gradual 合成配方 = **放气回充**:1 个冷凝器 + N 个指定材料(每占一格算
  1 个),把已存热量按 `amount × N` 排掉;已空(`use ≤ 0`)或放不完时
  返回空(不可合成);`hidden` 控制配方书展示。
  - RSH + 红石:每次放 10,000;LZH + 红石:5,000;LZH + 青金石:40,000。
- tooltip 显示 `ic2.reactoritem.durability`(剩余容量/上限),耐久条按
  已存热量比例绘制。

## 新设计

- `CondensatorItem`(注册于 `ModReactorItems`,INGREDIENTS 创造页):存热用
  既有 `ic2:reactor_heat` 数据组件(缺省 0),与 `ReactorHeatItem` 同组件
  不同用途;tooltip 与耐久条按 legacy 规则。
- `GradualRecipe implements CraftingRecipe`(配方类型 `ic2:gradual`,
  序列化器注册于 `ModProcessingRecipes`):与 26.1.2 原生合成管线集成,
  `assemble` 逐格校验冷凝器与放热材料,返回带更新后 `reactor_heat` 的
  单个冷凝器;非匹配材料/已空/无材料一律空结果。`placementInfo` 为
  NOT_PLACEABLE、`showNotification` 关闭(配方书不展示,与 legacy
  `hidden`/`isSpecial` 等效)。
- 配方 JSON:原料与冷凝器均为物品 id 直写,`amount`/`hidden` 透传;
  recipes.py 增加 `ic2:gradual` 分支。转换 604 → 607。
- 资源:`reactor.py` components 列表加两物品(items 定义/模型/纹理/双语名)。

## 测试证据

- `GradualRecipeTests.ventsStoredHeat`:RSH 存 15,000 + 1 红石 → 放 10,000
  余 5,000,产物为单个原物品。
- `partialVentKeepsRemainder`:LZH 存 45,000 + 1 青金石 → 余 5,000。
- `freshCondensatorIsRejected`:已空冷凝器与错误材料(钻石)均拒绝合成。
- IC2 与 GT 双模式 `runGameTestServer` 203 项全绿。

## 未验收 / 后续

- 反应堆内吸热行为(`canStoreHeat`/`alterHeat`)随 P12 反应堆本体迁移;
  本切片仅覆盖物品、存热组件与回充配方。
- 配方书实际展示(默认隐藏)与工作台摆放随实机测试;记录于
  `/home/codex/minecraft/待测试.md` 第 21 节。
