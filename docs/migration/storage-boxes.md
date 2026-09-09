# 储物箱五材质(P08)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../block/storage/box/TileEntity*StorageBox.java`
- 容量:木 27、青铜 45、铁 45、钢 63、铱 126 格;任意面可存取(`InvSlot.Access.IO`,
  `InvSide.ANY`);无权限、无电力。

## 新实现

- `machine/StorageBoxBlockEntity.java`:五材质共用方块实体,槽数取自 `MachineKind`;
  自动化端口任意面插入与抽取。
- 菜单按 9 列网格生成,`MachineKind.menuHeight` 随行数变化(124+行×18)。
- 资源由 `machines.py` 生成(五变体模型/掉落/语言)。

## 测试证据

- GameTest `StorageBoxTests`:
  - `storage_box_wooden`:27 槽逐槽存入、任意面(顶/底)存取一致、抽取按槽;
  - `storage_box_iridium`:126 槽全部可用。
- IC2 与 GT 两种能量模式 162 项 GameTest 全部通过(2026-09-09)。

## 未验收范围

- 大容量(126 格)界面滚动/超大 GUI 在低分辨率下的可用性(客户端)。
