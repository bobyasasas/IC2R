# 遮蔽墙重纹理(P13 切片十)

## legacy 行为依据

- `ItemObscurator`(电动物品 100,000 EU 容量/250 EU/t/层级 2):
  - **站立应用**(`onItemUseFirst`,非潜行,服务端):遮蔽器持有完整取样(refBlock/refVariant/
    refSide/refColorMuls)且 `canUse 5,000 EU` 时,目标方块若实现 `RetexturableBlock` 调
    `retexture(...)`,否则 `announceRetexture` 发 `RetextureEvent`(Forge 总线);应用成功扣 5,000 EU。
  - **潜行取样**(潜行,客户端):烘焙模型采样目标面(`getRenderInfo`:取路径上规整单位面的对角
    UV/sprite/tint,无规整面时退化为每 quad 首对角;translucent 方块不可取样),方块颜色经
    `BlockColors.getColor`;与已存取样相同则不重发;经 `sendPlayerItemData` 上行,服务端
    `onPlayerItemNetworkData` 复验 20,000 EU 后写 NBT。
- `WallBlock.retexture`(IC2 建筑泡沫墙,16 色):目标位换成 `Ic2Blocks.OBSCURED_WALL`,
  `TileEntityWall.initializeFromWall(color, side, refState, refVariant, refSide, colorMultipliers)`;
  BE 失败则还原原方块。
- `TileEntityWall`+`Obscuration` 组件:墙色 + 六面各一份 `ObscurationData(state, variant, side,
  colorMultipliers)`,变更经网络字段同步并触发客户端 rerender。
- 渲染 `WallModelForge`/`OverlayRenderUtil`:每面 = 墙色墙模型面 + 每取样纹理一个"整面平铺
  overlay"(偏移 0.001,BLOCK_UV_MAP 按面把取样 UV 矩形映射到面顶点,顶点色=颜色乘积;legacy
  顶点缓冲 ABGR,故 mapVertexColor 做通道交换)。
- 挖掘掉落:`adjustDrop` 返回 pick block,即对应墙色泡沫墙。
- 剥绝缘/取样对 IC2 泡沫墙 painter 不染色(涂色器切片已保持一致)。

## 新端移植

| 部件 | 文件 | 说明 |
|---|---|---|
| 遮蔽器物品 | `neoforge/.../item/ObscuratorItem.java` | `ElectricItemSpec(100000, 250, 2)`;应用/取样双模式;泡沫墙映射表按 `ModFoam.WALLS` 懒构建 |
| 取样数据 | `neoforge/.../component/ObscuratorReference.java` + `OBSCURATOR_REFERENCE` 组件 | record(blockId, variant, side, colorMuls),忠实 legacy NBT 字段;`resolveState`/`variantOf` 复刻 BlockStateUtil |
| 上行通道 | `neoforge/.../network/ObscuratorScanPayload.java` + `ModNetworking` | `RegisterPayloadHandlersEvent`(协议版本 "1");服务端复验持有物、电力与数据边界后写组件并扣 20,000 EU |
| 遮蔽墙方块/BE | `neoforge/.../block/ObscuredWallBlock.java`、`machine/ObscuredWallBlockEntity.java` | BE 持墙色+六面 FaceData;`getUpdateTag`/`onDataPacket(ValueInput)` 同步;`getModelData()` 发布 `WallRenderState` |
| 客户端模型 | `neoforge/.../client/model/ObscuredWallModel.java`(+`ObscuredFaceSampler`) | `CustomUnbakedBlockStateModel`(blockstate JSON `{"type":"ic2:obscured_wall"}`,注册于 `RegisterBlockStateModels`);`collectParts` 经 ModelData 读渲染态,逐面输出基础墙面+overlay;`MutableQuad` 完成整面/UV/颜色/法线 |
| 客户端扫描 | `neoforge/.../client/ObscuratorClient.java` | 取样(规整面优先,否则每 quad 首对角;translucent 拒绝)、tint 经 `BlockColors.getTintSource(...).colorInWorld`、去重、`ClientPacketDistributor.sendToServer` |
| 注册/资源 | `registration/ModObscurator.java`、blockstates/obscured_wall.json、models/item/obscurator.json、贴图、配方 `shaped/obscurator.json`(683/796) | 语言键 legacy 已带;无 BlockItem(与 legacy 一致) |

## 26.1.2 适配与有意差异

- **模型架构重写**:26.1.2 无 `BakedModel.getQuads`,新架构为 `BlockStateModel.collectParts →
  BlockStateModelPart.getQuads(face)`;自定义模型经 `CustomUnbakedBlockStateModel` codec 注册。
- **BakedQuad 结构化**:顶点不再是 int[](stride 8),为 record(Vector3fc 位置/packedUV/
  MaterialInfo);overlay 用 NeoForge `MutableQuad`(`setCubeFace`/`setUvFromSprite`/`setColor`/
  `recomputeNormals`/`toBakedQuad`)替代 legacy 手工字节操作;**顶点色为 ARGB,无需 legacy 的
  RGB↔BGR 通道交换**。
- **网格线程约束**:`collectParts` 不直接读 BE(注释明确线程不安全),数据经 BE
  `getModelData()` → `ModelData`(.property `RENDER_STATE`)→ `IBlockGetterExtension.getModelData(pos)`。
- **BE NBT 新 API**:`saveAdditional(ValueOutput)`/`loadAdditional(ValueInput)`;参考状态按
  legacy 方式持久化为 id+variant 字符串(读取复用 `ObscuratorReference.resolveState`)。
- **UV 空间**:取样与渲染统一在 sprite 空间(atlas 空间经 sprite `getU0/getU1` 换算),
  渲染端 `setUvFromSprite` 直接回放。
- **RetextureEvent 扩展分支**:IC2R 内无监听者,legacy 下观察行为为 PASS;新端不发布等价
  事件(无消费者),重纹理入口限定 IC2 泡沫墙。后续第三方扩展需求出现时再评估。
- **物品模型**:legacy `ic2:mask_overlay`(底图+电量条蒙版)不迁移;新端用整图 `obscurator.png`,
  电量经 `ElectricItem` 原生耐久条显示。
- legacy `getRenderInfo` 的"规整面优先"检查完整移植(位置/法线判据一致),26.1.2 直接以
  `Vector3fc` 计算。

## 测试证据

- GameTest `ObscuratorTests`(IC2/GT 双模式):
  - `obscurator_retextures_wall`:写入取样组件后站立 useOn,泡沫墙变 obscured_wall、BE 墙色与
    面数据正确、电量扣至 95,000;
  - `obscurator_requires_energy`:电量 0 时墙不变;
  - `obscurator_requires_reference`:无取样组件 PASS 不耗电;
  - `obscurator_sneak_passes_server`:潜行状态服务端无副作用(取样为客户端行为);
  - `obscurator_scan_payload`:直接投递 payload,handler 校验电力后写组件并扣 20,000 EU,
    非遮蔽器物品拒绝;
  - `obscured_wall_persistence`:BE 经 `saveWithFullMetadata`+`loadStatic` 往返后渲染态一致;
  - `obscured_wall_drops_color_wall`:破坏掉对应墙色泡沫墙。
- 服务端 GameTest 无法覆盖客户端烘焙采样与实际渲染;该部分列入 `待测试.md` §47 实机项。

## 遗留

- 实机(游戏内)渲染/采样验收见 `待测试.md` §47。
- P13 转出项:电缆泡沫覆盖随电缆切片;辐射效果随 P16。
