package com.marblemd.app.markdown

import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text

/** Any strong RTL letter makes a prose block RTL, even after English words or numbers. */
internal fun containsRtl(text: CharSequence): Boolean {
    var i = 0
    while (i < text.length) {
        val cp = Character.codePointAt(text, i)
        val direction = Character.getDirectionality(cp)
        if (Character.isLetter(cp) && (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC)) return true
        i += Character.charCount(cp)
    }
    return false
}

/** Visible text for search/direction; code is omitted when deciding prose direction. */
internal fun blockText(node: Node, includeCode: Boolean = true): String = buildString {
    fun visit(n: Node) {
        when (n) {
            is Text -> append(n.literal)
            is Code -> if (includeCode) append(n.literal)
            is FencedCodeBlock -> if (includeCode) append(n.literal)
            is IndentedCodeBlock -> if (includeCode) append(n.literal)
            is SoftLineBreak, is HardLineBreak -> append('\n')
            else -> {
                var child = n.firstChild
                while (child != null) {
                    visit(child)
                    child = child.next
                }
            }
        }
    }
    visit(node)
}

internal data class ReadingMatch(val blockIndex: Int, val excerpt: String)

/** One result per matching block, so tapping a result always has a precise scroll target. */
internal fun searchBlocks(plan: MarkdownRenderPlan, query: String): List<ReadingMatch> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return plan.blocks.mapNotNull { block ->
        val text = blockText(block.node).replace('\n', ' ')
        val at = text.indexOf(needle, ignoreCase = true)
        if (at < 0) null else {
            val start = (at - 45).coerceAtLeast(0)
            val end = (at + needle.length + 65).coerceAtMost(text.length)
            ReadingMatch(block.index, (if (start > 0) "…" else "") + text.substring(start, end) +
                (if (end < text.length) "…" else ""))
        }
    }
}
