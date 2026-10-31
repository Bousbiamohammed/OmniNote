package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NoteEntity::class, NoteLinkEntity::class, TaskEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omninote_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.noteDao(), database.taskDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: NoteDao, taskDao: TaskDao) {
            val now = System.currentTimeMillis()

            val welcomeNote = NoteEntity(
                title = "Welcome to OmniNote 🚀",
                emoji = "🚀",
                colorHex = "#2E384D", // Notion indigo / slate
                bannerColor = "#3B82F6",
                isPinned = true,
                tags = "guide,welcome,features",
                folder = "Getting Started",
                content = """
# Welcome to OmniNote

OmniNote combines the best superpowers of **Google Keep**, **Notion**, and **Obsidian** into a single, offline-first personal workspace.

> 💡 **Core Philosophy:** All your notes are saved locally in an offline SQLite database. Your thoughts stay on your device with zero cloud latency.

---

### 🎨 Google Keep Features
- [x] Color-coded card backgrounds
- [x] Quick-capture checklists & pinned notes
- [ ] Try unchecking or checking this task right here!
- [x] Fast instant search & tag filter

### 🧱 Notion Features
- **Page Icons & Covers:** Give every note an expressive emoji & banner
- **Callout Blocks:** Highlight insights like this box
- **Structured Blocks:** Headers, quotes, code snippets, and tables

### 🔮 Obsidian Features
- **WikiLinks:** Bidirectional internal links using `[[Note Title]]` syntax
- Check out our connected pages:
  - [[Project Roadmap 2026]]
  - [[Idea Scratchpad]]
  - [[Daily Journal]]
- **Interactive Graph View:** Tap the Graph tab in the top bar to visualize note connections!
- **Markdown Native:** Instant preview or raw markdown editing
                """.trimIndent(),
                createdAt = now,
                updatedAt = now
            )

            val roadmapNote = NoteEntity(
                title = "Project Roadmap 2026",
                emoji = "🎯",
                colorHex = "#1E3A34", // Keep Forest Green
                bannerColor = "#10B981",
                isPinned = true,
                tags = "work,planning,dev",
                folder = "Work",
                content = """
# Project Roadmap 2026 🎯

Strategic milestones for the next iteration. Connected to [[Welcome to OmniNote]] and [[Meeting Notes]].

### 📌 Quarter 1 Goals
- [x] Complete offline Room synchronization architecture
- [x] Build live Markdown AST renderer with interactive checkboxes
- [x] Implement Obsidian bidirectional [[WikiLinks]] graph parser
- [ ] Add custom themes and vault archive export

### 📊 Sprint Planning Table
| Feature | Status | Priority |
| :--- | :--- | :--- |
| Interactive Graph | ✅ Done | High |
| Checklist Sync | ✅ Done | High |
| Notion Callouts | ✅ Done | Medium |
| Local Backups | ⏳ In Progress | High |

> 💬 Reference idea backlog in [[Idea Scratchpad]].
                """.trimIndent(),
                createdAt = now - 3600000,
                updatedAt = now - 3600000
            )

            val scratchpadNote = NoteEntity(
                title = "Idea Scratchpad",
                emoji = "💡",
                colorHex = "#3D2B1F", // Keep Amber/Clay
                bannerColor = "#F59E0B",
                isPinned = false,
                tags = "ideas,brainstorm",
                folder = "Ideas",
                content = """
# Creative Ideas & Brainstorm 💡

A scratchpad for emerging thoughts, code snippets, and linked concepts.

```kotlin
// Live AST parsing in OmniNote
val note = NoteEntity(title = "MindVault", isPinned = true)
val backlinks = note.extractWikiLinks()
```

### 🧠 Concepts to Explore
- Graph clustering using Louvain algorithm
- Flashcard spaced repetition from markdown `#flashcard` tags
- See how this connects back to [[Welcome to OmniNote]] and [[Project Roadmap 2026]]!
                """.trimIndent(),
                createdAt = now - 7200000,
                updatedAt = now - 7200000
            )

            val meetingNote = NoteEntity(
                title = "Meeting Notes",
                emoji = "🤝",
                colorHex = "#372948", // Keep Lavender/Purple
                bannerColor = "#8B5CF6",
                isPinned = false,
                tags = "work,team",
                folder = "Work",
                content = """
# Architecture Sync Meeting 🤝

> 📅 Date: October 31, 2026
> 👥 Attendees: Core Engineering & Product

### Key Discussion Points
1. **Offline Reliability:** Verified that Room SQLite handles all local operations without network dependencies.
2. **Backlink Indexing:** Outbound references in [[Project Roadmap 2026]] are tracked in real time.
3. **Action Items:**
   - [x] Verify touch targets meet 48dp accessibility standards
   - [x] Add tag chips filter to home grid
   - [ ] Schedule Q2 review in [[Daily Journal]]
                """.trimIndent(),
                createdAt = now - 10800000,
                updatedAt = now - 10800000
            )

            val dailyNote = NoteEntity(
                title = "Daily Journal",
                emoji = "📅",
                colorHex = "#232938", // Keep Slate Blue
                bannerColor = "#6366F1",
                isPinned = false,
                tags = "daily,journal,habits",
                folder = "Journal",
                content = """
# Daily Note 📅

Daily reflection and productivity tracker.

### Habits Checklist
- [x] Morning meditation & reading (20m)
- [x] Review [[Project Roadmap 2026]]
- [ ] Drink 2.5L water
- [ ] 30m brisk walking / workout

### Today's Focus
> "Small disciplines repeated with consistency every day lead to great achievements gained slowly over time."

Remember to capture new sparks in [[Idea Scratchpad]].
                """.trimIndent(),
                createdAt = now - 14400000,
                updatedAt = now - 14400000
            )

            val insertedWelcomeId = dao.upsertNoteWithExtractedLinks(welcomeNote)
            val insertedRoadmapId = dao.upsertNoteWithExtractedLinks(roadmapNote)
            val insertedScratchpadId = dao.upsertNoteWithExtractedLinks(scratchpadNote)
            val insertedMeetingId = dao.upsertNoteWithExtractedLinks(meetingNote)
            val insertedDailyId = dao.upsertNoteWithExtractedLinks(dailyNote)

            val seedTasks = listOf(
                TaskEntity(
                    title = "Try checking and unchecking tasks in OmniNote",
                    isCompleted = false,
                    priority = TaskPriority.HIGH,
                    dueDateMillis = now,
                    tag = "welcome",
                    parentNoteId = insertedWelcomeId,
                    parentNoteTitle = "Welcome to OmniNote"
                ),
                TaskEntity(
                    title = "Review milestones in [[Project Roadmap 2026]]",
                    isCompleted = false,
                    priority = TaskPriority.HIGH,
                    dueDateMillis = now + 86400000L,
                    tag = "planning",
                    parentNoteId = insertedRoadmapId,
                    parentNoteTitle = "Project Roadmap 2026"
                ),
                TaskEntity(
                    title = "Explore interactive Obsidian graph view physics",
                    isCompleted = false,
                    priority = TaskPriority.MEDIUM,
                    dueDateMillis = null,
                    tag = "dev",
                    parentNoteId = insertedScratchpadId,
                    parentNoteTitle = "Idea Scratchpad"
                ),
                TaskEntity(
                    title = "Drink 2.5L water & track daily habits",
                    isCompleted = false,
                    priority = TaskPriority.LOW,
                    dueDateMillis = now,
                    tag = "habits",
                    parentNoteId = insertedDailyId,
                    parentNoteTitle = "Daily Journal"
                ),
                TaskEntity(
                    title = "Morning reflection & architecture review",
                    isCompleted = true,
                    priority = TaskPriority.MEDIUM,
                    dueDateMillis = now - 3600000L,
                    tag = "habits",
                    parentNoteId = insertedDailyId,
                    parentNoteTitle = "Daily Journal",
                    completedAt = now - 1800000L
                )
            )
            taskDao.insertTasks(seedTasks)
        }
    }
}
