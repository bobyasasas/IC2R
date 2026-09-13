# Scanner 手持扫描 GUI 切片（OD Scanner / OV Scanner）

legacy：`ItemScanner`（use 耗电→开手持菜单→立即扫描）、`ItemScannerAdv`（range 12 规格）、
`ContainerToolScanner` / `GuiToolScanner`（结果 GUI）、`StackUtil.oreTags`（矿石判定）。
port：`ScannerItem.use()` + `ScannerMenu` + `ScannerScreen` + `ModTools.SCANNER_MENU`。

## 行为映射

| legacy | port |
|---|---|
| 空手 use：tier1 扣 50 EU / tier2 扣 250 EU，电量不足返回 FAIL 不开 GUI | `ScannerItem.use`：电量门控在 ServerPlayer 分支之外（两侧一致 FAIL），扣电仅在服务端 |
| openManagedItem 开手持菜单（主手/副手槽位） | `ServerPlayer.openMenu(SimpleMenuProvider, data.writeVarInt(slot))`（crop_analyzer 范式）；菜单标题 `stack.getHoverName()` |
| 开菜单后立即 scan 玩家周围立方体（range 6/12，isAir 跳过） | `ScannerMenu` 服务端构造时 `scan(player.level(), player.blockPosition())`；`scanAround` 静态可测 |
| `StackUtil.isOreStack`：`Ic2ItemTags.ORES` + 8 个原版矿石 item tag | `Tags.Items.ORES`（=c:ores，含 IC2 四金属）+ `ItemTags.{COAL,COPPER,DIAMOND,GOLD,IRON,EMERALD,LAPIS,REDSTONE}_ORES` |
| `ItemComparableItemStack(pickStack, ignoreDamage)` 按 item 聚合计数 | `Object2IntOpenHashMap<Item>` 聚合（矿 VARIANT 各自成堆，与 legacy item 键一致） |
| `scanMapToSortedList` 数量降序 | 数量降序 + item raw id 决胜（legacy 未定义平序，取确定性） |
| GUI 最多 10 行 “count x name”，行色 5752026，标题 “ic2.scanner.found”（Find:）色 2157374，图标双列 x=135+(row&1)*15 / y=28+11*row，230 宽 | `ScannerScreen` 同坐标/同色/同文案；容器数据 `ContainerData` 21 int（[0] 条数，每行 rawId+total），行高 11、玩家物品栏 y=152/快捷栏 y=210 |
| `onDroppedByPlayer` 丢弃关菜单 | meter 同款：`containerMenu instanceof ScannerMenu` 且是本手槽位才关 |

## 现代化取舍（记档）

1. legacy `getCloneItemStack`（pick block 语义）在 26.1.2 已移除 → `new ItemStack(state.getBlock())`（矿石方块自身物品形态等价）。
2. legacy `setResults` 走 `sendContainerField("scanResults")` 网络字段 → `ContainerData` 打包同步（数据槽变更检测沿用原版机制，一次性下发）。
3. 窗口标题 legacy `container.ic2.scanner`（OD Scanner）→ `stack.getHoverName()`（OD/OV 自适配，少一条硬编码）。
4. GameTest 的 mock player 非 ServerPlayer，`openMenu` 路径不可直测（crop_analyzer 同）→ 服务端菜单直构造 + use() 电量门控可测。

## GameTest（ic2_tests，heat_room，IC2/GT 双模式）

| 测试 | 断言 |
|---|---|
| `scanner_menu_scan` | 3×3×3 立方体：iron_ore×2 聚成一堆居首、deepslate_iron_ore/copper_ore 各自成堆（数量降序 2,1,1）；全部通过矿石 tag；plain stone 不上报 |
| `scanner_menu_use` | 菜单直构造：stillValid 持手保持；scan 后 data slots 报 3 堆且首堆 2；handScanRange 6/12；charged use→SUCCESS 且 mock 侧不扣电（服务端才扣）；10 EU 下 use→FAIL |
| `scanner_menu_drop` | 持手开菜单后 `onDroppedByPlayer` → containerMenu 关闭（复位为 inventoryMenu） |

全量：IC2/GT 双模式 499×2 绿（基线 496 → 499）。

## 待人工视觉验收（留用户）

1. ScannerScreen 整体观感：230 宽扁平面板、“Find:” 标题、行文案与图标双列位的实际渲染效果（legacy 为 230×231 贴图 GUI）。
2. 矿石物品图标在双列条带中的排布密度（11px 行距下图标 16px 的 legacy 式重叠是否可接受）。
3. 扫描结果的即时性观感（开菜单后 1-2 tick 内行/图标经数据槽填充）。
