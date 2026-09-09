# 传送机与频率传送器(P08)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../item/tool/ItemFrequencyTransmitter.java`(121 行):无电力物品。
  右键传送机:未记忆时记住该机;再次右键另一台则两机互设目标(双向链接);
  自身/已建立链接给出提示且保持记忆;目标处非传送机则清除记忆。
  空中右键:链接后紧接着的一次只解除"刚设置"标记,之后再按清除记忆。
- `legacy/.../machine/tileentity/TileEntityTeleporter.java`(404 行):
  - 无内部缓冲、不接电缆:能量直接从相邻储能设备(`isTeleporterCompatible`,
    即储能箱/变压器类)均摊抽取;
  - 红石信号 + 已链接目标时激活;在 (x−1..x+2, y..y+3, z−1..z+2) 盒内找最近
    无载具存活实体传送;冷却 20t(接收方 `onTeleportTo`);
  - 能耗 `weight × (distance+10)^0.7 × 5`;weight:物品实体 100×数量/堆叠上限、
    动物/矿车/船 100、玩家 1000 + 背包重量(配置默认开)、恶魂 2500、
    凋灵 5000、末影龙 10000、怪物 500;乘客重量递归累加;
  - 目标失效(非传送机)自动清除;比较器有目标输出 15;
  - 粒子(蓝/绿)与充能/使用音效为客户端表现。

## 新实现

- `item/FrequencyTransmitterItem.java`:链接/解除逻辑等价移植;记忆改为
  数据组件 `frequency_pos`(BlockPos)与 `frequency_just_set`(Boolean),
  提示与 tooltip 走既有语言键。
- `machine/TeleporterBlockEntity.java`:继承 `MachineBlockEntity`(不接电网);
  红石检测、最近实体选择、冷却与目标失效等价移植;能量从相邻
  `EnergyStorageBlockEntity`(四级储能箱)与 `TransformerBlockEntity` 的
  `EnergyStore` 均摊抽取。
- weight 语义:保留全部 legacy 系数与背包重量开关的默认值(开);
  旧版玩家主手物品被重复计一次的怪癖未保留(记录为有意清理)。
- 简化/延后项:比较器输出、蓝绿粒子与音效事件(`ModSounds` 中已注册的
  传送机声音未接线)、`canEntityDestroy` 爆炸抗性未迁移;
  跨维度传送旧版本就不支持,保持同维度。
- 资源:`machines.py` 生成传送机 blockstate/模型/纹理/掉落表;
  `tools.py` 生成频率传送器物品定义/模型/纹理;`recipes.py` 解锁两配方
  (574/796)。

## 测试证据

- GameTest `TeleporterTests`(IC2/GT 双模式 185 项):
  - `teleporter_link`:两次 `useOn` 建立双向链接;64 石头物品实体从传送机
    跨 4 格传送到目标上方;相邻 MFE 恰好扣除
    `100 × (4+10)^0.7 × 5 ≈ 3170 EU`;
  - `teleporter_cooldown_shortage`:能量不足(100 EU)时实体不动;
    接收方冷却期内红石触发也不回传;
  - `teleporter_unlink`:空中两次使用后记忆清除。

## 未验收范围

- 比较器、粒子、音效、爆炸保护待后续切片;
- 真实玩家传送(ServerPlayer 路径)、红石实际搭建与双人场景随 M16 实机验收。
