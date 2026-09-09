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
- 注册于新 `ModExplosives`(FUNCTIONAL_BLOCKS 创造页);资源经
  legacy blockstate/模型/纹理移植脚本(FACING×LINKED 组合)。
- 贴面弹出使用显式置空气+掉落物(`removeBlock` 对普通方块等价,但对流体
  会重建 legacy block 的坑已在高炉切片确认过,这里同样规避)。

## 测试证据

- `DynamiteTests.placesWithFacingAndSupport`:北向贴墙放置保持朝向;失去
  支撑后方块消失(物品弹出)。
- `DynamiteTests.linkedStateToggles`:LINKED 状态在方块上保持。
- IC2 与 GT 双模式 `runGameTestServer` 240 项全绿。

## 未验收 / 后续

- ItemRemote 遥控器(REMOTE_LINKS 配对/群爆)与 ITnt 爆炸实体为
  P13 后续子切片。
- 客户端外观(方块模型/朝向形状)随实机测试(待测试.md 第 38 节)。
