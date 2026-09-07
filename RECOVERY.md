# 2.10.39-ex120-cannerfix1 源码恢复记录

本工作副本已从提供的 JAR 恢复为可构建的 Java 源码项目，包含 `cannerfix1` 罐装机修复。
JAR 内部的模组版本仍是 `2.10.39-ex120`，所以构建产物也使用这个版本号。

本文保存首次恢复时的验证记录与产物哈希。之后进行了可读性整理，当前代码的改动与验证见
[READABILITY.md](READABILITY.md)。

## 来源

- 原包：`ic2-forge-2.10.39-ex120-cannerfix1.jar`
- 原包 SHA-256：`e54c9d5be064379d7f45668a9670b32319087030303ef88ac49b613c8ed2069d`
- 源码基线：`bobyasasas/IC2R` 的 `forge/1.20.1` 分支，提交 `282f8379511bf9ebdbf41dda5ee2af96219ddb25`。
- 恢复分支：`recover/2.10.39-cannerfix1`。
- 恢复日期：2026-09-07。

先编译基线源码，再用 ForgeGradle 生成的 Minecraft 1.20.1 官方名称映射还原 JAR 中的 SRG 成员名，
用 ASM 比较类的指令与声明，最后用 Vineflower 1.12.0 恢复有差异的 Java 文件。
没有把原包的 `.class` 文件直接放入项目充当源码。

## 恢复范围

- 基线的 1,265 个类中，1,189 个与目标包的规范化字节码一致；目标有 35 个新增类和 1 个删除的内部类。
- 恢复 96 个 Java 文件：73 个已有文件、23 个新增文件。其余 842 个原有 Java 文件保留。
- 资源新增 43 个、更新 58 个、删除 18 个；另外更新了构建版本与 README。
- 涉及炸药与遥控器、收纳盒、补水单元、Jade 显示、扳手、流体容器、电网与反应堆、UU 复制、配方和世界生成等模块。
- 补回 Jade 编译依赖，采用适用于 Forge 1.20.1 的 Jade 11.13.2（Modrinth 版本 ID `LecuGude`）；运行时仍为可选依赖。

罐装机恢复了 `appliedMode`、模式变化时的音效刷新等逻辑。首次恢复时 `TileEntityCanner` 的完整规范化字节码与原包一致。

文件清单见 [recovery/source-changes.json](recovery/source-changes.json) 和
[recovery/resource-changes.json](recovery/resource-changes.json)。

## 构建与验证

环境：Minecraft 1.20.1、Forge 47.4.20、Java 17 编译工具链、Gradle Wrapper 8.14.5。

```sh
bash gradlew clean build --console=plain
python3 tools/recovery/verify.py /path/to/ic2-forge-2.10.39-ex120-cannerfix1.jar
```

构建产物：`build/libs/ic2-forge-2.10.39-ex120.jar`。
验证工具只解析字节码和资源，不执行原包或恢复包中的模组代码。

本次验证结果：

| 检查 | 结果 |
| --- | --- |
| 从干净构建目录执行 `clean build` | 通过 |
| 类文件清单 | 1,299 / 1,299，无缺失或多余类 |
| 类、字段、方法声明及泛型签名 | 全部一致 |
| 规范化后的完整类字节码 | 1,242 个一致，57 个仍有差异 |
| 方法调用、字段访问、LDC 常量的每类引用计数 | 全部一致 |
| 非代码文件 | 5,829 / 5,829，全部对应且内容等价 |
| `git diff --check` | 通过 |

字节码规范化忽略源码行号、局部变量调试名、栈帧及常量池和成员排列顺序。
资源比较忽略 CRLF/LF 差异，并对 JSON 比较解析后的内容；二进制资源按字节比较。
57 个类的差异包括条件分支排列、局部变量槽位分配等反编译重构结果。
声明及引用计数一致不能单独证明所有运行行为等价；本记录不把这些类视为逐指令一致。

本次构建产物 SHA-256：`5201000fb2c6b6217f92907df95051ec41c6a9fb305742f7ee0a751b7557ade9`。
重复构建可能因为 ZIP 时间戳等元数据产生不同的整体哈希，应使用验证脚本比较内容。

完整结果见 [recovery/verification.json](recovery/verification.json)。重新运行验证会在
`build/recovery-audit/` 生成报告、各类指令列表和 `bytecode.diff`。

## 限制

JAR 不包含丢失源码的完整注释、格式、开发文档、原始构建脚本或被编译器删除的代码。
恢复文件的这些内容无法保证与原始源码相同；构建脚本沿用仓库基线并补齐必要依赖。
反编译产生的泛型转换丢失和同名 `Builder` 类型混淆已修复并通过编译。

项目原来没有自动化测试，本次 Gradle 的 `test` 显示 `NO-SOURCE`。
尚未进行 Minecraft 客户端、专用服务器或实际存档的运行测试，音效、画面及完整游戏行为仍需游戏内验证。
`release.md` 是恢复前 2.10.34 的历史计划，不代表本次恢复版本的新发布说明。
