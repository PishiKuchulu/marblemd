package com.marblemd.app.library

import android.content.Context
import androidx.core.content.edit
import com.marblemd.app.model.MarkdownDocument
import com.marblemd.app.model.SaveState
import java.io.File

/**
 * Remembers what the user was reading.
 *
 * Two things are persisted:
 *
 *  * **recents** - every Markdown file opened before, with the scroll position
 *    it was left at, so "Continue reading" (and the recents list) returns to
 *    the exact same place;
 *  * **session** - the tabs that were open when the app went away, including
 *    unsaved text, so a crash or a swipe-away never loses work.
 */
class DocumentMemory(private val context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    // ------------------------------------------------------------- recents ---

    fun recents(): List<RecentDocument> =
        DocumentMemoryCodec.decodeRecords(preferences.getString(KEY_RECENTS, null))
            .mapNotNull(RecentDocument::fromRecord)
            .sortedByDescending { it.lastOpenedAt }
            .take(MAX_RECENTS)

    fun recent(uri: String): RecentDocument? = recents().firstOrNull { it.uri == uri }

    fun remember(
        uri: String,
        title: String,
        writable: Boolean,
        charCount: Int,
        snippet: String,
        scrollIndex: Int = 0
    ) {
        val existing = recent(uri)
        val updated = RecentDocument(
            uri = uri,
            title = title,
            lastOpenedAt = System.currentTimeMillis(),
            scrollIndex = if (scrollIndex > 0) scrollIndex else existing?.scrollIndex ?: 0,
            writable = writable,
            charCount = charCount,
            snippet = snippet.take(SNIPPET_CHARS)
        )
        val merged = listOf(updated) + recents().filterNot { it.uri == uri }
        writeRecents(merged.take(MAX_RECENTS))
    }

    /** Lightweight heartbeat while reading; never reorders the list. */
    fun updatePosition(uri: String, scrollIndex: Int) {
        val current = recents()
        val target = current.firstOrNull { it.uri == uri } ?: return
        if (target.scrollIndex == scrollIndex) return
        writeRecents(current.map { if (it.uri == uri) it.copy(scrollIndex = scrollIndex) else it })
    }

    fun forget(uri: String) {
        writeRecents(recents().filterNot { it.uri == uri })
    }

    fun clearRecents() {
        preferences.edit { remove(KEY_RECENTS) }
    }

    // ------------------------------------------------------------- session ---

    fun session(): List<MarkdownDocument> {
        // Last known on-disk sizes, used to detect a body that was lost while
        // persisting (the pre-fix format dropped bodies over a size cap and
        // then restored them as empty "Saved" tabs — AUD-001).
        val recentCounts = recents().associate { it.uri to it.charCount }
        return DocumentMemoryCodec.decodeRecords(preferences.getString(KEY_SESSION, null))
            .mapNotNull(SessionDocument::fromRecord)
            .map { document ->
                val content = readSpilledBody(document.id) ?: document.content
                val expectedChars = document.uri?.let { recentCounts[it] } ?: 0
                val bodyLost = document.writable &&
                    document.uri != null &&
                    content.isEmpty() &&
                    expectedChars > 0
                MarkdownDocument(
                    id = document.id,
                    title = document.title,
                    content = content,
                    uri = document.uri?.let(android.net.Uri::parse),
                    // A tab whose body did not survive the restore is made
                    // read-only and flagged as an error, so the first edit can
                    // never autosave an empty buffer over the real file. The
                    // user can still Save As to a new file.
                    writable = if (bodyLost) false else document.writable,
                    saveState = when {
                        bodyLost -> SaveState.ERROR
                        document.uri == null -> SaveState.UNSAVED
                        document.writable -> SaveState.SAVED
                        else -> SaveState.READ_ONLY
                    }
                )
            }
    }

    fun activeSessionId(): String? =
        DocumentMemoryCodec.decodeRecords(preferences.getString(KEY_SESSION, null))
            .mapNotNull(SessionDocument::fromRecord)
            .firstOrNull { it.active }
            ?.id

    fun sessionScrollPositions(): Map<String, Int> =
        DocumentMemoryCodec.decodeRecords(preferences.getString(KEY_SESSION, null))
            .mapNotNull(SessionDocument::fromRecord)
            .associate { it.id to it.scrollIndex }

    /**
     * Persists the open tabs. Document bodies live in per-tab files under the
     * app's private storage, never inside SharedPreferences: bodies have no
     * size cap here, preferences stay small no matter how large the open
     * documents are, and a body is never silently dropped (AUD-001/AUD-006).
     * The preferences record keeps only tab metadata; its content field is a
     * fallback that is used only when the body file could not be written.
     */
    fun saveSession(documents: List<MarkdownDocument>, activeId: String, scrollPositions: Map<String, Int>) {
        val kept = documents.take(MAX_SESSION_TABS)
        val records = kept.map { document ->
            val spilled = writeSpilledBody(document.id, document.content)
            SessionDocument(
                id = document.id,
                title = document.title,
                uri = document.uri?.toString(),
                content = if (spilled) "" else document.content,
                writable = document.writable,
                scrollIndex = scrollPositions[document.id] ?: 0,
                active = document.id == activeId
            ).toRecord()
        }
        pruneSpilledBodies(kept.map { it.id }.toSet())
        preferences.edit {
            putString(KEY_SESSION, DocumentMemoryCodec.encodeRecords(records))
            putLong(KEY_SESSION_SAVED_AT, System.currentTimeMillis())
        }
    }

    fun clearSession() {
        preferences.edit {
            remove(KEY_SESSION)
            remove(KEY_SESSION_SAVED_AT)
        }
        runCatching { spillDirectory().deleteRecursively() }
    }

    // ----------------------------------------------------- session bodies --

    private fun spillDirectory(): File = File(context.filesDir, SESSION_BODY_DIRECTORY)

    private fun spillFile(id: String): File {
        val safeId = id.replace(UNSAFE_ID_CHARS, "_")
        return File(spillDirectory(), "$safeId.body")
    }

    /** Writes via a temp sibling + rename so a crash cannot leave a torn body. */
    private fun writeSpilledBody(id: String, content: String): Boolean = runCatching {
        val directory = spillDirectory().apply { mkdirs() }
        val target = spillFile(id)
        val temp = File(directory, target.name + ".tmp")
        temp.writeText(content, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        true
    }.getOrDefault(false)

    private fun readSpilledBody(id: String): String? {
        val file = spillFile(id)
        if (!file.exists()) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    private fun pruneSpilledBodies(keptIds: Set<String>) {
        val directory = spillDirectory()
        if (!directory.isDirectory) return
        val keptNames = keptIds.map { spillFile(it).name }.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.name !in keptNames) runCatching { file.delete() }
        }
    }

    // ------------------------------------------------------------ internals --

    private fun writeRecents(recents: List<RecentDocument>) {
        preferences.edit {
            putString(
                KEY_RECENTS,
                DocumentMemoryCodec.encodeRecords(recents.map(RecentDocument::toRecord))
            )
        }
    }

    private companion object {
        const val PREFERENCES = "marblemd_library"
        const val KEY_RECENTS = "recents"
        const val KEY_SESSION = "session"
        const val KEY_SESSION_SAVED_AT = "session_saved_at"

        const val MAX_RECENTS = 50
        const val MAX_SESSION_TABS = 12
        const val SNIPPET_CHARS = 180

        const val SESSION_BODY_DIRECTORY = "session_bodies"
        val UNSAFE_ID_CHARS = Regex("[^A-Za-z0-9._-]")
    }
}
