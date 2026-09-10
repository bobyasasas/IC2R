# M16／P19：多人及性能（切片一：专服生命周期 + 重启持久化 + 资源重载 + 真实客户端联机 + 性能基线）

状态：切片一已交付并验证（2026-09-10）。P19 验收面（双人操作、断线、区块加载、重启、资源重载与性能基线）尚未整体完成——本切片覆盖：**专服启动/优雅与强杀关闭、世界重启持久化、`/reload` 资源重载、单人真实客户端联机（加入/传送/渲染/硬断线）、服务端性能基线**。剩余范围见第 6 节。

## 1. 环境与口径

- 专服：`./gradlew :neoforge:runServer`（MDG server run，`--nogui`），工作目录 `neoforge/run/`；`eula.txt` + `server.properties`（`level-name=IC2 Server Soak`、`online-mode=false`、`enable-rcon=true`、rcon 25575、view/simulation distance 8）。
- 世界：单机探针存档 "IC2 Migration Smoke" 的副本（拷入 `run/IC2 Server Soak`，专服世界目录布局与单机包一致）。机器坐标：发电机 `(-9,56,-6)`、低压变压器 `(-7,56,-6)`、橡胶木告示牌 `(-7,56,-4)`。
- 控制通道：`tools/migration/rcon.py`（纯 stdlib RCON 客户端；**26.1.2 实测**：auth 回包只有一包 id=请求 id type=2，无前置空 RESPONSE_VALUE；包长字段= id+type+payload+2 终止 null，读包必须消费整包否则残留 null 字节毁掉下一包头解析）。
- 无玩家时区块不加载（26.x 默认无出生点常载），`/data get block` 需先 `/forceload add -32 -32 31 31`。
- **口径说明**：dev 运行时 classpath 含 JEI+Jade full jar（devRunInterop extendsFrom runtimeClasspath 对所有 run 生效），本 soak 是"带 interop 的专服"，比发布 jar 专服更严苛；服务端日志确认 Jade/JEI 加载且唯一 ERROR 是 Jade 自身 26.1.2 registry 时机问题（`Failed to collect shearable blocks`，非致命、非 IC2 代码），两次启动+reload 共 3 次同一处，**无任何 IC2 错误**。

## 2. 生命周期与重启持久化（P19"重启"项）

| 阶段 | 证据 | 结果 |
|---|---|---|
| 启动 1 | `p19-server-lifecyle-lines.txt` | `Done (0.171s)`（世界加载完成行） |
| 机器加载 | `p19-rcon-boot1-machines-loaded.txt` | 发电机 `energy:3328.0d fuel:0 totalFuel:400 inventory:2 空槽`；变压器 `energy:256.0d mode:2`——与存档文件 NBT 逐字一致 |
| SIGTERM 强杀（操作失误但构成"进程终止→落盘"证据） | boot1 log 尾部 | vanilla 关闭钩子完整走完：`Saving chunks for level…`→`All dimensions are saved` |
| 启动 2（重启） | boot2 log | `Done (0.372s)` |
| 重启后持久化 | `p19-rcon-boot2-persistence.txt` | 发电机/变压器/告示牌 NBT 与重启前完全一致（含告示牌 front_text 四行发光蓝字） |
| 优雅关闭 | `p19-rcon-disconnect-stop.txt` + boot2 log | RCON `/stop`→`Stopping the server`→保存完整→进程 exit 0 |

## 3. 资源重载（P19"资源重载"项）

`/reload`（RCON）：完成约 1–2s；重载前后发电机/变压器 `data get` 输出逐字一致（`p19-rcon-reload.txt`）；IC2 的服务端 reload 监听（如 UU 价值图 `AddServerReloadListenersEvent`）无任何报错。日志证据 `p19-server-lifecyle-lines.txt` 同卷。

## 4. 真实客户端联机与断线（P19"双人/断线"的单人部分）

- 客户端 `./gradlew :neoforge:runClient -Pic2Join=127.0.0.1:25565`（xvfb 无头）约 20s 加入：`Dev joined the game`（boot2 次为 10s，二次加入有本地缓存）。
- `/tp Dev -7 57 -5` 到机器区；截图 `p19-client-in-server.png`：橡胶木告示牌四行文字在多人世界正确渲染，Jade tooltip 显示 "Rubber Wood / IndustrialCraft 2: Refactored"（Jade 服务端数据→客户端渲染链路活）；客户端日志 `Ic2JadePlugin loaded` + `IC2 JEI plugin registered 16 recipe categories`（联机会话内）。
- **硬断线**：SIGKILL 客户端进程组（模拟崩溃/断网）→ 服务端立即记录 `Dev lost connection: Disconnected` + `Dev left the game`，随后 `list`=0 人、`tick query` 正常、机器 `data get` 仍可读——服务端对断线无任何错误（`p19-rcon-disconnect-stop.txt`）。
- 双人**并发**操作未覆盖（见第 6 节）。

## 5. 性能基线（P19"性能基线"项，首版）

`/tick query`（vanilla 1.20.3+ 命令，20.0 TPS 目标）：

| 场景 | 平均 tick | P50 | P95 | P99 | 证据 |
|---|---|---|---|---|---|
| 空服（无人，forceload 16 chunk） | 0.3ms | 0.2 | 0.4 | 2.8 | `p19-rcon-boot1-machines-loaded.txt` |
| 1 玩家在机器区 | 1.2ms | 1.1 | 1.7 | 3.3 | `p19-rcon-player-online.txt` |
| 1 玩家持续采样 | 1.0ms | 0.9 | 1.5 | 4.8 | `p19-rcon-tps-sample.txt` |
| 断线后空服 | 0.3ms | 0.3 | 0.6 | 0.9 | `p19-rcon-disconnect-stop.txt` |

TPS 实测：gametime 差 990 tick / 采样墙钟约 49.5s ≈ 20.0 TPS 满速（`time query gametime` 110526→111516）。结论：1 玩家 + IC2 机器区块常载下 20 TPS 无压力（余量 98%），作为后续切片的对照基线。

## 6. 剩余 P19 范围（后续切片）

1. **双人并发**：两客户端同时在线、同区块交互（机器 GUI 同时开、能量竞争）。
2. **断线变体**：服务端踢人（`/kick`）、超时型断线（静默 drop）、断线瞬间持有 GUI 的机器状态。
3. **区块加载**：玩家行走/传送触发 chunk load/unload 时 IC2 机器 EnergyNet 重建（WorldEnergyNetworks chunk 扫描队列）的专项验证；本轮仅覆盖 forceload 常载。
4. **资源重载深化**：改动 IC2 数据包内容（如 uu_values world_scan.json）后 `/reload` 验证新值生效。
5. **性能基线扩展**：多人多机器负载（电网满载、反应堆运转）下的 tick 基线；发布 jar（无 interop）专服口径。

## 7. 证据清单（docs/migration/evidence/）

`p19-rcon-boot1-empty-server.txt`、`p19-rcon-boot1-machines-loaded.txt`、`p19-rcon-reload.txt`、`p19-rcon-player-online.txt`、`p19-rcon-tps-sample.txt`、`p19-rcon-boot2-persistence.txt`、`p19-rcon-disconnect-stop.txt`、`p19-server-lifecyle-lines.txt`（两次启动的 joined/left/lost connection/Saving chunks/Done 行）、`p19-client-in-server.png`（多人世界渲染截图）。
