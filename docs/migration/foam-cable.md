# 发泡电缆家族（第 17 轮切片：12 条 plain foam cable）

legacy 基线:`ic2/core/block/wiring/FoamCableBlock.java`(及 AbstractCableBlock 的泡沫分支)、
`ic2/core/block/wiring/CableFoam.java`、`ic2/core/item/tool/ItemSprayer.java`(CABLE 分支)、
`ic2/core/item/tool/ItemToolCutter.java`、`ic2/core/ref/Ic2Blocks.java`(配对注册)。

## legacy 行为依据

- **独立方块 id 配对**:foam cable 不是 cable 的状态,而是兄弟类
  (FoamCableBlock 与 CableBlock 都继承 AbstractCableBlock),经
  foamToCable IdentityHashMap + registerFoamCounterpart 一一配对注册。
  foam cable 属性带 randomTicks,state 仅 foamProperty(无六向连接、无
  WATERLOGGED);无 BlockItem(只能靠喷涂获得)、无独立配方。
- **CableFoam 17 态**:SOFT + HARD×16 色(染料墙色),DEFAULT_HARD 为浅灰。
  foam present 时 shape 恒全块;soft 时 occlusion 空(可见内部电缆)。
- **硬化**:onPlace/onLoad soft→scheduleTick(1);tick 里
  `random.nextFloat() < FoamBlock.getHardenChance(world,pos,state,FoamType.normal)`
  则 setValue(HARD 默认色),否则 re-schedule(1)。公式
  `1/(300×(16-光照)×20)`,即 300s 基准×光照衰减。
- **玩家破坏**:onDestroyedByPlayer 无条件把泡沫转回 cable counterpart
  (toCableState+withConnectionStates)并 return false——方块不真破坏、不掉落,
  泡沫壳消失恢复原电缆。
- **切割器**:AbstractCableBlock.attack 门 `!isHardFoam` →
  ItemToolCutter.removeInsulation → tryRemoveInsulation(types 表 insulation-1)。
  **legacy 缺陷如实记档**:
  1. removeInsulation 里 `(CableBlock) 强转`对 soft foam cable 会 CCE
     (FoamCableBlock 不是 CableBlock 子类);
  2. types map 按 CableType+insulation 存方块且**普通 cable 后注册覆盖 foam**
     ——tryRemoveInsulation 永远落到普通 cable:soft foam insulated cable 被剥
     绝缘=变低一级普通 cable(**泡沫丢失**)、0 级拒;
  3. hard 泡沫挡刀(拒剥);foam cable 上**加绝缘**(useOn)不可用
     (instanceof CableBlock 不命中)。
- **喷枪 CABLE 分支**:命中的是普通 CableBlock → 原位 BFS(六向,排除玩家
  朝向反方向)沿电缆网络把整段 cable 转 foam(SOFT),计费 100 mB/块,
  模式上限同泡沫(普通 10/单块 1)。点已 foam 的 cable:not CableBlock →
  走 ANY 分支贴面放 foam block(legacy 同)。

## 移植实现

- `neoforge/src/main/java/ic2/neoforge/energy/FoamCableBlock.java`:独立方块,
  字段 material/insulation/specification+CODEC 仿 CableBlock;FOAM 两态
  SOFT/HARD(StringRepresentable;legacy 17 态色族因 port 无电缆染色系统而
  简化为两态,记档);shape 恒全块、soft occlusion 空、注册带 randomTicks、
  noOcclusion+羊毛音效+0.2 强度与普通 cable 一致。
- **硬化**:onPlace soft→scheduleTick(1);tick=tickFoamHardening 复用
  FoamBlock.getHardenChance(FoamType.NORMAL→300)→HARD 或 re-schedule(1)。
  **26.1.2 无方块级 onLoad 钩子**(legacy onLoad 安全网不可用):randomTick
  兜底重挂 probe(注册了 randomTicks),存档往返丢 tick 也能复活硬化循环,记档。
- **破坏**:@Override IBlockExtension.onDestroyedByPlayer→
  CableBlock.connectedState(counterpart,level,pos)(重建六向连接)+return false
  ——方块不破坏、无掉落;getDrops/getCloneItemStack 委托 counterpart
  (CableBlock 上相应方法放宽为 public 供委托)。
- **切割**:`CutterItem.strip` 加 FoamCableBlock 分支:hard 拒/0 级拒/soft 降级
  ——legacy types 表覆盖语义的**合理化复刻**(剥一层+泡沫丢失→普通
  cable-1 级),耐久 3+掉橡胶+音效与剥普通电缆一致;attack 门 hard return。
  legacy CCE 缺陷不复现(无强转),记档。
- **喷涂**:`FoamSprayerItem.useOn` 加 CABLE 分支(三分支语义与 legacy 对齐):
  命中普通 CableBlock→BFS 沿网络原位转 counterpart(SOFT),计费/上限不变;
  foam cable 不可再喷(非 CableBlock,点它走 ANY 贴面=legacy 同)。
- **能量网**:`WorldEnergyNetworks.rebuild` 识别 FoamCableBlock→
  EnergyNode.Conductor(specification),喷涂后不断电;`CableBlock.connects`
  识别 foam cable(视觉连接);counterpart 互查经
  `ModMachines.cableCounterpart/foamCounterpart`(FOAM_CABLES 12 条注册,
  id 规则 `_cable`→`_foam_cable`,无 BlockItem)。
- **资产**:12 blockstate(variants foam=soft→`ic2:block/cf/foam`、
  foam=hard→`ic2:block/cf/wall_light_gray`,模型复用泡沫墙族)+12 loot
  (survives_explosion 掉对应普通 cable 物品)。

## 已知取舍(如实记档)

1. **17 态→2 态**:legacy HARD×16 色依赖电缆染色系统(Stainable),port 未
   移植电缆染色,统一硬化为浅灰(与 legacy 默认一致)。
2. **软泡沫不可沙子加速**:legacy AbstractCableBlock 无沙子右键(FoamBlock 才
   有),port 同;只有等待随机探针硬化。
3. **剥泡沫泡沫丢失**:与 legacy types 表行为一致(降级到普通电缆),非新增。
4. **destroyBlock 返回语义**:26.1.2 `ServerPlayerGameMode.destroyBlock` 恒返
   true,onDestroyedByPlayer 的 false 只抑制掉落/destroy 副作用——验收断言
   看转回的电缆状态而非返回值。

## 验证

- GameTest `neoforge/src/gameTest/java/ic2/neoforge/test/FoamCableTests.java`
  7 例:spray_dips(转换+计费+SOFT+调度硬化 tick)、network_spread(BFS 沿
  3 格网络+计费+单块模式)、carries_power(喷涂后发 1000 EU 仍到 MFE)、
  break_reveals(FakePlayer 破坏→转回普通电缆+六向连接重建)、
  cutter_strips(soft 降级+耐久 3/0 级拒/hard 拒)、hardening(固定
  RandomSource 桩直调 tickFoamHardening:输→re-schedule,赢→HARD)、
  registry_matrix(12 对注册+双向配对对称+id 后缀)。
- 全量 `:core:test + :neoforge:runGameTestServer` 472×2 IC2/GT 双模式绿。
