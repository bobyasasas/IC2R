# 交易机(P08)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityTradeOMat.java`
- 四槽:demand/offer 模板(各 1 件)、玩家投入、输出;无限模式(不消耗库存)与
  库存模式(从相邻供应容器取 offer、投入物分发回供应);stock 每 64 tick 盘点;
  totalTradeCount 持久化。

## 新实现

- `machine/TradeOMatBlockEntity.java`:同事务完成"从邻接供应取 offer → 输出插入 →
  扣除投入 → 分发回供应";无限模式跳过供应;`menuAction(0)` 切换无限。
- 自动化:仅输入槽可插、输出槽可抽,模板槽关闭。
- 资源由 `machines.py` 生成。

## 测试证据

- GameTest `TradeOMatTests`:
  - `trade_o_mat_infinite`:无限模式消耗需求、凭空给出报价并计数;
  - `trade_o_mat_supply`:库存模式从邻接储物箱取报价、投入物回流。
- IC2 与 GT 两种能量模式 168 项 GameTest 全部通过(2026-09-09)。

## 未验收范围

- 所有者模板编辑界面、stock 显示、多人同时交易(M16)。
