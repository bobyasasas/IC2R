# 高炉(blast_furnace)

2026-09-09 切片。迁移 legacy `TileEntityBlastFurnace`(钢锭量产机器,5 条
`ic2:blast_furnace` 配方)。

## 旧行为依据

- 无 EU;热量驱动:`heatUp()` 仅从**朝向面**的 `IHeatSource` 取热(需求
  `maxHeat - heat + 100`,上限 50,000;无热可得或无需求时每刻降温 1)。
  输入槽非空、进度已开始或有红石信号时才请求加热。
- 8,000 mB 空气罐:仅收 `Ic2Fluids.AIR`;`gainFluid()` 每刻从空气单元格
  (cellInput)抽满罐并把空单元送入 cellOutput。
- 配方元数据 `fluid`(每进度刻消耗的 mB)与 `duration`(进度刻数);
  热度满且输出可容纳时推进;铁锭→钢锭+炉渣 6000 刻、1 mB/刻(共 6000 mB,
  约 6 个空气单元)。
- 2 个升级槽(ItemConsuming/ItemProducing/FluidConsuming/RedstoneSensitive);
  输出槽 2;GuiSynced `guiHeat`/`guiProgress`;持久化 heat/progress。

## 新设计

- `MachineKind.BLAST_FURNACE("blast_furnace", 0, 5, 0, 0)`:槽 5 = 输入 0 /
  输出 1–2 / 空气单元进 3 / 出 4,2 升级槽(仅物品弹出/拉入,
  `UpgradeItem.Kind.suitable` 显式分支);六向朝向,DefaultDrop.Machine。
- `BlastFurnaceRecipe`(neoforge.recipe,参照 `CentrifugeRecipe`):原料、
  1–2 输出、`fluid`、`duration` 字段;类型/序列化器
  `ic2:blast_furnace` 注册于 `ModProcessingRecipes`。
- `BlastFurnaceBlockEntity extends MachineBlockEntity`(无能量基类):
  - `heatUp`:仅朝向面经 `WorkCapabilities.HEAT` 取热,请求/冷却节奏与
    legacy 相同(需求 50,100、无供给降温 1/刻)。
  - 空气支付逐刻提交(与 legacy 未检查的 `drainMbUnchecked` 一致);
    完成时的"投入扣除 + 双槽产出"为单一原子事务,任何失败整刻回滚
    (优于 legacy 的非原子 `operateOnce`)。
  - 单元补气走 `FluidContainerPort.of(inventory, CELL_IN, CELL_OUT)` + 过滤
    空气流体,每刻最多 1000 mB;`acceptsInventorySlot` 只允许空单元进入
    回收格(机器自写入)。
  - `menuValue(0/1)` = 热量/空气占比(float bits),进度走通用
    progress 字段;持久化 heat/progress。
- `BlastFurnaceScreen`:关闭 EU 能量条,绘制热量条(左)与空气表(右),
  悬停提示复用 legacy 键 `ic2.BlastFurnace.gui.heat/.air`。
- 资源:`machines.py` 列表;`recipes.py` 增加 `ic2:blast_furnace` 家族分支。
  配方转换 598 → 604(+5 高炉 +1 机器本体)。**钢链解锁**:
  铁锭/矿/碎矿/洗矿/尘 → 钢锭(+炉渣)。
- `rci_rsh`/`rci_lzh` 等其余 pending 与本切片无关,保持原状。

## 测试证据

- `BlastFurnaceTests.smeltsIronIntoSteel`:电热源 + 满充电能水晶(电池槽)→
  8 空气单元 → 快速探针配方(`ic2_tests:blast_furnace_probe`,金锭→
  钢锭+炉渣,200 刻,与主配方原料区分)在自然 tick 下完整跑通:产出钢+渣、
  输入清空、空气恰好消耗 200 mB。热链经真实 `WorkCapabilities.HEAT`。
- `staysColdWithoutHeat`:无热源时热量恒 0、零进度、输入保留。
- `airCellsFillTank`:8 单元恰好充满 8000 mB,空单元 8 个进入回收格。
- IC2 与 GT 双模式 `runGameTestServer` 200 项全绿。

## 排障记录(迁移差异说明)

- legacy 空气支付无事务;新实现曾把"每刻空气支付"与"完成产出"放进同一
  事务且只在完成刻提交,导致中间刻空气回滚——已改为逐刻提交。
- `WorkBuffer` 的每游戏刻提取预算在手动 serverTick 循环内不会重置
  (gameTime 冻结),热链测试必须用自然 tick + `runAfterDelay` 断言,
  并用满充电能水晶(1,000,000 EU)覆盖 50,100 HU 的取热需求。

## 未验收 / 后续

- 客户端实机:热量/空气表读数、单元槽交互、与固体/流体/电热源的贴面链、
  弹出/拉入升级、外观。记录于 `/home/codex/minecraft/待测试.md` 第 20 节。
