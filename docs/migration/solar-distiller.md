# 太阳能蒸馏器

被动流体机器：不接电网、不用燃料，把水缓慢蒸馏成蒸馏水。每 72 tick 为一个工作周期，周期开始在方块上方取样 `SolarGeneration.brightness`（与太阳能发电机同一亮度公式），亮度 > 0.5 且入水罐非空、蒸馏罐未满时转化 1 mB 水 → 1 mB 蒸馏水。

- 恒定 72 tick 速率（如实记录）：legacy 按 biome HOT=36/COLD=144 分档，但 `EnvProxy.biomeHasType` 桩恒 false，两档不可达；与 crops.md 既定决策一致，移植取常数 `TICK_RATE = 72`。周期相位用坐标哈希错开（对应 legacy `random.nextInt(tickRate)`，但确定性）。
- 双罐各 10,000 mB：入水罐只收水（legacy `fluidPredicate(Fluids.WATER)`），蒸馏罐外部只可抽取；流体自动化 `Capabilities.Fluid.BLOCK` 全侧面注册（insert→入水罐、extract→蒸馏罐）。
- 容器四槽位（legacy `ContainerSolarDistiller` 坐标）：左侧水下（17,27）/水出（17,45）——顶部自动化，水桶排水入罐、空桶移到出槽；右侧蒸馏入（136,64）/蒸馏出（136,82）——底部自动化，从蒸馏罐装桶。入口槽后端同样只收流体容器（防漏斗塞杂物）。
- 升级槽 2 个（legacy GUI 显示 2、`InvSlotUpgrade` 声明容量 3 的差异按 GUI 收敛为 2）；适用升级走通用默认集（变压器/储能/超频等非定向项，legacy `getUpgradableProperties` 为 ItemConsuming/ItemProducing/FluidProducing 桩）。legacy `getEnergy=40/useEnergy=true` 为升级结算桩、机器不用能量，未移植假能量账目。
- 屏幕双罐显示 + 光照百分比（progress=亮度×1000），无能量条、无进度条；无运行音效（legacy 同样没有）。
- 资源：blockstate 全 facing×active 变体映射同一静态模型（legacy 方块无朝向属性，port 侧 `MachineBlock` 恒带 FACING/ACTIVE）；模型与三张贴图（top/bottom/sides）照搬 legacy；配方 GGG/G G/CMC（玻璃+machine+facade_cell）；战利品表按扳手保机器/普通掉外壳惯例。

GameTest 3 项（含同步 test_instance JSON，双模式共 374 项）：规格与菜单 42 槽/流体端口规则（只进水、输出只出不进）/入口只收流体容器/升级槽位/罐保存重载；昼间开阔位每周期恰 +1 mB、夜晚与不透明遮罩停机且不耗水、遮罩解除恢复（各阶段按增量断言，规避真实方块 tick 与光照传播异步）；水桶入罐+空桶归位、蒸馏水装桶、顶/底自动化端口方向。

实测记录：菜单与端口规格、昼间周期、容器交换双模式通过；机房 (2,2,2) 有顶不可用于光照测试，用 (8,1,8) 开阔位（与太阳能发电机测试同款）；雨天正午亮度 11/16≈0.69 仍高于 0.5 门控，雨天不停机（legacy 同），不作为停机测试项。
