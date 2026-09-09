# 物质生成机(matter_generator)与 UU 增幅配方

2026-09-09 切片。迁移 legacy `TileEntityMatter`(320 行)与
`ic2:matter_fabricator` 配方家族(2 条)——P11 UU 链的第一块:产 UU 流体。

## 旧行为依据

- 纯能量缓冲机器:1,000,000 EU(tier 3,`matterFabricatorTier` 缺省 3);
  缓冲满(或输入停滞且仍有存余)时**清空整缓冲换 1 mB UU-Matter**,注入
  内部 8,000 mB 罐;`uuEnergyFactor` 缺省 1(EU:UU 固定 1,000,000:1)。
- 增幅:`scrap`(+5,000)/`Scrap Box`(+45,000)放入增幅格,计入 `scrap`
  计数(上限 10,000);缓冲每获得 1 EU,就消耗 1 点 scrap 并返还 5 EU
  ——即 scrap 把当刻收益放大 5 倍,约 1/6 时间完成。
- 红石信号门控全部工作(缓冲保留)。
- 罐内 UU 灌装空单元(OpType.Fill,顶面放置的容器格):整单元交换,
  产出专用 `uu_matter_cell` 物品到输出格。
- 4 升级槽;比较器按增幅格计数(未迁移,与近期切片一致)。

## 新设计

- `MachineKind.MATTER_GENERATOR("matter_generator", 1000000, 3, 0, 0)`:
  槽 3 = 增幅 0 / 输出 1 / 空单元进 2;4 升级槽(物品弹出/拉入);
  `electricalTier` 3。
- `MatterGeneratorBlockEntity extends PoweredBlockEntity`:
  - `energyNode()` sink(energy, 512, 3);`readyToGenerate` 用"本刻增益停滞
    或缓冲已满"规则(与 legacy 逻辑意图一致,见下方差异)。
  - scrap 增益:每刻 `bonus = min(scrap, 缓冲增量)`,`insert(5×bonus)` 并
    扣减计数;`consumeAmplifier` 经 `ic2:matter_fabricator` 配方查找。
  - 空气…不对,UU 灌装走 `FluidContainerPort.of(inventory, 2, 1)` +
    `move(tank → port)`,整单元交换,产出直接落到输出格。
  - `acceptsInventorySlot`:输出格接受流体容器(机器自写入灌满的单元);
    增幅格任意、单元格仅容器。
  - `menuValue(0)` = 罐占比;进度条 = 缓冲填充比例(÷1000)。
- `MatterFabricatorRecipe`(原料、input_count、`result` UU 值)与类型/
  序列化器 `ic2:matter_fabricator`;recipes.py 家族分支透传 result。
- 资源:`machines.py` 列表(十二态/Machine 掉落/挖掘标签/双语名)。
  `uu_matter` 流体与 `uu_matter_cell` 已由 fluids.py/ModCells 既有管线注册。

## 差异说明

- legacy `isReadyToGenerate` 依赖包电压抽象(`free < packetVoltage`);新实现
  以"缓冲本刻增益停滞(无输入)或已满"等价改写,半包行为不变:有电缆持续
  供电时只在满缓冲时产出,无输入时立即产出。
- 比较器输出未迁移(与近期切片一致)。

## 测试证据

- `MatterGeneratorTests.scrapAmplifiesAndGenerates`:1 scrap 计入 5,000;
  后续 5,000 EU 收益触发 25,000 EU 返还(scrap 归零);停滞缓冲产出 1 mB
  并清空缓冲。
- `fillsUuMatterCells`:1,000 批灌罐后,空单元(facade_cell)被灌成专用
  `ic2:uu_matter_cell` 落入输出格,罐清空。
- `redstoneGateStopsGeneration`:红石门控冻结产出且缓冲不损失。
- IC2 与 GT 双模式 `runGameTestServer` 206 项全绿;2 条 matter_fabricator
  配方进入 converted-recipes 加载清单。

## 未验收 / 后续

- 客户端实机:罐/进度/增幅读数、单元灌装手感、与 cable/MFSU 的真实供能、
  外观。记录于 `/home/codex/minecraft/待测试.md` 第 22 节。
- P11 余量:`uu_scanner`/`pattern_storage`/`replicator`(模式复制链)与
  `UuGraph` 物品价值扫描;手持扫描 GUI 此前已随扫描器切片确认未迁移。
