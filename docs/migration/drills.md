# 电动钻头(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../item/tool/ItemDrill.java`、`ItemElectricTool.java`、`ItemDrillIridium.java`:
  - `ic2:drill`:30,000 EU/100 EU/t/1 级;铁材质级;挖掘速度 8.0;单方块操作能耗 50 EU。
  - `ic2:diamond_drill`:同容量同等级;钻石材质级;速度 16.0;操作能耗 80 EU。
  - `ic2:iridium_drill`:300,000 EU/1,000 EU/t/3 级;速度 24.0;操作能耗 800 EU;
    手持默认无附魔,键盘模式切换在时运 III 与精准采集间切换(合成获得时无附魔)。
  - 挖掘速度受电量门控:`canUse(操作能耗)` 不满足时退化为 1.0 徒手速度;
    水下/空中 ×3 速度补偿、声音与攻击属性为 legacy 客户端细节。
  - 掉落正确性与电量无关,按材质级对照 `needs_stone/iron/diamond_tool` 判定,
    且方块必须在生效列表(镐/铲可挖标签)内。
  - `mineBlock`:方块破坏速度非 0 时消耗操作能耗;电量不足时 `manager.use` 先行
    拒绝,不部分扣电。
- 矿机联动(`IMiningDrill`):每方块 EU/t × tick 数 = drill 6×200、diamond 20×50、
  iridium 200×20;矿机收割时钻头磨损 50/80/800 EU;iridium 掉落按 fortune 3。

## 新实现

- `item/DrillItem.java`:数据组件驱动的电动钻头基类。`DataComponents.TOOL` 规则
  为 `deniesDrops(incorrect_for_<材质级>)` + `minesAndDrops(镐标签, 速度)` +
  `minesAndDrops(铲标签, 速度)`,注册期经
  `acquireBootstrapRegistrationLookup` 构建方块集。
- `isCorrectToolForDrops` 补充 legacy 生效列表约束(仅镐/铲可挖方块视为正确工具),
  不做电量门控;`getDestroySpeed` 在未充电或电量不足时退化为 1.0;
  `mineBlock` 仅在服务端、破坏速度非 0 且电量足够时按操作能耗放电
  (`discharge(..., ignoreLimit=true)`,与电动扳手一致),不足时不部分扣电。
- 矿机常量以访问器暴露:`minerEnergyPerTick`/`minerDuration`/`harvestEnergyCost`/
  `fortuneLevel`,供矿机切片直接消费。
- `registration/ModTools.java`:三个钻头注册(`stacksTo(1)`),进
  TOOLS_AND_UTILITIES 创造页。
- 简化项(有意记录):水下/空中 ×3 速度补偿、挖掘/破坏音效、攻击属性与
  `hurtEnemy` 行为未迁移;铱钻头键盘模式切换待 IC2 键盘 API 迁移后随矿机
  (或工具键盘交互)切片补齐——当前铱钻头 `fortuneLevel()` 常量供矿机收割使用,
  手持附魔切换不可用。
- 资源:`tools.py` 复制三物品模型/纹理并生成 `items/*.json` 定义;
  `recipes.py` 解锁三个钻头配方(569/796)。

## 测试证据

- GameTest `DrillItemTests`(IC2 与 GT 双模式均通过,177 项):
  - `drill_speed_and_drops`:未充电速度 1.0、充电后 drill 8.0/diamond 16.0、
    铲标签覆盖、掉落正确性与电量无关、铁级不可收黑曜石而钻石级可以、
    镐/铲列表外方块(原木)不生效;
  - `drill_discharge`:挖掘一方块恰好扣 50 EU、电量不足不部分扣电、
    铱钻头扣 800 EU、零破坏速度方块(蒲公英)不耗电;
  - `drill_miner_constants`:三个钻头的容量/传输/等级与矿机
    EU/t、tick 数、收割磨损、时运等级逐项断言。

## 未验收范围

- 手持挖掘的实际客户端节奏(破坏进度条、音效)待游戏内检查;
- 铱钻头键盘模式切换、水下/空中速度补偿待后续切片;
- 矿机收割路径(钻头磨损、iridium fortune 3 掉落)随矿机切片验收;
- 多人环境表现随 M16。
