package org.lnr.t2s

import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.content.component.data.TextData
import io.nightfish.lightnovelreader.api.text.ComponentProcessor
import io.nightfish.lightnovelreader.api.text.TextProcessor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.lnr.t2s.converter.TraditionalToSimplified

internal class ConversionProcessor(
    converter: TraditionalToSimplified,
    @Volatile var conversionEnabled: Boolean
) : TextProcessor {
    @Volatile private var converter: TraditionalToSimplified? = converter

    override val enabled: Boolean
        get() = conversionEnabled && converter != null

    // Recheck here: the host can have captured this processor before onUnload.
    override fun processText(text: String): String =
        if (conversionEnabled) converter?.convert(text) ?: text else text

    override fun processChapterContent(
        bookId: String,
        chapterContent: ChapterContent,
        componentProcessor: ComponentProcessor
    ): ChapterContent {
        val activeConverter = if (conversionEnabled) converter else null
        if (activeConverter == null) return chapterContent
        // Use one state snapshot for the whole chapter when settings change.
        val transform: (String) -> String = activeConverter::convert
        val original = chapterContent.content
        val components = original["components"] as? JsonArray
            ?: return chapterContent.copy(title = transform(chapterContent.title))
        val transformed = JsonArray(components.map { element ->
            val component = element as? JsonObject ?: return@map element
            val id = (component["id"] as? JsonPrimitive)?.content ?: return@map element
            // Never deserialize nontext components (e.g. image URIs).
            val type = componentProcessor.dataKClassMap[id] ?: return@map element
            if (!TextData::class.java.isAssignableFrom(type.java)) return@map element
            val serializer = componentProcessor.serializerMap[id] ?: return@map element
            val data = component["data"] ?: return@map element
            val decoded = serializer.fromJsonElement(data)
            if (decoded !is TextData<*>) return@map element
            val converted = decoded.processText(transform)
            JsonObject(component.toMutableMap().apply {
                put("data", converted.toJsonElement())
            })
        })
        return chapterContent.copy(
            title = transform(chapterContent.title),
            content = JsonObject(original.toMutableMap().apply {
                put("components", transformed)
            })
        )
    }

    fun close() {
        conversionEnabled = false
        converter = null
    }
}
