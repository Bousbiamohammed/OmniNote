package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    val pendingTasksCount: Flow<Int> = taskDao.getPendingTasksCount()
    val totalTasksCount: Flow<Int> = taskDao.getTotalTasksCount()

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun getAllTasksList(): List<TaskEntity> = taskDao.getAllTasksList()

    suspend fun createTask(
        title: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDateMillis: Long? = null,
        tag: String = "",
        parentNoteId: Long? = null,
        parentNoteTitle: String? = null
    ): Long {
        val task = TaskEntity(
            title = title.trim(),
            isCompleted = false,
            priority = priority,
            dueDateMillis = dueDateMillis,
            tag = tag.trim().removePrefix("#"),
            parentNoteId = parentNoteId,
            parentNoteTitle = parentNoteTitle?.trim(),
            createdAt = System.currentTimeMillis()
        )
        return taskDao.insertTask(task)
    }

    suspend fun toggleTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val newCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = newCompleted,
            completedAt = if (newCompleted) now else null
        )
        taskDao.updateTask(updated)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun clearCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }

    /**
     * Scans notes for markdown checklist items e.g., "- [ ] task" or "- [x] task"
     */
    fun extractChecklistsFromNotes(notes: List<NoteEntity>): List<NoteChecklistItem> {
        val items = mutableListOf<NoteChecklistItem>()
        val taskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])\]\s+(.*)$""")

        for (note in notes) {
            val lines = note.content.lines()
            for ((index, line) in lines.withIndex()) {
                val match = taskRegex.find(line)
                if (match != null) {
                    val isChecked = match.groupValues[2].equals("x", ignoreCase = true)
                    val text = match.groupValues[3].trim()
                    if (text.isNotBlank()) {
                        items.add(
                            NoteChecklistItem(
                                noteId = note.id,
                                noteTitle = note.title,
                                noteEmoji = note.emoji,
                                lineIndex = index,
                                isChecked = isChecked,
                                text = text
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    /**
     * Imports extracted markdown checklists from notes into dedicated TaskEntity entries
     */
    suspend fun importNoteChecklists(checklists: List<NoteChecklistItem>): Int {
        val existingTasks = taskDao.getAllTasksList()
        var addedCount = 0

        val tasksToInsert = checklists.mapNotNull { item ->
            // Avoid duplicate creation if task already exists with same title & parentNoteId
            val alreadyExists = existingTasks.any {
                it.title.equals(item.text, ignoreCase = true) && it.parentNoteId == item.noteId
            }
            if (!alreadyExists) {
                addedCount++
                TaskEntity(
                    title = item.text,
                    isCompleted = item.isChecked,
                    priority = TaskPriority.MEDIUM,
                    parentNoteId = item.noteId,
                    parentNoteTitle = item.noteTitle,
                    tag = "from-notes",
                    createdAt = System.currentTimeMillis(),
                    completedAt = if (item.isChecked) System.currentTimeMillis() else null
                )
            } else null
        }

        if (tasksToInsert.isNotEmpty()) {
            taskDao.insertTasks(tasksToInsert)
        }
        return addedCount
    }
}
