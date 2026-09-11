# 工具与拆装

已接入扳手、电动扳手、锻造锤和切线钳。模型沿用旧资源；挖掘行为接入原生 Tool 数据组件与普通生存破坏流程。

- 扳手：120 耐久，目标方块挖掘速度 6。右键按命中区域旋转，中心为当前面、边缘为相邻面、角落为背面；储能与变压器可朝六面，水平机器拒绝上下朝向。
- 电动扳手：12,000 EU，250 EU 物品传输上限，等级 1，挖掘一次消耗 100 EU。旋转不消耗电量，保持旧行为。
- 锻造锤：80 耐久；切线钳：60 耐久。IC2 无序合成按每次一耐久返回工具，最后一次不返还；合成预览不修改输入堆栈。
- 切线钳右键以一份橡胶加一层绝缘，消耗一次耐久；左键剥一层绝缘，消耗三次耐久并返还一份橡胶。最大绝缘层数由已有导线规格决定，无可用变体时不消耗材料。

正常掉落从旧 `Ic2Blocks` 声明生成。原本只掉外壳的机器，普通工具仍掉外壳，匹配 `ic2:wrenches` 标签时掉机器本体。储能设备掉本体时将 `stored_energy` 数据组件写入掉落物，重新放置恢复电量；保留比例使用服务器 `energy.storageDropRetention`，默认 0.8。

拆卸通过 Minecraft 和 NeoForge 的原生破坏流程，因此保护模组取消 `BreakBlockEvent` 时不会丢机器、耗耐久或扣电。没有另设绕过原生破坏事件的删除路径。

`ToolTests` 实测普通镐拆 MFSU 得高级外壳；100 EU 电动扳手拆满电 MFSU 得 32,000,000 EU 本体，电量归零，库存只掉一次，重新放置恢复储能。另测被取消的拆卸、旋转边界、绝缘材料与耐久守恒、合成工具的最后一次使用。
`ConsumptionTests` 补测罐头按饥饿缺口进食、空罐返还及满背包回滚。

38 项核心单测以及 IC2／GT 各 36 项 IC2 GameTest 加 1 项原版测试通过。客户端实测手持扳手右键旋转了朝西的 LV 变压器，命令校验其已朝北，模型正常且未误开机器菜单。

![客户端扳手旋转](images/wrench-rotation.png)

剩余工作：扳手的面选择叠层、工具箱与其他工具装备整合，客户端持续及多人验收。工具清单仍保留部分实现状态。

电链锯（e17d5a49）：`ChainsawItem` 30,000 EU、100 EU 传输上限、等级 1，与 legacy `ItemElectricToolChainsaw` 同规格。斧类方块按 legacy 速度 12 挖掘且不耗电（普通挖掘与蜘蛛网破坏免费）；未被 26.1.2 保留的 `onBlockStartBreak` 钩子由 `BreakBlockEvent` 监听替代：剪切模式开启时（默认开）可剪切植被（树叶/短草/蕨/枯灌木/垂根/藤蔓/绊线/羊毛/蜘蛛网，legacy 的 grass 按 1.20.3+ 改名映射为 short_grass）破坏即取消原版掉落、掉落方块本身并消耗 100 EU，附带 legacy 的破坏粒子、`BLOCK_DESTROY` game event 与猪灵激怒；右键剪切 `Shearable` 生物（羊等，掉落由原版剪切战利品表处理）与攻击均消耗 100 EU，+11 攻击伤害/-3 攻击速度对应 legacy DiggerItem 属性组合。剪切模式经潜行+使用切换（写 `chainsaw_disable_shear` 数据组件，沿用涂色器对 legacy mode-switch 键位组合的降级处理），切换提示复用 legacy 熔刻器的模式文案；掉落判定（斧类+蜘蛛网+可剪切，无电量门槛）与挖掘速度衰减（没电降为 1.0）与 legacy 逐条对齐。铁矿板×5+动力单元与钢锭×4+电路+RE 蓄电池两条成形配方，创造标签页在铱钻头之后。`ChainsawItemTests` 4 项双模式验收：规格/速度/掉落矩阵与免费挖掘、剪切破坏双向（开：掉自身耗 100 EU；关：原版掉落不耗电）、潜行切换往返、羊剪切与禁用直通。

电动背包（662e813f）：BatPack 60,000 EU/100 传输/等级 1、Advanced BatPack 600,000/1000/等级 2，均为胸甲槽、可外部放电（`ElectricItemSpec.externalOutput=true`，对应 legacy `canProvideEnergy`），护甲点数取 legacy Ic2ArmorMaterials.BAT_PACK/ADVANCED_BAT_PACK 材料（胸甲 8、韧性 2.0、零耐久零附魔），26.1.2 走数据组件（`ArmorMaterial.createAttributes` + `Equippable`，同 hazmat 先例），穿着层贴图从 legacy 拷贝。legacy 耗电分配入口 `ElectricItemManager.use` 的"先充后耗"语义移植为 `ElectricItemEnergy.use`：先从四护甲槽把穿戴电器的储能拉入手持物品（`chargeFromArmor`，双向忽略传输上限、以护甲物品等级为源等级门控），模拟确认够用后真正消耗，随后再拉一次补满；legacy `canUse` 在此之前以手持物品自身电量先行门控，因此空电钻不会从背包拉电。链锯、钻头、电动扳手、电动树洞钻四条 `ItemElectricTool` 消耗路径接入 `use`（有实体参数时），原版护甲外或无实体调用保持纯 `discharge`。羊等生物剪切改走 NeoForge `IShearable`（`onSheared` 返回掉落由调用方 `spawnShearedDrop` 生成，对应 legacy `IForgeShearable` 模式；26.1.2 的原版 `Shearable.shear` 路径已被 Neo 用 `if (false)` 禁用）。GameTest 补齐 `test_instance` JSON 后发现上轮链锯 4 测试因缺 JSON 从未运行——补齐 8 个实例（链锯 4 + 背包 4）后真实执行 356 项，并借此揪出并修复链锯剪切模式切换的双重否定 bug。背包 4 测试：规格/护甲属性/外放门控、穿戴拉电分配（空仓→满仓→耗一次操作）、等级门控（t1 背包喂不动 t3 铱钻头、t2 高级背包可喂 t1）、mineBlock 经背包配电与空钻不拉电的 canUse 语义。

## 护甲链一：纳米服与夜视仪（P16-c）

已接入 ElectricArmorItem 共享基建 + NanoSuit 四件 + Night Vision Goggles。量子服（ItemArmorQuantumSuit：飞行、药水清除、染色、更大账目）留待下一切片，届时复用同一基建。

- 充能护甲（CHARGED_PROTECTION）：26.1.2 的 `ItemStack.getAttributeModifiers()`（Neo 扩展）只在 `ATTRIBUTE_MODIFIERS` 组件为空时回落到 `item.getDefaultAttributeModifiers(ItemStack)`——静态组件会遮蔽动态属性。因此纳米服不携带静态属性组件，充能/未充能切换全部由 `ElectricArmorItem.getDefaultAttributeModifiers(stack)` 按电量动态返回：充能（≥5000 EU）时四件套按槽位携带 legacy `CHARGED_PROTECTION={3,6,8,3}`（盔 3/胸 8/腿 6/靴 3）+ 韧性 2.0；未充能回落材料 `NANO_SUIT`（防御全 0，对应 legacy `Ic2ArmorMaterials.NANO_SUIT`）。属性仅装备时生效、充能变化在下一次穿戴时刷新，与 legacy 1.20.1 的属性缓存行为一致（已如实记录为降级）。
- 能量减伤（legacy `ItemArmorElectric.damageArmor`）：`ElectricArmorHelper.onIncomingDamage`（`LivingIncomingDamageEvent`）按脚→腿→胸→盔逐件结算 `absorbed = min(剩余伤害 × 槽位占比 × 0.9, 电量/5000)`，槽位占比 HEAD/FEET 0.15、CHEST 0.4、LEGS 0.3；消耗 `absorbed × 5000` EU 并 `setAmount(剩余)`。`BYPASSES_ENCHANTMENTS`/`BYPASSES_INVULNERABILITY` 伤害源完全绕过（legacy 同门控）。注意 `minecraft:generic` 属于原版 `bypasses_armor` 标签——GameTest 用精确血量差分（20−6.4=13.6）钉住吸收账目。
- 摔落吸收（legacy `ItemArmorNanoSuit.absorbFall`）：`LivingFallEvent` 中纳米靴按 `max((int)距离−3, 0)` 点伤害 × 5000 EU 支付，伤害 ≥8 点或电量不足则不吸收；事件取消即免摔落伤害。legacy 橡胶靴的同类分支为零耐久恒 false 的死代码，未迁移。
- 夜视（legacy `getNightVisionOrNot/affectPlayer`，NanoSuit 头盔与夜视仪共享）：潜行+use 切换 `night_vision_active` 布尔组件（键位从 legacy Alt+模式键降级，同链锯/涂色器先例），开启后每 tick `ElectricItemEnergy.use(1.0)` 维持效果；天空光 >8 时失明 100t，否则夜视 300t（`NightVisionHelper`，`PlayerTickEvent.Post`）。
- Night Vision Goggles：200,000 EU/200 传输/等级 1（不可外放），27 耐久 `durability(27)`，头盔材料 3 点护甲/韧性 2.0（legacy `NIGHT_VISION_GOGGLES` 保护数组按 [靴,腿,胸,盔] 索引取盔 3）；无减伤非 ElectricArmorItem。配方暂缓：依赖的 luminator/reinforced_glass 尚未移植，避免配方引用缺失物品（已记录）。
- 纳米配方四条照搬 legacy（carbon_plate + energy_crystal [+ goggles]）；创造页 COMBAT；纳米服 UNCOMMON 经 `properties.rarity`（26.1.2 无 Item.getRarity 钩子）。
- GameTest 4 项（含同步的 test_instance JSON，共 360 项真实执行）：规格与充能/未充能属性（含 4999 EU 不足一次伤害电量时靴为 0 防）、generic 伤害精确账目（18000 EU 换 3.6 点）与 BYPASSES 门控、摔落吸收直调+事件接线（取消+耗 10000 EU、≥8 点拒绝）、夜视切换/耗电/明暗分支。测试基建勘误：NeoForge `FakePlayer` 构造即 `setInvulnerable(true)` 且新 ServerPlayer `waitingForRespawn=true` 永不被 FakePlayer 的空 tick 消化——`ServerPlayer.isInvulnerableTo` 据此吞掉一切伤害；测试以反射复位该标志并手动执行原版在实体 tick 中完成的穿戴属性瞬态同步（`forEachModifier`→transient modifier）。

## 量子服（P16-d）

已接入 QuantumSuitItem 四件 + QuantumArmorHelper，复用 ElectricArmorItem 充能属性基建与 ElectricArmorHelper 减伤/摔落接线。四件 10,000,000 EU/12000 传输/等级 4（不可外放）、`Rarity.RARE`、20000 EU/点，材料 `QUANTUM_SUIT`（防御全 0、韧性 2.0），充能态 CHARGED_PROTECTION 盔 3/胸 8/腿 6/靴 3 同样走 `getDefaultAttributeModifiers(stack)` 动态返回（不带静态组件，同纳米服勘误）。

- 能量减伤：吸收比率覆写为胸 1.2、其余 1.0（legacy `getDamageAbsorptionRatio`），`absorbed = min(剩余 × 槽位占比 × 比率, 电量/20000)`；摔落吸收 `max((int)距离−10, 0)` 点 × 20000 EU，**无纳米靴的七点上限**（legacy 语义：满电时任意高度可付即吸收），电量不足或零伤害不耗电；`addsProtection` 充能即算防化件（与 hazmat 组合按四槽齐全判定）。
- 头盔生命维持（legacy `inventoryTick`）：缺氧每 tick 补 200 ticks 耗 1000 EU；`needsFood` 时自动吃背包内罐头（`TinCanItem.eatFromInventory`：按饥饿缺口吃至多一堆、空罐回插、`food.eat` 前先确认空罐放得下）；按 legacy `potionRemovalCost` 付费清除 POISON 10000×(amp+1)、`ic2:radiation` 10000+amp×100、WITHER 25000×(amp+1)，其余效果不清。
- 胸甲喷气飞行（legacy `JetpackLogic`）：`clearFire` 免费每 tick；`jetpack_active` 开启后推力 power=1、电量 ≤5% 线性降推、高度 `maxY/0.9` 顶端 25 格内衰减、垂直 `min(motionY+0.2, 0.6)`、悬停（legacy 悬停键降级为潜行）钳制下降 −0.1、空中每 tick 耗 1–2 EU+legacy 固有 +6 quirk、推力时 `resetFallDistance`。
- 护腿疾速（`speed_enabled` 组件开关，默认开）：sprint 且 onGround/水中时按朝向推 0.22（水中 0.1），10 tick 计数器到付 1000 EU（`speed_ticker` 组件持久化）。
- 靴子超级跳（`jump_enabled` 默认开）：`canUse(4000)` 且在地面置满跳蓄力，离地起跳付 4000 EU、每 tick 蓄力 ×0.75 衰减、sprint 起跳水平 ×3.5（legacy 加速键降级为 sprint）。
- 染色：26.1.2 无 `DyeableLeatherItem`——数据驱动四条 `minecraft:crafting_dye` 配方（`createWithOriginalComponents` 保留电量等组件）+ `DYED_COLOR` 组件 + equipment 层 `dyeable` 标志 + 物品模型 `minecraft:dye` tint（未染色 −1 即原样）。物品 `getColor` 走原版 `ARGB.opaque`。
- 键盘依赖降级（如实记录）：legacy 读客户端键位（悬停/速度/跳跃/前进），服务器移植无法读取——模式开关统一为潜行+use 切对应布尔组件（含系统消息复用 legacy 文案），前进/加速用 `isSprinting` 替代，悬停用潜行替代；待人工验收键位降级 UX。
- 配方：胸/腿/靴三条照搬 legacy（iridium+lapotron_crystal+alloy|machine+glowstone_dust|rubber_boots，纳米件为基底）；**quantum_helmet 配方暂缓**（reinforced_glass 未移植，同 goggles 先例，已记录）。创造页 COMBAT。
- GameTest 6 项（含同步的 test_instance JSON，双模式共 366 项真实执行）：规格与充能属性/染色组件/hazmat 充能门控、generic 伤害精确账目（4.8 点 96000 EU、电量受限时坦克见底、BYPASSES 门控）、头盔三线生命维持（供氧 300 耗 1000、三罐 10→13 耗 1000、三毒全清耗 90100 且无关效果幸存）、喷气推力公式（含 5% 电量斜坡与维度顶高钳制的期望复算）/悬停 −0.1/每 tick 8 EU/摔落重置/地面不耗电、超级跳起跳 0.3 与离地 4000 EU、直调摔落 15 格 100000 EU 与 25 格 300000 EU（无上限）及事件取消、疾速 0.22 推力/第十 tick 付费/组件关闭断电。

## 穿戴装备家族二：Lappack/Energypack/太阳能头盔/静电靴/双喷气背包（P16-e）

已接入六件 legacy 装备，与 batpack 同模式走静态属性组件 + `Equippable`（26.1.2 数据组件先例），复用穿戴配电与摔落/减伤基建。

- **Lappack**：20,000,000 EU/2500 传输/等级 4、UNCOMMON、外部可放电（对应 legacy `canProvideEnergy`），胸甲 8 点/韧性 2.0；**Energypack**：2,000,000/1000/等级 3、外放、同胸甲材料。二者无能量减伤（legacy energyPerDamage=0/比率 0）。
- **太阳能头盔**：盔 3 点（无韧性）；`UtilityArmorHelper` 每 tick 以太阳能发电机同款亮度公式（`SolarGeneration.brightness`：天空光/15 × 太阳角因子 × 11/16 雨衰减，sandy 分支按勘误恒 false）在玩家位置取样，把亮度值 EU 充给胸甲槽的任何电动物品（满正午 = 1 EU/t）。
- **静电靴**：靴 3 点；水平位移累计 ≥5 格时给胸甲槽充 `min(3, 距离/5)` EU 并重置标记；骑乘或水中冻结标记（`static_boots_x/z` 数据组件，对应 legacy NBT x/z）。
- **电动喷气背包**：30,000 EU/60 传输/等级 1；**经典喷气背包**：30000 mB 沼气罐（`FLUID` 数据组件，同 CF 背包先例；罐与沼气流体比对用 `Holder.value()`——26.1.2 `FluidStackTemplate.fluid()` 返回 Holder 而非 Fluid）。二者实现 `JetpackLike`（推力 1.0/0.7、掉电斜坡 20%/5%、悬停系数 0.2/0.1、顶高分母 1.0/1.28），共享 `JetpackLogic`（legacy JetpackLogic 移植）：`motionY=min(+power×0.2, 0.6)`、顶高 25 格内线性衰减、空中每 tick 耗 1–2 单位（电动背包 +legacy +6 quirk；经典罐不足整 tick 拒付但仍动作）、`resetFallDistance`。
- **键盘依赖降级**（如实记录）：legacy 跳跃键按住推进+跳跃/模式键切悬停——服务器无法读键，两个背包统一为潜行+use 切 `jetpack_active` 组件（复用 quantum 先例与 legacy 文案），激活即持续推进，潜行=悬停下降钳制，sprint=legacy 前进推进；UX 待人工。
- 配方 8 条照搬 legacy：energy_pack/lappack（水晶链）、solar_helmet 与 static_boots 各含原版盔甲改装变体（`*_from_helmet/_from_boots`）、jetpack（facade_cell/circuit/redstone；legacy 的 `consuming` 标志对纯物品配方行为等价故省略）、jetpack_electric；创造页 COMBAT，经典背包同时提供满罐堆（对应 legacy `fillItemCategory` 双堆展示）。
- GameTest 5 项（双模式共 **371 项**真实执行）：六件规格/静态属性/罐语义（超量拒付、逐量扣减）、太阳光充电（测量单 tick 增量恰等于亮度公式、晴天/室内分离）、静电靴 5 格 1 EU→10 格 2 EU→骑乘冻结、电动背包推力复算（含顶高钳制与 5% 斜坡）/悬停 −0.1/地面不耗电/sneak+use 装备、经典背包推力/2 mB 扣减/1 mB 拒付/空罐不动。

## 喷气背包家族收尾：jetpack attachment 与经典背包世界侧充装（P16-f）

Legacy `ic2.core.item.armor.jetpack` 包（JetpackHandler/JetpackAttachmentRecipe）与
`ItemArmorFluidTank`（经典背包流体罐）补齐，详见 [jetpack-attachment.md](jetpack-attachment.md)。

- **附加配方** `ic2:jetpack_attachment`（无图案，`MapCodec.unit`/`StreamCodec.unit` 无字段序列化器）：
  电力喷气背包 + 非黑名单胸甲（26.1.2 用 `DataComponents.EQUIPPABLE` 判定 CHEST——
  `Mob.getEquipmentSlotForItem` 已是实例方法）+ 连接部件；黑名单固定 = 经典/电动背包、量子胸、鞘翅
  （legacy 的可配置 blacklist 勘误：port 无等效动态配置机制）。
- **电量转移**：电动护甲（ElectricItem）进自身电池；普通护甲进 `jetpack_charge` 组件
  （上限 30,000，对应 legacy JetpackHandler 作为 IBackupElectricItemManager 的 NBT `charge` 语义）。
- **虚拟背包视图**：`JetpackAttachmentHelper.jetpackView()` 返回电力背包参数（0.7/0.05/0.1/1.28）的
  `JetpackLike`，`JetpackLogic` 胸甲分派顺序 = 原生背包 → 附加视图；扣费沿用 +6 quirk（8/7）。
- **破损弹回**：`LivingIncomingDamageEvent` 记录胸甲（BYPASSES_INVULNERABILITY 豁免）→ 下 tick
  胸空 → 新电力背包带虚拟电量 `setItemSlot` 弹回（legacy LivingEquipmentChangeEvent 语义按效果等价移植）。
- **键盘降级**：sneak+use 持有已附加护甲切 `jetpack_active`（`RightClickItem` 取消原版装备替换），
  潜行=悬停；tooltip 黄行 + 非电动护甲的 EU 电量行（`DecimalFormat("0.##")`）。
- **世界侧充装**：`JetpackTankHandler extends ItemAccessResourceHandler<FluidResource>`（1 槽/30,000 mB/
  `isValid` 沼气限定对应 legacy `canFill`/`FLUID` 组件写回）注册 `Capabilities.Fluid.ITEM`；
  TANK 右键走 `MachineBlock.useItemOn` 既有 `FluidUtil` 路径端到端充装。
- **板物品**：`JetpackAttachmentPlateItem` 三行 tooltip（craft/attach/warning 键已预存）；
  配方 `shaped/jetpack_attachment_plate` 照搬 legacy（iridium_shard 填 `c:nuggets/iridium` tag）。
- GameTest 4 项（双模式共 **378 项**）：配方装配两条电量路径+黑名单+重复/缺件/已附加拒绝+板图案、
  附加飞行推力复算+悬停+地面免耗+电动护甲自电池路径、弹回带电量+护甲存活不换+未附加不弹、
  capability 沼气限定+组件写回+TANK 右键端到端。
