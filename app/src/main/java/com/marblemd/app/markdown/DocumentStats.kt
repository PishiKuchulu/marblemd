package com.marblemd.app.markdown

import kotlin.math.ceil

/**
 * Quick facts about a Markdown document, shown in the Document info dialog.
 * Pure Kotlin so the counting rules are unit-tested without Android.
 */
data class DocumentStats(
    val words: Int,
    val characters: Int,
    val charactersNoSpaces: Int,
    val lines: Int,
    val headings: Int,
    val readingMinutes: Int
) {
    companion object {
        private const val WORDS_PER_MINUTE = 200.0
        private val HEADING = Regex("^#{1,6}\\s+\\S")
        private val WHITESPACE = Regex("\\s+")

        fun of(content: String): DocumentStats {
            if (content.isEmpty()) {
                return DocumentStats(0, 0, 0, 0, 0, 0)
            }
            val words = content.split(WHITESPACE).count { it.isNotEmpty() }
            val headings = content.lineSequence().count { HEADING.containsMatchIn(it) }
            return DocumentStats(
                words = words,
                characters = content.length,
                charactersNoSpaces = content.count { !it.isWhitespace() },
                lines = content.lines().size,
                headings = headings,
                readingMinutes = if (words == 0) 0
                else ceil(words / WORDS_PER_MINUTE).toInt().coerceAtLeast(1)
            )
        }
    }
}
