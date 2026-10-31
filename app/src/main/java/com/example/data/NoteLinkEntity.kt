package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an outgoing/bidirectional link from a Markdown note.
 *
 * Foreign Key ensures cascading deletion if the source note is removed.
 * Indices optimize lookups for outgoing links (by sourceNoteId), incoming backlinks
 * (by targetTitle), and uniqueness constraint per (source, target) pair.
 */
@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["targetTitle"]),
        Index(value = ["sourceNoteId", "targetTitle"], unique = true)
    ]
)
data class NoteLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val linkId: Long = 0,

    /** The ID of the note containing the link */
    val sourceNoteId: Long,

    /** The raw target note title specified in [[targetTitle]] (trimmed, case-insensitive lookup) */
    val targetTitle: String,

    /**
     * Optional target note ID if target note is already resolved/exists in the database.
     * Stays null for unresolved/ghost notes (Obsidian style).
     */
    val targetNoteId: Long? = null,

    /** Optional display alias when using Markdown syntax [[Target|Display Alias]] */
    val alias: String? = null,

    /** Surrounding sentence or paragraph snippet around the link for backlink preview */
    val contextSnippet: String = "",

    /** Timestamp when the link was indexed */
    val createdAt: Long = System.currentTimeMillis()
)
