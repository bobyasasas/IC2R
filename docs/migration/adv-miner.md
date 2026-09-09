# 高级采矿机(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../machine/tileentity/TileEntityAdvMiner.java`(423 行):
  - 4,000,000 EU 缓冲,sink tier = min(2 + 矿机放电等级, 5)(默认 3),每刻最高 512 EU;
  - 扫描器格(OD/OV)决定扫描半径 16 / 32;每 20t 一批,自 (x−range−1, y−1, z−range)
    起按 X→Z→Y− 游标逐层向下方块扫描,**每检查一格 64 EU**(自扫描器扣),采到
    或批结束为止;每格扫描状态存 `mineTarget`,重载续扫;
  - `canMine`:液体与 `BucketPickup` 不可采;破坏速度 <0 不可采;掉落为空不可采;
    `EntityBlock`(容器类)不可采;再经 15 格内置过滤列表的黑/白名单门控
    (`blacklist` 默认 true);采矿_filter_card 插卡时优先用卡片配置;
  - `doMine`:掉落(精准采集可选)+ 512 EU;升级件提升批大小
    (5 × (augmentation + 1))与放电等级;
  - GUI 事件:重置游标 / 黑白名单切换 / 精准采集切换(仅停机时可切);
  - 红石输入时停机。

## 新实现

- `machine/AdvMinerBlockEntity.java`:游标扫描状态机等价移植;扫描器 64 EU/格、
  采矿 512 EU、20t 批节奏、16/32 半径、游标持久化、红石停机一致;
  精准采集以附魔工具栈进 loot 实现;掉落因无输出槽直接落在机器上方
  (旧版 distributeDrops 在无匹配槽时同样落地)。
- 升级:4 格升级侧栏(架构常规),接入等级固定 3、批大小固定 5——
  旧版 augmentation(强化升级)在新升级体系中无对应,未迁移并记录;
- 菜单:`MachineMenu` ADV_MINER 分支(扫描器 8,26 + 5×3 过滤格 36,44);
  `client/AdvMinerScreen` 三按钮(Restart / Switch Mode / Switch Silk Touch,
  既有语言键,黑/白名单文案随状态刷新)。
- **采矿过滤器卡片(`ic2:mining_filter_card`)未随本切片迁移**:其卡槽与
  手持编辑 GUI(`HandHeldMiningFilter`)随手持物品容器整合切片另行迁移;
  矿机内置 15 格过滤已覆盖全部玩法,配方链不依赖卡片(已核对)。
- 资源:`machines.py` 生成 blockstate/十二态模型/纹理/掉落表;
  `recipes.py` 解锁配方(575/796)。

## 测试证据

- GameTest `AdvMinerTests`(IC2/GT 双模式 188 项):
  - `adv_miner_sweep`:OD 扫描器供电后游标建立,逐层扫过并采掉
    石头/煤矿/铁矿,掉落(煤等)出现在机器上方,游标存活;
  - `adv_miner_whitelist`:切白名单(仅煤)后煤矿被采、石头保留;
  - `adv_miner_silk_reset`:精准采集开关使钻石矿掉本体;重置按钮清空游标;
  - 扫描器充电写回、槽位限制由断言覆盖。

## 未验收范围

- 采矿过滤器卡片(物品、卡槽、手持 GUI)随后续切片;
- 真实客户端按钮操作与外观随实机测试;多人随 M16。
