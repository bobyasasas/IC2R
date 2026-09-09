# 采矿过滤器卡片(mining_filter_card)

2026-09-09 切片。迁移 legacy `ItemMiningFilterCard` / `HandHeldMiningFilter` /
`ContainerMiningFilter` / `GuiMiningFilter`,并接入高级矿机卡槽
(`TileEntityAdvMiner.cardSlot` 优先覆盖机器过滤)。

## 旧行为依据

- `ItemMiningFilterCard.use`:空气中使用打开手持编辑界面(`IHandHeldInventory`)。
- `HandHeldMiningFilter`:45 格库存 + `blacklist` 布尔(缺省 `true`);构造时
  `getOrCreateNbtData` 即写入 NBT——**打开一次编辑界面后,卡片就永久接管过滤判定**。
- `ContainerMiningFilter` + `SlotHologramSlot`:45 格全息幽灵槽(5×9),每格上限 1;
  左键放入/替换 1 个手持物品,空手左键清除(空手右键半数,上限 1 时等价清除)。
  每次变更经 `makeSaveCallback` 立即写回卡片 NBT(`Items` 列表 + `blacklist`)。
- `GuiMiningFilter`:左上角 (8,10,100,15) 黑/白名单切换按钮
  (`ic2.MiningFilter.gui.mode.*`),tooltip 显示模式与条目数
  (`ic2.MiningFilter.tooltip.entries`)。
- `TileEntityAdvMiner.canMine`(legacy 273–288 行):卡槽有卡片 **且卡片有 NBT** 时,
  用卡片条目 + 卡片黑名单判定,完全忽略机器自身过滤;否则退回机器 15 过滤格 +
  机器黑名单。条目匹配是掉落物物品相等(`checkItemEquality`),与数量无关。
  卡片黑名单缺省 `true`(NBT 里没有 `blacklist` 键也按黑名单算)。

## 新设计

- 物品 `ic2:mining_filter_card`(`MiningFilterCardItem`,`stacksTo(1)`),注册于
  `ModTools`,进 TOOLS_AND_UTILITIES 创造页。
- 数据组件(`ModDataComponents`):
  - `ic2:mining_filter_items`(`ItemContainerContents`,≤45 条校验);
  - `ic2:mining_filter_blacklist`(布尔)。
  **`mining_filter_blacklist` 缺失 = 卡片从未编辑过**,沿用 legacy "无 NBT" 语义:
  高级矿机忽略卡片,机器过滤生效。
- 手持编辑:`MiningFilterMenu`(菜单类型 `ic2:mining_filter`,注册于 `ModTools`,
  客户端经 `IMenuTypeExtension` 读取手持槽索引)+ `MiningFilterScreen`(176×215,
  程序化背景)。要点:
  - 45 个幽灵槽 `mayPlace/mayPickup=false`,点击逻辑在 `MiningFilterMenu.clicked`
    服务端处理(`ContainerInput.PICKUP`:有手持物 → 写入 1 个;空手 → 清除),
    其他点击类型(含创造中键 CLONE)在幽灵槽上一律忽略,防止复制幽灵物品;
    变更后立即写回组件并 `broadcastFullState()` 同步。
  - 黑/白名单按钮走 `clickMenuButton(0)`,状态经 ContainerData 同步到客户端按钮文案。
  - 服务端打开菜单即写入 `mining_filter_blacklist`(对应 legacy
    `getOrCreateNbtData`);`stillValid` 校验卡片仍在原手持槽(引用相等),
    数字键 SWAP 与 shift-click 均不可把打开中的卡片移走;每次变更即写回,
    任何方式关闭界面都不丢编辑。
  - tooltip:未编辑卡片不显示(legacy 无 NBT 同);已编辑显示模式 + 条目数。
- 高级矿机:`MachineKind.ADV_MINER` 槽位 16→17(扫描器 0、过滤格 1..15、
  **卡片 16**、升级 17..20),`AdvMinerBlockEntity.CARD_SLOT`;
  `evaluateFilter` 卡片优先分支与 legacy 相同(编辑过 → 卡片条目 + 卡片模式,
  否则机器过滤格);条目匹配仍是物品相等。
- 资源:`tools.py` 生成 items 定义、模型与纹理(legacy 模型无 loader/overrides,
  直接复用);语言键 legacy 全集已在新语言文件中,补 `item.ic2.mining_filter_card`
  (en "Mining Filter Card" / zh "采矿过滤卡",按 legacy 中文)。

## 配方

legacy `shapeless/mining_filter.json`(高级电路 ×2 + 频率传送器 + **物品缓冲机**
→ 卡片)依赖未迁移的 `ic2:item_buffer`,按约定**保持 pending**
(recipe-catalog 原因已更新为 `unported item ic2:item_buffer`),不使用占位物品。
`ic2:item_buffer` 迁移后重跑 `recipes.py` 即自动解锁。当前卡片仅创造页可获得。

## 测试证据

- `MiningFilterCardTests.handheldMenuEditsCard`:mock 玩家 + 真实菜单,验证
  幽灵槽写入/清除、打开即标记"已编辑"、模式按钮写回组件。
- `MiningFilterCardTests.uneditedCardDefersToMachineFilter`:机器白名单 [煤],
  未编辑卡片在场 → 机器过滤仍生效(挖煤矿、保留石头)。
- `MiningFilterCardTests.editedCardOverridesMachineFilter`:机器白名单 [煤],
  编辑过的卡片白名单 [圆石] → 卡片接管(挖石头、保留煤矿、无煤掉落)。
- IC2 与 GT 双模式 `runGameTestServer` 全绿;提交 CI 见 agent.md 快照。

## 未验收 / 后续

- 客户端实机:手持卡片右键打开界面、全息槽点击手感、按钮与 tooltip 显示、
  高级矿机 GUI 卡槽与升级槽布局(`ic2:guiadvminer` 无独立纹理,程序化背景)。
  记录于 `/home/codex/minecraft/待测试.md` 第 17 节。
- 合成配方随 `ic2:item_buffer`(独立物流机器切片)迁移解锁。
- legacy 矿机家族的区块边界(chunk loading)行为:legacy 矿机不强制加载区块,
  迁移保持一致;agent.md §5 中"区块边界"验收项指实机跨区块挖掘观察,随实机补测。
