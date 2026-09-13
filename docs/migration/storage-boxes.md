# 储物箱五材质(P08)

更新日期:2026-09-12。

## 旧行为依据

- `legacy/.../block/storage/box/TileEntity*StorageBox.java`
- 容量:木 27、青铜 45、铁 45、钢 63、铱 126 格;任意面可存取(`InvSlot.Access.IO`,
  `InvSide.ANY`);无权限、无电力。
- 内容物随方块保留:`adjustDrop` 把全部槽位写回掉落物 NBT、`onPlaced` 放置时按位读回,
  破坏时不在地面散落(`getAuxDrops` 为空)。
- 物品 tooltip 两行(说明+槽数,`appendItemTooltip`)。

## 新实现

- `machine/StorageBoxBlockEntity.java`:五材质共用方块实体,槽数取自 `MachineKind`;
  自动化端口任意面插入与抽取。
- 菜单按 9 列网格生成,`MachineKind.menuHeight` 随行数变化(124+行×18)。
- 资源由 `machines.py` 生成(五变体模型/掉落/语言)。
- 内容物随方块物品保留(2026-09-12 对齐 legacy):`StorageBoxBlockEntity.preRemoveSideEffects`
  空覆写跳过 vanilla Container 散落;`MachineBlock.getDrops` 把 `inventory.copyToList()` 经
  `ItemContainerContents.fromItems`(按位保留槽位、截尾全空)写入 `DataComponents.CONTAINER`;
  `MachineBlock.setPlacedBy` 用 `copyInto` 按位读回逐槽插入。

## 测试证据

- GameTest `StorageBoxTests`:
  - `storage_box_wooden`:27 槽逐槽存入、任意面(顶/底)存取一致、抽取按槽;
  - `storage_box_iridium`:126 槽全部可用;
  - `storage_box_contents_ride_the_drop`:破坏不掉落地面散落物、掉落盒物品
    `CONTAINER` 槽位按位保留、重新放置后内容逐槽恢复;
  - `storage_box_tier_capacities`:五材质容量 27/45/45/63/126 逐档断言。
- IC2 与 GT 两种能量模式 531 项 GameTest 全部通过(2026-09-12)。

## 未验收范围

- 大容量(126 格)界面滚动/超大 GUI 在低分辨率下的可用性(客户端)。
- 物品 tooltip 文案(客户端渲染)。
