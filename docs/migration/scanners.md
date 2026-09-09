# 矿石扫描器(P07)

更新日期:2026-09-09。

## 旧行为依据

- `legacy/.../item/tool/ItemScanner.java`:OD 扫描器 `ic2:scanner`,
  100,000 EU/128 EU/t/1 级;手持右键消耗 50 EU 打开扫描结果界面(范围内矿石统计);
  `startLayerScan` 消耗 50 EU,返回 `range/2`(range 6 → 半径 3)。
- `legacy/.../item/tool/ItemScannerAdv.java`:OV 扫描器 `ic2:advanced_scanner`,
  1,000,000 EU/512 EU/t/2 级;层扫描消耗 250 EU,range 12 → 半径 6。
- 两扫描器均 `IBoxable`(可入工具箱);矿机 `mineLevel` 每层调用一次
  `startLayerScan`,返回 0 视为临时失败(停止推进)。

## 新实现

- `item/ScannerItem.java`:电动扫描器基类,携带层扫描常量
  (`layerScanCost`/`layerScanRange=range/2`)。
  `startLayerScan` 语义对齐 legacy `manager.use`:电量不足时**不扣电**返回 0;
  足够时消耗一次脉冲返回半径。
- `registration/ModTools.java`:`SCANNER`(100000,128,1,range 6,50 EU)、
  `ADVANCED_SCANNER`(1000000,512,2,range 12,250 EU),进 TOOLS_AND_UTILITIES
  创造页;两者加入 `ic2:toolbox_tools` 标签(legacy IBoxable)。
- 有意延后:手持右键扫描结果界面(HandHeldScanner 容器与矿石统计)随
  菜单/工具箱整合切片迁移;扫描音效未迁移。矿机所需的层扫描入口已可用。
- 资源:`tools.py` 复制模型/纹理并生成物品定义;`recipes.py` 解锁两配方
  (571/796)。

## 测试证据

- GameTest `ScannerItemTests.scanner_layer_scan`(IC2/GT 双模式,178 项):
  - 未充电返回 0;充电后单次脉冲扣 50 EU 返回半径 3,连续脉冲逐次扣费;
  - 电量不足(10 EU)不扣电返回 0(该断言曾抓出部分扣电缺陷并已修复);
  - 高级扫描器扣 250 EU 返回半径 6;
  - 两个扫描器容量/传输/等级逐项断言。

## 未验收范围

- 手持扫描结果界面、扫描音效待后续切片;
- 矿机 `mineLevel` 消费层扫描的整体验收随矿机切片;
- 多人环境表现随 M16。
