# chunk_loader 切片

legacy 蓝本：`ic2/core/block/machine/tileentity/TileEntityChunkLoader.java`（263 行，
`@NotClassic`）+ `ic2/core/ChunkLoaderLogic.java`（243 行）。解锁 `shaped/chunk_loader`
（4 锡板+末影珍珠+青金石+机器+电路），配方计数 773 → 774、pending 25 → 23（23 含本条
前已解 2 条 alloy）。

## 机器

- `MachineKind.CHUNK_LOADER("chunk_loader", 2500, 1, 0, 0)`：容量 2500、基础槽 1
  （discharge）、`upgradeSlots()` 特判 4（legacy InvSlotUpgrade 4 槽）；经 ModMachines
  自动注册 block/BlockItem/BlockEntity/MenuType。
- `ChunkLoaderBlockEntity extends PoweredBlockEntity`（**不能**用 UpgradeableBlockEntity：
  其 UpgradeProfile.Base 校验拒绝 ticks=0 的机器，首跑暴露）：
  - 每 tick `ensureOwnChunk()`（惰性等价 legacy onPlaced 的自身 chunk 种子）→ 电池放电
    进 buffer（FluidRegulator 范式）→ `energy.consume(loadedChunks.size() × 1.0 EU)`
    （legacy IC2Config.balance.euPerChunk 默认 1.0；port 尚无 balance config 族，常量+记档）；
  - 电力跟随：激活瞬间对 loadedChunks 全集加票、断电释放（legacy setActive 分支同位）；
  - 升级自扫（legacy setOverclockRates 等价）：变压器 +1 sink tier（封顶 5）、储能
    +10000 容量；`UpgradeKind.suitable(CHUNK_LOADER)` 仅变压器/储能（legacy
    UpgradableProperty 无 Processing）；
  - `onLoad` 补挂缺失票（legacy onLoaded 重挂；vanilla 票恢复时幂等跳过）；
  - `preRemoveSideEffects` 拆除释放全部票；loadedChunks 经 `Codec.LONG.listOf()` NBT 往返。
- ~~GUI 的 9×9 区块选择网格未做~~（后续轮已补齐，见下节「9x9 选择画布」）；
  `addChunk/removeChunk/isChunkInRange/maxChunks 9` 逻辑 1:1 落位，画布直接接线。

## 票（26.1.2 结构分歧记档）

`ChunkLoaderTickets`：`DeferredRegister(BuiltInRegistries.TICKET_TYPE)` 注册
`ic2:chunk_loader` = `TicketType(NO_TIMEOUT, PERSIST|LOADING|SIMULATION)`，
`addTicketWithRadius/removeTicketWithRadius(radius 2)`。
legacy 自管 SavedState（chunksToChunkLoaders）+ WorldData 运行时映射的两级恢复链在
26.1.2 由 FLAG_PERSIST 票的 vanilla TicketStorage 持久化天然覆盖，port 省去
SavedData（结构分歧，语义一致）。

## GameTest（ChunkLoaderTests 3 例）

| id | 覆盖 |
| --- | --- |
| chunk_loader_tickets_follow_power | 5 EU 跑 3 tick → ACTIVE+票在+余量恰 2 EU；抽干后 ACTIVE=false+票释放 |
| chunk_loader_breaking_releases | 激活后 destroyBlock → 票释放（preRemoveSideEffects） |
| chunk_loader_nbt_round_trip | 窗口内 chunk 可加/窗外 ±8 拒绝（isChunkInRange）；save/load 往返保留两 chunk |

IC2 模式隔离 3/3 绿；GT 模式抽验 `chunk_loader_tickets_follow_power` 绿（全量见 STATUS）。

## 9x9 选择画布（ChunkLoaderScreen，画布轮新增）

本节补齐 legacy `GuiChunkLoader` 的核心交互——机器周围 dx,dz∈[-4,4] 的 9x9 区块选择画布。

### 服务端语义（menuAction / menuValue）

legacy 点击语义为「已加载→移除，未加载→添加」，自身区块的移除被拒绝。port 将画布编码进
通用菜单链路（`MachineMenu.clickMenuButton` 默认分支 → `menuAction(id)`）：

- `menuAction(0..80)`：`dx = id % 9 - 4`，`dz = id / 9 - 4`，按 legacy 语义 toggle；
- `menuValue(0..2)`：81 格选中位图按 `cell = (dz+4)*9 + (dx+4)` 折成 3×27bit 词（int 安全范围）；
- `menuValue(3)` = 已加载数、`menuValue(4)` = 9（legacy `maxChunks`），客户端 "n / 9" 计数直接消费。

### 客户端（ChunkLoaderScreen）

- 画布绘制在 (8,18)..(152,162)；选中遮罩 `0x3000FF00`（半透明绿）、未选 `0x30FF0000`
  （半透明红），与 legacy 两常量一致；
- 基类能量条 (25,18..68) 与画布重叠，`showsEnergyBar()=false` 改为在 (162,18..68) 自画竖条；
- 升级列 (180..196) 与 discharge 槽 (162,143) 沿用 port 统一 202 宽槽位范式（菜单高度 252）；
- 悬停提示显示 ChunkPos 坐标与选中状态（`ic2.chunkloader.selected/unselected`，双语）。

### 三项现代化取舍（记档）

1. **地形缩略图简化**：legacy 为每格注册 16x16 `DynamicTexture`（逐块 `MapColor` 采样、
   退出 GUI 逐一释放）；port 取消纹理管理，每格用 8x8 采样（`WORLD_SURFACE` 高度图 +
   `getMapColor`）的均值色单色填充，未加载区块保持黑色。观感为简化版缩略图，
   **属人工视觉验收项**；
2. **事件链路改菜单按钮**：legacy 客户端经 `initiateClientTileEntityEvent`
   （`id=(dx+8&15)|((dz+8&15)<<4)`）→ 服务端 `onNetworkEvent`；port 走
   `handleInventoryButtonClick`（id 0..80）→ `menuAction`，与全机队菜单范式一致；
3. **布局适配槽位范式**：legacy 250 宽贴图布局不放升级列；port 统一 202 宽机器布局
   （升级列+discharge 右置），画布 (8..152) 与右列 (162..196) 分列。

### GameTest 增补（2 例）

| id | 覆盖 |
| --- | --- |
| chunk_loader_canvas_toggle | 空机 count=0/max=9；own 格 toggle 先加载后拒移；41 格加/减/再加位图精确（word1 bit13+bit14）；补满 9 后第 10 格拒绝；三词最终值与选中集逐位一致 |
| chunk_loader_canvas_reload | 3 格选中（含东南角 72 格）经 `loadStatic` 往返后三词与 count 完全一致 |

两例隔离 IC2/GT 双模式各绿；全量 496×2 绿（基线 494→496）。

### 待人工验收（客户端实机，保留用户签署）

1. 画布实机观感：MapColor 均值色块替代 legacy 逐块地形缩略图的差异是否可接受
   （未加载区块黑色、选中绿色遮罩、未选红色遮罩、n/9 计数位置）；
2. 点击加载/卸载区块的实际生效（远景区块出现、断电后卸载）与点击自身区块被拒的反馈；
3. 悬停提示（ChunkPos 坐标+选中状态）与能量竖条、discharge/升级列布局无重叠。
