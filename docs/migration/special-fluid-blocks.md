# 特殊流体方块(世界流体行为)

更新日期:2026-09-12。

## 旧行为依据

- `legacy/.../forge/EnvFluidHandlerForge.java` `createFluidBlock`(126-138 行):
  注册世界流体方块时按流体 id 分发,八个家族使用带行为的子类,
  其余家族用普通 `LiquidBlock`:
  - `steam`/`superheated_steam` → `SteamBlock`:实体入内失明 15 秒(300 tick)
  - `uu_matter` → `UUMatterBlock`:实体入内再生 II;玻璃瓶右键装水;
    异种流体邻居湮灭(熔岩源→黑曜石,其余源→空气)
  - `hot_water` → `HotWaterBlock`:实体入内再生 II 且接触源就地冷却为普通水;
    放置 360 tick 后自动冷却
  - `hot_coolant` → `HotCoolantBlock`:实体入内点燃 30 秒
  - `pahoehoe_lava` → `PahoehoeLavaBlock`:实体入内熔岩伤 4.0+点燃 30 秒;
    360 tick 冷却为玄武岩;触水直接变玄武岩
  - `air` → `AirBlock`:无行为空类(`entityInside` 空覆盖),惰性流体方块
  - `hydrogen` → `HydrogenBlock`:氢源放置或邻居变化时,3×3×3 邻域内发现火方块
    即自毁并触发 2.0 威力破坏性爆炸
- `construction_foam` → 已随泡沫切片交付(`neoforge/world/ConstructionFoamBlock`)

## 新实现

- `neoforge/src/main/java/ic2/neoforge/fluid/`(第 58 轮,eb348686+3c034424):
  - `SteamFluidBlock`:失明 300 tick;steam 与 superheated_steam 共用一类(同 legacy)
  - `UUMatterFluidBlock`:再生 II+玻璃瓶装水(`PotionContents.createItemStack`
    → 水瓶)+邻居异种流体湮灭;26.1 `neighborChanged` 无 neighborPos 参数,
    改为遍历六邻等效触发(超集差异记录不回删,留用户裁决)
  - `HotWaterFluidBlock`:再生 II+接触源冷却为普通水;放置后 360 tick 计划冷却
  - `HotCoolantFluidBlock`:点燃 30 秒(`igniteForSeconds`)
  - `PahoehoeLavaFluidBlock`:熔岩伤 4.0(`hurtServer`+`damageSources().lava`)+点燃;
    360 tick 冷却玄武岩;触水即玄武岩
  - `AirFluidBlock`:惰性镜像(保持家族分发与 legacy switch 一一对应)
  - `HydrogenFluidBlock`:26 邻火→自毁+`Level.explode(..., 2.0F, BLOCK)`
- `registration/ModFluids.blockFor`:按 `FluidDefinition` 分发八家族,
  其余家族保持普通 `LiquidBlock`

## 测试证据

- GameTest `FluidBlockTests` 9 项,IC2+GT 双模式 558×2 全绿(2026-09-12):
  - `fluid_steam_blinds`:steam/superheated_steam 各一头猪入内均失明
  - `fluid_uu_matter_bottles`:再生 II(amp 1)断言+玻璃瓶 `useItemOn` 得水瓶
    (`POTION_CONTENTS` is `Potions.WATER`)
  - `fluid_uu_matter_annihilates`:熔岩源→黑曜石,水源→空气
  - `fluid_hot_water_cools`:再生+接触源就地冷却;手动触发 360 tick 冷却
  - `fluid_hot_coolant_ignites`:猪点燃
  - `fluid_pahoehoe_burns`:受伤+点燃
  - `fluid_pahoehoe_basalt`:手动 tick 冷却玄武岩;湿路径邻居变化即玄武岩
  - `fluid_hydrogen_explodes`:火放置(垫下界岩)→氢源自毁爆炸,玻璃标记被炸毁。
    26.1 火方块无 infiniburn 支撑放置即灭;爆炸会同步炸毁火与支撑本身,
    属正确行为链(火放置→邻接通知→氢自检→爆炸)
  - `fluid_air_inert`:空气流体方块保持原位,实体无伤无火

## 未验收范围

- 各流体方块实机观感(渲染高度/流动/失明与再生的实机体验、氢爆炸实机演示)
  留人工;记录于 `/home/codex/minecraft/待测试.md` 第 144 节。
- uu_matter 湮灭为六邻遍历(legacy 每次仅检查单一邻居位置),触发语义等效;
  超集差异记录不回删,留用户裁决。
