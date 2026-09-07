# 第一批迁移验证记录

日期：2026-09-07。目标：Minecraft 26.1.2、NeoForge 26.1.2.107；Temurin Java 25；Gradle 9.2.1 / MDG 2.0.146。

| 检查 | 结果 | 边界 |
|---|---|---|
| `bash gradlew clean build :neoforge:runGameTestServer` | 通过 | 完整新模块构建及开发源集，不包含 legacy |
| JUnit | 16 项通过，0 失败／跳过 | 6 档电压、电流边界、每档 1000 个固定种子样本、覆盖设置、null 入口 |
| GameTest 专服 | 3 项通过 | 2 项 IC2 测试：铜板真实注册／建栈、core 运行时可用；另 1 项为 Minecraft 自带 always_pass |
| `python3 tools/migration/verify_artifact.py` | 通过 | 4 个 Java 25 类；core 已内嵌，测试未打包；铜板模型、纹理、两种语言检查 |
| `python3 tools/migration/progress.py --check` | 通过 | 状态／依赖／证据／看板一致，旧源码 SHA-256 与清单一致，core 源码平台引用检查 |
| 与恢复基线逐文件比较 | 通过 | legacy 内所有原始 src 文件（含资源）与 5d292712 的 src 逐字节相同 |
| actionlint | 通过 | GitHub Actions 工作流静态校验 |

本地初次使用 Java 21 启动 Gradle 时遇到 libraries.minecraft.net TLS 下载失败；以 Java 25 启动后成功。过程中发现并修复资源展开闭包在 Gradle configuration cache 下访问 project 的问题；最终构建可保存 configuration cache。

未验证：客户端实际渲染、菜单／多人同步、机器链路、能量网络、第三方集成和旧存档升级。当前产物是开发基础版本，不能作为完整旧版的替代品。

[首次远程 CI](https://github.com/bobyasasas/IC2R/actions/runs/34129849053) 已在提交 `9853b387` 通过构建、JUnit、GameTest、打包检查与看板检查，产物和测试报告已上传。M01 验收完成。

`jdeps --print-module-deps core/build/libs/core-3.0.0-migration.1.jar` 的输出仅为 `java.base`，确认当前 core 产物无平台依赖。
