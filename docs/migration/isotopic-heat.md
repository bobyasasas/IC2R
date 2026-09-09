# 同位素热源与 RT 发电机(P10)

更新日期:2026-09-08。切片提交见 git log `feat: port radioisotope heat source and generator`。

## 旧行为依据

- `legacy/.../block/heatgenerator/tileentity/TileEntityRTHeatGenerator.java`
- `legacy/.../block/generator/tileentity/TileEntityRTGenerator.java`
- `legacy/.../block/tileentity/TileEntityHeatSourceInventory.java`(热缓冲与单面输出)
- `legacy/.../item/ItemNuclearResource.java`(`RTG_PELLET`:不可堆叠、携带辐射、不能放入反应堆)
- 配方 `shaped/rt_heat_generator.json`、`shaped/rt_generator.json`、`shaped/rtg_pellet*.json`、
  `centrifuge/rtg_pellet_to_plutonium.json`
- 配置 `balance.energy.heatGenerator.radioisotope`、`balance.energy.generator.radioisotope`(默认 1.0)

旧语义:两种机器各有六个燃料槽,每槽限一个 RTG 燃料丸且**燃料永不消耗**;输出按指数增长
`2^(n-1) × 基数`(n 为已安装数量,n=0 时输出为零):

- RT 热源:基数 `2 × radioisotopeHeatMultiplier`(HU/t),热量进入热缓冲(上限即当前最大输出),
  **仅从方块朝向面**对外输出;
- RT 发电机:基数 `radioisotopeMultiplier`(EU/t),内部存储 20,000,tier 1(LV)输出,并带一个
  工具/电池充电槽。

## 新设计与缺陷差异

1. **取热只认朝向面**照旧:新版经 `WorkOutput`(Face.FRONT)实现,非朝向面能力查询返回 0。
2. **热缓冲有界且持久**:旧版 `HeatBuffer` 无显式上限(靠每刻补满约束);新版 `WorkBuffer`
   每刻向当前最大输出补足(`insert(target, target)`),随保存持久化,同刻提取预算语义与
   蒸汽动能机一致(重填不重置已提取额度)。
3. **燃料语义完整记录**:RTG 燃料丸不可堆叠;其**辐射伤害**(旧版对无防化服携带者施加
   辐射药水)依赖 P16 防化服护甲套件,本切片暂不实现,已在代码注释与本文明确记录;
   不创建任何占位前置。
4. **配方前置诚实挂起**:`rt_heat_generator`/`rt_generator` 配方依赖未迁移的
   `ic2:reactor_chamber`(P12),`rtg_pellet` 及变体依赖 `ic2:plutonium`(P09);
   `rtg_pellet_2`/`rtg_pellet_vertical_2` 因键位使用 `#c:ingots/plutonium` 公共标签而可加载,
   在标签获得成员(P09 钚注册)前无法合成,均已记录。`centrifuge/rtg_pellet_to_plutonium`
   保持 pending。
5. **倍率配置迁移**:GenerationConfig 新增 `radioisotopeHeatMultiplier` 与
   `radioisotopeMultiplier`(服务器配置,默认 1),与旧 balance 键一一对应。

## 新实现结构

| 内容 | 位置 |
|---|---|
| 纯规则(`2^(n-1)` 输出曲线) | `core/.../machine/RadioisotopeOutput.java` |
| RT 热源方块实体 | `neoforge/.../machine/RtHeatGeneratorBlockEntity.java` |
| RT 发电机方块实体 | `neoforge/.../machine/RtGeneratorBlockEntity.java` |
| 燃料物品 | `registration/ModReactorItems.java`(`RTG_PELLET`,创造模式"材料"页) |
| 倍率配置 | `machine/GenerationConfig.java` |
| 界面 | `client/RadioisotopeScreen.java`(六槽 + 已安装数/输出读数) |
| 注册 | `MachineKind`(RT_HEAT_GENERATOR/RT_GENERATOR)、`ModMachines`(工厂、HEAT 能力)、`MachineMenu`(槽位)、`MachineSounds` |
| 资源 | `machines.py` 生成两台机器 blockstate/模型/掉落/语言;燃料丸 items/model/纹理(`textures/item/resource/nuclear/rtg_pellet.png`) |

## 测试证据

- core JUnit `RadioisotopeOutputTest`:1–6 丸输出曲线(2..64)、零丸/零倍率停机、
  非法输入、极端倍率不溢出。
- GameTest(`RtGeneratorTests`,注册 `radioisotope_*` 五项):
  - `radioisotope_heat_curve`:0→0、1→2、4→16、6→64 HU;非朝向面抽取为 0;
    同刻预算限制第二次抽取。
  - `radioisotope_heat_reload`:缓冲热量与已装燃料经真实方块实体保存/替换后保留。
  - `radioisotope_generator`:三丸十刻产出 40 EU 并向真实 BATBOX 供电。
  - `radioisotope_charging`:电池槽为真实 RE 电池充电;后端拒绝无关物品。
  - `radioisotope_automation`:物品能力每槽只接受一枚燃料丸,自动化插入生效。
- IC2 与 GT 两种能量模式 153 项 GameTest 全部通过(2026-09-08,含本切片 5 项)。
- 客户端/专服人工验证按当前工作指示暂缓,机器已列入 `/home/codex/minecraft/待测试.md`。

## 未验收范围

- RTG 燃料丸辐射行为(P16 防化服)。
- 全部 RTG 配方的真实可合成性(P09 钚、P12 反应堆室迁移后转换剩余 pending 配方)。
- 真实双人与长时运行回归(M16)。
