# M16／P19：多人及性能（切片一：生命周期/持久化/reload/单客户端/性能基线；切片二：双人并发/断线变体/chunk load-unload 专项）

状态：切片一、切片二均已交付并验证（2026-09-10）。P19 验收面剩余：资源重载深化、满载性能扩展与发布 jar 口径、超时型断线（见第 9 节）。

## 1. 环境与口径

- 专服：`./gradlew :neoforge:runServer`（MDG server run，`--nogui`），工作目录 `neoforge/run/`；`eula.txt` + `server.properties`（`level-name=IC2 Server Soak`、`online-mode=false`、`enable-rcon=true`、rcon 25575、view/simulation distance 8）。
- 世界：单机探针存档 "IC2 Migration Smoke" 的副本（拷入 `run/IC2 Server Soak`，专服世界目录布局与单机包一致）。机器坐标：发电机 `(-9,56,-6)`、低压变压器 `(-7,56,-6)`、橡胶木告示牌 `(-7,56,-4)`。
- 控制通道：`tools/migration/rcon.py`（纯 stdlib RCON 客户端；**26.1.2 实测**：auth 回包只有一包 id=请求 id type=2，无前置空 RESPONSE_VALUE；包长字段= id+type+payload+2 终止 null，读包必须消费整包否则残留 null 字节毁掉下一包头解析）。
- 无玩家时区块不加载（26.x 默认无出生点常载），`/data get block` 需先 `/forceload add -32 -32 31 31`。**切片二修正**：26.1.2 出生点附近区块恒载——`spawnChunkRadius` gamerule 已不存在（`Incorrect argument`），`forceload remove all` 后出生点 2 chunk 内的机器区在 0 玩家下持续可读 6 分钟+（能量零漂移）；"无玩家不加载"仅对出生点以外区域成立（切片二实测）。
- **切片二新增**：客户端运行名可通过 `-Pic2User=<name>` 覆盖（build.gradle 透传 `--username`）——离线模式专服拒绝第二个同名 "Dev" 客户端，双人并发必须有不同用户名。机器区坐标：发电机 `(-9,56,-6)`、低压变压器 `(-7,56,-6)`、batbox `(-10,56,-6)`、橡胶木告示牌 `(-7,56,-4)`；远地测试电网：发电机 `(2000,100,2000)`、电缆 `(1999,100,2000)`、batbox `(1998,100,2000)`。
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

TPS 实测：gametime 差 990 tick / 采样墙钟约 49.5s ≈ 20.0 TPS 满速（`time query gametime` 110526→111516）。结论：1 玩家 + IC2 机器区块常载下 20 TPS 无压力（余量 98%），作为后续切片的对照基线。切片二补充：**2 玩家同区块** avg 1.1ms（P50 0.4 / P95 2.7 / P99 13.8ms），仍满速（`p19b-rcon-concurrent-start.txt`）。

## 6. 双人并发（切片二）

- 双客户端加入：客户端 A `Dev`（entity 1，19:27:49）+ 客户端 B `P19B`（`-Pic2User=P19B`，entity 2，19:28:53，`/127.0.0.1:45062`）同时在线，`/list`=2（`p19b-rcon-concurrent-start.txt`）。
- 双人同区块：两人 `/tp` 至机器区相邻站位；客户端 A 截图 `p19b-client-a-dual-player.png` 中 **P19B 玩家模型渲染在 Dev 旁**（多玩家位置同步+皮肤渲染活），Jade tooltip 正常。
- live 电网转移（同区块、双人在线、非 forceload 常载语义）：发电机 `/data merge {energy:3990.0d}` → 10s 后 `0.0d`；相邻 batbox `1344.0d`→`5334.0d`——**3990+1344=5334 精确守恒**（`p19b-rcon-demand-sink.txt`）。放置 batbox 的 `setblock` 即触发 EnergyNet place-scan 重建，无需重启。
- 发电机容量 clamp 实录：merge 6000 → 首 tick 回落 4000（容量上限 4k，非网格流；连续 20s 采样恒 4000 且无 sink 时电量保持——demand-driven 语义正确，`p19b-rcon-grid-live.txt`）。
- **机器 GUI 网络链路**：P19B（空手）由 `/tp P19B -7.5 57 -3.5 153 38` 旋转对准发电机（Jade tooltip "Generator / 0 EU / 4k EU"，`p19b-aim-jade.png`）→ xdotool 右键 → **Generator GUI 在无头客户端打开**（燃料槽/充电槽/火焰条 + JEI 面板同屏，`p19b-gui-open.png`）——IC2 容器菜单 open→sync 链路在真实多人会话中工作。

## 7. 断线变体（切片二）

- **持 GUI 硬断线**：Generator GUI 打开状态下 SIGKILL 客户端 B 进程组 → 服务端仅记录 `P19B lost connection: Disconnected` + `P19B left the game`（19:36:47），零异常、`list` 立即减员、发电机 `energy:0.0d` 状态一致——容器随玩家移除干净回收。
- **`/kick`**：`kick Dev P19 slice-2 kick variant` → 服务端 `Dev lost connection: P19 slice-2 kick variant`；客户端收到 kick 屏并**逐字渲染原因**："Connection Lost / P19 slice-2 kick variant / Back to Server List"（`p19b-kick-screen.png`）。
- **重连路径**：kick 屏 → Back to Server List → LAN discovery 条目 "IC2R P19 soak"（专服 LAN 广播被真实客户端发现）→ Join Server 成功二次重入（entity 重新分配，`list` 回到 1）。

## 8. chunk load/unload 专项（切片二，EnergyNet 重建）

26.1.2 实录与结果（全部经 RCON 取证，`p19b-chunk-unload-poll.txt`）：

1. **出生点恒载**：机器区在出生点 2 chunk 内，`forceload remove all` 后 0 玩家下仍持续可读 6 分钟+，batbox 恒 `5334.0d` 零漂移；`spawnChunkRadius` gamerule 已不存在。修正切片一"无玩家不加载"的结论：仅出生点以外成立。
2. **远地构建**：Dev 传送至 `(2000,100,1996)`（玩家 ticket 加载）→ `setblock` 发电机/电缆/batbox 三件套 → merge 3990 → 10s 后 gen `0.0d` / bat `3990.0d` 精确守恒——EnergyNet 在玩家 ticket 区块远地重建成功。
3. **卸载冻结**：kick Dev → **≤15s** 内 `data get` 变 `That position is not loaded`（最后读数 bat `3990.0d` 冻结）。
4. **回归恢复**：rejoin → tp 回 → bat `3990.0d` / gen `0.0d` 与卸载前逐字一致——跨卸载/重载状态连续，chunk 扫描队列在 reload 路径无重复注入。
5. **破坏重建**：`setblock air` 断电缆 → 发电机 merge 1000 → 10s 后保持 `1000.0d`（无路径零抽取，bat 不动）；重新放电缆 → 传输恢复；净守恒净测：merge 500 → bat `+500.0d` 精确。过程出现一次 `+32` 测量偏差，经复核为我方 setblock 与 merge 两条 RCON 命令之间约 3 tick 的残余放电竞争（10 EU/t × ~3.2 tick），非 IC2 异常，如实记录。
6. 全程服务端唯一 ERROR 仍为 Jade registry 时机噪声，**零 IC2 错误**。

## 9. 剩余 P19 范围（后续切片）

1. **资源重载深化**：改动 IC2 数据包内容（如 uu_values world_scan.json）后 `/reload` 验证新值生效。
2. **性能基线扩展**：多人多机器负载（电网满载、反应堆运转）下的 tick 基线；发布 jar（无 interop）专服口径。
3. **超时型断线**：静默 drop（网络层不发 disconnect）的服务端超时回收验证。

## 10. 证据清单（docs/migration/evidence/）

切片一：`p19-rcon-boot1-empty-server.txt`、`p19-rcon-boot1-machines-loaded.txt`、`p19-rcon-reload.txt`、`p19-rcon-player-online.txt`、`p19-rcon-tps-sample.txt`、`p19-rcon-boot2-persistence.txt`、`p19-rcon-disconnect-stop.txt`、`p19-server-lifecyle-lines.txt`（两次启动的 joined/left/lost connection/Saving chunks/Done 行）、`p19-client-in-server.png`（多人世界渲染截图）。

切片二：`p19b-server-log-extract.txt`（boot/join/tp/kick/left/stop 全时间线）、`p19b-rcon-concurrent-start.txt`（双人在线+tick）、`p19b-rcon-grid-live.txt`（容量 clamp 实录）、`p19b-rcon-demand-sink.txt`（守恒转移）、`p19b-chunk-unload-poll.txt`（远地卸载轮询）、`p19b-client-a-dual-player.png`（双玩家同帧渲染）、`p19b-aim-jade.png`（Jade 读取发电机 EU）、`p19b-gui-open.png`（无头客户端机器 GUI）、`p19b-kick-screen.png`（kick 原因逐字渲染）。远地电网无截图证据（截图时玩家朝向背对机器，服务器已关闭无法重拍）——远地构建/守恒/卸载/恢复全部以 RCON `data get` 时间线为证（`p19b-chunk-unload-poll.txt` + `p19b-server-log-extract.txt`）。
