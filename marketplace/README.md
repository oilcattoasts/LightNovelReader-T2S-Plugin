# 插件市场分发

`plugin.toml` 和 `icon.png` 用于官方市场索引。最终分发分支 `distribution` 的根目录还会包含用户实测的 `plugin.lnrp` 和许可文件。

当前分发包是 0.1.3、versionCode 4、API 3，与用户确认正常的 APK 逐字节一致，保留原签名。本次不重新签名、不改变已验证的代码或依赖。后续更新需继续使用相同证书；签名私钥不会出现在公开仓库中。

使用的格式参考官方已收录的 [plugin.toml](https://github.com/yukonisen/plugin-distributions/blob/potatolib/plugin.toml)，收录方式参考 [官方仓库说明](https://github.com/dmzz-yyhyy/LightNovelReader-PluginRepository/tree/store)。官方分发生成器及审核规则链接在本次准备时无法访问，具体收录结果以维护者审核为准。
