# 核循环收尾 + creative_generator 切片

legacy 蓝本：`ic2/core/item/ItemNuclearResource.java`（42 行）、
`ic2/core/item/reactor/ItemReactorLithiumCell.java`（40 行）与
`ItemReactorDepletedUranium.java`（40 行，共享 `AbstractDamageableReactorComponent`）、
`ic2/core/block/machine/generator/TileEntityCreativeGenerator.java`（76 行，`@NotClassic`）。
本轮 6 件均无 legacy JSON 配方（叶子级物品），配方计数维持 774、pending 维持 22。

## 核循环物品（4 件）

- `NuclearResourceItem`（near_depleted_uranium 15s / re_enriched_uranium 30s，amplifier 100）：
  `inventoryTick`（26.1.2 新签名 `(ItemStack, ServerLevel, Entity, @Nullable EquipmentSlot)`，
  server-only）对无完整防化服的携带者施加 `ModEffects.RADIATION`（legacy Ic2Potion.applyTo
  直接 addEffect 同位）；
  - `canBePlacedIn` 返回 false：legacy `IBaseReactorComponent.canBePlacedIn` 通道本恢复——
    `ReactorComponent` 接口新增 default true 方法，`NuclearReactorBlockEntity.acceptsInventorySlot`
    从裸 `instanceof` 门控改为 `component.canBePlacedIn(stack, this)`，辐照资源由此拒绝入堆。
- `DepletingRodItem`（lithium_fuel_rod / depleted_isotope_fuel_rod 合一，legacy 两子类 +
  `AbstractDamageableReactorComponent` 的 NBT use/耐久条/tooltip 三合一）：
  - REACTOR_USE DataComponent 计数（legacy NBT getUseOfItem 同位），`use()` clamp 至
    maxUse=10000；
  - `acceptUraniumPulse` 仅 heatRun 分支：`use + extraUse + reactor.getHeat()/3000`
    （lithium extraUse=0，靠堆芯吸热充能；depleted isotope extraUse=1，无热也自增 1，
    legacy `getExtraInfluence` 同位）；满额经 `reactor.setItemAt` 换产物——
    lithium → tritium_fuel_rod（Ic2Items.TRITIUM_FUEL_ROD）、isotope → re_enriched_uranium；
  - 耐久条分歧修正：legacy 两子类反转 `getUseFraction`，故 bar 宽度与色相都按正向
    use/max 报满度（width = fraction×13、color = hsv(fraction/3,1,1)）；
  - tooltip `ic2.reactoritem.durability`（剩余/总量），26.1.2 五参 appendHoverText 签名。

## creative_generator

- `MachineKind.CREATIVE_GENERATOR("creative_generator", 32000, 0, 0, 0)`：容量 32000
  （legacy ElectricItem 个位）、0 槽；经 ModMachines 自动注册全家族。
- `CreativeGeneratorBlockEntity extends PoweredBlockEntity`：`EnergyNode.Terminal.source(
  energy, 32, 1)` LV 源（legacy tier 1、emitsEnergyTo true）；每 tick
  `energy.forceAdd(capacity())` 等价 legacy getOfferedEnergy ∞ / drawEnergy 空的无穷源范式；
  `automation()` 读写全拒（无槽）；`machineProperties` 特判
  `strength(-1, Float.POSITIVE_INFINITY)`（legacy Ic2Blocks strength(-1,∞) 同位）+ 爆炸免疫。
- legacy `DefaultDrop.None` 语义：徒手/工具破坏无掉落、仅扳手拆取自身——machines.py
  loot 生成新增 None 分支（wrench 条件单臂）；无 GUI（legacy 无 ScreenHandler），
  port MenuType 为自动注册产物，无对应 catalog 条目。

## 资源管道修复（连带既有内容）

- machines.py blockstate 转换新增 `""` 单变体展开：legacy 单 `""` 变体的机器 blockstate
  展开为 port MachineBlock 统一 `facing × active` 12 变体。此修复连带覆盖 15 台既有机器
  （tank、5×storage_box、item_buffer、reactor 四件外设、solar_generator、sorting_machine、
  steam_repressurizer）——它们的 HEAD blockstate 缺 12 变体中 11 个（运行时 missing-model
  隐患），本次一并消除。
- machines.py pickaxe tag 维持第 10 轮合并式管道；creative_generator 在 legacy pickaxe
  tag 内，随清单自动并入。

## GameTest（NuclearCycleTests 3 例，451 = 448 + 3）

| id | 覆盖 |
| --- | --- |
| nuclear_resources_radiate | 无防护 mock player 持 near_depleted tick 即得 RADIATION（amp 100、15s）；穿全套防化服（含橡胶靴）持 re_enriched 不中毒；canBePlacedIn=false 拒绝入堆 |
| depleting_rods_pulse | 非 heat 脉冲不动 lithium；heat 15000 脉冲 +5（heat/3000）；9996+heat12000 满额换 tritium；isotope 无热 +1、9999+1 满额换 re_enriched_uranium |
| creative_generator_feeds | 容量 32000；tick 即满；抽干后一 tick 回满；destroySpeed -1 不可破坏 |

双模式全量：IC2 451 绿 + GT 451 绿（每例先隔离验证再入全量）。

## 记档

- 排除项：`item_tool_crowbar`（catalog item ic2:crowbar，若在 pending）唯一功能是
  ICoverHolder 电缆盖板拆除，port 无 cover 系统（grep 零命中），移植即空壳——维持
  pending 注记，待 cover 基建轮或明确降级决策后再处理。
- `DepletingRodItem` 的 legacy `changeItemStack`/durability NBT 结构由 REACTOR_USE
  DataComponent 替代（REACTOR_USE 族既有分歧，非本轮新增）。
