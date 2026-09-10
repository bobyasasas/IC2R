# 电缆电击(P13 切片九)

## legacy 行为依据

`legacy/forge-1.20.1/src/main/java/ic2/core/energy/grid/EnergyCalculatorUnified.java` 的
`applyCableEffects`(由 `applyDeferredEffects` 在每个世界刻延迟调用)对每条能量路径:

- `amount = path.maxPacketConducted`(本刻该路径承载的最大包功率,含线路损耗)。
- `amount > minConductorBreakdownEnergy` → 电缆熔毁移除;`amount > minInsulationBreakdownEnergy` → 剥绝缘。
- `amount > path.minInsulationEnergyAbsorption` → 对路径上每根导线,若
  `amount > conductor.getInsulationEnergyAbsorption()`,则 `shockEnergy = (int)(amount - absorption)`;
  导线方块位置 ±1 格的 AABB 与活体实体包围盒相交时,每条路径每实体取最大 shockEnergy,跨路径累加,
  最后 `damage = ceil(累计 / 64)` 以 `Ic2DamageSource.electricity`(`ic2:electricity` 伤害类型)结算。
- 全部分支由 `enableEnetCableMeltdown` 配置总开关控制。

阈值数值(全部取自恢复源码):

| 量 | 来源 | 值 |
|---|---|---|
| 绝缘吸收 | `AbstractCableBlock` 导体:`maxInsulation == 0` 时 `Integer.MAX_VALUE`;否则 `capacity < 128` 用 `getPowerFromTier(insulation)`,`capacity ≥ 128` 用 `getPowerFromTier(insulation + 1)` | 8/32/128/512/2048(锡 0/1 层、铜 0/1、金 0–2、铁 0–3) |
| 电力等级功率 | `EnergyNetGlobal.getPowerFromTier(tier) = 8 << (tier × 2)` | ULV 8、LV 32、MV 128、HV 512、EV 2048 |
| 导线熔毁 | `getConductorBreakdownEnergy() = capacity + 1` | 33/129/513/2049/8193(新端既有 CABLE_VOLTAGE 判定与此一致) |
| 剥绝缘 | `getInsulationBreakdownEnergy()` 恒为 9001 | 见下"有意不迁移" |

## 新端移植

- `core/.../energy/grid/CableSpec.java`:记录新增 `maxInsulation` 组件;
  `insulationAbsorption()` 复刻上表(玻璃/检测/分线盒族返回正无穷);
  `powerFromTier(tier)` 复刻 `8 << (tier×2)`(tier ≥ 30 时封顶 9.223372E18)。
- `core/.../energy/grid/PacketDistributor.java`:结果新增 `RouteLoad(target, conductors, maxPacket)`
  列表——IC2 模式记录实际发送量(accepted+损耗,等价 legacy `effectiveAmount`),GT 模式记录包电压;
  同一目标路径按刻内取峰值,对应 legacy `maxPacketConducted`。core 层不接触世界与实体。
- `neoforge/.../energy/WorldEnergyNetworks.java#applyCableShocks`:按路径最弱绝缘粗筛后,
  对每根过载导线方块 ±1 格 AABB(`getEntitiesOfClass(LivingEntity.class, …, isAlive)`)细筛,
  路径内取最大、跨路径求和,`ceil(shock / 64)` 点伤害经 `hurtServer` 以 `ic2:electricity` 结算;
  `cableShocks` 配置开关(`energy.cableShocks`,默认开)。
- `data/ic2/damage_type/electricity.json` 自 legacy 原样复制(exhaustion 0、scaling
  `when_caused_by_living_non_player`);死亡消息键 `death.attack.ic2.electricity(.player)` 已在语言文件中。

## 26.1.2 适配

- `LivingEntity.hurt` 已弃用,伤害走 `hurtServer(ServerLevel, DamageSource, float)`,
  与 `Ic2Explosion` 的既有调用一致。
- 伤害类型 Holder 经 `level.damageSources().damageTypes.getOrThrow(key)` 获取,
  资源键用 `net.minecraft.resources.Identifier`。

## 有意差异

- **剥绝缘不迁移**:legacy 剥绝缘阈值 9001 高于全部电缆熔毁阈值(capacity+1 ≤ 8193),
  且 `cablesToStrip.removeAll(cablesToRemove)` 优先熔毁——该分支在现有电缆数值表下不可达,
  行为上两种实现均为"只熔毁不剥绝缘"。玩家侧手动剥绝缘已由剥线钳(CutterItem)覆盖。
- **GT 模式同样应用电击**:legacy 只有 IC2 统一计算器。新端 GT 模式在包电压超过电缆电压上限时
  直接断路(不记录路径负载,因此不电击);包电压在上限以内但超过绝缘吸收时(如 2048 EU 过裸铁缆)
  仍按同一绝缘表电击。四项 GameTest 同时约束两种模式。
- legacy 的总开关对应新端既有 `cableMeltdown`/`machineExplosions` 加新 `cableShocks`,粒度更细。

## 测试证据

- core JUnit(`EnergyNetworkTest`):`routeLoadsTrackPeakPacketPowerPerRoute`(峰值包统计)、
  `insulationAbsorptionFollowsRecoveredTierTable`(锡 8/32、铜 32/128、金 32/512、铁 2048、玻璃 ∞)。
- GameTest `CableShockTests`(heat_room,y=1 地板层,发电机 32 EU 单包 → 电缆 → MFE/MFSU):
  - `uninsulated_cable_shock_uninsulated`:裸锡缆旁的猪掉血、链路仍在供电、缆未熔毁(两种模式);
  - `cable_shock_insulated`:一层绝缘吸收 32 EU 包,猪满血且链路正常(两种模式);
  - `cable_shock_glass`:玻璃纤维缆永不电击,猪满血且链路正常(两种模式);
  - `cable_shock_gold_meltdown`:MFSU 2048 EU 包过裸金缆,IC2 模式猪被 32 点电击致死且缆熔毁,
    GT 模式断路不电击但缆同样熔毁。
- IC2 与 GT 两种能量模式分别 286 项 GameTest 全部通过(此前 282 + 新增 4)。

## 遗留

- P13 剩余:遮蔽墙重纹理(ItemObscurator 电动工具 + obscured_wall + TileEntityWall 参考面渲染,
  自涂色器切片拆出);电缆泡沫覆盖随电缆切片;辐射效果随 P16。
- 实机(游戏内)验收项见 `待测试.md` §46。
