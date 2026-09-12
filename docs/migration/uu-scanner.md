# P11 复制链切片一:水晶存储盘(crystal_memory)

2026-09-09 切片。开始迁移 P11 复制链,本切片先行迁移存储盘物品——
legacy `ItemCrystalMemory`(内嵌 ItemStack 的模式存储盘)及其空白晶体
`raw_crystal_memory`(既有 MaterialDefinition 物品管线)。

## 旧行为依据

- `crystal_memory`(stacksTo 1)内嵌一个 `Pattern` ItemStack(NBT);
  `readItemStack`/`writeContentsTag` 读写。
- tooltip:空白盘显示 `<Empty>`;已记录盘显示 `Item: <物品名>`
  与 `UU-Matter: <UuIndex 值>`(价值部分随 UuGraph 切片)。
- 合成:raw_crystal_memory 冶炼成 crystal_memory(+ 能量水晶充电链等
  关联配方)。

## 新设计

- `CRYSTAL_MEMORY_PATTERN` 数据组件(`ItemContainerContents`,单槽),
  与过滤器卡片/工具箱同一组件化惯例;空白 = 组件缺省。
- `CrystalMemoryItem.readPattern/writePattern`:读写内嵌 ItemStack;
  tooltip 空白/已记录两态(物品名)。
- 注册于 `ModReactorItems`(INGREDIENTS 创造页);reactor.py 资源管线
  (items 定义/模型/纹理/双语名);raw_crystal_memory 已由
  MaterialDefinition 管线注册,不重复注册。

## 切片四:replicator(2026-09-09)

- `ReplicatorBlockEntity`:16,000 mB UU 罐(fluidSlot 灌 `uu_matter_cell`
  → 1,000 mB 入罐、空单元落 cell 槽);模式取自相邻 pattern_storage;
  SINGLE/CONTINUOUS/STOPPED 三模式(menuAction 2/3/4);每 tick 消耗
  1 mB UU + 512 EU,产出模式物品。**文档化单位简化**:模式堆叠数 =
  所需 UU 毫升,legacy 1 mB=1000 UU 单位换算随 UuGraph 数据包化统一。
- **付费门**:罐空时 `tank.extract` 返回 0 即停机——修复了"先计数后扣费"
  导致的无中生有复制;产出事务在扣费成功后原子提交。
- 另修复:事务 `Transaction.openRoot()` 未闭合导致同刻其他机器开启事务
  报"already active"的泄漏。

## 测试证据

- `ReplicatorTests.replicatesPatternFromStorage`:UU 单元灌罐 → SINGLE 模式
  产出铁锭、空单元收集、单次后停机。
- `modeStopsWithoutUu`:无 UU 供给时不产出。
- IC2 与 GT 双模式 `runGameTestServer` 231 项全绿。

## 测试证据(切片一)

- `CrystalMemoryTests.recordsAndReadsPattern`:空白盘读出为空 → 写入
  铁锭 → 读回铁锭 → 写空盘清空(组件写/清语义)。
- IC2 与 GT 双模式 `runGameTestServer` 224 项全绿。

## 切片三:pattern_storage(2026-09-09)

- `PatternStorageBlockEntity`(无能量,1 盘槽):去重模式列表(按物品
  相等)持久化(`patterns` ItemStack 列表);菜单事件 0/1 前后浏览、2 把
  选中模式写回盘槽水晶记忆;`addPattern/getPatterns` 实现扫描器与复制机
  的共享模式源。
- scanner 完成扫描时的落点优先级:盘槽水晶记忆优先,否则写入相邻
  pattern_storage(legacy `record()` 分支)。
- 待补:浏览按钮的实机操作、replicator 的模式消费。

## 切片二:uu_scanner 本体(2026-09-09)

- `ic2.core.uu.UuValueGraph`(core,纯 Java):节点按物品 id 键;初始值
  seed + 配方转换(成本:合成 0/熔炼 14);min 赋值传播直至稳定
  (legacy `Node.setValue/updateValue` 算法等价)。
- `UuScannerBlockEntity`(extends PoweredBlockEntity,512,000 EU 缓冲、
  2 槽 = 输入 + 水晶盘、sink 256×1):每 tick 256 EU、3300 tick 完成扫描,
  将输入物品写为水晶盘模式;价值表未覆盖的物品 FAILED 且不耗电。
- `UuScannerScreen`(输入/水晶盘/进度)与客户端注册;uu_scanner 方块
  资源与合成入列。
- **已知裁剪**:价值图种子仅 iron/copper ingot(完整配方图解析器与
  `uu_values.json` 数据包化随后续切片);pattern_storage 邻接链接未迁移
  (无盘时 FAILED,等价 legacy TRANSFER_ERROR 分支)。

## 测试证据(切片二)

- `UuScannerTests.scansSeededItemOntoMemory`:铁锭扫描 3300 tick 后记录到
  水晶盘、输入消耗、补电续跑。
- `unknownItemFails`:价值表外物品(钻石)不消耗、盘保持空白。

## 测试证据(切片一)

- `CrystalMemoryTests.recordsAndReadsPattern`:写/读/清空组件语义。
- IC2 与 GT 双模式 `runGameTestServer` 226 项全绿。

## 切片五:UuGraph 数据包化与扫描覆盖面扩展(2026-09-10)

- **数据包化**:`UuValueReloadListener` 注册到
  `AddServerReloadListenersEvent`(全局 NeoForge 总线);每个
  `uu_values/*.json` 为 `{"item": id, "value": n}` 数组,合并后非空集
  即替换内置种子。发布 `data/ic2/uu_values/world_scan.json`——legacy
  `IC2UuScanConfig` 默认 world-scan 清单 **128 条逐字迁移**
  (cobblestone=1.0 基准,含分数值与 1.7E8 级大值);`UuSeed` 值域由
  int 放宽为 double。修复:`getAsInt()` 截断分数值(首个 GameTest 运行
  即暴露,dirt 14.857→14)。
- **扫描覆盖面**:legacy 覆盖面 = world-scan 配置清单(UuIndex 的配方
  resolver 在 IC2R 中从未注册,属死代码);数据包发布后扫描器可扫 128
  种世界方块/物品,叠加 IC2 加工族转换(成本 0)形成闭包。
- **replicator 换算统一**(legacy TileEntityReplicator):
  `patternUu = getInBuckets(值×1e-5)`,1 bucket = 1000 mB,每 tick 消耗
  `1e-4` bucket(=0.1 mB)+ 512 EU,小数余量经 `extraUuStored` 银行在
  整数 mB 排空间结转;替换此前"模式堆叠数 = 所需 mB"的单位简化。
  `requiredMb = 价值/100`(cobblestone 0.01 mB、铁锭 13,428.95 mB);
  价值非有限(:不在种子集/闭包)的模式跳过不运行(legacy 会永久空转
  烧 UU,新端防御性停止,文档化偏差)。
- **修复**:replicator 此前只检查 `stored() >= 512` 从不扣能——补
  `energy.extract(512)`(legacy `useEnergy`);银行扣减在排空失败时回补
  (legacy 泄漏不回补);`progressMaximum` 修正为当前模式的
  `ceil(requiredMb)`(原为 patterns[0] 的堆叠数)。
- **勘误(切片二)**:"价值图种子仅 iron/copper ingot"与"配方图解析器
  ……数据包化随后续切片"的裁剪记录至此闭环;legacy 配方 resolver 死
  代码与合成/熔炼转换不迁移(legacy 自身未接线,新端加工族转换已按
  0 成本覆盖)。

## 测试证据(切片五)

- `uu_values_datapack`:cobblestone=1.0 与 dirt=14.857483653272267 从
  已发布数据包经 reload listener 读入(内置种子不含这两项,端到端证明);
- `replicator_value_uu`:铁锭整跑 134,290 tick,恰排水
  ceil(价值/100)=13,429 mB + 单次产出后停机;
- `replicator_single` 改用 cobblestone(1 tick/1 mB);`uu_scanner_unknown`
  的未知物品由钻石(已入 world-scan)改为下界之星;
- IC2 与 GT 双模式 `runGameTestServer` 299 项全绿。

## 切片六:UU 链收尾——两段式扫描、复制机升级与价值快照(2026-09-11)

- **扫描机两段式忠实化**(legacy `TileEntityScanner`):扫描完成(3300
  tick)只消耗输入并在内存保留 pattern,须经 GUI record 按钮
  (legacy network event 1)写入盘槽水晶盘或相邻 pattern_storage;落点
  不可用(无盘且无库/库拒收)→ `TRANSFER_ERROR` 卡住保留成品
  (event 0 删除重置)。菜单 id 0=删除、1=保存。
- **ALREADY_RECORDED 预检**:盘或相邻库已有同物品 → 不开扫即
  `ALREADY_RECORDED` 重置,零耗电(对比输入物品判定)。
- **失败双分支**:图不认识的物品(下界之星)首 tick `FAILED` 零耗电;
  图认识但值∞的物品烧满 3300 tick 后 `FAILED` 卡住(legacy 怪癖保留,
  输入变更或 reset 才恢复)。
- **tier 勘误**:初版 port 用 256V,legacy `Energy.asBasicSink(512000, 4)`
  tier 4 = 512V,已改 `sink(energy, 512, 1)`。
- **持久化**:progress/currentStack/pattern/state 经
  ValueInput/ValueOutput 存取(GameTest `BlockEntity.loadStatic` 往返)。
- **复制机超频/升级**(legacy `InvSlotUpgrade`):4 升级槽
  (`MachineKind.REPLICATOR` upgradeSlots=4);超频器 uu/tick ÷0.7ⁿ、
  EU/tick ×1.6ⁿ;变压器 sink tier min(4+n,5);储能 +10000 EU 容量
  (`EnergyStore.resize` + 存读往返 `forceAdd` 补回,luminator 先例)。
  升级白名单按 legacy `UpgradableProperties`(无 FluidProducing,排除
  流体喷射器)。
- **复制机按钮 legacy 化**:0/1=浏览(仅停止态)、3=停止(清进度)、
  4=单次、5=连续(替换初版 2/3/4);选中图案变更(ItemStack 同物品同
  组件比较)→ 进度清零+停机(legacy `refreshInfo`)。
- **∞ 值图案勘误(切片五)**:"∞ 值模式跳过不运行(防御性停止)"至此
  按legacy 忠实化——移除 isFinite 门,∞ 值图案永续烧 UU 永不完成
  (GameTest 钉死该怪癖)。银行扣减失败回补(切片五修复)保留。
- **持久化**:uuProcessed/extraUuStored(银行)/index/mode/pattern
  (选中图案)+ **UU 罐序列化**(初版遗漏,对齐冷凝器
  `tank.serialize/child("tank")` 范式)。
- **水晶盘价值快照**:26.1.2 tooltip 仅客户端、port 价值图仅服务端 →
  记录时(scanner record / pattern_storage 写盘)把 buckets 值烘焙进
  `CRYSTAL_MEMORY_VALUE` 组件;tooltip 用 core `SiString`
  (legacy `Util.toSiString` 逐字移植,含 NaN/∞/SI 前缀表/进位/去尾零)
  显示 "UU-Matter: %sB"。**文档化偏差**:legacy tooltip 显示活图值,
  port 显示记录时快照。清空图案连带清值(legacy 单 NBT 键)。
- **客户端**:UuScannerScreen 增删除/保存按钮、ReplicatorScreen 重写
  (last/Stop/single/repeat 五按钮 + 图案 x/y 索引显示),按钮经
  `handleInventoryButtonClick`(EnergyOMat 先例)。

## 测试证据(切片六)

- `uu_scanner_scan` 强化两段式:完成时盘仍空、输入已耗 → record 后落盘
  + 价值快照≈图值×1e-5;`uu_scanner_unknown` 强化首 tick 零耗电;
  `uu_scanner_already_recorded`、`uu_scanner_persistence`(100 tick 进度
  往返后续扫完成)、`uu_scanner_input_change`、
  `uu_scanner_record_failure`(TRANSFER_ERROR 保持)。
- `replicator_single/value_uu/no_uu` 菜单 id 对齐(4/5/3);
  `replicator_overclock`(恰耗 819.2 EU/tick + 储能扩容)、
  `replicator_persistence`(进度 1.0 mB/罐/银行往返 + 续跑)、
  `replicator_browse_stop`(运行中拒浏览、停止清进度、换图案重置)、
  `replicator_valueless`(∞ 图案 20 tick 恰排 2 mB 永不完成)。
- `crystal_memory_value` 快照组件往返;core `SiStringTest` 7 组断言
  (退化值/前缀缩放/E±n 兜底/进位/去尾零/0.999→"999 m" 怪癖)。
- `pattern_storage_transfer` 补 record 步骤对齐两段式。
- IC2 与 GT 双模式 `runGameTestServer` **405 项全绿**;`:core:test`
  全绿(含 SiStringTest);verify_artifact/progress --check/git
  diff --check 全过。

## 未验收 / 后续

- ~~水晶盘 tooltip 的 UU 价值显示~~:切片六以记录时快照方案落地
  (偏差见上);**tooltip 渲染与屏幕按钮实机观感留人工验收**
  (待测试.md 新增节)。
- 扫描机/复制机屏幕布局对 legacy GUI 的还原度(人工比对)。

