package com.ck.typenote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val MAX_CHARS = 3000
private const val TRASH_DAYS = 30L

/**
 * One typed sheet. [struck] is a mask the same length as [text]:
 * '1' means that letter was crossed out with backspace (typewriter-style correction).
 */
data class Note(
    val id: Long,
    val text: String = "",
    val struck: String = "",
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null

    /** The text without crossed-out letters, used for search, titles and sharing. */
    fun cleanText(): String = buildString {
        text.forEachIndexed { i, c -> if (struck.getOrNull(i) != '1') append(c) }
    }

    val title: String
        get() = cleanText().lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(48)
            ?: "Untitled sheet"

    val preview: String
        get() = cleanText().replace('\n', ' ').trim().take(90)
}

/** Saves notes as a small JSON file in the app's private storage. */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    private val _notes = MutableStateFlow(load())
    val notes: StateFlow<List<Note>> = _notes

    fun get(id: Long): Note? = _notes.value.firstOrNull { it.id == id }

    fun create(): Note {
        val now = System.currentTimeMillis()
        val note = Note(id = now, created = now, updated = now)
        _notes.value = listOf(note) + _notes.value
        persist()
        return note
    }

    fun save(note: Note) {
        val stamped = note.copy(updated = System.currentTimeMillis())
        val list = _notes.value
        _notes.value = if (list.any { it.id == note.id }) {
            list.map { if (it.id == note.id) stamped else it }
        } else listOf(stamped) + list
        persist()
    }

    fun moveToTrash(id: Long) = update(id) { it.copy(deletedAt = System.currentTimeMillis()) }
    fun restore(id: Long) = update(id) { it.copy(deletedAt = null) }

    fun deleteForever(id: Long) {
        _notes.value = _notes.value.filterNot { it.id == id }
        persist()
    }

    fun emptyTrash() {
        _notes.value = _notes.value.filterNot { it.isDeleted }
        persist()
    }

    private fun update(id: Long, change: (Note) -> Note) {
        _notes.value = _notes.value.map { if (it.id == id) change(it) else it }
        persist()
    }

    private fun load(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val cutoff = System.currentTimeMillis() - TRASH_DAYS * 24 * 60 * 60 * 1000
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Note(
                    id = o.getLong("id"),
                    text = o.optString("text"),
                    struck = o.optString("struck"),
                    created = o.optLong("created"),
                    updated = o.optLong("updated"),
                    deletedAt = if (o.has("deletedAt")) o.getLong("deletedAt") else null,
                )
            }.filterNot { it.deletedAt != null && it.deletedAt < cutoff }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun persist() {
        val arr = JSONArray()
        _notes.value.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("text", n.text)
                put("struck", n.struck)
                put("created", n.created)
                put("updated", n.updated)
                n.deletedAt?.let { put("deletedAt", it) }
            })
        }
        val tmp = File(file.parentFile, "notes.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(arr.toString())
            tmp.delete()
        }
    }
}
