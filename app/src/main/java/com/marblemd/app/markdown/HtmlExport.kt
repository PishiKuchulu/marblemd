package com.marblemd.app.markdown

import com.marblemd.app.model.DirectionMode
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import java.net.URI
import org.commonmark.renderer.html.HtmlRenderer

/** Creates a portable document without executing raw HTML from an untrusted Markdown file. */
internal object HtmlExport {
    private fun sanitizeDestinations(node: Node) {
        when (node) {
            is Link -> if (!safeDestination(node.destination, image = false)) node.destination = ""
            is Image -> if (!safeDestination(node.destination, image = true)) node.destination = ""
        }
        var child = node.firstChild
        while (child != null) {
            sanitizeDestinations(child)
            child = child.next
        }
    }

    private fun safeDestination(destination: String, image: Boolean): Boolean {
        if (destination.any { it <= ' ' || it == '\\' }) return false
        val uri = runCatching { URI(destination) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(java.util.Locale.ROOT) ?: return true
        return scheme == "http" || scheme == "https" || (!image && scheme == "mailto")
    }

    fun render(markdown: String, title: String, direction: DirectionMode): String {
        val extensions = listOf(TablesExtension.create(), StrikethroughExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build()
        val document = parser.parse(markdown)
        sanitizeDestinations(document)
        val body = renderer.render(document)
        val escapedTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;")
        val forced = when (direction) {
            DirectionMode.AUTO -> "auto"
            DirectionMode.RTL -> "rtl"
            DirectionMode.LTR -> "ltr"
        }
        return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>$escapedTitle</title><style>
:root{color-scheme:light dark} body{max-width:52rem;margin:auto;padding:1.5rem;font:18px/1.8 system-ui,sans-serif;overflow-wrap:anywhere}
pre{direction:ltr;text-align:left;overflow:auto;padding:1rem;background:#8882;border-radius:.5rem} code{font-family:monospace} :not(pre)>code{direction:ltr;unicode-bidi:isolate;background:#8882} img{max-width:100%;height:auto} table{border-collapse:collapse;display:block;overflow:auto} td,th{border:1px solid #888;padding:.4rem} blockquote{border-inline-start:3px solid #888;padding-inline-start:1rem;margin-inline:0} a{color:#5687cf}
</style></head><body data-direction="$forced"><main>$body</main><script>
// Decide per prose block, ignoring inline code. Code is always LTR.
for (const el of document.querySelectorAll('p,h1,h2,h3,h4,h5,h6,li,td,th,blockquote')) {
  const clone = el.cloneNode(true); clone.querySelectorAll('code,pre').forEach(x => x.remove());
  const rtl = [...clone.textContent].some(c => /\p{Letter}/u.test(c) && /[\u0590-\u05ff\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff\ufb1d-\ufdff\ufe70-\ufeff]/u.test(c));
  el.dir = document.body.dataset.direction === 'auto' ? (rtl ? 'rtl' : 'ltr') : document.body.dataset.direction;
}
</script></body></html>"""
    }
}
