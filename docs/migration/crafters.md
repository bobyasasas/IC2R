# 工业工作台 + 批量制作机(industrial_workbench / batch_crafter)

2026-09-12 切片。迁移 legacy `TileEntityIndustrialWorkbench` /
`ContainerIndustrialWorkbench` / `GuiIndustrialWorkbench` 与
`TileEntityBatchCrafter` / `ContainerBatchCrafter` / `GuiBatchCrafter`。

## 旧行为依据

- 工作台:31 真实槽 + 3 个计算输出槽。主 3×3 网格走 vanilla 合成;锤/钳
  各一组 2×1 `[tool, input]` 槽组合出第三种配方(如板材/导线);产品取出后
  rebalance 把网格余料补齐到最大堆叠;清空按钮把 3×3 网格整体挪进 18 格缓冲。
- 批量制作机:9 格全息模板(幽灵格,持有单件样品,点击存/清)+ 9 格原料行 +
  9 格容器行 + 1 电池槽 + 4 升级槽。原料行必须能做出与模板相同的配方
  (`ingredientsMatch` 按配方 id 比较),2 EU/t × 40 t 完成一次合成,产物进
  输出槽;原料各减 1,余量(桶等)优先回原格,再进容器行,最后掉落。
- GUI:工作台 194×228(升级列特例 x=170,清除按钮),批量机 202×206;
  升级槽 4 格,批量机右下电池槽。

## 新设计

- `MachineKind.INDUSTRIAL_WORKBENCH`(容量 0、31+4 槽、不耗能)与
  `BATCH_CRAFTER`(20000、20+4 槽、40t/2EU);`menuWidth` 202 默认,
  工作台 194 特例;`inventoryY()=menuHeight-82`。
- `IndustrialWorkbenchBlockEntity`:
  - 预览槽 `CraftResultSlot`×3(combo0=主网格 vanilla 配方;combo1/2=
    锤/钳 `[tool,input]` 2×1 组合),`onTake` 走 `consumeCraft`。
  - **CraftingInput 裁剪语义**:26.1.2 `CraftingInput.of` 把网格裁剪到
    非空包围盒,索引不再对应原槽位;全部改用 `ofPositioned` 取
    `(left, top)` 偏移,消耗槽位按 vanilla `ResultSlot.onTake` 同款
    `slot = base + x + left + (y + top) * width` 重推。
  - 清空网格 = `clearGrid`:网格内容 insert 进缓冲行,放不下返回 false
    不动(legacy 按钮语义);take 后 `rebalance` 把网格余料补到堆叠上限。
  - 自动化端口仅缓冲行 [9,27) 可插、不可抽(legacy 单向缓冲);工具槽
    只收对应工具 tag。
- `BatchCrafterBlockEntity`:
  - `serverTick` 复用 `MachineProcess` 40t/2EU 节拍;fits 探针
    (`ingredientsMatch`+`outputFits`)在主事务**外**执行——探针自开
    root 事务,嵌套 root 同线程禁止。
  - `craft()` 在调用方事务内原子完成:assemble→输出槽 insert→按
    left/top 映射逐格 extract 1→余量回原格/容器行/掉落列表;返回 null
    即整体回滚(能量与物品同事务)。
  - 全息格只经菜单按钮(`hologramClick`,carried 存/空清)修改,
    `HologramSlot.index()` 即按钮 id;持久化 `hologram` 子节点。
  - `acceptsInventorySlot`:原料格 0-8 做"替换后仍得同配方"校验,
    输出/容器/电池行放行(**内部 insert 也走 isValid,输出槽不可拒**)。
- 菜单:`MachineMenu` WORKBENCH/BATCH 分支按上文槽位序;两 Screen
  (工作台清空按钮经 `handleInventoryButtonClick`→`clearGrid`,
  批量机全息格点击);关闭音效并入 `MachineSounds` 穷举。
- 资产:blockstate(工作台 4 向;批量机 4 向×active)、六面贴图从
  legacy 拷贝、物品模型、loot 自掉落、shaped 合成×2、双语名称与
  `ic2.workbench.clear`。

## 测试证据

- `CrafterTests.workbench`:菜单槽 31 是计算预览且 1 原木→4 板;
  `onTake` 恰耗 1 原木;`clearGrid` 把 5 圆石整体挪进缓冲。
- `CrafterTests.batch`:全息 4 号模板原木+原料行 3 原木+200EU,
  40 tick 后输出 4 板、原料剩 2、能量恰耗 80EU。
- IC2 与 GT 双模式 `runGameTestServer` 488 项全绿(基线 486→488)。

## 未验收 / 后续

- 客户端实机:工作台三输出列与清空按钮、锤/钳组合实配、批量机全息
  点击与模板替换校验提示,均属人工视觉/UX 验收,记录于
  `/home/codex/minecraft/待测试.md` 第 107 节。
- 工作台 rebalance 为简化实现(补齐到堆叠上限,legacy 按 64 上限同向),
  随实机验证观察。
