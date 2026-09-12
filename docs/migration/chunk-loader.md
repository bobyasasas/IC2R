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
- GUI 的 9×9 区块选择网格（legacy ContainerChunkLoader + onNetworkEvent）未做，menu 条目
  维持 pending 与手持 GUI 基建同批；`addChunk/removeChunk/isChunkInRange/maxChunks 9`
  逻辑已 1:1 落位供 GUI 轮直接接线。

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
