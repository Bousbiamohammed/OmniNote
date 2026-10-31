package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-N Room relation representing a note with its extracted outgoing links.
 */
data class NoteWithLinks(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "sourceNoteId"
    )
    val outgoingLinks: List<NoteLinkEntity> = emptyList()
)

/**
 * Projection representing an incoming backlink with surrounding Markdown context snippet
 * and source note metadata.
 */
data class BacklinkItem(
    val sourceNoteId: Long,
    val sourceTitle: String,
    val sourceEmoji: String,
    val contextSnippet: String,
    val updatedAt: Long
)

/**
 * Projection representing an edge between two resolved notes in the knowledge graph.
 */
data class LinkGraphEdge(
    val sourceId: Long,
    val targetId: Long,
    val sourceTitle: String,
    val targetTitle: String
)

/**
 * Intermediate data class extracted from Markdown content.
 */
data class ExtractedLink(
    val targetTitle: String,
    val alias: String? = null,
    val contextSnippet: String = ""
)
