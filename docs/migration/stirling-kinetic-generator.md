# 斯特林动能发电机切片：stirling_kinetic_generator

legacy `TileEntityStirlingKineticGenerator` + `ContainerStirlingKineticGenerator`
（HU 拉取加热流体并以 4 HU→12 kU 记账 bank 成 kU 的动能源），port 落位为
`StirlingKineticGeneratorBlockEntity`（MachineBlockEntity + FluidMachine），
HU/kU 两侧复用 Work 能力体系（HEAT 消费侧拉取 + KINETIC provider 输出）。

## 记账语义（逐条对照 legacy）

- 双罐各 2000 mB：冷端仅接受 heating 配方可加热流体（数据驱动 CachedCheck），
  热端全收；容器槽 0/1（冷进冷出）、2/3（热进热出）。
- HU 缓冲上限 1000：每 tick 从除朝向外五面拉取（`getCapability(HEAT, 邻位, 反向)`，
  hasChunk 预检），请求量 `min(1000-heat, source.available())`，参与事务提交。
- 转换批次 `batch = min(heat/4, (2000-ku)/3, min(输出空间,输入量)*配方热量 - 液热累计)`，
  每批次 `ku += batch*12`、`heat -= batch*4`、`liquidHeatStored += batch`；
  液热累计满一个 `recipe.heat()`（water/coolant 均 20 HU/mB）才转移整 mB
  （输入 extract 与输出 insert 同一事务，失败整体回滚含 kU 记账）。
- **kU 缓冲溢出忠实保留**：legacy 热满时单 tick 批次上限可令 kuBuffer 一次性越过
  2000（1000 HU→3000 kU），仅提取预算受限——port 用
  `WorkBuffer(Integer.MAX_VALUE)` 构造绕开容量校验（SteamTurbine 先例），
  停机条件改由 `stored >= 2000` 判断（ku>2000 时 (2000-ku)/3 为负自然停转）。
- kU 仅从朝向面输出（`WorkOutput` 默认 FRONT，bandwidth=2000）；
  `fuelRemaining/fuelMaximum` 即 kU 储量供能量条与 menuValue(0)。
- 3 升级槽按 legacy ItemConsuming/ItemProducing/FluidConsuming/FluidProducing
  ⇒ `UpgradeItem.suitable` directional() 全四类放行（与 heat exchanger 同列）。

## 注册、GUI 与配方

- `MachineKind.STIRLING_KINETIC_GENERATOR("stirling_kinetic_generator",0,4,0,0)`：
  verticalFacing、menuHeight 184、upgradeSlots 3 特判；
  BE 工厂 case 直分派（不进 workConversion()），ModMachines 注册 KINETIC provider。
- 菜单布局仿 legacy ContainerStirlingKineticGenerator：冷容器/出罐在左（8/26, 65）、
  热容器/入罐在右（134/152, 65），升级列由通用 upgradable 分支 (180,17+18i)；
  `preferredSlot` 含流体→槽 0、空容器→槽 2 的移位路由。
- `StirlingKineticScreen`：无能量条/进度条，双 `FluidTankDisplay`
  （legacy 坐标 19/145）+ Stored kU 文本行，注册于 IndustrialCraftClient。
- heating 配方数据驱动（镜像 cooling 先例）：`HeatingRecipe` record
  （1 mB 模板 + 正热量校验）+ `HEATING` RecipeType/Serializer，2 条 JSON
  `data/ic2/recipe/heating/{hot_water,hot_coolant}.json`（heat=20）。
- 资源由 `tools/migration/resources/machines.py` 清单生成（blockstate/models/
  textures/loot/pickaxe tag）；lang 沿用既有双语条目（zh "斯特林动能发生器"）。

## 验证

- 3 例 GameTest（`StirlingKineticTests`，460×2 IC2/GT 双模式全绿）：
  1. `heats_fluids_and_banks_ku`：10 线圈 coil（同 tick 预算 100 HU）支撑单 game tick
     内 10 轮手动 tick 的确定性记账 → 99 mB 水/1 mB hot_water/300 kU 精确断言 +
     FRONT 面 available==300、热拉面 0 + `loadStatic` 重载保留 kU/罐/液热记账。
     **记档**：手动多轮 serverTick 共享同一 gameTime 的 WorkBuffer 提取预算——
     源带宽不足时第 2 轮起 `available()==0`、拉热停摆，属预算体系设计语义
     （heatTransactions 已断言同 tick 预算持久），测试以带宽冗余规避而非放宽断言。
  2. `containers_route_both_ways`：流体口 0 口只进（water/coolant 可入、
     hot_water 无配方拒、0 口不可抽取）、1 口只出；水桶→冷罐/空桶←热罐的容器
     原子槽路由；升级槽 FLUID_EJECTOR/FLUID_PULLING 放行、OVERCLOCKER 拒绝。
  3. `chain_charges_batbox`：电热线圈(西)→斯特林动能→动能转换器(东,面西)→BatBox
     序列式链路，`storage.energy()>=64` 后断言真实水量被消耗（IC2/GT 双模式）。
- catalog 同轮翻转 4 条：item/block/block_entity/menu `ic2:stirling_kinetic_generator`
  → implemented；配方计数 777→778（heating 新增 1 条模板组），registry pending 148→144。
