package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for Markdown notes.
 *
 * Implements complete CRUD operations, reactive Flows, and dedicated queries
 * to fetch linked notes by title for Obsidian-style bidirectional linking (wikilinks).
 */
@Dao
interface NoteDao {

    // ==========================================
    // 1. Create (Insert) Operations
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    // ==========================================
    // 2. Read (Query) Operations
    // ==========================================

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title COLLATE NOCASE LIMIT 1")
    fun getNoteByTitleFlow(title: String): Flow<NoteEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM notes WHERE title = :title COLLATE NOCASE AND isTrashed = 0)")
    suspend fun doesNoteExistWithTitle(title: String): Boolean

    @Query("SELECT * FROM notes WHERE title IN (:titles) AND isTrashed = 0")
    suspend fun getNotesByTitles(titles: List<String>): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0")
    suspend fun getAllActiveNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForExport(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 0")
    fun getActiveNotesCount(): Flow<Int>

    /**
     * Search notes by title query for Obsidian [[ autocomplete picker
     */
    @Query("""
        SELECT * FROM notes
        WHERE isTrashed = 0
          AND title LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
        LIMIT :limit
    """)
    fun searchNotesByTitle(query: String, limit: Int = 15): Flow<List<NoteEntity>>

    /**
     * Full-text search across titles, markdown content, and tags
     */
    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0 AND isTrashed = 0
          AND (
              title LIKE '%' || :query || '%'
              OR content LIKE '%' || :query || '%'
              OR tags LIKE '%' || :query || '%'
          )
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchActiveNotes(query: String): Flow<List<NoteEntity>>

    // ==========================================
    // 3. Update Operations
    // ==========================================

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinStatus(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :isArchived, isPinned = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateArchiveStatus(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isTrashed = 1, isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isTrashed = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    // ==========================================
    // 4. Delete Operations
    // ==========================================

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // ==========================================
    // 5. Obsidian Bidirectional Linking Queries
    // ==========================================

    /**
     * Fetch all source notes that link TO a given note title (Incoming Backlinks).
     *
     * Example: If note "Daily Note" contains [[Project Roadmap 2026]],
     * calling getNotesLinkingToTitle("Project Roadmap 2026") returns "Daily Note".
     */
    @Query("""
        SELECT notes.*
        FROM notes
        INNER JOIN note_links ON notes.id = note_links.sourceNoteId
        WHERE note_links.targetTitle = :title COLLATE NOCASE
          AND notes.isTrashed = 0
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun getNotesLinkingToTitle(title: String): Flow<List<NoteEntity>>

    /**
     * Content-level fallback search for notes mentioning [[title]] directly in Markdown text.
     */
    @Query("""
        SELECT * FROM notes
        WHERE isTrashed = 0
          AND (
              content LIKE '%[[' || :title || ']]%'
              OR content LIKE '%[[' || :title || '|%'
          )
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getNotesMentioningTitle(title: String): Flow<List<NoteEntity>>

    /**
     * Fetch all target notes that are LINKED BY a given source note title (Outgoing Forward Links).
     *
     * Example: If note "Project Roadmap 2026" links to [[Meeting Notes]],
     * calling getNotesLinkedByTitle("Project Roadmap 2026") returns "Meeting Notes".
     */
    @Query("""
        SELECT targetNotes.*
        FROM notes AS targetNotes
        INNER JOIN note_links ON targetNotes.title = note_links.targetTitle COLLATE NOCASE
        INNER JOIN notes AS sourceNotes ON sourceNotes.id = note_links.sourceNoteId
        WHERE sourceNotes.title = :title COLLATE NOCASE
          AND targetNotes.isTrashed = 0
          AND sourceNotes.isTrashed = 0
        ORDER BY targetNotes.title ASC
    """)
    fun getNotesLinkedByTitle(title: String): Flow<List<NoteEntity>>

    /**
     * Fetch all raw link metadata entities where targetTitle matches
     */
    @Query("SELECT * FROM note_links WHERE targetTitle = :targetTitle COLLATE NOCASE")
    fun getIncomingBacklinksForTitle(targetTitle: String): Flow<List<NoteLinkEntity>>

    /**
     * Fetch all outgoing links originating from a specific source note ID
     */
    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId ORDER BY targetTitle ASC")
    fun getOutgoingLinksForNote(sourceNoteId: Long): Flow<List<NoteLinkEntity>>

    /**
     * 1-to-N Room relation returning a Note embedded with all its outgoing link records
     */
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteWithOutgoingLinks(noteId: Long): Flow<NoteWithLinks?>

    /**
     * Rich projection returning all notes that link to the given title,
     * including surrounding Markdown context snippets and source note emojis.
     */
    @Query("""
        SELECT notes.id AS sourceNoteId,
               notes.title AS sourceTitle,
               notes.emoji AS sourceEmoji,
               note_links.contextSnippet AS contextSnippet,
               notes.updatedAt AS updatedAt
        FROM note_links
        INNER JOIN notes ON notes.id = note_links.sourceNoteId
        WHERE note_links.targetTitle = :targetTitle COLLATE NOCASE
          AND notes.isTrashed = 0
        ORDER BY notes.updatedAt DESC
    """)
    fun getBacklinkItemsForTitle(targetTitle: String): Flow<List<BacklinkItem>>

    /**
     * Count incoming backlinks for a given note title
     */
    @Query("""
        SELECT COUNT(*)
        FROM note_links
        INNER JOIN notes ON notes.id = note_links.sourceNoteId
        WHERE note_links.targetTitle = :targetTitle COLLATE NOCASE
          AND notes.isTrashed = 0
    """)
    fun getBacklinkCountForTitle(targetTitle: String): Flow<Int>

    /**
     * Query graph edges where both source and target notes exist and are active
     */
    @Query("""
        SELECT note_links.sourceNoteId AS sourceId,
               targetNotes.id AS targetId,
               sourceNotes.title AS sourceTitle,
               targetNotes.title AS targetTitle
        FROM note_links
        INNER JOIN notes AS sourceNotes ON sourceNotes.id = note_links.sourceNoteId
        INNER JOIN notes AS targetNotes ON targetNotes.title = note_links.targetTitle COLLATE NOCASE
        WHERE sourceNotes.isTrashed = 0 AND targetNotes.isTrashed = 0
    """)
    fun getAllLinkGraphEdges(): Flow<List<LinkGraphEdge>>

    /**
     * Unresolved / "Ghost" links: targets referenced in notes that don't have a matching note yet
     */
    @Query("""
        SELECT DISTINCT note_links.targetTitle
        FROM note_links
        INNER JOIN notes ON notes.id = note_links.sourceNoteId
        WHERE notes.isTrashed = 0
          AND note_links.targetTitle NOT IN (
              SELECT title FROM notes WHERE isTrashed = 0
          )
        ORDER BY note_links.targetTitle ASC
    """)
    fun getUnresolvedLinkTitles(): Flow<List<String>>

    // ==========================================
    // 6. Link Table Management & Transactions
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NoteLinkEntity>)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteLinksForSource(sourceNoteId: Long)

    @Transaction
    suspend fun syncNoteLinks(noteId: Long, extractedLinks: List<ExtractedLink>) {
        deleteLinksForSource(noteId)
        if (extractedLinks.isNotEmpty()) {
            val linkEntities = extractedLinks.map { extracted ->
                val targetNote = getNoteByTitle(extracted.targetTitle)
                NoteLinkEntity(
                    sourceNoteId = noteId,
                    targetTitle = extracted.targetTitle,
                    targetNoteId = targetNote?.id,
                    alias = extracted.alias,
                    contextSnippet = extracted.contextSnippet
                )
            }
            insertLinks(linkEntities)
        }
    }

    @Transaction
    suspend fun upsertNoteWithExtractedLinks(note: NoteEntity): Long {
        val noteId = if (note.id == 0L) {
            insertNote(note)
        } else {
            updateNote(note)
            note.id
        }
        val extracted = note.extractLinksWithMetadata()
        syncNoteLinks(noteId, extracted)
        return noteId
    }
}
