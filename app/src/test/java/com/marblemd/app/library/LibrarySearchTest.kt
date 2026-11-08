package com.marblemd.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {

    private fun recent(uri: String, title: String, snippet: String) = RecentDocument(
        uri = uri,
        title = title,
        lastOpenedAt = 0L,
        scrollIndex = 0,
        writable = true,
        charCount = 0,
        snippet = snippet
    )

    private fun open(id: String, title: String, uri: String?, content: String) =
        LibrarySearch.OpenDocument(id = id, title = title, uri = uri, content = content)

    @Test
    fun emptyQueryFindsNothing() {
        val results = LibrarySearch.search(
            "  ",
            listOf(open("1", "Notes", null, "hello world")),
            listOf(recent("u1", "Notes", "hello"))
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun titleHitsRankBeforeContentHits() {
        val results = LibrarySearch.search(
            "kotlin",
            listOf(
                open("1", "Random notes", null, "something about kotlin coroutines"),
                open("2", "Kotlin guide", null, "unrelated body")
            ),
            emptyList()
        )
        assertEquals(2, results.size)
        assertEquals(LibraryHitKind.OPEN_TITLE, results[0].kind)
        assertEquals("2", results[0].documentId)
        assertEquals(LibraryHitKind.OPEN_CONTENT, results[1].kind)
    }

    @Test
    fun matchingIsCaseInsensitive() {
        val results = LibrarySearch.search(
            "KOTLIN",
            listOf(open("1", "notes", null, "Written in kotlin")),
            emptyList()
        )
        assertEquals(1, results.size)
    }

    @Test
    fun openDocumentShadowsItsRecentEntry() {
        val results = LibrarySearch.search(
            "report",
            listOf(open("1", "Weekly report", "content://a", "body text")),
            listOf(recent("content://a", "Weekly report", "report snippet"))
        )
        assertEquals(1, results.size)
        assertEquals(LibraryHitKind.OPEN_TITLE, results[0].kind)
    }

    @Test
    fun recentTitleAndSnippetHitsAreFound() {
        val results = LibrarySearch.search(
            "invoice",
            emptyList(),
            listOf(
                recent("u1", "Invoice March", "paid in full"),
                recent("u2", "Shopping", "the invoice is attached")
            )
        )
        assertEquals(2, results.size)
        assertEquals(LibraryHitKind.RECENT_TITLE, results[0].kind)
        assertEquals("u1", results[0].recentUri)
        assertEquals(LibraryHitKind.RECENT_SNIPPET, results[1].kind)
    }

    @Test
    fun excerptCentresOnTheMatch() {
        val text = "a".repeat(100) + " needle " + "b".repeat(100)
        val excerpt = LibrarySearch.excerpt(text, 101, 6)
        assertTrue(excerpt.startsWith("…"))
        assertTrue(excerpt.endsWith("…"))
        assertTrue(excerpt.contains("needle"))
    }

    @Test
    fun persianTextMatchesWithoutCase() {
        val results = LibrarySearch.search(
            "یادداشت",
            listOf(open("1", "دفتر", null, "این یک یادداشت روزانه است")),
            emptyList()
        )
        assertEquals(1, results.size)
        assertEquals(LibraryHitKind.OPEN_CONTENT, results[0].kind)
    }
}
