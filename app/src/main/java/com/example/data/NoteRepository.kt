package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(private val noteDao: NoteDao) {
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val activeNotesCount: Flow<Int> = noteDao.getActiveNotesCount()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun getNoteByTitle(title: String): NoteEntity? = noteDao.getNoteByTitle(title)

    suspend fun getAllActiveNotesList(): List<NoteEntity> = noteDao.getAllActiveNotesList()

    suspend fun insert(note: NoteEntity): Long = noteDao.upsertNoteWithExtractedLinks(note)

    suspend fun update(note: NoteEntity) {
        noteDao.upsertNoteWithExtractedLinks(note)
    }

    fun getBacklinkItems(targetTitle: String): Flow<List<BacklinkItem>> =
        noteDao.getBacklinkItemsForTitle(targetTitle)

    fun getNotesLinkingToTitle(title: String): Flow<List<NoteEntity>> =
        noteDao.getNotesLinkingToTitle(title)

    fun getNotesLinkedByTitle(title: String): Flow<List<NoteEntity>> =
        noteDao.getNotesLinkedByTitle(title)

    fun getNotesMentioningTitle(title: String): Flow<List<NoteEntity>> =
        noteDao.getNotesMentioningTitle(title)

    fun searchNotesByTitle(query: String, limit: Int = 15): Flow<List<NoteEntity>> =
        noteDao.searchNotesByTitle(query, limit)

    fun getOutgoingLinks(sourceNoteId: Long): Flow<List<NoteLinkEntity>> =
        noteDao.getOutgoingLinksForNote(sourceNoteId)

    fun getNoteWithOutgoingLinks(noteId: Long): Flow<NoteWithLinks?> =
        noteDao.getNoteWithOutgoingLinks(noteId)

    val linkGraphEdges: Flow<List<LinkGraphEdge>> = noteDao.getAllLinkGraphEdges()

    val unresolvedLinkTitles: Flow<List<String>> = noteDao.getUnresolvedLinkTitles()

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrash(note: NoteEntity) {
        noteDao.updateNote(note.copy(isTrashed = true, isArchived = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        noteDao.updateNote(note.copy(isTrashed = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchive(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isArchived = !note.isArchived,
                isPinned = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePermanently(id: Long) = noteDao.deletePermanently(id)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    /**
     * Toggles a checkbox markdown task item at a specific line in note content
     * e.g. "- [ ] Task" <-> "- [x] Task"
     */
    suspend fun toggleChecklistLine(note: NoteEntity, lineIndex: Int) {
        val lines = note.content.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val uncheckedRegex = Regex("""^(\s*[-*+]\s+)\[\s\]\s+(.*)$""")
            val checkedRegex = Regex("""^(\s*[-*+]\s+)\[[xX]\]\s+(.*)$""")

            val newLine = when {
                uncheckedRegex.matches(line) -> {
                    val match = uncheckedRegex.find(line)!!
                    "${match.groupValues[1]}[x] ${match.groupValues[2]}"
                }
                checkedRegex.matches(line) -> {
                    val match = checkedRegex.find(line)!!
                    "${match.groupValues[1]}[ ] ${match.groupValues[2]}"
                }
                else -> line
            }
            lines[lineIndex] = newLine
            val updatedContent = lines.joinToString("\n")
            noteDao.upsertNoteWithExtractedLinks(note.copy(content = updatedContent, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Exports entire offline vault to a JSON string
     */
    suspend fun exportVaultAsJson(): String {
        val notes = noteDao.getAllNotesForExport()
        val array = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("emoji", n.emoji)
            obj.put("colorHex", n.colorHex)
            obj.put("bannerColor", n.bannerColor ?: "")
            obj.put("isPinned", n.isPinned)
            obj.put("isArchived", n.isArchived)
            obj.put("isTrashed", n.isTrashed)
            obj.put("tags", n.tags)
            obj.put("folder", n.folder)
            obj.put("createdAt", n.createdAt)
            obj.put("updatedAt", n.updatedAt)
            array.put(obj)
        }
        val wrapper = JSONObject()
        wrapper.put("vaultName", "OmniNote Vault")
        wrapper.put("version", 1)
        wrapper.put("exportedAt", System.currentTimeMillis())
        wrapper.put("notes", array)
        return wrapper.toString(2)
    }

    /**
     * Imports notes from a JSON string into Room
     */
    suspend fun importVaultFromJson(jsonString: String): Int {
        val wrapper = JSONObject(jsonString)
        val array = wrapper.getJSONArray("notes")
        val imported = mutableListOf<NoteEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val note = NoteEntity(
                title = obj.optString("title", "Untitled"),
                content = obj.optString("content", ""),
                emoji = obj.optString("emoji", "📝"),
                colorHex = obj.optString("colorHex", "#202124"),
                bannerColor = obj.optString("bannerColor").ifEmpty { null },
                isPinned = obj.optBoolean("isPinned", false),
                isArchived = obj.optBoolean("isArchived", false),
                isTrashed = obj.optBoolean("isTrashed", false),
                tags = obj.optString("tags", ""),
                folder = obj.optString("folder", "Vault"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = System.currentTimeMillis()
            )
            imported.add(note)
        }
        noteDao.insertAll(imported)
        return imported.size
    }
}
