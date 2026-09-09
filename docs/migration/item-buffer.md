# 物品缓冲机(item_buffer)

2026-09-09 切片。迁移 legacy `TileEntityItemBuffer` / `ContainerItemBuffer` /
`GuiItemBuffer`(被动物流机器,无能量)。

## 旧行为依据

- 两组各 24 格:左组 `InvSide.NOTSIDE`(顶/底面)、右组 `InvSide.SIDE`(水平面),
  都是 `Access.IO`。外部自动化按面访问对应分组,弹入弹出互不混流。
- 2 个升级件槽;`updateEntityServer` 逐刻运行升级件 `onTick`(弹出/拉入):
  两个槽都有升级件时**交替**执行,只有一个时每刻执行。
- 比较器输出按两组总填充率(`calcRedFromInvSlots`)。
- `getEnergy(): 40.0` 是升级件能量查询的哑实现,机器本身不耗电。
- GUI:双 4×6 网格(左 8,18 / 右 98,18)+ 2 升级槽,无能量/进度显示。

## 新设计

- `MachineKind.ITEM_BUFFER`(容量 0、48 内容格、`upgradeSlots()=2`,
  仅弹出/拉入升级适用——`UpgradeItem.Kind.suitable` 显式分支,对应 legacy
  `UpgradableProperty.ItemProducing`)。
- `ItemBufferBlockEntity extends MachineBlockEntity`(无能量基类,同储物箱先例):
  - `automation(side)`:水平面 → 右组 [24,48),顶/底面 → 左组 [0,24),
    用 `ResourcePort` 按槽位区间放行插入与抽取;升级件自动传输
    (`UpgradeTransfers.tick`)与外部自动化走同一端口,无第二套规则。
  - `serverTick` 仅调 `UpgradeTransfers.tick(level, this)`;升级槽后端限制由
    `MachineBlockEntity.acceptsInventorySlot` 基类逻辑负责(GUI 限制不是唯一防线)。
  - **差异**:legacy 双升级件交替执行(各减半速率),新实现与全模组一致地
    每刻运行所有方向升级件;单升级件行为完全相同,双升级件速率更高。
    这是统一升级框架的取舍,非缺陷修复。
- 菜单:`MachineMenu` ITEM_BUFFER 分支按 legacy 坐标(左 8,18 / 右 98,18,
  4×6)+ 通用升级槽;`ItemBufferScreen` 关闭能量条与进度条;GUI 高 232。
- 资源:`machines.py` 列表加入 `item_buffer`(blockstates/模型/纹理/物品定义/
  掉落 Self/镐与扳手挖掘标签/needs_iron_tool/双语名称)。
- 配方解锁:`shaped/item_buffer.json`(机器 + `c:chests/wooden` ×2 +
  铁外壳 ×6)与 **`shapeless/mining_filter.json`(采矿过滤器卡片)**,
  转换数 575 → 577;`rci_rsh`/`rci_lzh` 仍因其余前置 pending。
- 比较器输出未迁移(与传送机/矿机一致,列入待测试),机器无循环音效。

## 测试证据

- `ItemBufferTests.ejectorSendsSidesOut`:弹出升级把右组 8 圆石经东面送入相邻
  木储物箱,左组 4 泥土不动。
- `ItemBufferTests.pullingTakesFromAbove`:拉入升级从上方储物箱把 5 粗铁拉进左组,
  右组保持空。
- `ItemBufferTests.portsAndUpgradeSlots`:北面端口拒左组/收右组、顶面端口相反
  (后端端口级验证);升级槽拒绝超频器。
- IC2 与 GT 双模式 `runGameTestServer` 194 项全绿;配方链经 recipes.py 转换并
  进入 converted-recipes 加载清单。

## 未验收 / 后续

- 客户端实机:双网格 GUI、升级槽放置、与真实管道/漏斗的配合、比较器(未实现)。
  记录于 `/home/codex/minecraft/待测试.md` 第 18 节。
- 采矿过滤器卡片的合成实机验证(配方已解锁,随卡片实机测试一并做)。
