package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3)
}

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["priority"]),
        Index(value = ["dueDateMillis"]),
        Index(value = ["parentNoteId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentNoteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateMillis: Long? = null,
    val tag: String = "",
    val parentNoteId: Long? = null,
    val parentNoteTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Represents a checklist line extracted directly from an existing Markdown note
 * e.g., "- [ ] Drink 2.5L water" in note "Daily Journal"
 */
data class NoteChecklistItem(
    val noteId: Long,
    val noteTitle: String,
    val noteEmoji: String,
    val lineIndex: Int,
    val isChecked: Boolean,
    val text: String
)
