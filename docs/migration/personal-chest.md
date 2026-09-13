# 个人保险箱(P08)

更新日期:2026-09-12。

## 旧行为依据

- `legacy/.../block/personal/TileEntityPersonalChest.java`
- 54 格安全箱:首个打开者成为所有者(`checkAccess` 服务端认领);非所有者无菜单、
  无法自动化、无法扳手拆除;所有者须清空后才能扳手拆除。
- `checkAccess` 含 OP 豁免:`getServer().getPlayerList().isOp(profile)` 直接放行,
  且 OP 打开他人保险箱不改变归属。
- `canEntityDestroy` 覆写为 false:IC2 爆炸(dynamite/itnt 链路)不破坏保险箱,
  内容物不会因爆炸散落。

## 新实现

- `machine/PersonalChestBlockEntity.java`:54 槽,所有者存 UUID+名称并随方块持久化;
  `createMenu` 在服务端认领/拒绝(非所有者返回 null,即无法打开菜单)。
- `machine/PersonalChestGuard.java`:订阅 `BreakBlockEvent`,非所有者或含物品时取消破坏
  并向玩家发送提示;空箱所有者可拆。
- 自动化端口全拒绝(旧版 `InvSlot.Access.NONE`)。
- 菜单:6×9 网格(`MachineMenu` 专用分支,222 高);资源由 `machines.py` 生成。
- `permits` 的 OP 豁免(2026-09-12):`isOperator` 走
  `player.level().getServer().getPlayerList().isOp(player.nameAndId())`,
  与 legacy `isOp(profile)` 同义;OP 打开他人保险箱同样不认领。
- `PointExplosion.blastProof`(2026-09-12):IC2 自有爆炸破坏方块时跳过
  personal chest,对应 legacy `canEntityDestroy=false`。
  (26.1.2 的 vanilla 爆炸与 NeoForge `IBlockExtension.canEntityDestroy` 不再
  挂接,故保护落在自有爆炸链路上。)

## 测试证据

- GameTest `PersonalChestTests`:
  - `personal_chest_claim`:首开认领、所有者保持访问、陌生人被拒、所有权与内容经
    真实方块实体保存/替换后保留;
  - `personal_chest_automation`:物品端口存在但后端拒绝自动化插入;
  - `personal_chest_blast_spared`(2026-09-12):箱内存钻石后引爆
    `PointExplosion`,方块留存且 3 颗钻石原位。
- OP 正路径(真实 OP 玩家放行)无法在 GameTest 里构造:mock 玩家不在 ops 名单,
  `isOp` 恒 false;豁免分支留实机验收。
- IC2 与 GT 两种能量模式 533 项 GameTest 全部通过(2026-09-12)。

## 未验收范围

- 真实双开/拆箱提示文案的表现(M16);箱盖开合音效与渲染动画未实现(差异已记录)。
