# M14／P17：可选模组集成（JEI、Jade、AE2）

状态：进行中（2026-09-10，M14-a JEI 切片 + M14-b Jade 切片 + M14-c JEI 剩余类别切片交付）。本文记录目标版本评估证据、集成策略与已验证边界。

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

## 已交付：M14-b 切片（Jade 机器 EU/进度）

- 依赖解析勘误：本开发环境 DNS 解析不了 `maven.modrinth.com`，改走 **CurseMaven**（`curse.maven:jade-324717:8651070` = Jade-mc26.1-NeoForge-26.1.10.jar，与 Modrinth 26.1.10+neoforge 同一二进制，字节数一致 1,003,732 已核对）；`compileOnly` 策略与 JEI 相同。
- `ic2/neoforge/interop/jade/Ic2JadePlugin`（`@WailaPlugin`，落位**普通包**而非 client 包：Jade 在装了 Jade 的专服上也会加载插件做数据同步，本类只引用 Jade 公共 API 与机器类，无任何 net/minecraft/client 引用，专服边界校验通过）。
- 两个 provider 走 Jade **universal 视图**模式（服务端 `IServerExtensionProvider` + 客户端 `IClientExtensionProvider`，渲染复用 Jade 内置 energy/progress 样式与配置）：
  - `MachineEnergyProvider`（注册到 `PoweredBlockEntity.class`）：`energy.stored()/capacity()` → `EnergyView.Data`，单位 " EU"，capacity≤0 不上报；
  - `MachineProgressProvider`（注册到 `MachineBlockEntity.class`）：`progress()/progressMaximum()` → `ProgressView.Data`，maximum≤0 不上报，客户端文本复用既有词表 `ic2.jade.progress`（"%s%%"，en/zh 都已在 lang 里）。
- GameTest 数量不变（345×2，Jade 不在运行时，provider 逻辑只接受编译期校验）。

## 已交付：M14-c 切片（JEI 剩余机器配方类别 9 类）

- 通用布局：`MachineCategory<T extends Recipe<?>> extends AbstractRecipeCategory<RecipeHolder<T>>`（宽 116 高 62），`Layout<T>` 函数式接口让每个配方族自绘槽位，类别只负责箭头（箭头 x 可配，罐装双输入用 60，其余 46）+ `CategoryLayouts` 槽位助手（物品输入复用 `jei.ic2.input_count` tooltip；流体槽 `setFluidRenderer(amount,false,16,16)+add(Fluid,amount)`——注意 `addFluidStack(Fluid,long)` 在 JEI 29.x 已标记 removal，用 `add(Fluid,long)`；`FluidStackTemplate` 是 `Holder<Fluid>` 的 record，取流体用 `fluid().value()` 而非 `getFluid()`）。
- 9 个新 JEI 类型/类别/催化剂（JEI 类型 id 与原版 RecipeType 注册 id 对齐）：
  - `ic2:canner_bottle`（罐装·灌装：容器+添加剂→产物）与 `ic2:canner_enrich`（罐装·富集：流体+添加剂→流体），催化剂 CANNER；
  - `ic2:fermenting`（发酵，流体→流体，tooltip 需热量/施肥间隔），催化剂 FERMENTER；
  - `ic2:cooling`（冷却，流体→流体，tooltip 需热量），催化剂 LIQUID_HEAT_EXCHANGER；
  - `ic2:electrolyzing`（电解，流体→至多 6 向流体列，tooltip EU/t+耗时+各输出方向），催化剂 ELECTROLYZER；
  - `ic2:ore_washing`（洗矿，物品+水→至多 3 产物），催化剂 ORE_WASHING_PLANT，水为原版 `Fluids.WATER` 按 `recipe.water()` mB 展示；
  - `ic2:centrifuge`（热离心，物品→至多 3 产物，tooltip 需热量 minHeat），催化剂 CENTRIFUGE；
  - `ic2:blast_furnace`（高炉，物品+空气→至多 2 产物，tooltip 耗时 duration，空气按 `recipe.fluid()` mB 展示），催化剂 BLAST_FURNACE；
  - `ic2:matter_fabricator`（物质制造，物品→UU 流体 `result` mB），催化剂 MATTER_GENERATOR；UU/空气流体经 `ModFluids.FAMILIES.get(FluidDefinition.…).source().get()`。
- `ClientRecipeCache.byType(RecipeType)`：泛型快照查询（签名对齐 `RecipeMap.byType` 的 `<I extends RecipeInput, T extends Recipe<I>>`，否则推断失败）。`ElectricCraftingRecipe`/`GradualRecipe` 只有 serializer 没有 RecipeType（走原版合成界面），不建 JEI 类别。
- lang：en_us/zh_cn 各 +9 类别名 +6 tooltip 键（requires_heat/fertilizer_interval/duration/eu_per_tick/side）；zh_cn 顺带补齐 M14-a 的 7 个处理类别名与 2 个 tooltip 键。
- GameTest `jei_categories_non_empty` 扩展到 16 个配方族（7 处理 + 9 新类别）非空断言，双能量模式 345 全绿。

## 后续切片

- P17 收尾：AE2 结论存档（见上），JEI+Jade 真实客户端联验（JEI 面：16 个类别页布局/tooltip/催化剂入口，重点电解多输出与高炉空气槽；Jade 面：机器能量条/进度条/百分比文本样式）。
- 电解配方数据包实际输出≤2（数据包上限 6），类别输出列最多渲染 3 行；若未来数据包单配方输出超过 3，需调整 electrolyzing 布局（已在待测试.md §66 记录）。
