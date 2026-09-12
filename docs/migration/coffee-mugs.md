# Coffee Mugs（石杯咖啡家族）

切片：pending 配方清收路线第六步（tank 家族分歧决策留用户侧，coke_kiln 体量大，
本家族为最小 pending 族）。移植 legacy `ItemMug`（单类四枚举），解锁 3 条配方 +
1 条落入流体转换桶（如实记录）。

## Legacy 本体（legacy/forge-1.20.1/src/main/java/ic2/core/item/ItemMug.java）

单类 `ItemMug` + `MugType{empty, cold_coffee, dark_coffee, coffee}`，`stacksTo(1)`，
**非原版 Food**——饮用流程手写：

- **finishUsingItem**（仅 Player 且 type≠empty）：
  `amplifyEffect` 依次作用于 MOVEMENT_SPEED、DIG_SPEED：
  已有效果 → `amp<maxAmplifier` 则 +1；`duration += extraDuration`；返回新 amp；
  无效果 → 挂 300t/amp0，返回 1。
  | type | maxAmplifier | extraDuration |
  |---|---|---|
  | cold_coffee | 1 | 600 |
  | dark_coffee | 5 | 1200 |
  | coffee | 6 | 1200 |
  coffee 结算时 `highest -= 2`（反噬阈值后移）。
  `highest >= 3` → CONFUSION `(highest-2)*200` t amp0；`>= 4` → HARM amp `highest-3`
  （即时伤害，实体 tick 时结算）。返回 `new ItemStack(EMPTY_MUG)`——喝完变空杯
  （不是 container item，整组替换）。
- **use flow**：`getUseDuration` 饮品 32 / 空杯 0；`getUseAnimation` DRINK / NONE；
  `use()` 仅饮品 `startUsingItem`，否则原版 PASS。

## 配方（4 条）

1. `shaped/empty_mug`：石头 SS /SSS/SS → empty_mug（已转换）。
2. `shapeless/cold_coffee_mug`：empty_mug + coffee_powder + **水 1000 mB（fluid 成分）**
   → cold_coffee_mug。**保持 pending**——转换器判定 fluid 成分需专用方案
   （与 water_to_snow_block、coolant 族同属"流体/成分转换"桶，原因串如实入
   recipe-catalog）；物品注册后该配方可在 fluid 方案落地时自动解锁。
3. `smelting/dark_coffee_mug`：cold_coffee_mug 熔炼 200t/0.1xp（已转换）。
4. `shapeless/coffee_mug`：dark_coffee_mug + 糖 + 奶桶（奶桶不返还，legacy 同）
   （已转换）。

另 `shapeless/coffee_powder`（咖啡豆手磨）与 `macerator/coffee_beans_to_coffee_powder`
此前已随 coffee_beans/coffee_powder 材料注册转换，非本切片。

## Port 落点

- `MugItem`：MugType 携带 (maxAmplifier, extraDuration)，语义逐参一致；
  26.1.2 名映射：MOVEMENT_SPEED→`MobEffects.SPEED`、DIG_SPEED→`HASTE`、
  CONFUSION→`NAUSEA`、HARM→`INSTANT_DAMAGE`、UseAnim→`ItemUseAnimation`、
  `getUseDuration(stack, entity)` 新签名。
- `ModItems`：EMPTY_MUG/COLD_COFFEE_MUG/DARK_COFFEE_MUG/COFFEE_MUG 四件注册
  （stacksTo(1)）+ INGREDIENTS 创造页（legacy FARMING/GENERAL 分组差异如实记录）。
- 资源：brewing/ 四张贴图 legacy 照搬 + models/item 四件 + items 定义四件；
  en_us/zh_cn 语言键已预置。

## GameTest（MugTests 3 例，双模式）

- `mug_drink_effects`：冷咖啡首口→速度/急迫各 300t amp0、返空杯；第二口→
  amp 封顶 1、时长 300+600=900t。
- `mug_use_flow`：空杯 duration 0/NONE/use PASS 不起手；满杯 32/DRINK/use 起饮
  （isUsingItem + getUseItem==手持栈）。
- `mug_overdrink_backfire`：黑咖啡三口→amp2/2700t 无反噬；第四口→NAUSEA 200t、
  仍返空杯；第五口→amp4、NAUSEA 400t、INSTANT_DAMAGE 效果已挂（**mock player
  不 tick，即时伤害在效果 tick 结算——断言效果实例存在而非血量下降**）。

## 验证

build → GT IC2 模式 428=425+3 ×2 → GT 能量模式 ×2 → core test → verify_artifact →
progress 跑+--check → git diff --check → CI 双模式步。
recipes.py 重跑：converted 755→758（pending 41→38，cold_coffee_mug 桶迁移）；
registry-catalog 翻转 4 条（item 306→310/528，item pending 43→39）；STATUS.md 重渲染。

## 待人工验收（视觉/UX，保留人工）

四枚石杯贴图（空/冷/黑/咖啡）实机外观；饮用动作（DRINK 动画 32t）与返空杯手感。
