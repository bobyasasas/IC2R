# 配方清收切片:recipes.py 全量重建、18 条翻转与加载清单收敛

2026-09-11 切片(横切,无工作包号,属 §10 资源和配方迁移流程的债务清收)。
背景:此前多轮以"手写配方文件+台账标 pending"交付配方,导致脚本—台账—加载
清单三方漂移:

- 台账(recipe-catalog.json)记 689/796(agent.md 行 73 滞后值),实际已转换 714;
- 加载清单(converted-recipes.json)欠 47 条——18 条手工配方、tfbp 家族、
  luminator、jetpack_attachment 从未纳入 loadedRecipes 加载验证;
- 两处手工成分改动(#c:glass、#c:chests/wooden)偏离转换器,重跑会被冲掉。

本轮修脚本而非续手写,重跑全量重建使三方收敛。

## 转换器修复(tools/migration/resources/recipes.py)

1. `registered` 由"仅 assets/ic2/items/*.json"扩为再并集
   registry-catalog 中 registry==item 且 status==implemented 的 id
   (机器方块物品与无客户端定义的晚期切片物品,如 terraformer/nuke);
2. `supported` 增 `ic2:jetpack_attachment`(runtime 配方家族,legacy JSON
   仅 type 无载荷;target 保持 root 路径——JetpackAttachmentTests 按裸 id
   `ic2:jetpack_attachment` 引用,不落 shaped/ 子目录);
3. 通用分支 result 可选化(runtime 家族无 result 可拷);manifest id 改按
   target 派生(原按 source 派生,与 root 落盘路径错位且漏手工条目)。

## 重跑结果(2026-09-11)

- 台账 714→732 converted,64 pending;相对 HEAD 0 文件删除、0 条回退。
- 翻转 18 条:shaped/advanced_batpack、batpack、chainsaw、
  chainsaw_from_steel、energy_pack、lappack、nano_boots、nano_chestplate、
  nano_helmet、nano_leggings、nuke、quantum_boots、quantum_chestplate、
  quantum_leggings、solar_helmet、solar_helmet_from_helmet、static_boots、
  static_boots_from_boots。
- 加载清单 687→732(+45:补 47 条欠账,减 rubber_boat×2 退回,见下),
  loadedRecipes 对 732 条逐条断言 byKey().isPresent()。
- 语义变化仅 2 处,均为向 legacy/NeoForge 标准忠实化:
  - reinforced_glass ×2:`#c:glass`→`#c:glass_blocks`(NeoForge 内置标签,
    values 含 colorless/cheap/tinted 即全部原版玻璃);随之一并删除孤儿标签
    `data/c/tags/item/glass.json`(手写 forge:glass 值拷贝,已无引用);
  - crop_harvester:`#c:chests/wooden`→`minecraft:chest`(legacy 原文即
    item chest,手写版引入的漂移被纠正)。
  - 其余为 count:1 补齐、key 排序、尾换行等价 diff。

## 审计纠正:rubber_boat 退回 pending

registry-catalog 曾把 `ic2:rubber_boat`(item)标 implemented,但证据文件中
并无该物品——无 items 定义物品的证据交叉审计发现物品本体与 RubberBoatEntity
均未移植(boat 家族待切片)。catalog 条目已改回 pending 并注明;转换器随即将
shaped/shapeless 两条 rubber_boat 配方退回 pending(prune 删除本轮初版误生成
的两个 JSON)。

## pending 余量(64 条构成)

- result 未注册约 50 条:bronze 工具/护甲 7、tank 家族 4(bronze/iron/
  steel/iridium)、boat 家族 3(rubber/carbon/electric)、charging 电池 3、
  coffee mugs 4、coke_kiln 3、cables 2(detector/splitter)、sheet 3
  (rubber/resin/wool)、单件(mining_laser、nano_saber、meter、
  batch_crafter、chunk_loader、industrial_workbench、fluid_bottler、
  fluid_distributor、weighted_*、solid_canner、stirling_kinetic_generator、
  containment_box、refractory_bricks、reinforced_door、
  redstone_inverter_upgrade、single_use_battery、advanced_charging_re_battery
  等);
- ingredient 阻塞 3 条:mining_laser、iron_tank(离心/模式存储/蒸汽再压);
- 流体/成分转换 7 条:water_to_snow_block、coal_fuel_dust×2、
  hydrated_tin_dust、obsidian、biomass、coolant 族(需 fluid 成分或
  damageable 组件方案)。

## GameTest(CraftingTests 扩展 4 例,双模式 409=405+4 全绿)

范式沿用 crafting_charge:`recipeAccess().byKey(Registries.RECIPE, …)` 取
`CraftingRecipe`,`CraftingInput.of(3,3,9 格含 EMPTY)` 后断言 matches 与
assemble 产物 id/count。

- crafting_power_armor:nano_helmet(2 行图案在 3×3 滑配,碳板+能量水晶+
  夜视镜)+quantum_chestplate(nano 胸甲为芯+铱/高级合金);
- crafting_packs:batpack(6 re_battery 环绕+电路+木板,注意中列自上而下
  电路/木板/空)+energy_pack(机壳外框+能量水晶两侧居中行);
- crafting_chainsaw_routes:铁板路线(chainsaw)与钢锭路线
  (chainsaw_from_steel)双配方 matches/assemble 一致+负例(电路换机壳
  不得匹配);
- crafting_utility:static_boots_from_boots(铁靴+羊毛+3 绝缘铜缆——电缆
  BlockItem 经 ModMachines.CABLES 取)+nuke(4 厚中子反射层+2 高级电路+
  高级机器,legacy hidden 标记属指南书内部标记未迁移,registry-catalog
  已记)。

## 验证链

build → GT IC2 模式 409 → GT 默认模式 409 → core test →
verify_artifact → progress.py 跑+--check → git diff --check 全绿。
待人工:配方书实机外观与创造页分组不属本切片验收范围。
