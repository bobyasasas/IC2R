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

## 测试证据

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

## 未验收 / 后续

- UuGraph 全量配方图解析器与 `uu_values.json` 数据包化;pattern_storage
  邻接链接;replicator;tooltip 的 UU 价值显示。
- 客户端外观随实机测试(待测试.md 第 30 节)。
