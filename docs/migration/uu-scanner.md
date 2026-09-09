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

## 未验收 / 后续

- uu_scanner 本体(依赖 UuGraph core 化:334 行配方图价值计算)、
  pattern_storage 邻接链接、replicator;tooltip 的 UU 价值显示随 UuGraph。
- 客户端外观随实机测试(待测试.md 第 30 节)。
