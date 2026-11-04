package com.marblemd.app.markdown

import com.marblemd.app.model.DirectionMode
import org.commonmark.parser.Parser
import org.junit.Assert.*
import org.junit.Test

class ReadingToolsTest {
    private val parser = Parser.builder().build()

    @Test fun rtlDetectionHandlesMixedTextAndNeutralCharacters() {
        assertTrue(containsRtl("English first 123 سلام"))
        assertTrue(containsRtl("עברית"))
        assertFalse(containsRtl("English ۱۲۳ !"))
        assertFalse(containsRtl("😀"))
    }

    @Test fun proseDirectionIgnoresInlineAndBlockCode() {
        val prose = parser.parse("English `سلام`").firstChild
        assertFalse(containsRtl(blockText(prose, includeCode = false)))
        assertTrue(containsRtl(blockText(prose)))
        val code = parser.parse("```\nسلام\n```").firstChild
        assertEquals("", blockText(code, includeCode = false))
    }

    @Test fun searchFindsProseAndCodeAndReturnsBlockIndices() {
        val doc = parser.parse("# Hello\n\nسلام دنیا\n\n```\nHELLO code\n```")
        val blocks = generateSequence(doc.firstChild) { it.next }.mapIndexed { i, n -> MarkdownRenderBlock(i, n) }.toList()
        val plan = MarkdownRenderPlan(1, blocks, emptyList())
        assertEquals(listOf(0, 2), searchBlocks(plan, "hello").map { it.blockIndex })
        assertEquals(listOf(1), searchBlocks(plan, "دنیا").map { it.blockIndex })
        assertTrue(searchBlocks(plan, "  ").isEmpty())
    }

    @Test fun htmlExportEscapesUntrustedHtmlAndIncludesTablesAndCode() {
        val html = HtmlExport.render("# سلام\n\n<script>alert(1)</script>\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\n```kotlin\nval x = 1\n```", "<title>", DirectionMode.RTL)
        assertTrue(html.contains("<title>&lt;title&gt;</title>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("val x = 1"))
        assertTrue(html.contains("data-direction=\"rtl\""))
    }

    @Test fun htmlExportSanitizesDangerousLinks() {
        val html = HtmlExport.render("[bad](javascript:alert%281%29)", "safe", DirectionMode.AUTO)
        assertFalse(html.contains("href=\"javascript:"))
    }
}
