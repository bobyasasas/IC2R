# 反编译源码可读性整理

2026-09-07，对恢复清单中的 96 个 Java 文件进行了整理，范围由
`recovery/source-changes.json` 的 `source_files` 字段限定。

## 改动

- 统一为四空格缩进，整理导入、长参数列表和链式调用。使用 google-java-format 1.28.0 的 AOSP 格式。
- 清理 245 处重复的内部类型限定，如 `TileEntityCanner.Mode` 在本类中直接写为 `Mode`。
- 重命名 123 处标识符使用：例如 `var11` 改为 `world`，`bl2` 改为 `canPlaceFluid`，`chargeMats` 改为 `chargeMaterialSlots`。
- 48 处字符串使用改回已有常量；补水单元使用 `CHARGES`，Jade 显示使用 `MIN_VISIBLE` 与已有 NBT 键常量，颜色写为 ARGB 十六进制。
- 手工整理罐装机输出类型分支、Jade 进度回退逻辑、颜色解析和时间格式化、遥控炸药遍历、逐步修复配方的校验流程。
- 注释说明罐装机 `appliedMode` 的作用、未加载区块的链接保留规则、配方按材料槽计数的规则，以及需要保留的泛型转换。
- 分别使用 `StateDefinition.Builder` 与 `LootParams.Builder` 对应的导入，避免很长的全限定类型名。

这批文件中不再有 `varN`、`bl`、`bl2`、`f6` 等识别出的占位变量；超过 150 字符的行从 199 行降为 0 行。
具体统计见 [recovery/readability-metrics.json](recovery/readability-metrics.json)。

## 验证

`bash gradlew build --console=plain` 与 `git diff --check` 均通过。

整理前后共有 1,299 个类，其中 1,294 个类的完整规范化字节码一致。
另 5 个类为 `TileEntityCanner`、`ItemRemote`、`GradualRecipe`、`Ic2ProgressProvider` 和 `JadeConfigHelper`。
已逐项检查其指令差异：来自提前返回、循环继续、条件分支排列及模式匹配变量槽位。
比较也确认所有类/字段/方法声明、泛型签名、每类方法调用/字段访问/LDC 常量引用计数一致，5,829 个资源文件全部对应且内容等价。

比较保留了浮点条件的原有行为，没有把 `!(ratio <= limit)` 直接替换为对 NaN 行为不同的 `ratio > limit`。
配方的材料计数、数值运算顺序、NBT 键、枚举顺序和网络接口均按原逻辑保留。

- [整理前后对比](recovery/readability-before-after.json)
- [当前产物与用户原始 JAR 的对比](recovery/readability-verification.json)
- [首次源码恢复记录](RECOVERY.md)

可用 `python3 tools/recovery/verify.py /path/to/before.jar /path/to/after.jar` 重复静态比对。
规范化忽略调试信息、栈帧及常量池和成员排列顺序；资源比较忽略换行格式，JSON 比较解析后的内容。

以上是构建、静态比对与代码检查，不是游戏内运行测试。当前项目仍没有自动化游戏测试，
原有的反编译恢复限制也仍适用。
