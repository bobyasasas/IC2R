# 特斯拉线圈（Tesla Coil）

Legacy `TileEntityTesla`（注册名 TESLA_COIL）移植：红石门控的防御机器，无 GUI、无槽位、
无升级（`Energy.asBasicSink(this, 10000.0, 2)` = 10,000 EU 罐/等级 2 sink）。

## 行为（对照 legacy updateEntityServer/shock）

- **红石门控**：无红石信号时整台机器静止（不耗电、不电击）——legacy `Redstone` 组件的
  `hasRedstoneInput`，port 用 `level.hasNeighborSignal`（Centrifuge 先例）。
- **待机耗电**：有信号时每 tick 先扣 1 EU；能量不足 1 EU 时该 tick 全部跳过（含 ticker）。
- **32 tick 周期**：`++ticker % 32 == 0` 时电击。相位初值按坐标哈希错开
  （对应 legacy `IC2.random.nextInt(32)`——server 侧共享随机不可复现，按太阳能蒸馏器
  坐标哈希先例）。
- **伤害结算**：`totalDamage = (int) stored / 400`；半径 4（AABB −4..+5）内全部 LivingEntity
  （`NO_CREATIVE_OR_SPECTATOR`）平分（整数除法，余数丢弃 = legacy quirk 保留）；每实体
  **全套 hazmat 免疫**（`HazmatLike.hasCompleteHazmat`，免疫者仍计入分母——legacy 语义）；
  `hurtServer(ic2:electricity 伤害源, damage)`（26.1.2 `Entity.hurt` 已变 void，取带
  `ServerLevel` 且返回 boolean 的 `hurtServer`）；任一命中则**再扣 totalDamage × 400 EU**
  （满罐 10,000 → 单次 24 伤 × 400 = 9,600）。
- **粒子**：命中实体处 per damage 点蓝色尘埃（legacy DustParticleOptions (0.1,0.1,1.0) →
  26.1.2 ARGB 0xFF1919FF）。
- **跨阶段账目不变量**：满罐+单实体时无论相位 T₁∈[1,32]，64 tick 后剩余恒为
  10,000 − 64（待机）− 9,600（一次电击）= **336 EU**（第二窗口 totalDamage=0 不再扣）——
  GameTest 以此精确锚定。

## 接线

- `MachineKind.TESLA_COIL("tesla_coil", 10000, 0, 0, 0)` + `electricalTier()=2`；
  `ModMachines.createEntity` 穷尽 switch 新增；`MachineSounds` null 组（legacy 无运行音效）。
- `TeslaCoilBlockEntity extends PoweredBlockEntity`（10,000 EU/0 槽），
  `EnergyNode.Terminal.sink`（tier 2 电压）。
- 资产：legacy 贴图 2 张（`machine/misc/`）、cube_bottom_top 模型、blockstate 全
  facing×active 变体同模型（legacy 无朝向属性差异）、战利品表扳手保机器/普通掉 `ic2:machine`
  （legacy DefaultDrop.Machine）、lang 键已预存（en/zh）。
- 配方 `shaped/tesla_coil`：RRR/RMR/ICI（redstone×5 + mv_transformer + circuit + iron_casing，
  照搬 legacy）。

## 验证（GameTest 3 项，双模式 381 项全绿）

- `tesla_coil_shock`：满罐+上电+猪（10 HP<24 伤）64 tick 内死亡；账目精确 336 EU。
- `tesla_coil_gate`：无信号 64 tick 猪活且罐原封不动（待机也不扣）；上电但抽至 200 EU
  （<400 → totalDamage=0）64 tick 后恰 136（只付待机）、猪仍活。
- `tesla_coil_hazmat`：三实体分摊 24 → 每实体 8：两只无甲猪各剩精确 2.0 HP（同时证明
  分母计入免疫者与伤害平分），全套 hazmat 僵尸满血（免疫）。

## 状态

M11/P08 辅助机器链新增特斯拉线圈。勘误记录：26.1.2 `Entity.hurt` void 化与
`hurtServer(ServerLevel,...)` boolean 返回；`Level.random` protected 须经 `getRandom()`；
GameTest 假玩家不在实体列表，`getEntitiesOfClass` 不可见——实体扫描类测试必须用真实 spawn 的 Mob。
