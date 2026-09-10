# P13 爆炸链切片一:dynamite(定时/遥控炸药棒)

2026-09-09 切片。迁移 legacy `BlockDynamite`(P13 爆炸链第一件:遥控配对
炸药棒;遥控器与爆炸实体随后续切片)。

## 旧行为依据

- 双状态:`FACING`(UP/南北东西,无 DOWN)+ `LINKED` 布尔(遥控配对标记)。
- 五向 VoxelShape(FLOOR/NORTH/SOUTH/WEST/EAST)。
- `canSurvive`:贴面必须 sturdy(DOWN 朝向除外);支撑丢失经
  `updateShape`/`neighborChanged` 弹出物品(legacy `popResource`)。
- `getStateForPlacement`:优先点击面,再轮询可用面;无可用面返回空。
- 材质:0 强度、草声、无碰撞、instant-break、noLootTable、
  PushReaction.DESTROY。

## 新设计

- `DynamiteBlock`(neoforge.world):`EnumProperty<Direction> FACING`
  (26.1.2 已并入 EnumProperty)+ `BooleanProperty LINKED`;
  `neighborChanged` 采用 26.1.2 新签名(含 `@Nullable Orientation`,
  `net.minecraft.world.level.redstone` 包);支撑检查失败即弹出并清格。
- 支撑弹出(切片三补齐):`updateShape`(ScheduledTickAccess 新签名)在
  支撑面失效时显式 `Block.popResource` 掉落 DYNAMITE 物品并返回空气;
  方块保持 noLootTable,原生 `destroyBlock` 掉落不适用。
- 注册于新 `ModExplosives`(FUNCTIONAL_BLOCKS 创造页);资源经
  legacy blockstate/模型/纹理移植脚本(FACING×LINKED 组合)。
- 贴面弹出使用显式置空气+掉落物(`removeBlock` 对普通方块等价,但对流体
  会重建 legacy block 的坑已在高炉切片确认过,这里同样规避)。
- `DYNAMITE` BlockItem(切片三补齐):simple block item 覆盖放置;
  `ItemDynamite.use` 的投掷实体与红石引燃随后续切片。

## 测试证据

- `DynamiteTests.placesWithFacingAndSupport`:北向贴墙放置保持朝向;
  先置支撑、撤支撑后方块消失并掉落 dynamite 物品实体。切片三修复了原
  测试的两处缺陷(断言误用相对坐标致空转通过;移除的是朝向侧而非支撑侧)。
- `DynamiteTests.linkedStateToggles`:LINKED 状态在方块上保持。
- IC2 与 GT 双模式 `runGameTestServer` 243 项全绿。

## 未验收 / 后续

- 投掷炸药 `DynamiteEntity`(fuse 100)与粘性炸药 `StickyDynamiteEntity`
  (攀附、dynamite_sticky 配方)。
- dynamite 红石引燃与 40 tick 引信链(legacy `BlockDynamite.checkPlacement`)。
- legacy `Ic2Explosion` 自定义爆炸(dropRate/实体伤害)语义。
- 客户端外观(方块模型/朝向形状)随实机测试(待测试.md 第 38 节)。
