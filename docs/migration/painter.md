# 涂色器(P13 爆炸链切片八)

legacy 基线:`ic2/core/item/tool/ItemToolPainter.java`(继承 `ItemToolCrafting`)、
`ic2/core/ref/Ic2Items.java` 17 个注册、配方 `data/ic2/recipes/shaped/painter.json` 与
`shapeless/<color>_painter.json` ×16、声音 `item.painter.use`。

## legacy 行为依据

- **17 个物品**:`painter`(无色)+ `<color>_painter` ×16(DyeColor 枚举序),
  全部 `stacksTo(1).durability(32)`;创造页 TOOLS_AND_UTILITIES。
- **useOn 染色**(无色直接 PASS),`colorBlock` 按 legacy 顺序逐分支:
  1. state 任意 property 的值类是 `DyeColor` → 直接改 property(同色拒绝);
  2. `canColor`:方块 id path 不含颜色名(同色拒绝,如白羊毛不可再涂白);
  3. WOOL tag → `<color>_wool`;玻璃/染色玻璃 → `<color>_stained_glass`;
     玻璃板/染色玻璃板 → 染色板(保留属性);BEDS tag → 双半床(先 air 后回填,
     flag 48);CANDLES tag → 染色蜡烛(保留属性);旗帜/墙旗(两分支平级)
     → 染色(保留属性);TERRACOTTA tag → 染色陶瓦;带釉陶瓦 → 染色(保留朝向);
     ConcretePowderBlock → 染色混凝土粉末;WOOL_CARPETS tag → 染色地毯;
     SHULKER_BOXES tag → 换色方块并搬运 BlockEntity NBT(saveWithId/loadStatic);
     minecraft 命名空间 path 含 "concrete" → 染色混凝土。
  4. IC2 自有墙(`<color>_wall`)不在任何分支/标签中 → PASS,不被染色(legacy 相同)。
- **损耗**:成功染色后 `damagePainter`:`hurt(1)`;耐久耗尽时——未开 autoRefill
  → 手持替换为满耐久无色 painter;开启 autoRefill → 从背包消耗一个同色 painter
  替换手持(排除手持槽;创造模式直接复制),无色 painter 进背包,找不到备件则
  手持替换为无色 painter。替换成功播破坏声。
- **autoRefill**:legacy 经 `IC2.keyboard` 模式切换键 + `use` 切换 NBT 布尔,
  提示 "Painter automatic refill mode enabled/disabled"(无语言键,显示英文原文)。
- **羊染色**:`interactLivingEntity` 对颜色不同的羊 `setColor` 并损耗 1 点耐久。
- **tooltip**:有色 painter 显示 `ic2.tooltip.tool.uses_left`(剩余次数);
  无色 painter 无 tooltip(legacy `color != null` 才调用 super)。
- **工具箱**:ItemToolPainter 实现 IBoxable(canBeStoredInToolbox=true)。
- **配方**:painter = 羊毛 tag ×3 + 铁锭 ×2(shaped " AA"/" BA"/"B  ");
  `<color>_painter` = painter + 对应染料(shapeless,consuming)。

## 移植实现

- `ic2.neoforge.item.PainterItem`:`color` 可空;useOn/colorBlock 13 分支顺序与
  legacy 一致;床 flag 48 = `UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS`;
  潜影盒换色用 `saveWithFullMetadata` + `BlockEntity.loadStatic` + `setBlockEntity`
  (26.1.2 序列化需 registryAccess);`state.getProperties()` 替代 legacy
  getValues().keySet() 遍历。
- `ModTools.PAINTER` + `PAINTERS`(EnumMap<DyeColor, DeferredItem>)+ `painter(DyeColor)`
  访问器;创造页工具页签 17 件。
- `ModDataComponents.PAINTER_AUTO_REFILL`(Boolean 组件)替代 legacy NBT;
  潜行空手右键(use)切换,sendSystemMessage 提示 `ic2.painter.auto_refill.*`。
- 耐久损耗用原生 `hurtAndBreak(1, player, hand)`:自动处理耐久附魔与创造免耗,
  破坏声由引擎播放。**必须在损耗前缓存 `stack.getItem()`**:26.1.2 中破坏后的
  stack 是共享 EMPTY,其 `getItem()` 返回 `Items.AIR`(见陷阱)。
- 背包消耗 `consumePainterFromInventory`:遍历 `Inventory.getSelectionSize()`
  主背包槽、排除 `getSelectedSlot()`;26.1.2 的 `Inventory.items`/`selected` 已私有,
  用 `getItem(slot)`/`getSelectedSlot()`。
- 声音:服务端 `level.playSound(null, pos, ModSounds.ITEM_PAINTER_USE, PLAYERS)`
  (与 treetap 约定一致;ModSounds/sounds.json/ogg 资源此前已就位)。
- 工具箱:`toolbox_tools.json` 追加 17 个 id(替代 IBoxable)。
- 配方:`recipes.py` 转换 17 条(665→682/796),羊毛 tag 转为 `#minecraft:wool`。

## 26.1.2 陷阱(本切片新增)

- **破坏后的 ItemStack 是共享 EMPTY**:`hurtAndBreak` 达到上限时 `shrink(1)`,
  之后 `stack.isEmpty()` 为 true 且 `stack.getItem()` 返回 `Items.AIR`——
  任何"破坏后取自身物品"的逻辑都必须在损耗前缓存 Item。
- **Inventory 私有化**:`items` 与 `selected` 字段不可直接访问,须用
  `getItem(slot)`、`getSelectedSlot()`、`Inventory.getSelectionSize()`(主背包 36 格)。
- **GameTest 结构支撑**:heat_room 只有 y=0 一层床岩地板,ORIGIN=(17,17,17) 悬空;
  需要支撑检测的方块(地毯在邻居更新时消失、蜡烛掉落、混凝土粉末坠落)必须放在
  y=1 地板层。本切片 PainterTests 的 ORIGIN 已改为 (17,1,17)。

## 有意差异

- **键位**:legacy autoRefill 经 IC2.keyboard 模式键切换;新端无键位系统,
  用潜行空手右键(与建筑泡沫喷枪模式切换同一约定)。
- **autoRefill 提示**:legacy 直接把英文句子当 translatable key(无翻译);
  新端用规范键 `ic2.painter.auto_refill.enabled/disabled`,补 en/zh 文案。
- **损耗路径**:legacy `stack.hurt(1, random, serverPlayer)` + 手动替换;
  新端 `hurtAndBreak` 原生路径(创造免耗行为一致,破坏声由引擎播放,
  不再单独播 ITEM_BREAK)。
- **StainableBlock/RetexturableBlock API 未迁移**:legacy 中 `StainableBlock.setColor`
  在 IC2 内部没有调用者(仅为 addon API);`RetexturableBlock.retexture` 的调用方
  ItemObscurator(遮蔽器)与 `obscured_wall`/`TileEntityWall` 重纹理链路依赖
  电动工具与客户端烘焙采样,拆分为独立后续切片。
- **羊染色声音/爱心粒子**:legacy 与新端均无特殊粒子,行为一致。

## 测试证据

`neoforge/src/gameTest/java/ic2/neoforge/test/PainterTests.java` 11 项
(双模式各 282 项全绿,含本切片):

- `painter_recolors_wool`:白羊毛→红羊毛,耐久 +1;同色拒绝且不再损耗。
- `painter_recolors_glass`:普通玻璃→染色玻璃;白色玻璃板→红色玻璃板。
- `painter_recolors_terracotta_concrete`:陶瓦/混凝土粉末/混凝土三族染色。
- `painter_recolors_carpet_candle`:地毯与蜡烛染色。
- `painter_recolors_bed`:染半张床两半同时变红且 foot/head 保留。
- `painter_recolors_shulker`:潜影盒换色后 5 颗钻石保留。
- `painter_wear_reverts`:耐久 31 的红刷最后一次涂色后还原为满耐久无色刷。
- `painter_auto_refill`:备件接管(满耐久红刷在手)且无色刷入包。
- `painter_dyes_sheep`:羊变红且耐久 +1。
- `painter_plain_passes`:无色刷对羊毛 PASS。
- `painter_toggles_auto_refill`:潜行 use 两次开→关组件翻转。

## 遗留

- 遮蔽墙重纹理切片:`ItemObscurator`(100,000 EU 电动物品、取样/应用双模式)、
  `obscured_wall` + `TileEntityWall`(参考方块面渲染)、`RetextureEvent`。
- `ItemToolPainter.canBeStoredInToolbox` 已由 `toolbox_tools` 标签承接;
  IItemHudInfo(复习 HUD)legacy 未在移植侧实现,保持省略。
