package org.lnr.t2s

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.plugin.PluginContext
import io.nightfish.lightnovelreader.api.text.TextProcessingRepositoryApi
import org.lnr.t2s.converter.TraditionalToSimplified
import java.util.UUID

@Plugin(
    name = "繁体转简体",
    version = 1,
    versionName = "0.1.0",
    author = "LNR T2S Plugin Contributors",
    description = "离线转换正文、书名和目录；保留原始缓存。",
    updateUrl = "",
    apiVersion = 4
)
class T2SPlugin(
    context: Context,
    private val pluginContext: PluginContext,
    private val textRepository: TextProcessingRepositoryApi
) : LightNovelReaderPlugin {
    private val preferences = context.getSharedPreferences(
        "${pluginContext.packageName}.settings", Context.MODE_PRIVATE
    )
    private var selected by mutableStateOf(preferences.getBoolean("enabled", true))
    private var failure by mutableStateOf<String?>(null)
    private var processor: ConversionProcessor? = null

    override fun onLoad() {
        if (processor != null) return
        try {
            val converter = pluginContext.getAsset("opencc/TSCharacters.txt")
                .reader(Charsets.UTF_8).use { characters ->
                    pluginContext.getAsset("opencc/TSPhrases.txt")
                        .reader(Charsets.UTF_8).use { phrases ->
                            TraditionalToSimplified(characters, phrases)
                        }
                }
            val active = ConversionProcessor(converter, selected)
            // API 4 keeps registered processors forever and ignores duplicate IDs.
            // A per-load ID permits reloads; onUnload makes the old slot inert.
            textRepository.registerProcessors(
                pluginContext.ofId("t2s_${UUID.randomUUID()}"), active
            )
            processor = active
            failure = null
        } catch (exception: Exception) {
            failure = "词典加载失败，请重新安装插件后重启阅读器。"
            Log.e("LnrT2S", "Cannot initialize conversion dictionaries", exception)
        }
    }

    override fun onUnload() {
        processor?.close()
        processor = null
    }

    private fun setConversionEnabled(value: Boolean) {
        preferences.edit().putBoolean("enabled", value).apply()
        selected = value
        processor?.conversionEnabled = value
    }

    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier.padding(paddingValues)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("繁体转简体", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用离线转换")
                Switch(
                    checked = selected,
                    onCheckedChange = ::setConversionEnabled,
                    enabled = failure == null && processor != null
                )
            }
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("转换正文、书名、简介和目录，原始缓存保持不变。")
            Text("修改后请退出当前章节并重新进入，以重新加载正文和分页。")
            Text("请关闭阅读设置中的“简转繁”，避免两种转换同时运行。")
            Text("EPUB 导出会沿用当前转换设置。插图中的文字不会转换。")
            Text("示例", style = MaterialTheme.typography.titleMedium)
            Text("原文：乾隆年間，他回覆了這封信。")
            Text("预览：" + (processor?.processText("乾隆年間，他回覆了這封信。")
                ?: "乾隆年間，他回覆了這封信。"))
            Text(
                "词典：OpenCC 1.1.9 · Apache-2.0。仅转换字形和词组，不转换台湾地区用语。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
