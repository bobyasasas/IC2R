# 注册清单与数据组件

旧注册通过 JDK Java AST 解析器读取，包含物品 511、方块 247、方块实体 157、实体 8、菜单 55、声音 62、配方序列化器 17、配方类型 13、流体族 17、游戏事件 5。流体族仍须在 M08 展开为静态流体、流动流体及对应方块；它不是单一流体注册数。

所有旧 ID 当前决策为保留。`registry-catalog.json` 的 `pending` 表示尚未落地；`implemented` 必须附实现与验证证据。M04 完成的是完整注册／资源清单、迁移决策及已接入资源的检查，不表示整个注册系统完成。源码和资源分别由 `legacy-inventory.json`、`resource-inventory.json` 锁定。

已接入 140 个普通材料、4 个可充电电池及 62 个声音事件。普通材料使用独立 `MaterialDefinition` 表，保留旧堆叠数量和稀有度；特殊机器、工具、放射性物品均保留待迁移状态。审查中发现旧版 4 个材料没有英／中文物品名称，现已补齐。

电池沿用 10000/100000/1000000/10000000 EU 容量与 100/256/2048/8092 EU 传输限制，保留旧版堆叠和使用行为。电量计算独立到 core；平台适配器处理单件限制、模拟和组件赋值。旧版 `ic2:charge` 模型谓词改为 26.1 的 `range_dispatch` 属性，客户端属性注册与服务端入口隔离。客户端实际外观仍由 M16 验收。

新增组件 `ic2:charge`、`ic2:fluid`、`ic2:remote_links`、`ic2:hydration_uses`。流体采用不可变 `FluidStackTemplate`；空容器通过组件缺省表示。遥控链接包含维度，复制坐标和列表后保存，最多 1024 个唯一目标。不同维度不会误用同一坐标，过多目标与非法电量在解码入口拒绝。旧 NBT 转换及遥控实际引爆仍分别由 M15、M12 负责。

验证：19 项核心 JUnit 通过；GameTest 7 项通过（6 项 IC2，1 项 Minecraft 自带）；覆盖全部普通材料注册、组件的物品存盘／网络往返、可变输入的防御性复制、充放电模拟、堆叠电池拒绝和无效电量。打包检查覆盖 144 个物品定义、161 个模型依赖、声音资源及客户端／核心模块依赖边界。

复现：

```bash
bash gradlew build :neoforge:runGameTestServer
python3 tools/migration/verify_artifact.py
# 重新从只读基线提取清单；需 JDK 25
python3 tools/migration/catalog/catalog.py
```
