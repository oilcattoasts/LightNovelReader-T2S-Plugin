# LightNovelReader 繁体转简体插件

针对 [LightNovelReader #508](https://github.com/dmzz-yyhyy/LightNovelReader/issues/508) 的独立文本处理插件，不需要修改宿主或逐个改造数据源。

## 选择正确的安装包

| 阅读器 | 模块 | 安装包 |
| --- | --- | --- |
| **1.2.3d [10203005] / API 3** | `plugin-api3` | **`LightNovelReader-T2S-0.1.3-api3-debug.apk.lnrp`** |
| dev/1.3 / API 4 | `plugin` | `LightNovelReader-T2S-0.1.0-debug.apk.lnrp` |

`0.1.0` 为 API 4 包，无法在 1.2.3d 上启用。截图中的“基于旧版本构建”是通用的 API 不兼容提示，并不说明插件 API 一定比宿主低。API 3 版使用稳定依赖 `api:0.3`、旧版 `PluginContext`、单参数注册接口和 `SimpleTextComponentData`，不是只改元数据绕过检查。

0.1.3 补齐与宿主一致的 core library desugaring（`desugar_jdk_libs:2.1.5`），修复加载完成、点击小说时 `getLastUpdated()` 时间类型不匹配导致的闪退。

0.1.2 修复了 0.1.1 在“阅读中”页面提前复制加载占位对象、导致一直加载的问题。更新后请启用插件并完全退出、重开阅读器。

## 功能

- 离线转换章节正文、章节标题、卷目录以及宿主交给文本处理链的书名、作者、简介等信息。
- OpenCC 1.1.9 `TSCharacters` / `TSPhrases` 词典，词组优先、前向最长匹配，歧义映射取词典第一候选。
- 全局开关，保存设置；默认启用。
- 原始缓存保持不变；关闭后重新读取可恢复原文。
- 保留章节 ID、前后章链接、图片、自定义组件及章节 JSON 的其他字段。
- JVM 转换核心，无 JNI、联网服务或额外 Android 权限。

这不是官方 OpenCC 原生引擎的完整移植；实现的是上述两份词典的单次最长匹配。首版不提供简转繁、地区用语转换、按书籍配置或图片 OCR。富文本的每个文本节点独立转换，词组跨样式节点时无法共享上下文；字形转换仍然有效。

## 安装与使用

1. 根据上表选择对应 API 的包；**1.2.3d 请用 0.1.3-api3**，API 3 与 API 4 不属于同一兼容组。
2. 在宿主插件管理中导入构建生成的 `.apk.lnrp` 文件并启用；也可按项目插件发现机制安装原始 APK 后重启宿主。
3. 在插件详情页进入“繁体转简体”设置，确认转换开关。
4. 关闭宿主阅读设置中的“简转繁”，避免先转繁再转简。
5. 修改开关后退出当前章节再重新进入。已经载入的章节和分页不会原地刷新。

EPUB 导出走相同的正文处理链，会沿用当前开关。导出过程中请保持开关不变。图片内文字不转换。清除宿主应用数据会清除开关；插件开关使用宿主中以插件包名隔离的 SharedPreferences 保存，移除插件文件后可能保留。

## 构建

环境：JDK 17、Gradle 9.6.1、AGP 9.2.1、Kotlin / Compose Compiler 2.4.10、Android SDK Platform 37.0、Build Tools 36.0.0。Kotlin 和 Compose 依赖与本次发布的 API 对齐。

设置 `ANDROID_HOME`，或在本地 `local.properties` 中配置 `sdk.dir`，然后运行：

```sh
./gradlew :converter:check :plugin-api3:testDebugUnitTest :plugin-api3:lintDebug :plugin-api3:packageDebugPlugin
python3 scripts/verify-api3-apk.py plugin-api3/build/outputs/lnrp/LightNovelReader-T2S-0.1.3-api3-debug.apk.lnrp
# API 4 开发分支版本：
./gradlew :converter:check :plugin:testDebugUnitTest :plugin:lintDebug :plugin:packageDebugPlugin
```

安装包：

```text
plugin-api3/build/outputs/lnrp/LightNovelReader-T2S-0.1.3-api3-debug.apk.lnrp
plugin/build/outputs/lnrp/LightNovelReader-T2S-0.1.0-debug.apk.lnrp
```

本地构建产物放在 `dist/`，不提交到源码分支。市场分发文件位于 `distribution` 分支。4,439 项核心断言、5 项 API 3 回归测试、签名和实际 DEX 接口检查已通过；用户已确认 0.1.3 在 1.2.3d 上正常运行。

Debug 包用于验证，使用本机 Android 调试签名。分发稳定版本时应配置长期保管的 release 签名；宿主更新插件时会校验签名，不要交替使用不同机器生成的调试包。`assembleRelease` 默认生成未签名 APK，不能直接当作已签名发布包。

API 依赖为官方仓库 `https://maven.nariko.org/release` 的 `io.nightfish.lightnovelreader:api:0.4-SNAPSHOT`，仅 `compileOnly` 引用，不将另一份宿主 API 打进插件。Snapshot 可能变动；本次对照的发布源码是 `api-0.4-20261008.161610-18-sources.jar`。

API 3 模块使用同仓库的稳定版本 `io.nightfish.lightnovelreader:api:0.3`，Compose runtime / foundation-layout 1.11.3 和 Material 3 1.5.0-alpha22 与 1.2.3d 对齐。两个模块共用转换核心及词典，宿主适配与测试独立编译。

只验证转换核心时无需 Android SDK、Gradle 或下载依赖：

```sh
./scripts/test-converter.sh
```

## 项目结构

```text
converter/                     Java 转换核心与全词典回归验证
plugin/src/main/kotlin/         插件入口、设置页、正文适配
plugin/src/main/assets/opencc/  随包离线词典
plugin/src/test/                实际 LNR API 的处理器集成测试
plugin-api3/                    1.2.3d / API 3 兼容实现与测试
THIRD_PARTY_LICENSES/           第三方许可
docs/validation.md             验证记录与宿主验收步骤
```

## 生命周期

API 4 的处理器注册表没有注销接口，且重复 ID 会被忽略。插件每次加载注册一个独立 ID；卸载时关闭旧处理器并释放词典引用，允许同进程重新启用。宿主注册表中仍会留下一条停用记录，直到宿主进程退出。若宿主以后提供注销接口，应改用固定 ID 并在卸载时注销。

## 许可与来源

插件代码采用 Apache-2.0，见 [LICENSE](LICENSE)。词典来自 [OpenCC ver.1.1.9](https://github.com/BYVoid/OpenCC/tree/ver.1.1.9/data/dictionary)，许可及来源见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。构建布局和插件发现方式参考 [官方插件模板](https://github.com/dmzz-yyhyy/LightNovelReaderPlugin-Template)。本项目不是上游官方发布的插件。
