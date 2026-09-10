# M14／P17：可选模组集成（JEI、Jade、AE2）

状态：进行中（2026-09-10，M14-a 切片交付）。本文记录目标版本评估证据、集成策略与已验证边界。

## 目标版本评估（2026-09-10 取证）

| 模组 | 对 MC 26.1.2 NeoForge 的目标版本 | 证据 |
|---|---|---|
| JEI | **29.37.0.98**（2026-09-08 发布） | maven `https://maven.blamejared.com/mezz/jei/jei-26.1.2-neoforge/maven-metadata.xml`（latest 29.37.0.98）；API 构件 `jei-26.1.2-common-api` / `jei-26.1.2-neoforge-api` 同版本 |
| Jade | 26.1.10+neoforge（2026-08-15 发布） | Modrinth 项目 jade 版本列表（game_version 26.1.2，loader neoforge） |
| AE2 | 26.1.11-beta（2026-08-25 发布，beta） | Modrinth 项目 `ae2`（slug 即 ae2，applied-energistics-2 会 404）版本列表 |

结论：三者均有 26.1.2 NeoForge 目标。AE2 为 beta 且 legacy IC2 2.10.39 与 AE2 无双向集成点（无 AE2 专用桥接类），P17 对 AE2 的验收项即"版本确认、无需集成面"，除非后续出现明确集成需求。

## 集成策略

1. **构建**：`neoforge/build.gradle` 增加 blamejared maven 仓库，JEI 两个 API 构件以 `compileOnly` 引入。运行时 classpath 永远不含 JEI——`runGameTestServer`（无 JEI）全绿即无依赖启动证明。
2. **插件发现**：JEI 通过 `@JeiPlugin` 注解扫描插件类；模组内没有任何其他代码引用 `Ic2JeiPlugin`，JEI 不在时该类从不加载，不会有 NoClassDefFoundError。
3. **客户端边界**：JEI 全部类位于 `ic2/neoforge/client/interop/jei/`，满足 `verify_artifact.py` 的"非 client 包不得引用 net/minecraft/client"专服边界。
4. **配方来源**（26.1.2 实测）：客户端 `Level` 只暴露 `RecipeAccess`（无完整配方表），JEI 自身在 NeoForge `RecipesReceivedEvent`（bytecode：`JustEnoughItemsClient.onRecipesReceivedEvent` → `event.getRecipeMap()` → `Internal.setClientSyncedRecipes` → 启动插件加载）内启动。因此 `ClientRecipeCache` 用 dist 判定（`FMLEnvironment.getDist().isClient()`，注意 26.1 的 FML 已无 `dist` 公有字段）在 `IndustrialCraft` 构造时注册同一事件的监听，把 `RecipeMap` 快照交给插件的 `registerRecipes` 使用。
5. **API 形状**（JEI 29.x 与旧版差异大，均以 API jar 反汇编/源码核实）：插件入口 `IModPlugin.getPluginUid()` 返回 `net.minecraft.resources.Identifier`；类别基类 `AbstractRecipeCategory<T>(IRecipeType, Component, IDrawable icon, w, h)`，只须覆写 `setRecipe`（可覆写 `draw`）；配方类型用 `mezz.jei.api.recipe.types.IRecipeType.create(ns, path, class)`（旧 `mezz.jei.api.recipe.RecipeType.create` 已标记 removal）；催化剂走新名 `addCraftingStation(type, ItemLike...)`（`addRecipeCatalyst` 已标记 removal）；以 `RecipeHolder` 为 T 注册可让 `getIdentifier` 默认实现回填配方 ID（书签/高级 tooltip）。

## 已交付：M14-a 切片（处理机族 7 类别）

- `Ic2JeiPlugin`：按 `ProcessingMethod` 建 7 个 JEI 类型（ic2:macerator / extractor / compressor / metal_former_extruding / metal_former_rolling / metal_former_cutting / block_cutter）+ 7 个类别（`MachineProcessingCategory` 共用布局：输入槽（input_count>1 时 tooltip 注明）→ 箭头 → 输出列（带权重 tooltip））+ 机器方块催化剂（metal_former 三模式共用）。
- `ClientRecipeCache.processing(method)`：从快照按原版 RecipeType 取 `RecipeHolder` 列表喂给 JEI。
- GameTest `processing_jei_categories`（`jei_categories_non_empty`）：断言 7 个处理配方族在服务端都有已加载配方——JEI 类别渲染的原料在数据包层恒非空。

## 已验证 / 未验证

已验证（本轮证据）：
- 对真实 JEI 29.37.0.98 API 构件编译零告警；
- 无依赖启动：双能量模式 GameTest 345 全绿（运行时无 JEI）；
- `verify_artifact.py` 专服/客户端边界通过（577 类）；
- `git diff --check`、`progress.py --check` 通过。

未验证（待后续）：JEI 插件的真实客户端运行时表现（类别页布局、tooltip、催化剂入口）需要装 JEI 的真实客户端——记录于待测试.md §64，M14 收尾验收时执行。

## 后续切片

- M14-b：Jade 26.1.10 集成（机器 EU 存量/进度的 WAILA tooltip，compileOnly + `@WailaPlugin` 同策略）。
- M14-c 起：JEI 剩余配方类别（罐装、热力三族、离心、洗矿、高炉、物质生成、电炉/感应炉）与 JEI 信息页（P15 卡信息展示面共用）。
- P17 收尾：AE2 结论存档（见上），三模组真实客户端联验。
