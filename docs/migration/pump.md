# 泵(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityPump.java`、`ic2/core/util/PumpUtil.java`
- 规格:20 EU 存储、tier 1、每操作 20 tick(1 EU/t)、4 升级槽、8,000 mB 流体罐;
  容器槽(空桶顶进/满桶侧出)自动从罐装桶;面向面液体源优先,否则向后追踪流动水找源
  (`PumpUtil.searchFluidSource`,含空气桥与最大步数);每次操作抽至多 1,000 mB 并移除
  源块;与矿机联动供液(miner.liquidPos)。

## 新实现

- `machine/PumpBlockEntity.java`:20 tick 周期、每次抽≤1,000 mB 并移除源块
  (`setBlock(AIR)`,无限水源由原版流体再生机制自然成立);水源搜索简化为
  **从面向格出发的 BFS**(下/四向扩散,上限 32 格)——与旧版"沿流动反查+空气桥"的
  差异记录:实际水域(整片源区)行为一致,极端流动桥场景可能取到更近的源。
- 罐→桶装填走 `FluidContainerPort`(空桶 0 进、满桶 1 出);流体能力:罐只可抽取。
- 矿机联动(miner.liquidPos)待矿机迁移后接入;当前单机模式与旧版一致。
- 资源由 `machines.py` 生成。

## 测试证据

- GameTest `PumpTests`:
  - `pump_faced_water`:面向水源一操作抽 1,000 mB、源块消失、进度归零;
  - `pump_fills_buckets`:空桶自动装满并进入输出槽、罐水被消耗;
  - `pump_progress_reload`:进行中的进度经真实方块实体保存/替换后保留并继续。
- IC2 与 GT 两种能量模式 171 项 GameTest 全部通过(2026-09-09)。

## 未验收范围

- 与矿机的供液联动(随矿机切片)、升级槽速率(升级系统统一验收 P09)、BFS 与旧版
  追踪在极端地形的差异、真实多人取水(M16)。
