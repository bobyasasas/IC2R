# 加工与合成配方迁移

已注册铁炉、打粉机、提取机和压缩机，以及 13 个普通材料方块／机器外壳。

- 铁炉：160 刻／次，使用原版燃料时长。加工中断清空进度，已点燃燃料继续消耗，与电力机器暂停进度的语义分开。
- 打粉、提取、压缩：300 刻／次、2 EU／刻。与电炉共用 `ProcessingBlockEntity` 的事务、库存、经验与持久化，只替换配方匹配。
- 随机产出只抽取一次并写入存档；输出堵塞、等待及重新加载不会重复抽取。
- 合成配方保留 `ic2:shaped` / `ic2:shapeless` ID，委托原生匹配和配方书显示；另实现电量继承、隐藏标记和容器返还／消耗。
- 固体物品材料使用 NeoForge 的现代 Ingredient Codec。可选材料组合使用 `neoforge:compound`；Forge 通用标签迁移到 `c`，包括 `stone`→`stones`、`glass`→`glass_blocks` 等更名。
- Minecraft 的 `chain` 已更名为 `iron_chain`，转换器明确映射。

`recipe-catalog.json` 覆盖旧目录全部 796 条 JSON。每条已转换配方纳入 GameTest 加载清单；其余逐条记录未迁移类型、原料、结果、组件或流体条件。
这避免了数据包解析报错却被「服务器正常启动」掩盖。当前转换数量由 STATUS.md 自动统计。

资源转换入口依次运行 `machines.py`、`material_blocks.py`、`recipes.py`。`base.py` 提供无导入副作用的公共函数。
源 JAR 恢复基线保持不变；进度检查同时校验旧代码与旧资源哈希。

目前通过 34 项核心单测、20 项 IC2 GameTest，另有 Minecraft 的 always_pass。
基础机器仍标记「部分实现」：升级、声音、部分特殊配方和后续机器前置尚在迁移。
