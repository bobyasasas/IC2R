# 切块机(block_cutter)与切割刀片

2026-09-09 切片。迁移 legacy `TileEntityBlockCutter` / `BlockCuttingBlade`
(`@NotClassic` 附加机器,ic2:block_cutter 配方家族,17 条配方——全部 pending
清单中最大的单一阻塞项)。

## 旧行为依据

- `TileEntityStandardMachine` 子类:4 EU/t、每刀 450 tick、储能 1800 EU(4×450,
  恰好一刀)、tier 1、1 个输出槽、4 升级槽(Processing/Transformer/EnergyStorage/
  ItemConsuming/ItemProducing 全集)。
- 独立刀片槽(`InvSlotConsumableClass`,只收 `IBlockCuttingBlade`):
  `getRecipeResult()` 在刀片缺失或 `blade.getHardness(stack)` 小于配方元数据
  `hardness` 时返回 null 并置 `bladeTooWeak` GUI 状态;刀片为永久物品,无耐久。
- 刀片三档:铁 3、钢 6、钻石 9(构造时按 `BlockCuttingBladeType` 固定);
  tooltip 显示分档说明与硬度值(`ic2.*_cutting_blade.info`、
  `ic2.cutting_blade.hardness`)。
- 配方:原木→木板(hardness 2)、木板→木棍(2)、铜/青铜/锡/铅/金块→板(2)、
  铁块/青金块→板(5)、钢块/黑曜石→板(8),即铁刀切木质与软金属、钢刀切铁、
  钻石刀切钢与黑曜石。
- 方块:DefaultDrop.AdvMachine(扳手返还高级机器壳)。

## 新设计

- `ProcessingMethod.BLOCK_CUTTER`(core 枚举,复用 ProcessingRecipe 类型/序列化器
  自动注册循环);`ProcessingRecipe` 增加 `hardness` 可选字段(缺省 0,仅切块机
  读取),数据包 JSON 原样保留 legacy 元数据。
- `MachineKind.BLOCK_CUTTER("block_cutter", 1800, 4, 450, 4)`:槽 4 =
  输入 0 / 输出 1 / 电池 2 / **刀片 3**,4 升级槽(euPerTick>0 默认),
  与其他标准机共用 `UpgradeableBlockEntity` 能量与升级框架。
- `BlockCutterBlockEntity extends SingleInputBlockEntity`:覆写 `findJob`——
  刀片缺失或硬度不足时置 `bladeTooWeak` 并返回 null(空转不耗电,状态经
  `menuValue(0)` 同步);`BlockCutterScreen` 在该状态下显示 legacy 原文提示
  `ic2.BlockCutter.gui.bladeTooWeak`。
- `CuttingBladeItem` 注册于 `ModTools`(创造页 TOOLS_AND_UTILITIES);
  `MachineMenu` 增加刀片槽 (38,17,上限 1) 与 shift-click 归位。
- 资源:`machines.py` 列表加入 `block_cutter`(十二态、AdvMachine 掉落、挖掘
  标签、双语名称);`tools.py` 加三把刀(items 定义/模型/纹理);语言键为
  legacy 全集拷贝,无需新增。
- `recipes.py`:`ic2:block_cutter` 加入 processing_types 并透传 `hardness`。
  配方转换 577 → 598(+17 切割 +3 刀片 +1 机器),切割家族的
  "recipe family not ported" 阻塞全部解除。

## 测试证据

- `BlockCutterTests.cutsBlockIntoPlates`:铁刀切铜方块(硬度 2)→ 9 铜板,
  恰好耗 1800 EU。
- `BlockCutterTests.weakBladeStallsAndDiamondResumes`:铁刀对黑曜石(硬度 8)
  完全停机、不耗电、上报 bladeTooWeak;换钻石刀后原输入续切完成。
- `BlockCutterTests.missingBladeStalls`:无刀片直接停机,零耗电。
- IC2 与 GT 双模式 `runGameTestServer` 197 项全绿;17 条切割配方进入
  converted-recipes 加载清单。

## 未验收 / 后续

- 客户端实机:刀片槽放置、bladeTooWeak 提示展示、与升级件配合(超频后
  operationsPerTick)、外观。记录于 `/home/codex/minecraft/待测试.md` 第 19 节。
- 切割配方依赖 `ic2:steel_plate`/钢块(已存在)——钢锭本身的量产来自
  `ic2:blast_furnace` 家族(5 条,仍 pending),是下一个待迁移机器。
