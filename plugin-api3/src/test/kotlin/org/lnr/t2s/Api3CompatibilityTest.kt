package org.lnr.t2s

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import io.nightfish.lightnovelreader.api.ApiCompat
import io.nightfish.lightnovelreader.api.ApiMetadata
import io.nightfish.lightnovelreader.api.PluginContext
import io.nightfish.lightnovelreader.api.book.MutableChapterContent
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.content.component.SimpleTextComponentData
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.text.ComponentProcessor
import io.nightfish.lightnovelreader.api.text.TextProcessingRepositoryApi
import io.nightfish.lightnovelreader.api.text.TextProcessor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.lnr.t2s.converter.TraditionalToSimplified
import java.io.StringReader
import java.time.LocalDateTime

class Api3CompatibilityTest {
    private fun processor() = ConversionProcessor(
        TraditionalToSimplified(StringReader("體\t体\n書\t书\n"), StringReader("")), true
    )

    @Test fun targetHostAcceptsPluginAndCanInjectConstructor() {
        val metadata = T2SPlugin::class.java.getAnnotation(Plugin::class.java)!!
        assertEquals(3, ApiMetadata.API_VERSION)
        assertTrue(ApiCompat.isSupported(metadata.apiVersion, 3))
        assertFalse(ApiCompat.isSupported(4, 3))
        assertNotNull(T2SPlugin::class.java.getConstructor(
            Context::class.java, PluginContext::class.java, TextProcessingRepositoryApi::class.java
        ))
        assertNotNull(TextProcessingRepositoryApi::class.java.getMethod(
            "registerProcessors", TextProcessor::class.java
        ))
    }

    @Test fun convertsLegacyTextWithoutMutatingCachedMutableChapter() {
        val raw = Json.parseToJsonElement("""{
            "metadata":"繁體",
            "components":[
                {"id":"simple_text","bookmark":"keep","data":{"text":"繁體書","style":"keep"}},
                {"id":"image","data":{"uri":"https://example.org/繁體.png"}},
                {"id":"custom","data":{"text":"繁體"}},
                "future-format"
            ]
        }""") as JsonObject
        val chapter = MutableChapterContent("chapter-1", "繁體書", raw, "prev", "next")
        val result = processor().processChapterContent("book", chapter, ComponentProcessor(
            mapOf("simple_text" to SimpleTextComponentData.jsonSerializer),
            mapOf("simple_text" to SimpleTextComponentData::class), raw
        ))
        assertNotSame(chapter, result)
        assertEquals("繁体书", result.title)
        assertEquals("繁體書", chapter.title)
        assertSame(raw, chapter.content)
        val before = raw["components"]!!.jsonArray
        val after = result.content["components"]!!.jsonArray
        assertEquals("繁体书", after[0].jsonObject["data"]!!.jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(before[0].jsonObject["bookmark"], after[0].jsonObject["bookmark"])
        assertEquals(before[0].jsonObject["data"]!!.jsonObject["style"], after[0].jsonObject["data"]!!.jsonObject["style"])
        assertEquals(before.drop(1), after.drop(1))
        assertEquals(raw["metadata"], result.content["metadata"])
        assertEquals("chapter-1", result.id)
        assertEquals("prev", result.lastChapter)
        assertEquals("next", result.nextChapter)
    }

    @Test fun disabledAndUnloadedProcessorsReturnOriginal() {
        val processor = processor()
        assertEquals("繁体书", processor.processText("繁體書"))
        processor.conversionEnabled = false
        assertEquals("繁體書", processor.processText("繁體書"))
        val raw = Json.parseToJsonElement("""{"components":[]}""") as JsonObject
        val chapter = MutableChapterContent("chapter", "繁體書", raw)
        assertSame(chapter, processor.processChapterContent(
            "book", chapter, ComponentProcessor(emptyMap(), emptyMap(), raw)
        ))
        processor.conversionEnabled = true
        assertEquals("繁体书", processor.processText("繁體書"))
        processor.close()
        processor.conversionEnabled = true
        assertFalse(processor.enabled)
        assertEquals("繁體書", processor.processText("繁體書"))
    }

    @Test fun readingListTracksBookFilledAfterProcessorReturns() {
        // Same loading contract as 1.2.3d getStateBookInformation: an initially
        // empty observable instance is returned before local/network data arrives.
        val source = PendingBook()
        val processor = processor()
        val displayed = processor.processBookInformation(source)
        assertTrue(displayed.isEmpty())
        assertEquals("", displayed.title)
        var invalidated = false
        val observer = SnapshotStateObserver { it() }
        observer.start()
        try {
            observer.observeReads(Any(), { _: Any -> invalidated = true }) { displayed.title }
            source.title = "繁體書"
            source.author = "繁體作者"
            source.description = "繁體簡介"
            Snapshot.sendApplyNotifications()
            assertTrue("UI must observe the host object's later updates", invalidated)
            assertFalse(displayed.isEmpty())
            assertEquals("繁体书", displayed.title)
            assertEquals("繁体作者", displayed.author)
            assertEquals("繁体簡介", displayed.description)
            assertEquals("繁體書", source.title)
            source.title = "書的更新"
            assertEquals("书的更新", displayed.title)
            processor.close()
            assertEquals("書的更新", displayed.title)
        } finally { observer.stop(); observer.clear() }
    }

    @Test fun pendingChapterRemainsLiveAndExplicitCopiesAreSnapshots() {
        val processor = processor()
        val source = MutableChapterContent("chapter", "", JsonObject(emptyMap()))
        val view = processor.processChapterContent("book", source, ComponentProcessor(
            emptyMap(), mapOf("simple_text" to SimpleTextComponentData::class), source.content
        ))
        assertTrue(view.isEmpty())
        // Read while empty to exercise invalidation of the content cache too.
        assertTrue(view.content.isEmpty())
        source.title = "繁體書"
        source.content = Json.parseToJsonElement("""{
            "components":[{"id":"simple_text","data":{"text":"繁體書"}}]
        }""") as JsonObject
        source.nextChapter = "next"
        assertFalse(view.isEmpty())
        assertEquals("繁体书", view.title)
        assertTrue(view.content.toString().contains("繁体书"))
        assertTrue(source.content.toString().contains("繁體書"))
        assertEquals("next", view.nextChapter)
        val copy = view.copy()
        val mutableCopy = view.toMutable()
        assertNotSame(source, mutableCopy)
        source.title = "書的更新"
        source.content = Json.parseToJsonElement("""{
            "components":[{"id":"simple_text","data":{"text":"書的更新"}}]
        }""") as JsonObject
        assertEquals("书的更新", view.title)
        assertTrue(view.content.toString().contains("书的更新"))
        assertEquals("繁体书", copy.title)
        assertEquals("繁体书", mutableCopy.title)
        processor.conversionEnabled = false
        assertSame(source.content, view.content)
        assertEquals("書的更新", view.title)
    }

    private class PendingBook : BookInformation {
        override val id = "book"
        override var title by mutableStateOf("")
        override var subtitle by mutableStateOf("")
        override var author by mutableStateOf("")
        override var description by mutableStateOf("")
        override var publishingHouse by mutableStateOf("")
        // No Android implementation is required for text-only state observation.
        override val coverUri: Uri get() = error("Text conversion must not eagerly copy coverUri")
        override val tags = emptyList<String>()
        override val wordCount = WordCount(0)
        override val lastUpdated = LocalDateTime.MIN
        override val isComplete = false
    }
}
