# 小件打包切片：wind_meter + refractory_bricks + reinforced_door

legacy 蓝本：`ic2/core/item/ItemWindMeter.java`、`ic2/core/block/BlockRefractoryBricks.java`、
`Ic2Blocks.REINFORCED_DOOR`（原版 `DoorBlock` 铁变体）。本轮一次性解锁三条 pending 配方
（`shaped/wind_meter`、`shaped/refractory_bricks`、`shaped/reinforced_door`），
配方计数 768 → 771、pending 28 → 25。

## wind_meter（风速计）

- `neoforge/src/main/java/ic2/neoforge/item/WindMeterItem.java`：继承 `ElectricItem`
  （spec 10000/100/tier 1），单次操作扣 50 EU。
  - `use`：空手读玩家所在高度风强（`WorldWind.windAt`，负值截 0），保留两位小数发
    `ic2.wind_meter.info`；电量不足返回 `PASS` 不耗电（对应 legacy `use` 失败分支）。
  - `onItemUseFirst`（对应 legacy `onItemUseFirst` 对两类风力机器的读取）：
    - `WindTurbineBlockEntity`：机器 `ACTIVE=false` 时按初始 `operation` 状态区分
      `rotor.none` / `rotor.blocked`，返回 `FAIL` 且不耗电；运转中 `INTERFERENCE`
      报 `blocked`（遮挡×3 = 转子直径），否则按 `environment`（遮挡后有效风强）报
      `obstructed` / `effective`。
    - `WindGeneratorBlockEntity`：`wind × (1 − obstructions/567)` 换算有效风，
      factor ≥ 1 视为全遮挡；同样 `obstructed` / `effective` 两档。
  - 新增访问器 `TurbineBlockEntity.operation()`、`WindGeneratorBlockEntity.obstructions()`
    （对应 legacy `getActive`/`getObstructions` 读取面）。
- 注册：`ModTools.WIND_METER`，创造页频率发射器之后。

## refractory_bricks（耐火砖）

`ModMaterialBlocks.REFRACTORY_BRICKS`：`strength(2, 10)` + `requiresCorrectToolForDrops` +
`SoundType.STONE`，镐 tag 纳入（material_blocks.py 管道），掉落自身（loot_table 照搬）。

## reinforced_door（强化门）

`ModMaterialBlocks.REINFORCED_DOOR`：26.1.2 `DoorBlock(BlockSetType.IRON, properties)`
（注意参数序 type 在前），`strength(50, 150)` + `SoundType.METAL`。
原版 26.1.2 iron_door blockstate 同为 32 变体、不含 powered，legacy 资源直接照搬
（`tools/migration/resources/small_blocks.py`）。

## 分组注记

legacy 创造分组：wind_meter → GENERAL、refractory_bricks → MATERIALS、
reinforced_door → GENERAL；port 映射到 FREQUENCY_TRANSMITTER 后的 TOOLS 页、
BUILDING_BLOCKS、INGREDIENTS（既有分组映射惯例）。

## 验证

`WindMeterTests` 6 例（`EntityTickingTests.wrap`，test_instance max_ticks 60）：

| id | 覆盖 |
| --- | --- |
| wind_meter_bare_use | 空手读风 SUCCESS + 恰扣 50 EU |
| wind_meter_low_charge_passes | 20 EU 时 PASS 且电量不变 |
| wind_meter_generator_reading | 发电机有效风读取 + 扣电 + obstructions ≥ 0 |
| wind_meter_stopped_turbine | 停转涡轮 FAIL / rotor.none + 不耗电 |
| refractory_bricks_drop | 破坏掉落自身 |
| reinforced_door_half_semantics | 破坏下半：上半消失、仅掉 1 门 item |

IC2 模式 6/6 绿；GT 模式抽验 `wind_meter_generator_reading` 绿（全量双模式见 STATUS）。
