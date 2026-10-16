# 验证记录

## 0.1.3：修复时间类型 DEX 接口不兼容

13:00:27 的用户崩溃日志明确报告：

```text
java.lang.AbstractMethodError:
j$.time.LocalDateTime BookInformation.getLastUpdated()
at MutableBookInformation.update(...)
```

宿主的 app 和 api 模块均启用了 `isCoreLibraryDesugaringEnabled`，使用 `desugar_jdk_libs:2.1.5`。此前插件遗漏同项配置，BookView 在 JVM 上实现的是正确的 Java 接口，但 DEX 中仍返回 `java.time.LocalDateTime`，与宿主改写后的 `j$.time.LocalDateTime` 不匹配。异步加载完成或打开详情后调用该方法即会崩溃。

API 3 插件现已使用与宿主相同的兼容转换配置。5 个 JVM 回归测试、Lint 和打包通过。新增 `scripts/verify-api3-apk.py`，直接解析 APK 中 DEX 的已定义方法：

- 检查旧 0.1.2 包：失败，实际签名 `()Ljava/time/LocalDateTime;`，与用户异常吻合。
- 检查新 0.1.3 包：通过，实际签名 `()Lj$/time/LocalDateTime;`。
- 同时检查未重复打包宿主 API 类及词典资源存在。

该检查覆盖 JVM 测试无法发现的 Android 打包问题。手机实际运行仍待用户验证。更新包：`dist/LightNovelReader-T2S-0.1.3-api3-debug.apk.lnrp`。

## 0.1.2：最近阅读一直加载修复

用户报告 0.1.1 启用后“阅读中”始终显示骨架占位，而探索页转换正常。核对 1.2.3d 的 `BookRepository.getStateBookInformation`：它先返回一个空的 Compose 可观测对象，再在协程中更新同一个对象。0.1.1 对空对象提前创建快照，导致界面持有的对象不再接收更新。同类风险也存在于 `getStateChapterContent`。

API 3 处理器现在返回实时只读转换视图：书籍字段及章节内容读取时跟随原始对象；原始数据不变，显式调用 `copy()` / `toMutable()` 时才生成快照。章节转换按原始 JSON 对象缓存，数据更新时失效；停用后直接显示原文。

`testDebugUnitTest`、`lintDebug`、`packageDebugPlugin` 已通过。5 个 API 3 测试均通过，包括新增的“空书籍对象随后加载完成，UI 状态观察者必须收到通知”和“空章节随后填充、后续更新、复制和开关行为”。Lint 0 错误。手机实际运行仍待用户验证。

安装 `dist/LightNovelReader-T2S-0.1.2-api3-debug.apk.lnrp` 后启用插件，并完全退出、重开阅读器，避免界面继续持有旧版创建的空快照。

## 0.1.1：1.2.3d / API 3 兼容修复

用户设备版本为 `1.2.3d [10203005]`。已核对上游 `1.2.3d` 标签：宿主 `API_VERSION = 3`，兼容组为 `{2,3}`。原 `0.1.0` 包使用 API 4，触发通用的 `plugin_api_incompatible` 提示。

新增独立模块 `plugin-api3`，使用稳定 API `0.3` 重新编译，适配根包下的 `PluginContext`、单参数 `registerProcessors(TextProcessor)`、旧版 `SimpleTextComponentData` 和 `MutableChapterContent`，并复制可变书籍信息后再转换。版本号 `0.1.1`、versionCode 2、apiVersion 3，与旧包使用同一调试签名，可覆盖导入。

本次构建命令已通过：

```sh
./gradlew :plugin-api3:testDebugUnitTest :plugin-api3:lintDebug :plugin-api3:packageDebugPlugin
```

3 个 API 3 集成测试全部通过：

- 使用已发布 API 3 的兼容检查器验证新包被接受，并确认 API 4 会被拒绝；检查实际注入构造函数和注册接口。
- 转换旧版可变章节的副本，验证原文、章节 ID、前后章链接、图片、自定义组件与额外 JSON 字段保留。
- 验证关闭、重启用与卸载后的处理行为。

Lint 为 0 错误、10 个警告，主要是有意与宿主对齐的旧依赖、插件发现接收器、应用图标和备份配置提醒。APK 签名验证通过，并已确认与原 `0.1.0` 包的证书一致；词典逐字节检查通过。尚未在用户手机上安装运行，运行效果仍需宿主验收。

供该设备使用的安装包：`dist/LightNovelReader-T2S-0.1.1-api3-debug.apk.lnrp`。在插件管理中覆盖导入后启用；如仍显示原版本，重启阅读器再检查。

## 自动验证

转换核心的独立脚本及 Gradle `:converter:check` 已通过：4,439 项断言，包括全部两份词典条目、词组最长匹配、歧义首候选、扩展汉字、空文本、非中文字符、换行、16 万 UTF-16 单元章节、32 次并发转换及错误词典拒绝。

长章节耗时在本机 JDK 17 约 6 ms，仅作为桌面 JVM 的冒烟检查，不能替代 Android 设备性能测量。

2026-10-16 的本地 Android 构建已通过：

```sh
./gradlew :plugin:testDebugUnitTest :plugin:lintDebug :plugin:packageDebugPlugin
```

4 个实际 API 集成测试全部通过，无失败或跳过。覆盖关闭开关、卸载后的旧引用、重新开启、章节标识不变、原始 JSON 不变、未知组件和图片 URL 保留，以及已识别段落的转换和样式保留。

Lint 为 0 错误、4 警告：Compose BOM 版本更新提醒（有意与 API 对齐）、插件发现接收器公开（与宿主发现协议一致，回调不执行操作）、Android 12 备份规则提醒、没有应用图标（插件没有启动器页面）。这些警告不影响本次调试包构建。

安装包检查通过：ZIP 完整性、两份词典与源码逐字节一致、许可已打包、插件入口及转换核心类存在、DEX 中没有重复打包宿主 API 类。Manifest 的 `lnr_plugin` 与发现接收器配置正确，最低 Android API 24。`apksigner verify --verbose` 通过 APK v2 签名验证。

交付调试包为 `dist/LightNovelReader-T2S-0.1.0-debug.apk.lnrp`；安装包与源码压缩包的 SHA-256 见 `dist/SHA256SUMS.txt`。

## 宿主手动验收

以下需要真实的 API 4 宿主和 Android 设备，尚未执行：

1. 导入插件，确认插件名称、版本、API 兼容性和设置页可用。
2. 关闭宿主简转繁。在线打开繁体章节，确认书名、目录、正文和章节标题转换；检查插图仍然存在。
3. 提前缓存同一本书，断网后重新进入章节，确认无需下载即可转换。
4. 在滚动、翻页两种模式中浏览长章节，检查分页、前后章和阅读位置恢复。
5. 关闭插件内开关，重新进入章节，确认恢复原文；重启宿主确认设置保留。
6. 在同一进程中停用再启用插件，重新打开章节，确认转换恢复且旧处理器不再生效。
7. 分别在开启、关闭转换时导出 EPUB，检查正文、标题、图片和目录。
8. 包含其他插件自定义组件的章节，确认这些组件仍可正常渲染。
9. 检查含颜色、加粗等样式的文本；跨样式节点的词组是首版的已知限制。
10. 连续加载多章并切换开关，检查实际手机的耗时、内存和界面响应。
