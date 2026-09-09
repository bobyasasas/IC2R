# 充电垫(P08)

更新日期:2026-09-08。

## 旧行为依据

- `legacy/.../block/wiring/tileentity/TileEntityChargePadBlock.java` 及四个子类
  (BatBox/CESU/MFE/MFSU)
- 规格:tier 1/2/3/4,输出速率 32/128/512/2048,存储 40,000/300,000/4,000,000/40,000,000
- 行为:每 2 tick 检测垫上玩家,按 **主手→副手→盔甲→热栏→背包** 顺序逐件充电;
  单件充电绕过物品单次上限(旧版 charge tier=MAX、ignoreLimit=true),受存储余量与
  `输出×2` 预算约束;充入成功且物品未满时机器显示活跃。

## 新实现

- `machine/ChargepadBlockEntity.java`:继承 `PoweredBlockEntity`,四变体共用;
  serverTick 用 AABB(整格、高 0.9375)查询垫上玩家并调用 `chargeInventory`。
- 能量节点:朝向面为网络输出(可反向馈电),其余面为输入;与储能设备同模式。
- 顺序化充电入口 `chargeInventory(Player)` 公开,供 GameTest 确定性验证。
- 资源由 `machines.py` 从旧 blockstate/模型/掉落/语言生成(四变体十二态)。

## 与旧版差异

- 粒子特效(活跃时的蓝色尘埃)与红石模式按钮暂未实现,列入客户端待测;
- 旧版充电跳过 DEBUG 物品,新版以 ElectricItem 接口为界,DEBUG 物品不属于 ElectricItem
  自然跳过,行为一致。

## 测试证据

- GameTest `ChargepadTests`:
  - `chargepad_inventory`:空电池单周期充入全部存量(1000 EU),垫存储清零;
  - `chargepad_order_limits`:单周期只充第一件(主手优先),存量不足时只转存量;
  - `chargepad_network`:MFSU 通过非输出面向充电垫供电。
- IC2 与 GT 两种能量模式 158 项 GameTest 全部通过(2026-09-08)。

## 未验收范围

- 粒子/红石模式界面、真实玩家站立(非 mock)体验、多人同垫(M16)。
