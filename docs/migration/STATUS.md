# NeoForge 26.1.2 迁移进度

更新：2026-09-07 · Minecraft 26.1.2 / NeoForge 26.1.2.107 / Java 25

阶段完成：**8 / 17**。完整迁移的旧 Java 文件：**2 / 938**。

这些计数不表示功能完成率或工时进度。当前是迁移开发版本，旧机器与旧世界兼容性仍待验收。

此页由 `plan.json` 生成；修改后运行 `python3 tools/migration/progress.py`。

| 任务 | 状态 | 前置任务 | 验收标准 |
|---|---|---|---|
| M01 构建与架构基础 | 已完成 | — | Java 25 构建、核心单测、平台启动测试、CI 和进度校验全部通过。 |
| M02 电压与电流规则抽离 | 已完成 | — | 独立于 Minecraft 编译；覆盖六档电压、边界、溢出和机器设置切换。 |
| M03 铜板注册与资源切片 | 已完成 | — | 保留 ic2:copper_plate；注册、模型、纹理及语言可解析；专服 GameTest 通过。客户端外观另由 M16 验收。 |
| M04 全部注册 ID 与资源清单 | 已完成 | M01, M03 | 逐项登记旧物品、方块、实体、菜单 ID；每项明确保留、替换或废弃；材质和模型检查通过。 |
| M05 物品数据组件与持久化 | 已完成 | M01, M04 | 用数据组件表达电量、流体、遥控绑定；保存与网络 Codec 往返测试；无静态 ItemStack 初始化。 |
| M06 能量网络计算内核 | 已完成 | M02 | 抽离拓扑、损耗和分配规则；能量守恒、过压、断网重连测试；世界访问通过接口注入。 |
| M07 配方与机器状态机 | 已完成 | M02 | 先迁移单输入机器；配方匹配、进度、暂停和产出以纯逻辑测试覆盖；无全局 IC2 服务读取。 |
| M08 物品／流体传输适配 | 已完成 | M01, M05 | 对接 26.1.2 传输 API；模拟与提交一致；容量、侧面权限和回滚测试通过。 |
| M09 首台可用电力机器 | 进行中 | M06, M07, M08 | 发电机—导线—电炉的最小链路；耗能、配方、存取、区块重载与专服 GameTest 通过。 |
| M10 菜单与网络同步 | 进行中 | M05, M09 | 类型化 payload、服务端校验；双人交互与断线重连；客户端渲染 API 更新。 |
| M11 罐装机及其余加工机器 | 待开始 | M09, M10 | 按机器族拆分后续提交；优先覆盖 cannerfix1 路径，再覆盖升级、泵、存储和高阶机器。 |
| M12 反应堆与爆炸系统 | 待开始 | M06, M09 | 热量规则独立测试；反应堆、核弹、炸药与遥控的游戏回归；权限与持久化验证。 |
| M13 作物、世界生成及装备 | 待开始 | M04, M05, M08 | 进入阶段前按作物／树木矿物／工具装备拆子任务；资源生成和存档重载测试。 |
| M14 JEI、Jade 与 AE2 集成 | 待开始 | M10, M11 | 按实际支持 26.1.2 的版本接入；每个集成独立边界；缺少可选模组仍能启动。 |
| M15 旧存档兼容策略 | 待开始 | M04, M05, M11, M12, M13 | 在副本上验证 ID、NBT 和版本跨度；给出可复现转换流程，或明确不支持的项目。 |
| M16 客户端、多人及性能验收 | 待开始 | M10, M11, M12, M13, M14 | 客户端视觉检查、双人专服、保存重载与性能基线；不得以编译通过替代功能验收。 |
| M17 26.1.2 候选版本发布 | 待开始 | M15, M16 | 功能对照清单审查、完整构建、校验和与 Actions 候选 Release；此阶段前只发 CI 开发产物。 |

## 注册迁移覆盖

| 注册类别 | 已实现 | 部分实现 | 基线总数 |
|---|---:|---:|---:|
| item | 148 | 14 | 511 |
| block | 0 | 14 | 247 |
| block_entity | 0 | 2 | 157 |
| entity | 0 | 0 | 8 |
| menu | 0 | 0 | 55 |
| sound | 62 | 0 | 62 |
| recipe_serializer | 0 | 0 | 17 |
| recipe_type | 0 | 0 | 13 |
| fluid_family | 0 | 0 | 17 |
| game_event | 0 | 0 | 5 |

流体族尚须展开为实际流体与方块。完整状态见 [注册清单](registry-catalog.json)。

## 验证证据

- M01：[settings.gradle](../../settings.gradle), [build.gradle](../../core/build.gradle), [build.gradle](../../neoforge/build.gradle), [verification.md](../../docs/migration/verification.md), [build.yml](../../.github/workflows/build.yml), [progress.py](../../tools/migration/progress.py)
- M02：[VoltageTierTest.java](../../core/src/test/java/ic2/core/energy/VoltageTierTest.java), [ElectricalProfileTest.java](../../core/src/test/java/ic2/core/energy/ElectricalProfileTest.java), [verification.md](../../docs/migration/verification.md)
- M03：[ModItems.java](../../neoforge/src/main/java/ic2/neoforge/registration/ModItems.java), [verification.md](../../docs/migration/verification.md), [RegistrationTests.java](../../neoforge/src/gameTest/java/ic2/neoforge/test/RegistrationTests.java), [verify_artifact.py](../../tools/migration/verify_artifact.py)
- M04：[registry-catalog.json](../../docs/migration/registry-catalog.json), [resource-inventory.json](../../docs/migration/resource-inventory.json), [RegistryScanner.java](../../tools/migration/catalog/RegistryScanner.java), [verify_artifact.py](../../tools/migration/verify_artifact.py), [registry-components.md](../../docs/migration/registry-components.md)
- M05：[ModDataComponents.java](../../neoforge/src/main/java/ic2/neoforge/component/ModDataComponents.java), [RemoteLinks.java](../../neoforge/src/main/java/ic2/neoforge/component/RemoteLinks.java), [ComponentTests.java](../../neoforge/src/gameTest/java/ic2/neoforge/test/ComponentTests.java), [registry-components.md](../../docs/migration/registry-components.md)
- M06：[EnergyGraph.java](../../core/src/main/java/ic2/core/energy/grid/EnergyGraph.java), [PacketDistributor.java](../../core/src/main/java/ic2/core/energy/grid/PacketDistributor.java), [EnergyNetworkTest.java](../../core/src/test/java/ic2/core/energy/grid/EnergyNetworkTest.java), [energy-transfer.md](../../docs/migration/energy-transfer.md)
- M07：[MachineProcess.java](../../core/src/main/java/ic2/core/machine/MachineProcess.java), [MachineProcessTest.java](../../core/src/test/java/ic2/core/machine/MachineProcessTest.java), [MachineTests.java](../../neoforge/src/gameTest/java/ic2/neoforge/test/MachineTests.java)
- M08：[ResourcePort.java](../../neoforge/src/main/java/ic2/neoforge/transfer/ResourcePort.java), [TransferTests.java](../../neoforge/src/gameTest/java/ic2/neoforge/test/TransferTests.java), [energy-transfer.md](../../docs/migration/energy-transfer.md)
- M09：[WorldEnergyNetworks.java](../../neoforge/src/main/java/ic2/neoforge/energy/WorldEnergyNetworks.java), [MachineTests.java](../../neoforge/src/gameTest/java/ic2/neoforge/test/MachineTests.java)
- M10：[MachineMenu.java](../../neoforge/src/main/java/ic2/neoforge/menu/MachineMenu.java), [MachineScreen.java](../../neoforge/src/main/java/ic2/neoforge/client/MachineScreen.java)

## 依赖关系

```mermaid
flowchart TD
  M01["M01 构建与架构基础 · 已完成"]
  M02["M02 电压与电流规则抽离 · 已完成"]
  M03["M03 铜板注册与资源切片 · 已完成"]
  M04["M04 全部注册 ID 与资源清单 · 已完成"]
  M01 --> M04
  M03 --> M04
  M05["M05 物品数据组件与持久化 · 已完成"]
  M01 --> M05
  M04 --> M05
  M06["M06 能量网络计算内核 · 已完成"]
  M02 --> M06
  M07["M07 配方与机器状态机 · 已完成"]
  M02 --> M07
  M08["M08 物品／流体传输适配 · 已完成"]
  M01 --> M08
  M05 --> M08
  M09["M09 首台可用电力机器 · 进行中"]
  M06 --> M09
  M07 --> M09
  M08 --> M09
  M10["M10 菜单与网络同步 · 进行中"]
  M05 --> M10
  M09 --> M10
  M11["M11 罐装机及其余加工机器 · 待开始"]
  M09 --> M11
  M10 --> M11
  M12["M12 反应堆与爆炸系统 · 待开始"]
  M06 --> M12
  M09 --> M12
  M13["M13 作物、世界生成及装备 · 待开始"]
  M04 --> M13
  M05 --> M13
  M08 --> M13
  M14["M14 JEI、Jade 与 AE2 集成 · 待开始"]
  M10 --> M14
  M11 --> M14
  M15["M15 旧存档兼容策略 · 待开始"]
  M04 --> M15
  M05 --> M15
  M11 --> M15
  M12 --> M15
  M13 --> M15
  M16["M16 客户端、多人及性能验收 · 待开始"]
  M10 --> M16
  M11 --> M16
  M12 --> M16
  M13 --> M16
  M14 --> M16
  M17["M17 26.1.2 候选版本发布 · 待开始"]
  M15 --> M17
  M16 --> M17
```

[架构与工作约定](architecture.md) · [原始文件清单](legacy-inventory.json)
