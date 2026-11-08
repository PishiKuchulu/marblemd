package com.marblemd.app.library

import java.util.Locale

/** Where a library search hit matched, best first. */
enum class LibraryHitKind {
    OPEN_TITLE,
    RECENT_TITLE,
    OPEN_CONTENT,
    RECENT_SNIPPET
}

/** One library search result: either an open tab or a recent document. */
data class LibrarySearchHit(
    val kind: LibraryHitKind,
    val documentId: String?,
    val recentUri: String?,
    val title: String,
    val excerpt: String
)

/**
 * Full-text search across the library: open tabs (title + whole body) and
 * recents (title + the stored snippet; a recent's file is not opened just to
 * search it). A document that is both open and recent is reported once, as
 * the open tab, because its full text is available there.
 *
 * Pure Kotlin on purpose: the ranking and excerpt rules are unit-tested
 * without Android.
 */
object LibrarySearch {

    data class OpenDocument(
        val id: String,
        val title: String,
        val uri: String?,
        val content: String
    )

    fun search(
        query: String,
        openDocuments: List<OpenDocument>,
        recents: List<RecentDocument>
    ): List<LibrarySearchHit> {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return emptyList()

        val openUris = openDocuments.mapNotNull { it.uri }.toSet()
        val titleHits = mutableListOf<LibrarySearchHit>()
        val bodyHits = mutableListOf<LibrarySearchHit>()

        for (document in openDocuments) {
            val titleMatch = document.title.lowercase(Locale.ROOT).contains(needle)
            val contentAt = document.content.lowercase(Locale.ROOT).indexOf(needle)
            when {
                titleMatch -> titleHits += LibrarySearchHit(
                    kind = LibraryHitKind.OPEN_TITLE,
                    documentId = document.id,
                    recentUri = null,
                    title = document.title,
                    excerpt = if (contentAt >= 0) {
                        excerpt(document.content, contentAt, needle.length)
                    } else {
                        leadText(document.content)
                    }
                )
                contentAt >= 0 -> bodyHits += LibrarySearchHit(
                    kind = LibraryHitKind.OPEN_CONTENT,
                    documentId = document.id,
                    recentUri = null,
                    title = document.title,
                    excerpt = excerpt(document.content, contentAt, needle.length)
                )
            }
        }

        val recentTitleHits = mutableListOf<LibrarySearchHit>()
        val recentSnippetHits = mutableListOf<LibrarySearchHit>()
        for (recent in recents) {
            if (recent.uri in openUris) continue
            val titleMatch = recent.title.lowercase(Locale.ROOT).contains(needle)
            val snippetAt = recent.snippet.lowercase(Locale.ROOT).indexOf(needle)
            when {
                titleMatch -> recentTitleHits += LibrarySearchHit(
                    kind = LibraryHitKind.RECENT_TITLE,
                    documentId = null,
                    recentUri = recent.uri,
                    title = recent.title,
                    excerpt = if (snippetAt >= 0) {
                        excerpt(recent.snippet, snippetAt, needle.length)
                    } else {
                        recent.snippet
                    }
                )
                snippetAt >= 0 -> recentSnippetHits += LibrarySearchHit(
                    kind = LibraryHitKind.RECENT_SNIPPET,
                    documentId = null,
                    recentUri = recent.uri,
                    title = recent.title,
                    excerpt = excerpt(recent.snippet, snippetAt, needle.length)
                )
            }
        }

        return titleHits + recentTitleHits + bodyHits + recentSnippetHits
    }

    /** A window around the match, mirroring the in-document search excerpts. */
    internal fun excerpt(text: String, matchAt: Int, needleLength: Int): String {
        val flat = text.replace('\n', ' ')
        val start = (matchAt - 45).coerceAtLeast(0)
        val end = (matchAt + needleLength + 65).coerceAtMost(flat.length)
        return (if (start > 0) "…" else "") + flat.substring(start, end) +
            (if (end < flat.length) "…" else "")
    }

    /** The first non-blank line, used when only the title matched. */
    internal fun leadText(content: String): String =
        content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(120) ?: ""
}
