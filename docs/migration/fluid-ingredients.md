# 流体成分转换（配方解锁专项）

更新日期:2026-09-11。

## 旧行为依据

- `RecipeIo.parseInput` 遇 `{"fluid": ..., "amount": ...}` 生成
  `RecipeInputFluidContainer`;`matches` 经 `Ic2FluidStack.get(subject)`
  要求载体物品（cell/桶等流体容器）当前流体相同且容量 ≥ 请求 mB;
- 固定液格（water_cell/lava_cell 等）与通用容器（空 cell 装组件液）一样
  可满足请求;固定液格在 legacy 流体路径 drain 后 shrink 并余
  `facade_cell`（`ItemClassicCell.drainMb`）;
- `MachineRecipeHelper.apply` 在配方余量存在时以余量替代输入槽,
  即消耗 1 满格 cell、输入槽回填 1 空格 cell;
- legacy 合成（ic2:shaped/shapeless）同经该容器语义,加工后留空瓶;
  但水车（`TileEntityWaterGenerator` + `InvSlotConsumable.consume`）按
  recipe remainder 分流:桶留容器按 1 EU/tick,水 cell **无 container
  item**、彻底消耗按 2 EU/tick——cell 的"drain 换空瓶"不属于通用
  crafting-remainder 通道。

## 新实现

- `recipe/FluidItemIngredient.java`:NeoForge `ICustomIngredient`
  （`IngredientType` 注册名 `ic2:fluid`）。JSON
  `{"neoforge:ingredient_type": "ic2:fluid", "fluid": ..., "amount": ...}`;
  `test` 经 `ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM)`
  遍历槽位比对流体与容量;`items()` 返回固定液匹配 cell + 恒含
  `facade_cell`（组件可装任意液）;非 simple（网络同步走 streamCodec）;
- `FluidCellItem.getCraftingRemainder`:含液 cell 余量 `facade_cell`,
  已空 cell 无余量（原样消耗）——经 `ItemInstanceExtension` 对原版合成
  `getRemainingItems` 与加工余量通道同时生效;
- `ProcessingBlockEntity.consumeInputs`:extract 后按余量回填 INPUT 槽
  （`FuelHeatBlockEntity` 范式）;槽内数量超出 input_count 导致回填失败时
  整事务回滚——仅精确数量可加工,与 legacy 精确匹配一致;
- `WaterGeneratorBlockEntity.generate`:水 cell 强制按无容器路径（彻底
  消耗,2 EU/tick）,仅桶类保留原版余量——对齐 legacy 水车分流,
  cell 的合成余量不得误入水车;
- `UuValues.collect`:改用 `Ingredient.items()`（对 custom 成分返回候选
  物品;`getValues()` 对 custom 成分抛异常）。流体 cell 无 UU seed,
  节点值 ∞,不产生有效转换边,UU 图谱不受污染;
- `recipes.py.ingredient()`:识别 `{"fluid", "amount"}` 键输出
  `ic2:fluid` 成分——shaped keys / shapeless / processing 三路自动生效。

## 配方解锁（7 条,761 → 768）

- `compressor/water_to_snow_block`(water 1000 → snow_block);
- `shaped/coal_fuel_dust` + `shapeless/coal_fuel_dust`(water 1000,
  产出 8);
- `shapeless/cold_coffee_mug`(water 1000);
- `shapeless/hydrated_tin_dust`(water 1000);
- `shapeless/obsidian`(2×water + 2×lava);
- `shaped/reactor_coolant_cell`(ic2:coolant 1000)。

## 测试证据

- GameTest `FluidIngredientTests`(IC2/GT 双模式 437×2 全绿):
  - `fluid_ingredient_shapeless`:water_cell 满足 cold_coffee_mug 的
    1000 mB 水请求,assemble 命中注册物品,`getRemainingItems` 槽位 2
    回 `facade_cell`,lava_cell 负例不匹配;
  - `fluid_ingredient_shaped_coolant`:coolant_cell 满足 shaped 键位,
    assemble 出 reactor_coolant_cell,余量回空瓶,water_cell 负例;
  - `fluid_ingredient_compressor`:水车外加工通道——压缩机 600 EU 把
    water_cell 压成 snow_block,空瓶留在输入槽,能量恰好耗尽;
- 既有 `water_cell_automation`（水车对 cell 彻底消耗 2 EU/tick）与
  `bucketPersistence`（桶留桶 1 EU/tick）保持绿,证明分流无回归;
- `tools/migration/progress.py --check` 通过;STATUS.md 761→768。

## 遗留与边界

- registry-catalog 本轮无需翻转:未新增物品/方块/流体注册条目,fluid
  成分复用既有 cells(partial:客户端外观随实机验收)与 coolant 流体
  (implemented);reactor_coolant_cell 维持 partial,本切片补齐其配方;
- 剩余 pending 28 条全部是未移植物品的配方(tank 家族/fluid 机器/
  电缆/meter 等),与成分语义无关;
- 流体成分的 JEI/EMI 展示(jei 信息插件对 custom 成分的渲染)未做,
  属客户端展示切片。
