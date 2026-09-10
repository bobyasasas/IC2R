# M14／P17：可选模组集成（JEI、Jade、AE2）

状态：进行中（2026-09-10，M14-a JEI 切片 + M14-b Jade 切片 + M14-c JEI 剩余类别切片 + P17-AE2 桥接切片交付）。本文记录目标版本评估证据、集成策略与已验证边界。

## 目标版本评估（2026-09-10 取证）

| 模组 | 对 MC 26.1.2 NeoForge 的目标版本 | 证据 |
|---|---|---|
| JEI | **29.37.0.98**（2026-09-08 发布） | maven `https://maven.blamejared.com/mezz/jei/jei-26.1.2-neoforge/maven-metadata.xml`（latest 29.37.0.98）；API 构件 `jei-26.1.2-common-api` / `jei-26.1.2-neoforge-api` 同版本 |
| Jade | 26.1.10+neoforge（2026-08-15 发布） | Modrinth 项目 jade 版本列表（game_version 26.1.2，loader neoforge） |
| AE2 | 26.1.11-beta（2026-08-25 发布，beta） | Modrinth 项目 `ae2`（slug 即 ae2，applied-energistics-2 会 404）版本列表 |

结论：三者均有 26.1.2 NeoForge 目标。AE2 为 beta。

> **AE2 结论勘误（2026-09-10，P17-AE2 切片）**：本文此前写"legacy IC2 2.10.39 与 AE2 无双向集成点"系**错误结论**——重新排查 `legacy/forge-1.20.1/src/main/java/ic2/integration/ae2/Ic2Ae2Plugin.java`（432 行）确认 legacy 存在完整 EU→AE 单向桥（详见下节），本轮已按其语义移植到新端。

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

## 已交付：P17-AE2 切片（EU→AE2 能量桥接）

legacy 排查勘误后的移植（legacy 蓝本：`ic2/integration/ae2/Ic2Ae2Plugin.java`，432 行）：

- **legacy 语义**：EU→AE 单向桥，换算 1 EU = 2 AE（`EU_TO_AE_RATIO=2.0`）；仅识别 `ae2:energy_acceptor` 方块；sink tier 4（512 EU/t）。
- **AE2 API 反射自适应**：与 legacy 相同的四个方法名字符串（`GridHelper.getExposedNode(Level,BlockPos,Direction)` → `IGrid.getEnergyService` → `IEnergyService.injectPower(double,Actionable.MODULATE)`），AE2 不在或 26.x 改名时静默降级到能量能力兜底——本类不编译期依赖 AE2，可安全随 IC2 主 jar 分发。
- **兜底通道**：legacy 的 `ForgeCapabilities.ENERGY` 在 NeoForge 26.1.2 已不存在，兜底改为 `Capabilities.Energy.BLOCK` + `EnergyHandler.insert(int, TransactionContext)`（`Transaction.openRoot()` + commit，1 AE = 2 EU 换算）。
- **挂载机制（架构偏差，等价设计）**：legacy 走全局 EnergyNet tile 注册表（place/break/chunk 扫描维护 `IEnergySink`）；新端能量是 per-ServerLevel 邻接图（`WorldEnergyNetworks`），无全局注册表——新增 `WorldEnergyNetworks.ExternalTerminals` SPI：图 rebuild 时对每个电缆邻居查询 `terminal(level,pos)`，acceptor 作为外部 sink 终端直接进图，**天然继承电缆损耗/电压门/过载熔毁语义**；`afterDistribution` 在每 tick 分包后把缓冲排空转发给 AE2/兜底通道。place/break 事件（`EntityPlaceEvent`/`BreakBlockEvent`）即时维护桥表并 invalidate 所在电网，不需要 legacy 的 32 位/刻 chunk 扫描队列。
- **需求模型偏差（记录在案）**：legacy 用 demand 查询（`getEnergyDemand`/FE 余量）按需拉包；新端 PacketDistributor 是 pull 分包，acceptor 终端以「512 EU/t × 30 A 缓冲 + 每 tick 排空」近似 demand——净吞吐等价（每 tick ≤ 15 360 EU），但电网满载拥堵时的包分配时序与 legacy 有细微差异。AE2 网络实际需求（injectPower 被拒部分）按比例缩回。
- **tag**：`data/ic2/tags/block/ae2_energy_acceptor.json` 引用 `ae2:energy_acceptor` 且 `"required": false`（26.1 tag 严格加载下无 AE2 环境也能整体加载）。
- **验证（GameTest 三件套，`ic2_tests:test_energy_acceptor` 方块 + gameTest 包内同名 tag 合并 + `RegisterCapabilitiesEvent` 提供 `EnergyHandler`）**：
  - `ae2_bridge_feeds`：发电机→锡电缆→acceptor，缓冲确实充入且发电机有放电；
  - `ae2_bridge_ratio`：直接相邻，`stored == round(delivered × 2)` 精确等式（1 EU = 2 AE）；
  - `ae2_bridge_no_path`：acceptor 与发电机之间无电缆路径时零抽取。
  - 双能量模式各 348 测试全绿（含三桥接测试）。

## 已交付：M14 真实客户端联验准备 + 无头冒烟（2026-09-10）

- **运行时 mod 注入**：MDG 2.0.146 对 MC 26.1.2 **移除了 `additionalRuntimeClasspath`**（RunModel 的该配置 setter 会抛 "no additional classpath anymore"，错误信息指引走标准配置）。26.1.2 走 mod locator rework：NeoForge 直接扫描 source-set runtime classpath 发现 mod。因此新增独立配置 `devRunInterop`（JEI 29.37.0.98 full jar + Jade 8651070 full jar）并 `configurations.runtimeClasspath.extendsFrom`——实测 `dependencies --configuration runtimeClasspath` 含 JEI/Jade，`runtimeElements`（发布 POM）不含，编译期仍只见 API jar（既有策略不破）。
- **run 参数**：client run 支持 `-Pic2Join=host:port`（--quickPlayMultiplayer）/ `-Pic2World="存档名"`（--quickPlaySingleplayer），MDG DevLaunch `@`-参数文件按 JVM 参数文件规则解析引号（空格存档名安全）。
- **勘误一（`@OnlyIn` 阻断）**：`ItntRenderer` 残留 `@OnlyIn(Dist.CLIENT)`——26.1 已移除该注解的运行时剥离行为，FML 对用注解的 mod 弹**模态警告对话框**阻断启动（quickPlay 不触发）。注解已删（类在 client 包且仅被客户端注册引用，无需注解）。
- **勘误二（创造栏崩溃，真 bug）**：`ModFoam.creativeContents` 对 **没有 BlockItem 的 foam 方块** `event.accept(FOAM)`——`asItem()` 得 `Items.AIR`，26.1.2 `ItemStack.getCount()` 对 air 栈返回 0，NeoForge `BuildCreativeModeTabContentsEvent.assertStackCount` 抛 `The stack count must be 1 for 0 minecraft:air`。后果不止缺 foam：异常中断整个事件分发，**排在 ModFoam 之后的 10 个注册类监听器（Armor/Crops/Toolbox/Upgrades/Cells/Fluids/MaterialBlocks/Machines 等）全部未执行**，创造栏大面积缺货且 JEI 物品表 "Building Blocks" 组崩溃。修复：移除该 accept（foam 由喷枪放置、无物品形态，创造栏本就不该出现）。已审计其余 17 个注册类的全部 `accept` 调用点，无同类问题。
- **冒烟结果（`xvfb-run -a ./gradlew :neoforge:runClient '-Pic2World=IC2 Migration Smoke'`，无头 Xvfb+Mesa）**：
  - JEI full 29.37.0.98 装载，`Ic2JeiPlugin: IC2 JEI plugin registered 16 recipe categories`（真实客户端、进世界、配方同步后）；
  - Jade full 装载，`snownee.jade.addon.*` 各插件加载，`ic2.neoforge.interop.jade.Ic2JadePlugin loaded`；
  - quickPlaySingleplayer 直入既有 "IC2 Migration Smoke" 存档（截图存证：橡胶木/机器/3.0.0 迁移标记牌渲染正常）；
  - 错误清单仅剩无头环境预期项（narrator flite 库、OpenAL 声音设备），无 mod 加载/运行时错误。
- **仍待人工验收**：JEI 类别页视觉布局、tooltip 文案、催化剂入口的**人眼复核**，以及 Jade 机器 EU/进度条的实际悬浮显示——本轮已把"装 JEI+Jade 的真实客户端可启动可游玩"从待验证项销掉，视觉细节仍留待 M14 收尾人工验收。

## 后续切片

- P17 收尾：JEI+Jade 真实客户端**人工**验收（JEI 面：16 个类别页布局/tooltip/催化剂入口，重点电解多输出与高炉空气槽；Jade 面：机器能量条/进度条/百分比文本样式）——本轮无头冒烟已证插件装载与类别注册，视觉细节需人眼复核。
- 电解配方数据包实际输出≤2（数据包上限 6），类别输出列最多渲染 3 行；若未来数据包单配方输出超过 3，需调整 electrolyzing 布局（已在待测试.md §66 记录）。
