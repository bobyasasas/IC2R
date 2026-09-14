# 裸电缆家族一致性收尾（第 50 轮切片：12 条 plain cable）

legacy 基线:`ic2/core/block/wiring/AbstractCableBlock.java`(非 foam 分支)、
`ic2/core/item/ItemCable.java`、`ic2/core/energy/profile/ElectricalDisplay.java`、
`ic2/core/ref/Ic2Blocks.java`(12 变体注册)。

## 范围

glass_fibre_cable、copper/insulated_copper、gold/insulated_gold/double_insulated_gold、
iron/insulated_iron/double_insulated_iron/triple_insulated_iron、tin/insulated_tin——
12 个变体共享 `CableBlock` 一条代码路径,此前电击/熔毁/输电语义已由 cable-shocks 与
energy-transfer 切片按代表变体验收;本轮补齐变体级证据与两处遗留缺口。

## legacy 行为依据

- **含水**:`AbstractCableBlock` 非 foam 分支实现 `SimpleWaterloggedBlock`——
  state 含 WATERLOGGED;放置时目标位是水则 waterlogged=true;updateShape 为
  waterlogged 方块调度水 tick;canPlaceLiquid/placeLiquid/pickupBlock 标准水桶交互。
  (foam 分支无 WATERLOGGED,与 port 既有 FoamCableBlock 一致。)
- **ItemCable tooltip**:`appendCableTooltip` 按 EnergyNetMode 分支——
  GT 模式三行(`ic2.electric.tooltip.cable.max_voltage/max_amperage/loss`,裸缆损耗 ×2);
  IC2 模式两行(`item.ic2.cable.tooltip0/1`,Max 电压 EU/t 与损耗)。语言键两套均已预存。
- **数值表**:电压 32/128/512/2048/8192(锡/铜/金/铁/玻璃)、电流 1/2/3/4/8、
  经典损耗 0.2/0.2/0.4/0.8/0.025、绝缘阶梯上限 1/1/2/3/0,已在第 20 轮
  `CableSpec` 恢复。

## 新端移植

- `neoforge/.../energy/CableBlock.java`:实装 `SimpleWaterloggedBlock`——state 加
  WATERLOGGED(默认 false);`getStateForPlacement` 入水放置 waterlogged=true;
  `getFluidState` 返回水源;`updateShape` 为含水状态调度水 tick(legacy 同款)。
  剪线钳升/降绝缘走 `withPropertiesOf`,六向连接与含水状态同时保留。
  blockstate JSON 为 multipart 结构,新属性无需资源改动。
- `neoforge/.../item/CableItem.java`(新):继承 BlockItem,`appendHoverText`
  按 `EnergyConfig.MODE` 复刻两套文案——GT:电压等级名+数值/最大电流/损耗
  (裸缆已按 `insulated()` 编码 ×2);IC2:Max EU/t 与损耗。
- `ModMachines.cable()`:12 变体改用 CableItem 注册(legacy ItemCable 对位),
  detector/splitter 特例仍为普通 BlockItem。
- 游戏测试 `CableFamilyTests`(heat_room):
  - `cable_family_specs`:12 变体全量断言电压/电流/双模式损耗/绝缘阶梯与 CableItem;
  - `cable_insulation_ladder`:铁缆 0→3 层加绝缘(耗橡胶+工具磨损)、满层拒橡胶
    无磨损、三轮剥离回裸缆、裸缆再剥无事件;
  - `cable_waterlogged`:入水放置 waterlogged、流体状态读作水源、剪绝缘保水、
    桶拾取复原;
  - `cable_connection_mask`:六向连接掩码(机器/电缆/泡沫电缆连,惰性方块与空气不连)。

## 有意差异

- 电缆染色(legacy colorProperty,绝缘达阈值可染色并影响连接)依赖染色系统,port 未
  移植——与 foam-cable.md 既有决策一致(统一默认色);如需染色作单独切片由用户裁决。
- 电缆连接对第三方能量接口(IEnergySink/IEnergyEmitter)在 legacy 会显示连接;
  port 视觉连接仅覆盖 IC2 机器与电缆,第三方外部端子(AE2 桥)仅参与电网不参与视觉,
  能量语义不变。

## 测试证据

- 双模式 GameTest 540×2 全绿(此前 536 + 新增 4,`cable_family_specs`、
  `cable_insulation_ladder`、`cable_waterlogged`、`cable_connection_mask`)。
- 既有回归:`cable_shock_*` 4 条、`foam_cable_*` 7 条、`detector_cable_*` 5 条、
  `machine_chain/reconnect` 输电与拓扑失效全部不受影响。

## 外观资源收尾

- 绝缘线与玻璃纤维线的默认颜色恢复为 legacy blockstate 使用的 `white`，不再错误
  引用偏暗的 `light_gray`；旧色纹理继续保留，供未来染色功能使用。
- 12 个普通变体及 detector/splitter 改回 legacy 的共享 `core_N` / `side_N` 精确 UV
  模型，连接臂通过 multipart 旋转复用。模型和新增白色纹理均与 legacy 文件逐字节一致。
- 保持模型环境光遮蔽和方块光照，不添加自发光。旧构建实机已确认导线在白天和暗室中
  随环境光变化、没有错误发光；修复后实机复测负责确认白色绝缘与连接段 UV。
- 包含本资源替换的隔离全量验证为 IC2/GT 双模式各 578 项全部通过；产物检查确认所有
  模型父级和纹理依赖可解析。

## 遗留

- CableItem tooltip 与含水状态的实机(游戏内)观感留人工验收,见 `待测试.md` §136。
- detector/splitter cable 物品在 legacy 是否带独立 tooltip:legacy 特例物品为普通
  BlockItem(无 ItemCable 子类),port 保持一致,无差异。
