package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["title"]),
        Index(value = ["isPinned"]),
        Index(value = ["isArchived", "isTrashed"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val emoji: String = "📝",
    val colorHex: String = "#202124", // Default dark slate or Keep color
    val bannerColor: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val tags: String = "", // Comma-separated: "work,ideas,dev"
    val folder: String = "Vault",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getTagList(): List<String> {
        return tags.split(",")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
    }

    /**
     * Extracts all raw target titles from [[Note Title]] or [[Note Title|Alias]] wikilinks
     */
    fun extractWikiLinks(): List<String> {
        val regex = Regex("""\[\[(.*?)(?:\|(.*?))?\]\]""")
        return regex.findAll(content)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    /**
     * Extracts links with surrounding sentence/line context snippet and optional alias
     */
    fun extractLinksWithMetadata(): List<ExtractedLink> {
        val lines = content.lines()
        val regex = Regex("""\[\[(.*?)(?:\|(.*?))?\]\]""")
        val results = mutableListOf<ExtractedLink>()
        val seen = mutableSetOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            for (match in regex.findAll(trimmedLine)) {
                val target = match.groupValues[1].trim()
                val alias = match.groupValues[2].trim().ifEmpty { null }

                if (target.isNotEmpty() && seen.add(target.lowercase())) {
                    val snippet = if (trimmedLine.length > 140) {
                        trimmedLine.take(137) + "..."
                    } else {
                        trimmedLine
                    }
                    results.add(
                        ExtractedLink(
                            targetTitle = target,
                            alias = alias,
                            contextSnippet = snippet
                        )
                    )
                }
            }
        }
        return results
    }

    /**
     * Extracts hashtags like #idea, #todo
     */
    fun extractInlineTags(): List<String> {
        val regex = Regex("""#([a-zA-Z0-9_-]+)""")
        return regex.findAll(content).map { it.groupValues[1].trim() }.distinct().toList()
    }
}
