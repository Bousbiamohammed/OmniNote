package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.AppDatabase
import com.example.data.NoteChecklistItem
import com.example.data.NoteEntity
import com.example.data.NoteRepository
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import com.example.data.sync.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SearchScope(val label: String, val iconEmoji: String) {
    ALL("All", "🔍"),
    TITLE_ONLY("Titles", "🔤"),
    CHECKLISTS("Checklists", "☑️"),
    WIKILINKS("WikiLinks", "🔗"),
    CODE_BLOCKS("Code", "💻")
}

data class SearchSuggestion(
    val noteId: Long,
    val title: String,
    val emoji: String,
    val matchedSnippet: String
)

enum class ViewMode {
    STAGGERED_GRID,
    LIST
}

enum class TaskFilter(val label: String, val iconEmoji: String) {
    ALL("All", "📋"),
    PENDING("Pending", "⏳"),
    COMPLETED("Completed", "✅"),
    HIGH_PRIORITY("High Priority", "🔥"),
    TODAY("Due Today", "📅")
}

sealed class Screen {
    object Home : Screen()
    data class Editor(val noteId: Long? = null, val initialTitle: String? = null) : Screen()
    data class Graph(val focusNoteId: Long? = null) : Screen()
    object TodoList : Screen()
    object Archive : Screen()
    object Trash : Screen()
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = NoteRepository(database.noteDao())
    val authManager = AuthManager(application)
    val firestoreSyncRepository = FirestoreSyncRepository(application)
    val taskRepository = TaskRepository(database.taskDao(), database.noteDao())

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        // Silent auto-sign-in on startup
        authManager.attemptAutoSignIn(viewModelScope)

        // Observe auth state and trigger auto-sync when user is active
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    syncWithCloud()
                }
            }
        }
    }

    val allActiveNotes: StateFlow<List<NoteEntity>> = repository.allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- TASK STATE & FLOWS ---
    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasksCount: StateFlow<Int> = taskRepository.pendingTasksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _taskFilter = MutableStateFlow(TaskFilter.ALL)
    val taskFilter: StateFlow<TaskFilter> = _taskFilter.asStateFlow()

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _taskSelectedTag = MutableStateFlow<String?>(null)
    val taskSelectedTag: StateFlow<String?> = _taskSelectedTag.asStateFlow()

    val availableTaskTags: StateFlow<List<String>> = allTasks.combine(_taskSearchQuery) { tasks, _ ->
        tasks.mapNotNull { it.tag.takeIf { t -> t.isNotBlank() } }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Markdown checklists extracted live from active notes
    val noteChecklists: StateFlow<List<NoteChecklistItem>> = allActiveNotes.combine(_taskSearchQuery) { notes, query ->
        val list = taskRepository.extractChecklistsFromNotes(notes)
        if (query.isBlank()) list
        else list.filter { it.text.contains(query, ignoreCase = true) || it.noteTitle.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _taskFilter,
        _taskSearchQuery,
        _taskSelectedTag
    ) { tasks, filter, query, tag ->
        val trimmed = query.trim()
        val calendar = Calendar.getInstance()
        val startOfToday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = startOfToday + 86400000L

        tasks.filter { task ->
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.HIGH_PRIORITY -> task.priority == TaskPriority.HIGH
                TaskFilter.TODAY -> task.dueDateMillis != null && task.dueDateMillis in startOfToday..endOfToday
            }

            val matchesQuery = if (trimmed.isBlank()) true else {
                task.title.contains(trimmed, ignoreCase = true) ||
                task.tag.contains(trimmed, ignoreCase = true) ||
                (task.parentNoteTitle != null && task.parentNoteTitle.contains(trimmed, ignoreCase = true))
            }

            val matchesTag = if (tag == null) true else task.tag.equals(tag, ignoreCase = true)

            matchesFilter && matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _selectedColor = MutableStateFlow<String?>(null)
    val selectedColor = _selectedColor.asStateFlow()

    private val _searchScope = MutableStateFlow(SearchScope.ALL)
    val searchScope = _searchScope.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.STAGGERED_GRID)
    val viewMode = _viewMode.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen = _currentScreen.asStateFlow()

    private val _screenStack = mutableListOf<Screen>()

    // Filtered active notes combining search, tag, color, and searchScope
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allActiveNotes,
        _searchQuery,
        _selectedTag,
        _selectedColor,
        _searchScope
    ) { notes, query, tag, color, scope ->
        val trimmedQuery = query.trim()
        notes.filter { note ->
            val matchesScope = when (scope) {
                SearchScope.ALL -> true
                SearchScope.TITLE_ONLY -> true
                SearchScope.CHECKLISTS -> note.content.contains("- [ ]") || note.content.contains("- [x]") || note.content.contains("- [X]")
                SearchScope.WIKILINKS -> note.extractWikiLinks().isNotEmpty()
                SearchScope.CODE_BLOCKS -> note.content.contains("```")
            }

            val matchesQuery = when {
                trimmedQuery.isBlank() -> true
                scope == SearchScope.TITLE_ONLY -> note.title.contains(trimmedQuery, ignoreCase = true)
                trimmedQuery.startsWith("#") -> {
                    val cleanTag = trimmedQuery.removePrefix("#")
                    note.getTagList().any { it.contains(cleanTag, ignoreCase = true) } ||
                    note.extractInlineTags().any { it.contains(cleanTag, ignoreCase = true) }
                }
                trimmedQuery.startsWith("[[") -> {
                    val cleanLink = trimmedQuery.removePrefix("[[").removeSuffix("]]")
                    note.extractWikiLinks().any { it.contains(cleanLink, ignoreCase = true) }
                }
                trimmedQuery.startsWith("todo:", ignoreCase = true) || trimmedQuery.startsWith("task:", ignoreCase = true) -> {
                    val taskQuery = trimmedQuery.substringAfter(":").trim()
                    (note.content.contains("- [ ]") || note.content.contains("- [x]")) &&
                    (taskQuery.isEmpty() || note.content.contains(taskQuery, ignoreCase = true))
                }
                trimmedQuery.startsWith("code:", ignoreCase = true) -> {
                    val codeQuery = trimmedQuery.substringAfter(":").trim()
                    note.content.contains("```") && (codeQuery.isEmpty() || note.content.contains(codeQuery, ignoreCase = true))
                }
                else -> {
                    note.title.contains(trimmedQuery, ignoreCase = true) ||
                    note.content.contains(trimmedQuery, ignoreCase = true) ||
                    note.tags.contains(trimmedQuery, ignoreCase = true) ||
                    note.extractWikiLinks().any { it.contains(trimmedQuery, ignoreCase = true) }
                }
            }

            val matchesTag = if (tag == null) true else {
                note.getTagList().any { it.equals(tag, ignoreCase = true) } ||
                note.extractInlineTags().any { it.equals(tag, ignoreCase = true) }
            }

            val matchesColor = if (color == null) true else {
                note.colorHex.equals(color, ignoreCase = true)
            }

            matchesScope && matchesQuery && matchesTag && matchesColor
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Instant search suggestions as user types
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = combine(
        allActiveNotes,
        _searchQuery
    ) { notes, query ->
        val trimmed = query.trim()
        if (trimmed.length < 2) emptyList()
        else {
            notes.filter { note ->
                note.title.contains(trimmed, ignoreCase = true) ||
                note.content.contains(trimmed, ignoreCase = true)
            }.take(5).map { note ->
                val snippet = if (note.title.contains(trimmed, ignoreCase = true)) {
                    "Title match: ${note.title}"
                } else {
                    val line = note.content.lines().find { it.contains(trimmed, ignoreCase = true) } ?: ""
                    if (line.length > 60) line.take(57) + "…" else line
                }
                SearchSuggestion(
                    noteId = note.id,
                    title = note.title,
                    emoji = note.emoji,
                    matchedSnippet = snippet
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available unique tags
    val availableTags: StateFlow<List<String>> = allActiveNotes.combine(_searchQuery) { notes, _ ->
        val tagsSet = mutableSetOf<String>()
        notes.forEach { note ->
            tagsSet.addAll(note.getTagList())
            tagsSet.addAll(note.extractInlineTags())
        }
        tagsSet.filter { it.isNotEmpty() }.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchScope(scope: SearchScope) {
        _searchScope.value = scope
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun selectColor(color: String?) {
        _selectedColor.value = if (_selectedColor.value == color) null else color
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedTag.value = null
        _selectedColor.value = null
        _searchScope.value = SearchScope.ALL
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.STAGGERED_GRID) ViewMode.LIST else ViewMode.STAGGERED_GRID
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenStack.isNotEmpty()) {
            _currentScreen.value = _screenStack.removeAt(_screenStack.size - 1)
            return true
        }
        if (_currentScreen.value !is Screen.Home) {
            _currentScreen.value = Screen.Home
            return true
        }
        return false
    }

    fun signInWithGoogle(activity: Activity, onError: (String) -> Unit) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {
                syncWithCloud()
            },
            onError = onError
        )
    }

    fun signOutGoogle() {
        authManager.signOut(viewModelScope)
    }

    fun syncWithCloud(onComplete: ((Boolean, String) -> Unit)? = null) {
        if (!firestoreSyncRepository.isUserSignedIn()) {
            onComplete?.invoke(false, "Please sign in with Google first")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                // 1. Upload local active notes to Firestore
                val allLocal = repository.getAllActiveNotesList()
                for (note in allLocal) {
                    firestoreSyncRepository.uploadNote(note)
                }

                // 2. Fetch remote notes to merge any missing notes into local Room SQLite
                val remoteResult = firestoreSyncRepository.getCloudNotes()
                if (remoteResult.isSuccess) {
                    val remoteNotes = remoteResult.getOrThrow()
                    for (remote in remoteNotes) {
                        val localExists = allLocal.any { it.title.equals(remote.title, ignoreCase = true) }
                        if (!localExists && remote.title.isNotBlank()) {
                            val newLocal = NoteEntity(
                                title = remote.title,
                                content = remote.content,
                                emoji = remote.emoji,
                                colorHex = remote.colorHex,
                                bannerColor = remote.bannerColor,
                                isPinned = remote.isPinned,
                                isArchived = remote.isArchived,
                                isTrashed = remote.isTrashed,
                                tags = remote.tags,
                                folder = remote.folder,
                                createdAt = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                updatedAt = remote.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                            )
                            repository.insert(newLocal)
                        }
                    }
                }

                _lastSyncTime.value = System.currentTimeMillis()
                _isSyncing.value = false
                onComplete?.invoke(true, "Synchronized with Google Firestore")
            } catch (e: Exception) {
                _isSyncing.value = false
                onComplete?.invoke(false, e.localizedMessage ?: "Sync error")
            }
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePin(note)
            if (firestoreSyncRepository.isUserSignedIn()) {
                val updated = repository.getNoteById(note.id)
                if (updated != null) firestoreSyncRepository.uploadNote(updated)
            }
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleArchive(note)
            if (firestoreSyncRepository.isUserSignedIn()) {
                val updated = repository.getNoteById(note.id)
                if (updated != null) firestoreSyncRepository.uploadNote(updated)
            }
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrash(note)
            if (firestoreSyncRepository.isUserSignedIn()) {
                val updated = repository.getNoteById(note.id)
                if (updated != null) firestoreSyncRepository.uploadNote(updated)
            }
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFromTrash(note)
            if (firestoreSyncRepository.isUserSignedIn()) {
                val updated = repository.getNoteById(note.id)
                if (updated != null) firestoreSyncRepository.uploadNote(updated)
            }
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePermanently(id)
            if (firestoreSyncRepository.isUserSignedIn()) {
                firestoreSyncRepository.deleteNote(id)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            val trashed = repository.trashedNotes
            repository.emptyTrash()
        }
    }

    fun toggleChecklistLine(note: NoteEntity, lineIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleChecklistLine(note, lineIndex)
            if (firestoreSyncRepository.isUserSignedIn()) {
                val updated = repository.getNoteById(note.id)
                if (updated != null) firestoreSyncRepository.uploadNote(updated)
            }
        }
    }

    fun getBacklinksFor(targetTitle: String): List<com.example.data.BacklinkItem> {
        val trimmed = targetTitle.trim().lowercase()
        return allActiveNotes.value.filter { note ->
            if (note.title.trim().lowercase() == trimmed) false
            else note.extractWikiLinks().any { it.trim().lowercase() == trimmed }
        }.map { source ->
            val snippet = source.extractLinksWithMetadata()
                .find { it.targetTitle.trim().lowercase() == trimmed }?.contextSnippet ?: ""
            com.example.data.BacklinkItem(
                sourceNoteId = source.id,
                sourceTitle = source.title,
                sourceEmoji = source.emoji,
                contextSnippet = snippet,
                updatedAt = source.updatedAt
            )
        }
    }

    fun handleWikiLinkClick(title: String) {
        val cleanTitle = title.trim()
        val existing = allActiveNotes.value.find { it.title.equals(cleanTitle, ignoreCase = true) }
        if (existing != null) {
            navigateTo(Screen.Editor(noteId = existing.id))
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val newNote = NoteEntity(
                    title = cleanTitle,
                    content = "# $cleanTitle\n\nConnected from [[${allActiveNotes.value.firstOrNull()?.title ?: "Vault"}]]\n\n- [ ] Initial task",
                    emoji = "📝",
                    colorHex = "#202124"
                )
                val id = repository.insert(newNote)
                if (firestoreSyncRepository.isUserSignedIn()) {
                    val saved = repository.getNoteById(id)
                    if (saved != null) firestoreSyncRepository.uploadNote(saved)
                }
                viewModelScope.launch(Dispatchers.Main) {
                    navigateTo(Screen.Editor(noteId = id))
                }
            }
        }
    }

    fun openOrCreateDailyNote() {
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        val dailyTitle = "Daily Note - $dateStr"
        val existing = allActiveNotes.value.find { it.title.equals(dailyTitle, ignoreCase = true) }
        if (existing != null) {
            navigateTo(Screen.Editor(noteId = existing.id))
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val dailyNote = NoteEntity(
                    title = dailyTitle,
                    emoji = "📅",
                    colorHex = "#232938",
                    bannerColor = "#6366F1",
                    tags = "daily,journal",
                    folder = "Journal",
                    content = """
# $dailyTitle 📅

### Daily Priorities
- [ ] Primary milestone
- [ ] Secondary objective
- [ ] 30m reading / study

### Daily Log
> Reflect on key events, insights, and connected pages like [[Welcome to OmniNote]].
                    """.trimIndent()
                )
                val id = repository.insert(dailyNote)
                if (firestoreSyncRepository.isUserSignedIn()) {
                    val saved = repository.getNoteById(id)
                    if (saved != null) firestoreSyncRepository.uploadNote(saved)
                }
                viewModelScope.launch(Dispatchers.Main) {
                    navigateTo(Screen.Editor(noteId = id))
                }
            }
        }
    }

    fun saveNote(note: NoteEntity, onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = if (note.id == 0L) {
                repository.insert(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            } else {
                repository.update(note.copy(updatedAt = System.currentTimeMillis()))
                note.id
            }
            if (firestoreSyncRepository.isUserSignedIn()) {
                val saved = repository.getNoteById(id)
                if (saved != null) {
                    firestoreSyncRepository.uploadNote(saved)
                }
            }
            onSaved?.invoke(id)
        }
    }

    fun exportVaultJson(): String {
        return runBlocking(Dispatchers.IO) {
            repository.exportVaultAsJson()
        }
    }

    fun importVaultJson(json: String): Int {
        val count = runBlocking(Dispatchers.IO) {
            repository.importVaultFromJson(json)
        }
        if (firestoreSyncRepository.isUserSignedIn()) {
            syncWithCloud()
        }
        return count
    }

    // --- TASK ACTION METHODS ---

    fun setTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun setTaskSelectedTag(tag: String?) {
        _taskSelectedTag.value = if (_taskSelectedTag.value == tag) null else tag
    }

    fun addTask(
        title: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDateMillis: Long? = null,
        tag: String = "",
        parentNoteId: Long? = null,
        parentNoteTitle: String? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.createTask(
                title = title,
                priority = priority,
                dueDateMillis = dueDateMillis,
                tag = tag,
                parentNoteId = parentNoteId,
                parentNoteTitle = parentNoteTitle
            )
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.toggleTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTask(task)
        }
    }

    fun deleteTaskById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTaskById(id)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.clearCompletedTasks()
        }
    }

    fun importAllNoteChecklists(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val checklists = taskRepository.extractChecklistsFromNotes(allActiveNotes.value)
            val addedCount = taskRepository.importNoteChecklists(checklists)
            onComplete?.invoke(addedCount)
        }
    }

    fun toggleNoteChecklist(noteId: Long, lineIndex: Int) {
        val note = allActiveNotes.value.find { it.id == noteId } ?: return
        toggleChecklistLine(note, lineIndex)
    }
}
