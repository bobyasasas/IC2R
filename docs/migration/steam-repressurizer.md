# 蒸汽再压缩机(P10)

更新日期:2026-09-08。切片提交见 git log `feat: port steam re-pressurizer`。

## 旧行为依据

- `legacy/forge-1.20.1/src/main/java/ic2/core/block/machine/tileentity/TileEntitySteamRepressurizer.java`
- `legacy/.../ic2/core/ref/Ic2FluidTags.java`(Fabric 环境 `c:steam`、Forge 环境 `forge:steam`)
- `legacy/.../ic2/core/init/IC2Config.java` 的 `balance.steamRepressurizer`(默认 16/32,范围 0..Integer.MAX_VALUE)
- 旧配方 `shaped/steam_repressurizer_from_tank.json`、`shaped/steam_repressurizer_from_iron_tank.json`
- 旧界面 `guisteamrepressurizer.xml`(176×166,双大流体显示,无外部蒸汽时显示 NO STEAM)

旧语义:输入槽 10,000 mB 仅接受 IC2 普通／过热蒸汽;输出槽 10,000 mB;每批 10 mB 输入＋1 HU→配置倍率 mB 输出(普通默认 16、过热默认 32);一刻内循环多批直到热量、输入或输出空间耗尽;热量跨刻／跨保存保留;从蒸汽标签取第一个流体静态缓存作为输出,标签无成员时整机停机并在界面提示。

## 旧实现缺陷与新设计

1. **取热多取缺陷(已修复)**:旧 `getHeat()` 以 `输入/10` 为目标向邻机取热,但目标不减去已存储的 `currentHeat`,已有热量被重复计入目标,邻机每刻被多扣。新 core 规则 `Repressurization.heatRequest()` 请求量 = `min(输入/10, 1000) − 储备`,储备有界(≤1,000 HU),邻机只为差额付费。回归:GameTest `repressurizer_prepaid`、core `RepressurizationTest`。
2. **输出候选静态缓存(已修复)**:旧实现首次查询后把标签第一个成员缓存到静态字段,注册顺序决定选择且重启前永不更新。新版每刻解析 `c:steam` 标签(NeoForge 26.1.2 的 common 标签约定,`Tags.Fluids` 亦用 `c` 前缀),候选按注册 ID 字典序取第一个 source 流体,排除 IC2 自身普通／过热蒸汽(避免输出=输入的循环)。**输出槽已存的兼容流体优先于标签最小值**,数据包／标签重载后不稀释、不混装已存输出;排空后自动采用新候选。回归:GameTest `repressurizer_candidate`。
3. **配置零的破坏性行为(已修复)**:旧版 `steamPerSteam=0` 时 `canOutput(0)` 恒真,循环继续消耗输入与热量而零产出,会把整槽蒸汽"烧光"。新版倍率为 0 时整机停机,视为无效配置而非模式。差异与回归:GameTest `repressurizer_zero_rate`。
4. **热储备持久化**:储备随 NBT(`reserve`)保存,载入时钳制到 [0,1000]。已支付热量在输出阻塞、保存重载后均保留。回归:GameTest `repressurizer_reload`。
5. **界面文案现代化**:旧版滚动显示 "FOUND …/NO STEAM";新版在无任何兼容外部蒸汽时显示固定提示(`ic2.steam_repressurizer.no_steam`),并直接显示两档倍率(读自服务器配置,客户端同步)。热量储备以原燃料条位置显示。

## 新实现结构

| 内容 | 位置 |
|---|---|
| 纯规则(批循环、热请求、有界储备) | `core/.../machine/Repressurization.java` |
| 方块实体(事务、取热、候选解析、同步、持久化) | `neoforge/.../machine/SteamRepressurizerBlockEntity.java` |
| 倍率配置(默认 16/32,服务器配置) | `neoforge/.../registration/BalanceConfig.java`(`ic2-balance-server.toml`) |
| 注册(MachineKind/ModMachines/MachineMenu) | `machine/MachineKind.java` 等,方块／物品／方块实体／菜单随现有管线自动注册 |
| 界面 | `client/SteamRepressurizerScreen.java`(双流体显示、倍率、无蒸汽提示) |
| 空标签数据包 | `data/c/tags/fluid/steam.json`(`replace:false`,由其他模组或数据包追加成员) |
| 资源 | `machines.py` 生成 blockstate／模型／掉落／语言;机器为 active×facing 十二状态 |

事务边界:取热(`WorkSource.extract`)、输入扣除、输出填充与储备更新在同一 `Transaction` 提交;`StateJournal` 保证回滚一致。输出槽出现非候选流体时机器停机且不覆盖用户放置的流体。

## 配方

- `steam_repressurizer_from_tank`(铁机壳×4、储罐×2、铜锅炉、导热件)已转换:528/796。
- `steam_repressurizer_from_iron_tank` 保持 pending,原因 `unported item ic2:iron_tank`;待 P08 铁罐迁移后随真实依赖转换,不以占位物品提前"修复"。

## 测试证据

- core JUnit `RepressurizationTest`:守恒(每批 10 mB+1 HU→倍率)、整刻受输入/热量/空间三重约束、零倍率停机、热请求差额与边界、预付热量在阻塞下保留。
- GameTest(注册于 `RegistrationTests`,实例 JSON `repressurizer_*.json`):
  - `repressurizer_idle`:无候选时不启动、不取热、邻机电热源 EU 不动。
  - `repressurizer_ratios`:MFE→电热源→再压缩机完整链路,普通 1,000 mB→1,600 mB、过热→3,200 mB,储备归零,热源产热 200–229 HU。
  - `repressurizer_prepaid`:输出空间不足一批时取热预付,释放空间后精确转换一批,储备随后补足到新输入需求(99 HU)。
  - `repressurizer_candidate`:多候选按 ID 序选中、重载后不稀释已存输出、排空后采用新候选。
  - `repressurizer_zero_rate`:配置 0 时输入、热量、输出全部不变。
  - `repressurizer_reload`:预付热量经真实 BlockEntity 保存／替换后保留。
- IC2 与 GT 两种能量模式 148 项 GameTest 全部通过(2026-09-08,含本切片 6 项)。
- 客户端验证截图:`docs/migration/images/steam-repressurizer-gui.png`(见提交)。

## 未验收范围

- 真实专服双端界面与同步验收归 M16。
- `steam_repressurizer_from_iron_tank` 配方随 `ic2:iron_tank` 迁移转换。
- 配方前置链(铜锅炉、导热件等)的获取可达性随 P09 核材料与前置配方整体验收。
