# 磁化机与铁栅栏(P08)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityMagnetizer.java`(100 EU、tier 1、4 升级槽)
- `legacy/.../block/inherit/Ic2FenceBlock.java`(栅栏柱垂直搜索≤20 格找磁化机;
  多磁化机均摊 2 EU boost;玩家攀爬:每刻 +0.075 上升×1.03、上限 0.5/金属靴 1.5/
  Alt 键 0.1;潜行温和下降;摔落距离清零)

## 新实现

- `machine/MagnetizerBlockEntity.java`:小存储热源式 boost 入口(`canBoost`/`boost`)。
- `world/IronFenceBlock.java`:继承原版栅栏(连接状态由原版处理);`entityInside` 委托
  公开的 `lift(ServerLevel, BlockPos, Player)`(同刻每玩家一次);搜索磁化机改为检查
  相邻方块的方块实体类型,不再依赖旧网络字段。
- 注册:`MachineKind.MAGNETIZER`(LV)、`ModMaterialBlocks.IRON_FENCE`(+创造页);
  资源:旧 blockstate multipart/模型/纹理复制,配方 `iron_casing_to_iron_fence`
  (金属成型机挤压)随注册解锁(562/796)。

## 测试证据

- GameTest `MagnetizerTests`:
  - `magnetizer_lift`:供电后玩家上升速度>0,一次分摊扣 2 EU;
  - `magnetizer_unpowered`:无 EU 不上升;1 EU 不够、2 EU 恰好一次。
- IC2 与 GT 两种能量模式 166 项 GameTest 全部通过(2026-09-09)。

## 未验收范围

- 真实玩家连续攀爬、金属靴加速、Alt 慢速、潜行下降(输入相关,GameTest 覆盖核心
  速度规则);栅栏连接渲染的客户端核对;升级槽(过载)未接入。
