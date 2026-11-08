package com.marblemd.app.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentStatsTest {

    @Test
    fun emptyDocumentIsAllZero() {
        val stats = DocumentStats.of("")
        assertEquals(0, stats.words)
        assertEquals(0, stats.characters)
        assertEquals(0, stats.lines)
        assertEquals(0, stats.readingMinutes)
    }

    @Test
    fun countsWordsCharactersAndLines() {
        val stats = DocumentStats.of("hello world\nsecond line here")
        assertEquals(5, stats.words)
        assertEquals(28, stats.characters)
        assertEquals(24, stats.charactersNoSpaces)
        assertEquals(2, stats.lines)
    }

    @Test
    fun countsAtxHeadingsOnly() {
        val content = "# Title\n\n## Section\nnot a # heading\n###### Deep\n####### Too deep"
        assertEquals(3, DocumentStats.of(content).headings)
    }

    @Test
    fun readingTimeRoundsUpAtTwoHundredWordsPerMinute() {
        val twoHundred = (1..200).joinToString(" ") { "word" }
        assertEquals(1, DocumentStats.of(twoHundred).readingMinutes)
        assertEquals(2, DocumentStats.of("$twoHundred extra").readingMinutes)
    }

    @Test
    fun whitespaceOnlyDocumentHasNoWords() {
        val stats = DocumentStats.of("   \n  ")
        assertEquals(0, stats.words)
        assertEquals(0, stats.readingMinutes)
    }
}
