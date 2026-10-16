package org.lnr.t2s

import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.content.component.data.ParagraphComponentData
import io.nightfish.lightnovelreader.api.text.ComponentProcessor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lnr.t2s.converter.TraditionalToSimplified
import java.io.StringReader

class ConversionProcessorTest {
    private fun processor() = ConversionProcessor(
        TraditionalToSimplified(StringReader("繁\t繁\n體\t体\n書\t书\n"), StringReader("")), true
    )

    @Test fun disableAndUnloadReturnOriginalText() {
        val processor = processor()
        assertTrue(processor.enabled)
        assertEquals("繁体书", processor.processText("繁體書"))
        processor.conversionEnabled = false
        assertFalse(processor.enabled)
        assertEquals("繁體書", processor.processText("繁體書"))
        processor.conversionEnabled = true
        assertEquals("繁体书", processor.processText("繁體書"))
        processor.close()
        assertFalse(processor.enabled)
        assertEquals("繁體書", processor.processText("繁體書"))
        // A stale reference cannot reactivate a released converter.
        processor.conversionEnabled = true
        assertFalse(processor.enabled)
        assertEquals("繁體書", processor.processText("繁體書"))
    }

    @Test fun unknownComponentsImagesAndRootMetadataArePreserved() {
        val raw = Json.parseToJsonElement("""{
            "metadata": {"source": "繁體來源"},
            "components": [
                {"id":"unknown:custom", "data":{"text":"繁體"}, "extra":true},
                {"id":"image", "data":{"uri":"https://example.org/繁體.png"}},
                "future-component-format"
            ]
        }""") as JsonObject
        val chapter = ChapterContent("chapter-1", "繁體書", raw, "prev", "next")
        val transformed = processor().processChapterContent(
            "book", chapter, ComponentProcessor(emptyMap(), emptyMap(), raw)
        )
        assertEquals("繁体书", transformed.title)
        assertEquals(raw, transformed.content)
        assertEquals("chapter-1", transformed.id)
        assertEquals("prev", transformed.prevChapter)
        assertEquals("next", transformed.nextChapter)
        assertEquals("繁體書", chapter.title)
    }

    @Test fun disabledProcessorDoesNotRebuildChapter() {
        val processor = processor().apply { conversionEnabled = false }
        val raw = Json.parseToJsonElement("""{"components":[]}""") as JsonObject
        val chapter = ChapterContent("chapter", "繁體書", raw)
        assertSame(chapter, processor.processChapterContent(
            "book", chapter, ComponentProcessor(emptyMap(), emptyMap(), raw)
        ))
    }

    @Test fun knownParagraphRetainsStyleAndWrapperFields() {
        val id = ParagraphComponentData.id.toString()
        val raw = Json.parseToJsonElement("""{
            "metadata":"keep",
            "components":[{
                "id":"$id", "bookmark":"keep",
                "data":{"paragraph":{"textNodes":[{"text":"繁體書","style":{"italic":true}}]}}
            }]
        }""") as JsonObject
        val chapter = ChapterContent("chapter", "繁體書", raw)
        val result = processor().processChapterContent("book", chapter, ComponentProcessor(
            mapOf(id to ParagraphComponentData.jsonSerializer),
            mapOf(id to ParagraphComponentData::class), raw
        ))
        assertEquals("繁体书", result.title)
        assertTrue(result.content.toString().contains("繁体书"))
        assertTrue(result.content.toString().contains("\"bookmark\":\"keep\""))
        assertEquals(raw["metadata"], result.content["metadata"])
        val inputNode = raw["components"]!!.jsonArray[0].jsonObject["data"]!!
            .jsonObject["paragraph"]!!.jsonObject["textNodes"]!!.jsonArray[0].jsonObject
        val outputNode = result.content["components"]!!.jsonArray[0].jsonObject["data"]!!
            .jsonObject["paragraph"]!!.jsonObject["textNodes"]!!.jsonArray[0].jsonObject
        assertEquals(inputNode["style"], outputNode["style"])
        assertTrue(chapter.content.toString().contains("繁體書"))
    }
}
