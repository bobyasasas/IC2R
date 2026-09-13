# 交易机(P08)

更新日期:2026-09-12。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityTradeOMat.java`
- 四槽:demand/offer 模板(各 1 件)、玩家投入、输出;无限模式(不消耗库存)与
  库存模式(从相邻供应容器取 offer、投入物分发回供应);stock 每 64 tick 盘点;
  totalTradeCount 持久化。

## 新实现

- `machine/TradeOMatBlockEntity.java`:同事务完成"从邻接供应取 offer → 输出插入 →
  扣除投入 → 分发回供应";无限模式跳过供应。
- 第 54 轮所有者界面:`owner`/`ownerName` 入 NBT,`permits(player)` 复刻
  `TileEntityPersonalChest.checkAccess`(无主首触认领、OP 或 owner 放行);
  `handleButton(player,0)` 复刻 `onNetworkEvent` 且仅 OP 可翻转(`canToggleInfinite`,
  服务端每次点击复核);`createMenu` 按打开者传 `tradeEditable`, legacy
  Open/Closed 双容器合一布局,访客的 demand/offer 模板槽只读。
- 客户端 `client/TradeOMatScreen.java`:OP 见 20x20 "∞" 按钮(legacy (152,4)),
  按钮可见性用 vanilla 同步的 `LocalPlayer.permissions()`(≥GAMEMASTERS);
  访客见 stock 行(负数显 ∞、0 红色 0xff5555、其余 0xff404040);本机无 EU,
  去掉通用屏的空能量条。布局坐标沿用现代统一槽位(模板 56/102,17;出入 56/102,53),
  stock 行画在 (8,46)/(48,46)(legacy Closed 在 (12,60)/(50,60),因出入槽位置不同平移)。
- 自动化:仅输入槽可插、输出槽可抽,模板槽关闭。
- 资源由 `machines.py` 生成。

## 测试证据

- GameTest `TradeOMatTests`:
  - `trade_o_mat_infinite`:无限模式消耗需求、凭空给出报价并计数;
  - `trade_o_mat_supply`:库存模式从邻接储物箱取报价、投入物回流。
- 第 54 轮新增:
  - `trade_o_mat_owner`:首触认领、owner 经菜单放模板、访客菜单模板槽
    mayPlace/mayPickup 全拒而输入槽可插;
  - `trade_o_mat_toggle_gate`:非 OP 认领者 `clickMenuButton(player,0)` 被拒、
    无限标记不变,直接 `toggleInfinite()` 仍可用(OP 场景无 mock,留人工)。
- IC2 与 GT 两种能量模式 548 项 GameTest 全部通过(2026-09-12,本地零失败)。

## 未验收范围

- GUI 观感(∞ 按钮、stock 行配色与位置)与 OP 名单实机翻转、多人同时交易(M16)
  留人工;
- legacy ∞ 按钮在专用服务器上因客户端取不到 server 而不显示的怪癖,端口改用
  同步权限集后在专用服务器同样可见(现代化差异,记录不回删)。
