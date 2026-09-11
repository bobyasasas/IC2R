# 能源交易机（Energy-O-Mat）

Legacy `TileEntityEnergyOMat`（注册名 ENERGY_O_MAT）移植：玩家向其他玩家**反向售电**的
个人机器——机主锁定需求物品与单价，访客投入物品换取 EU 信用；机器只在信用余额内从电网
拉电，缓冲后经充电槽/标记面输出。

## 行为（对照 legacy updateEntityServer）

- **换物付账**：input 槽中与 demand 模板一致的整组物品，经 `StackUtil.distribute` 语义
  **整组落入相邻容器**（任一方向、逐槽合并）后交易才成立：input 清空、
  `paidFor += euOffer`（默认 1,000，下限 100）。放不下则事务回滚、物品原样退回
  （TradeOMat drawOffer/giveBack 的 Transaction 模式复用；注意 insert 直接传剩余量——
  TradeOMat 的 `min(remaining, present)` 是**取出**侧语义，误用于插入会恒插 0）。
- **信用门控**：`paidFor > 0` 才从电网收电（legacy `getDemandedEnergy =
  min(paidFor, 余量)` 按 EU 计量；port 的 PacketDistributor 无 sink 端计量钩子，改为
  `acceptsFrom(side) = 非标记面 && paidFor>0` 的**连接级门控** + 每 tick 以
  `stored − lastSeen` 计量到账、等额扣信用——粒度为一个包的差额（如实记录）。
  paidFor 跨 0 时 `WorldEnergyNetworks.invalidate` 重建拓扑（EnergyStorage sending 先例）。
- **缓冲与输出**：进账进 10,000 EU 缓冲（储能升级 +10,000/个）；`stored≥1` 时充电槽按
  物品传输限制充电（RE 电池 100 EU/t——legacy InvSlotCharge 同为按 tick 上限）；标记面
  （正面）作为唯一输出口把缓冲回吐电网（`emitsTo(facing)`），sink tier 恒 1
  （legacy `getSinkTier=MAX_VALUE` 全电压接受的最接近 port 语义）。
- **升级**：legacy 每 tick 重算 `getEnergyStorage(10000,0,0)` 与 `1+extraTier`；port 手写
  switch（Miner 先例）：储能→容量 10,000×(1+n)，变压器→tier（cap 5，Miner 同款 port 界限）。
- **调价**：legacy 网络事件 0-3/4-7 = −/+ 100,000/10,000/1,000/100，`attemptSet` 下限
  100；port `menuAction(0..7)` 同映射，`EnergyOMatScreen` 八键键盘 + 实时价格文本。
- **legacy IPersonalBlock 认领/不可破坏（canEntityDestroy=false、扳手需 permitsAccess）**
  随 TradeOMat 同待遇简化，如实记录；lang 键已预存（en/zh）。

## 接线

- `MachineKind.ENERGY_O_MAT("energy_o_mat", 10000, 3, 0, 0)` + `upgradeSlots()=1`
  （默认公式不适用——`euPerTick>0` 才给 4 槽）+ `menuHeight()=184`（价格键盘几何）；
  `ModMachines.createEntity` 穷尽 switch 新增；`MachineSounds` null 组（legacy 无运行音效）。
- `EnergyOMatBlockEntity extends PoweredBlockEntity`（demand/input/charge+1 升级槽；
  demand 槽 limit 1，自动化端口 input 收/charge+input 出、demand 模板不可自动化）。
- 菜单：demand(38,74)/input(74,74)/charge(110,74)+升级列（x=180 通用）；shift-click
  电类→充电槽、其余→input（`preferredSlot` 分支）。
- 资产：legacy 贴图 4 张（`personal/`）、cube 模型（north=front）、blockstate 全
  facing×active 变体同模型、战利品表扳手保机器/**普通掉自身**（legacy DefaultDrop.Self，
  同 trade_o_mat；非 tesla 的 ic2:machine）、配方 `shaped/energy_o_mat`（RBR/CMC：
  redstone×2 + re_battery + insulated_copper_cable×2 + machine，照搬 legacy）。

## 验证（GameTest 4 项，双模式 385 项全绿）

- `energy_o_mat_trade`：demand+input 各 1 铁锭+相邻储物箱，一 tick 后 input 空、铁锭入箱、
  `paidFor==1000`、`acceptsFrom(UP)` 翻真、缓冲仍为 0（交易本身不凭空产生 EU）。
- `energy_o_mat_gate`：无信用时 `acceptsFrom(UP)==false`、仅标记面 `emitsTo`；即使不付费，
  手动灌入缓冲仍按传输限制（6 tick）充满充电槽电池、paidFor 恒 0。
- `energy_o_mat_charge`：先交易买 1,000 信用，灌 600 EU：首 tick 到账计量扣信用至恰 400，
  电池经 8 tick 恰好 600、缓冲清零（到账↔信用↔充电的账目闭环）。
- `energy_o_mat_price`：−100→900、−100000 触底 100、+1000→1,100、`menuValue(0)` 同步。

## 状态

M11/P08 辅助机器链新增能源交易机。勘误记录：TradeOMat giveBack 的插入语义与 drawOffer
不同（`min(remaining,present)` 仅适用于取出）；RE 电池 100 EU/t 传输限制使单 tick 充电
断言必须多 tick 化。待人工：实机 GUI 键盘布局与价格文本渲染。M14 人工验收保留人工，
P20 待清单全部验收后再启动。
