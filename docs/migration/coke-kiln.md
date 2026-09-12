# 焦炭窑(coke_kiln 家族)

2026-09-12 切片(第 16 轮)。迁移 legacy `TileEntityCokeKiln` /
`TileEntityCokeKilnHatch` / `TileEntityCokeKilnGrate` 3×3×3 多方块与
`ic2:coke` 物品,附带 3 条 `ic2:shapeless` 窑体配方解锁(配方 pending
18 → 15)与 registry-catalog 10 条翻转(item×4 + block×3 + block_entity×3,
implemented 677 → 687 / pending 90 → 80)。

## 旧行为依据(legacy 2.10.39)

- **无 EU**。窑体每 **20 刻**做一次"工作相位"(每实例
  `updateTicker = nextInt(20)` 随机相位,错峰;`tickPhase++ % 20` 不等于
  相位时跳过),每次相位推进 20 进度,单次作业 1800 进度(90 个相位)。
- **结构**(控制器朝向面反方向为内部):底层 3×3 耐火砖中心为炉篦
  (grate)、中层 3×3 中心留空且控制器位于其中一条边中央、顶层 3×3
  中心为投放口(hatch)。结构破损即重置进度并停机。
- **配方两条硬编码**:煤 → 焦炭 + 500 mB 木馏油(1800 刻);任意
  `minecraft:logs` 标签原木 → 木炭 + 250 mB 木馏油(1800 刻)。投入 1 个
  产 1 组,持续作业直到投放口清空。
- **投放口**:单槽,仅自身朝向面可插入,任何面不可抽出(物品只能落进
  火箱)。
- **炉篦**:64 桶(64000 mB)木馏油罐,任意面可读写;GUI 桶右键单支
  装/抽、shift 整桶(事件 0/1,方向锁防止刚装满的桶被立刻抽回)。
- **控制器**:输出槽产物仅可抽出不可插入;持久化 progress,重载后配方
  重新推导(与 legacy 一致)。
- 焦炭作为熔炉燃料燃烧 3200 tick(legacy `getBurnTime`)。

## 新设计

- `MachineKind.COKE_KILN / COKE_KILN_HATCH / COKE_KILN_GRATE`(0 EU、
  1/1/0 槽、无能耗);`ModMachines` 自动注册方块/物品/BE/菜单;石头
  mapColor + STONE 音效(保留 METAL 基础 2,10/requiresCorrectTool)。
- `CokeKilnBlockEntity`:20 刻相位门逐字复刻(`RandomSource.create()`
  每实例一次);`hasValidStructure` 三层 27 格校验(中心自校验
  `getBlockEntity(cPos) == this`);`canWork` 用模拟事务预检输出槽与
  罐容量,`finishWork` 消耗 1 投入 + 原子产出;配方重载重推导。
- `CokeKilnHatchBlockEntity`:`ResourcePort` 仅 `side == FACING` 可插入、
  全向禁抽。`CokeKilnGrateBlockEntity`:实现 `FluidMachine`,
  `MachineFluidTank(64000)` 任意面读写;`transferCursor` 逐字复刻
  TankBlockEntity(Boolean filling 方向锁 + `oneByOne` 光标逐支)。
- 菜单/屏幕:窑输出槽(111,30)、投放口单槽(79,35)、炉篦无物品槽;
  三屏关闭能量条(窑保留进度条),炉篦屏绘制流体表 + 点击罐区触发
  `clickMenuButton(0/1)`。`MachineMenu.clickMenuButton` 增加炉篦分支
  (沿用 current-menu/距离/方块身份守卫)。
- **焦炭燃料**:`data/neoforge/data_maps/item/furnace_fuels.json`
  (`{"ic2:coke": {"burn_time": 3200}}`)。**路径教训**:data map 文件的
  datapack 命名空间即 map id 的命名空间——给 `neoforge:furnace_fuels`
  供数必须放在字面 `data/neoforge/data_maps/item/` 下
  (`DataMapLoader.fileToId` 以文件命名空间拼 map id;放 `data/ic2/...`
  会被当成不存在的 `ic2:furnace_fuels` 静默跳过并 WARN)。
- 资源:`machines.py` 列表追加三机器(hatch/grate 的 blockstate 额外
  展开 `active` 变体——仅限这两个 id,已交付机器保持逐字节不变);
  coke 物品 + 双语 tooltip(多方块拼装说明)+ 镐可挖标签 + 掉落表。

## 测试证据(`CokeKilnTests` 5 例,真实结构 setBlock + 手动相位循环)

- `coke_kiln_coal`:2 煤两次完整作业 → 焦炭 ×2、罐 1000 mB 木馏油、
  投入口清空后停机;输出槽插入拒绝/全向可抽;菜单 37 槽;焦炭
  `getBurnTime == 3200`(真实验证 fuel data map 加载)。
- `coke_kiln_logs`:橡木原木 → 木炭 ×1 + 恰好 250 mB。
- `coke_kiln_structure`:裸窑停机 → 围合后作业 → 拆顶砖 → 进度重置
  停机;投放口正面插入 4、背面拒绝、全向禁抽、拒绝路径零泄漏。
- `coke_kiln_grate_fluid`:端口任意面收木馏油/拒第二流体/任意面抽空/
  64000 上限(回滚验证);GUI 桶单支装、shift 整桶抽、未知按钮拒绝;
  炉篦零物品自动化。
- `coke_kiln_persistence`:900 进度存档 → `loadStatic` 还原
  progress/operationLength → 重新入世界续完剩余 900 → 焦炭 + 500 mB;
  hatch 内容与 grate 流体各自 round-trip。
- 手动循环技巧:相位门每 20 次调用恰好一次工作相位,
  `passes(N) = 20×N` 次 `serverTick` 对任意随机相位确定性地给 N 个
  相位(90 相位 = 1800 进度 = 一次完整作业)。

## 验证

5 例 `-PtestSelection` 隔离全绿 → IC2 模式全量 **465** 全绿(460+5)
→ GT 模式全量 465 连续两轮全绿(首轮 1 例 flaky 未捕获名字,与本切片
确定性循环无关,如实记录)。

## 排障记录(如实)

- fuel data map 首放 `data/ic2/neoforge/data_maps/item/`(想当然多加了
  `neoforge/` 段)与 `data/ic2/data_maps/item/`(命名空间错误)均加载
  失败,GameTest `getBurnTime` 实测 0 暴露后定位:必须
  `data/neoforge/data_maps/item/furnace_fuels.json`。
- 炉篦 `MachineKind` 槽数为 0,物品 `ResourcePort` 对空 handler 调
  `insert(0,…)` 会直接 `IndexOutOfBounds` 而非返回 0——测试改断言
  `size() == 0`(与 legacy"炉篦无物品访问"语义一致)。
- 还原 BE 的结构自校验(`getBlockEntity(cPos) == this`)对游离对象恒
  假:`loadStatic` 产物须 `level.setBlockEntity` 装回世界才能续跑,
  GameTest 据此修正。

## 未验收 / 后续

- 客户端实机:三屏布局与流体表渲染、桶光标交互手感、多方块外观与
  active 贴图、拼装引导 tooltip。记录于 `/home/codex/minecraft/待测试.md`
  第 102 节(人工视觉/UX 验收,保留用户侧签署)。
