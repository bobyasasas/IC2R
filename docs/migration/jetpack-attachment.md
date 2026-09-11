# Jetpack Attachment 与经典喷气背包世界侧充装

Legacy `ic2.core.item.armor.jetpack` 包的最后两块拼图：JetpackAttachmentRecipe/JetpackHandler 的
胸甲附加系统，与 ItemArmorFluidTank（经典喷气背包 30,000 mB 沼气罐）的世界侧充装。
此前切片已交付两个背包本体与共享飞行物理（JetpackLogic/JetpackLike，见 EquipmentPackTests）。

## 附加系统（Legacy JetpackHandler + JetpackAttachmentRecipe）

- **配方** `ic2:jetpack_attachment`：电力喷气背包 + 任意非黑名单胸甲 + 连接部件，无图案
  任意摆放。黑名单（legacy 配置默认 + 固定项）：经典背包、电力喷气背包、量子胸甲、鞘翅
  （port 侧为固定集合，26.1.2 无等效动态配置）。胸甲判定用 `DataComponents.EQUIPPABLE`
  组件（26.1.2 的 `Mob.getEquipmentSlotForItem` 为实例方法，配方上下文不可用）。
- **电量转移**：附加后背包电量进入胸甲——电动护甲（ElectricItem）进自身电池；普通护甲进
  虚拟 `jetpack_charge` 组件（上限 = 电力喷气背包容量 30,000，对应 legacy 备用电
  气管理器的 NBT `charge` 语义）。
- **附加标记**：`jetpack_attached`（legacy NBT `hasIC2Jetpack`）；附加后参数 = 电力喷气
  背包（0.7 / 0.05 / 0.1 / 1.28），chargeLevel = 电量 / 30,000，飞行耗电沿用 legacy
  `drainEnergy` 的 +6 quirk。
- **破损弹回**：受击（非 BYPASSES_INVULNERABILITY 伤害源）时记录胸甲；下一 tick 胸部为空
  则视为护甲破碎，新的电力喷气背包带原电量弹回胸部。
- **键盘降级**：legacy 读客户端跳跃/悬停键；port 以 `jetpack_active` 组件门控 + 潜行右键
  持有护甲切换（`PlayerInteractEvent.RightClickItem` 取消原版装备替换），潜行 = 悬停下降。
- **Tooltip**：黄色 `ic2.jetpackAttached` 行；非电动护甲追加虚拟电量 EU 行（0.## 格式）。

## 经典喷气背包世界侧充装（Legacy ItemArmorFluidTank / StandardFluidItem）

- `JetpackTankHandler extends ItemAccessResourceHandler<FluidResource>`：1 槽、容量
  `JetpackItem.CAPACITY_MB`（30,000 mB）、内容存共享 `FLUID` 数据组件、
  `isValid` 限定沼气（legacy `canFill`）；注册于 `Capabilities.Fluid.ITEM`（ModArmor.JETPACK）。
- 世界侧充装走 `MachineBlock.useItemOn` 的既有 `FluidUtil.interactWithFluidHandler`
  路径（太阳能蒸馏器切片已有）：手持经典喷气背包对 TANK 右键即从罐体转移沼气；管道
  自动化经同一 capability。

## 验证

- GameTest（ic2_tests，双模式 378 项全绿）：
  - `jetpack_attachment_recipe`：配方装配（电动/普通护甲两条电量路径）、黑名单四项、
    重复与缺件拒绝、板本体 legacy 图案装配。
  - `jetpack_attached_flight`：附加飞行物理（推力 0.14、悬停 -0.1、地面不耗电、+6 quirk
    扣费 8/7）与电动护甲自身电池路径。
  - `jetpack_pop_back`：受击记录 → 破损弹回带电量；护甲存活/未附加不弹回。
  - `jetpack_world_fill`：capability 沼气限定、填充/抽 取写回组件、TANK 右键端到端充装。
- `:core:test` 110 项、verify_artifact、progress --check 全绿。

## 状态

M13/P16 喷气背包家族收尾完成。经典喷气背包世界侧充装为 legacy ItemArmorFluidTank 的
唯一剩余行为，至此 jetpack 家族（经典/电动/量子/附加）全部迁移完毕。
