# 采矿管道(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../block/machine/MiningPipeBlock.java`(0.375–0.625 窄柱全高形状,
  光照遮挡按形状判定);`Ic2Blocks.MINING_PIPE_TIP` 为普通整块。
- 两方块均 6.0 硬度/10.0 爆炸抗性、金属声、需正确工具(石质级镐)。
- **原版 JAR(含恢复基线)即无两方块的掉落表**:徒手/镐破坏不掉落任何物品;
  管道回收走矿机收回模式(withdraw),尖端是矿机程序化放置的内部方块。
- 尖端无物品形态、无语言键;管道物品入创造物品栏(TOOLS_AND_UTILITIES)。
- 配方:4 铁板 + 树胶取器 → 16 管道。

## 新实现

- `world/MiningPipeBlock.java`:同样的窄柱形状与 `useShapeForLightOcclusion`。
- `registration/ModMaterialBlocks.java`:`MINING_PIPE`(含 BlockItem,进原版
  INGREDIENTS 创造页)与 `MINING_PIPE_TIP`(无 BlockItem,与旧版一致)。
- 资源:`material_blocks.py` 复制旧 blockstate/模型/纹理/语言(尖端无语言键);
  标签随管线恢复:`mineable/pickaxe`、`needs_stone_tool`。无掉落表是有意保留。
- 配方 `shaped/mining_pipe` 由 `recipes.py` 解锁转换(566/796)。同次管线重跑
  还解锁了三个此前被未移植物品阻塞的机器配方:`magnetizer`(依赖铁栅栏)、
  `pump`(依赖采矿管道)、`trade_o_mat`(依赖木质箱标签)。

## 测试证据

- GameTest `MiningPipeTests`:
  - `mining_pipe_shape`:窄柱截面(0.375–0.625)、全高、形状光照遮挡、
    镐/石工具标签、BlockItem 指向管道方块;
  - `mining_pipe_tip`:尖端独立注册、保留工具要求、无物品形态(`asItem()==AIR`);
  - `mining_pipe_no_drops`:两方块均无掉落表(破坏不掉落,回收走矿机收回)。

## 未验收范围

- 管道放置/收回的实际世界行为随矿机切片迁移后一并验收;客户端渲染核对
  (窄柱模型)待游戏内检查;多人环境表现随 M16。
