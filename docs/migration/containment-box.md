# 手持核料容器切片：containment_box（铅封盒）

legacy `ItemContainmentbox` + `HandHeldContainmentbox` + `ContainerContainmentbox`
（12 格手持核料容器，内容持久在物品栈自身），port 复用 MiningFilterCardItem 手持容器范式
（ItemContainerContents DataComponent + openMenu + 组件回写 Container）。

## 门控 1:1（legacy HandHeldContainmentbox.canPlaceItem）

| legacy 类 | port 类 | 例 |
| --- | --- | --- |
| ItemNuclearResource | NuclearResourceItem | near_depleted_uranium、re_enriched_uranium |
| ItemReactorUranium | FuelRodItem（MoxFuelRodItem 为其子类，一并覆盖） | uranium/mox 燃料棒单双四联 |
| DepletingRod（port 归并同类） | DepletingRodItem | lithium_fuel_rod、depleted_isotope_fuel_rod |

- `ContainmentBoxMenu.accepts()` 静态门控同时驱动 `mayPlace`（GUI 手放/移位），
  `quickMoveStack` 对玩家区 shift-click 先尝试 12 个箱内槽（`moveItemStackTo [0,SLOTS)`，
  空槽走 mayPlace 拒收普通物品），失败再按 vanilla 主背包↔快捷栏转移——
  铁锭等普通物品永不进箱、留在玩家侧（GameTest 断言全箱槽空+玩家侧铁锭总数不变）。

## 持久化与菜单

- `ic2:containment_box_items` DataComponent：`ItemContainerContents`，
  persistent CODEC validate ≤12 格 + STREAM_CODEC 网络同步。
- Menu 内 `BoxContents implements Container`：构造时 `copyInto` 读组件，
  每次 `setItem/removeItem/setChanged` 写回 `ItemContainerContents.fromItems`——
  箱格即组件视图，关 GUI 即持久，重开恢复（GameTest 断言组件非 null+重开槽 0 恢复铀×4）。
- `stillValid`：客户端恒真；服务端用 `player.getInventory().getItem(slot) == box`
  引用相等（同 MiningFilter 范式），箱子被移走即关菜单。
- 布局与 legacy 像素一致：12 格 4 列×3 行 at `53+col*18, 19+row*18`；
  玩家区 84/142 标准行距；Screen 用通用 `ContainerScreenBase` 无需专有贴图。
- 打开方式同 MiningFilterCardItem：`use()` → `openMenu(SimpleMenuProvider, buf→writeVarInt(slot))`，
  主手取 `getSelectedSlot()`、副手取 `SLOT_OFFHAND`。

## 注册与资源

- 物品 `ModTools.CONTAINMENT_BOX`（stacksTo(1)、Rarity.UNCOMMON）、
  菜单 `CONTAINMENT_BOX_MENU`（IMenuTypeExtension 读 VarInt）、创造页 tools 组、
  Screen 注册、双语 lang（item/container 各一条，zh "铅封盒"）。
- 贴图/模型/items 定义由 `tools/migration/resources/tools.py` 清单生成（复制自 legacy）。
- 配方 `data/ic2/recipe/shaped/containment_box.json`：III/ICI/III，
  中心木箱 tag（`#c:chests/wooden`），8 角铅外壳（`ic2:lead_casing`）→ 1 铅封盒。
- **资源生成器记档**：painter 轮曾手工向 `toolbox_tools.json` tag 追加 17 个 painter
  而 tools.py 清单未同步，本轮重跑生成器时被旧清单冲掉；已把 painter 家族补进
  tools.py 生成清单使重跑幂等，tag 恢复与 HEAD 一致（生成器清单必须始终先于手工合并）。

## 验证

- 3 例 GameTest（`ContainmentBoxTests`，457×2 IC2/GT 双模式全绿）：
  1. `accepts_nuclear_only`：四类核料 accepts 矩阵 + 铁锭/木箱/空栈拒绝 + 物品注册入包。
  2. `menu_persists_contents`：铀×4 移位进箱 → 组件非 null → 重开菜单恢复槽 0。
  3. `shift_click_rejects_plain_items`：铁锭 shift-click 全箱槽空且玩家侧总数 8 不变，
     铀 shift-click 进箱槽 0 且离开玩家背包。
- catalog 同轮翻转 2 条：item `ic2:containment_box` + menu `ic2:containment_box` → implemented。
