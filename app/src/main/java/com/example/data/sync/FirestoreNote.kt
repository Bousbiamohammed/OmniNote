package com.example.data.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val emoji: String = "📝",
    val colorHex: String = "#202124",
    val bannerColor: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val tags: String = "",
    val folder: String = "Vault",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title,
            "content" to content,
            "emoji" to emoji,
            "colorHex" to colorHex,
            "bannerColor" to bannerColor,
            "isPinned" to isPinned,
            "isArchived" to isArchived,
            "isTrashed" to isTrashed,
            "tags" to tags,
            "folder" to folder
        )
        return map.filterValues { it != null }
    }
}
