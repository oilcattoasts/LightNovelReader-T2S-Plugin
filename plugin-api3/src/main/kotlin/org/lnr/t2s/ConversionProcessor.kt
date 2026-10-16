package org.lnr.t2s

import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.MutableChapterContent
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.content.component.SimpleTextComponentData
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
    override val enabled: Boolean get() = conversionEnabled && converter != null

    override fun processText(text: String): String =
        if (conversionEnabled) converter?.convert(text) ?: text else text

    override fun processBookInformation(bookInformation: BookInformation): BookInformation {
        val active = if (conversionEnabled) converter else null
        if (active == null) return bookInformation
        // getStateBookInformation returns an empty observable object first,
        // then fills that same object asynchronously. A snapshot would freeze it.
        return BookView(bookInformation)
    }

    private inner class BookView(private val source: BookInformation) : BookInformation by source {
        override val title get() = processText(source.title)
        override val subtitle get() = processText(source.subtitle)
        override val author get() = processText(source.author)
        override val description get() = processText(source.description)
        override val publishingHouse get() = processText(source.publishingHouse)
        override fun copy(): BookInformation = toMutable()
        override fun toMutable() = MutableBookInformation(
            id, title, subtitle, coverUri, author, description, tags,
            publishingHouse, wordCount, lastUpdated, isComplete
        )
    }

    override fun processChapterContent(
        bookId: String,
        chapterContent: ChapterContent,
        componentProcessor: ComponentProcessor
    ): ChapterContent {
        val active = if (conversionEnabled) converter else null
        if (active == null) return chapterContent
        return ChapterView(chapterContent, componentProcessor)
    }

    private inner class ChapterView(
        private val source: ChapterContent,
        private val componentProcessor: ComponentProcessor
    ) : ChapterContent by source {
        private var cachedSource: JsonObject? = null
        private var cachedContent: JsonObject? = null
        override val title get() = processText(source.title)
        override val content get() = convertedContent()
        override fun copy(): ChapterContent = toMutable()
        override fun toMutable() = MutableChapterContent(id, title, content, lastChapter, nextChapter)

        @Synchronized private fun convertedContent(): JsonObject {
            val original = source.content
            val active = if (conversionEnabled) converter else null
            if (active == null) return original
            if (cachedSource === original) return cachedContent!!
            val converted = convertContent(original, componentProcessor, active)
            cachedSource = original
            cachedContent = converted
            return converted
        }
    }

    private fun convertContent(
        original: JsonObject,
        componentProcessor: ComponentProcessor,
        active: TraditionalToSimplified
    ): JsonObject {
        val components = original["components"] as? JsonArray ?: return original
        val converted = JsonArray(components.map { element ->
            val component = element as? JsonObject ?: return@map element
            val id = (component["id"] as? JsonPrimitive)?.content ?: return@map element
            if (componentProcessor.dataKClassMap[id] != SimpleTextComponentData::class) {
                return@map element
            }
            val data = component["data"] as? JsonObject ?: return@map element
            val text = data["text"] as? JsonPrimitive ?: return@map element
            if (!text.isString) return@map element
            JsonObject(component.toMutableMap().apply {
                put("data", JsonObject(data.toMutableMap().apply {
                    put("text", JsonPrimitive(active.convert(text.content)))
                }))
            })
        })
        return JsonObject(original.toMutableMap().apply { put("components", converted) })
    }

    fun close() {
        conversionEnabled = false
        converter = null
    }
}
