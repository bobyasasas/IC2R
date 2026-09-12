# Charging 电池家族（charging batteries）

切片：pending 配方清收路线第五步。移植 legacy 5 个电池物品，解锁 6 条 pending 配方
（converted 749→755，pending 47→41）。

## Legacy 本体（legacy/forge-1.20.1/src/main/java/ic2/core/item/）

- `ItemBatterySU`（single_use_battery，1200 EU / tier 1 / 可堆叠 64）：
  `use()` 遍历快捷栏 0–8（`target != stack` 实例判等跳过自身），
  `ElectricItem.manager.charge(target, energy, tier, ignoreTransferLimit=true, simulate=false)`
  累计扣减余量；只要有成分被充入（`Util.isSimilar` 1e-5 判等不成立）即
  `decSize` 消耗 1 个并返回 SUCCESS，否则 PASS 不消耗。无音效、无消息。
- `ItemBatteryChargeHotbar extends ItemBattery`（4 件）：
  | 物品 | 容量 | 传输 | tier | 堆叠 | 稀有度 |
  |---|---|---|---|---|---|
  | charging_re_battery | 40,000 | 128 | 1 | 16 | COMMON |
  | advanced_charging_re_battery | 400,000 | 1,024 | 2 | 16 | COMMON |
  | charging_energy_crystal | 4,000,000 | 8,192 | 3 | 16 | COMMON |
  | charging_lapotron_crystal | 40,000,000 | 32,768 | 4 | 16 | UNCOMMON |
  - **被动馈电**（inventoryTick）：`!world.isClientSide` 且 `getGameTime() % 10 < tier`
    且 mode.enabled 时，遍历快捷栏 0–8（`limit > 0` 界内）：
    ① `charge(target, limit, tier, ignoreTransferLimit=false, simulate=true)` 探测目标
    接受量（尊重目标自身传输上限）；② `discharge(stack, accepted, tier,
    ignoreTransferLimit=true, external=false, simulate=false)` 真放电自身；
    ③ `charge(target, accepted, tier, ignoreTransferLimit=true, simulate=false)` 真充入；
    `limit -= accepted`。跳过：空位、`Mode.NOT_IN_HAND` 时手持位
    （`player.inventory.selected`）、其它 `ItemBatteryChargeHotbar` 实例（含自身）。
  - **右键三态循环**：`ENABLED(true) → DISABLED(false) → NOT_IN_HAND(true)` → ENABLED；
    模式存 NBT `mode` 字节（缺省/越界回落 ENABLED）；服务端 `sideProxy.messagePlayer`
    发 `ic2.tooltip.mode`（含 `ic2.tooltip.mode.<mode>` 参数）chat 消息。
  - **tooltip**：模式行（ic2.tooltip.mode + 参数）；GuiToolbox 内另显 boxable 提示。
  - `canBeStoredInToolbox`：仅 DISABLED 可入工具箱（IBoxable）。
  - `@NotClassic`：经典 profile 隐藏。

## Port 落点（neoforge/src/main/java/ic2/neoforge/item/）

- `SingleUseBatteryItem`：1:1 use() 语义（`Math.abs(energy - CAPACITY) >= 1e-5` 判耗）。
- `ChargingBatteryItem extends BatteryItem`：馈电循环三步与 legacy 逐参一致；
  模式存新数据组件 `ic2:battery_mode`（Codec.intRange(0,2)+VAR_INT，镜像 NBT 字节）；
  右键循环 + `sendSystemMessage`（26.1.2 对 legacy displayClientMessage(msg,false) 的改名，
  mock player 上为 no-op）；appendHoverText 输出模式行（26.1.2 TooltipDisplay 新签名）。
- `ModItems`：5 件注册（chargingBattery 助手镜像 battery 助手）+ INGREDIENTS 创造页
  （跟随 port 既有一族电池分组，legacy 为 TOOLS_AND_UTILITIES——分组差异如实记录）。

## 平台分歧（如实记录）

1. `@NotClassic`（profile 系统）无 port 对应——port 无 classic/exp profile 切换。
2. `IBoxable.canBeStoredInToolbox`（DISABLED 才可入工具箱）与 GuiToolbox boxable
   提示无 port 对应面——port 工具箱（ToolboxMenu mayPlace=!locked）暂无 IBoxable
   过滤系统；系统级差异，非本切片引入。
3. 创造页分组随 port 既有电池族进 INGREDIENTS（legacy TOOLS_AND_UTILITIES）。
4. 26.1.2 `inventoryTick` 无 int slot/isSelected 参数——NOT_IN_HAND 手持位判定改用
   `player.getInventory().getSelectedSlot()`，语义等价。
5. 键位/UX：模式切换消息为 chat 行；tooltip 模式行无 legacy boxable 着色分支。

## GameTest（ChargingBatteryTests，3 例，双模式）

- `single_use_battery_charge`：满 5 组电池右键→drill（30000/100/t1）恰 +1200、
  组 5→4、SUCCESS；充满 drill 后二次右键→PASS 且不消耗。
- `charging_battery_tick_feed`：charging_re_battery（40000 满电）喂 drained drill 一
  合格 tick：drill 恰 +100（其自身 100 EU 传输上限封顶，非电池 128）、电池恰 40000−100、
  兄弟 charging_energy_crystal 恒 0（instanceof 排除）。
- `charging_battery_mode_gate`：右键×1→DISABLED 不馈电；再×1→NOT_IN_HAND；
  手持位（slot 0=selected）跳过、移到 slot 2 后恰 +100/电池恰 39900。
- **确定性**：legacy 馈电门是 `gameTime % 10 < tier`——测试用 `runAfterDelay` 把单次
  馈电对齐到下一个合格世界 tick（phase→0），杜绝批内时序漂移；test max_ticks=120。

## 资源

- 贴图 21 张自 legacy 照搬（battery/ 下 4×5 级充电电池 + single_use 1 张）。
- 新增 `items/<id>.json` ×5（4 件 range_dispatch ic2:charge 阈值 0.2 步进镜像
  advanced_re_battery 范式；SU 单模型）+ `models/item/<id>.json` ×5 +
  `models/item/battery/<id>_1..4.json` ×16（分档模型，镜像既有电池族布局）。
- en_us / zh_cn 语言键已预置（含 ic2.tooltip.mode 族）。

## 验证

- build → GT IC2 模式 425（422+3）多轮 → GT 能量模式多轮 → core test →
  verify_artifact → progress 跑+--check → git diff --check → CI 双模式步。
- recipes.py 重跑：converted 749→755、pending 47→41；加载清单动态校验
  （processing_loaded_recipes 逐条断言 byKey 存在）。

## 待人工验收（视觉/UX，保留人工）

- 4 件充电电池电量分档贴图（0–4 五档 range_dispatch）实机外观与 SU 电池贴图。
- 右键模式循环 chat 提示文案与 tooltip 模式行实机显示。
