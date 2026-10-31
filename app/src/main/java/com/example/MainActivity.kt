package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NoteEntity
import com.example.ui.graph.GraphViewScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.TodoListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OmniNoteApp()
                }
            }
        }
    }
}

@Composable
fun OmniNoteApp(viewModel: NotesViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val allActiveNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchScope by viewModel.searchScope.collectAsStateWithLifecycle()
    val searchSuggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val pendingTasksCount by viewModel.pendingTasksCount.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val taskFilter by viewModel.taskFilter.collectAsStateWithLifecycle()
    val taskSearchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()
    val taskSelectedTag by viewModel.taskSelectedTag.collectAsStateWithLifecycle()
    val availableTaskTags by viewModel.availableTaskTags.collectAsStateWithLifecycle()
    val noteChecklists by viewModel.noteChecklists.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = currentScreen !is Screen.Home) {
        viewModel.navigateBack()
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Home, is Screen.Archive, is Screen.Trash -> {
                val displayNotes = when (screen) {
                    is Screen.Archive -> archivedNotes
                    is Screen.Trash -> trashedNotes
                    else -> filteredNotes
                }

                HomeScreen(
                    notes = displayNotes,
                    allActiveNotesCount = allActiveNotes.size,
                    archivedNotesCount = archivedNotes.size,
                    trashedNotesCount = trashedNotes.size,
                    pendingTasksCount = pendingTasksCount,
                    availableTags = availableTags,
                    selectedTag = selectedTag,
                    selectedColor = selectedColor,
                    searchQuery = searchQuery,
                    viewMode = viewMode,
                    currentScreen = screen,
                    onSelectNote = { noteId ->
                        viewModel.navigateTo(Screen.Editor(noteId = noteId))
                    },
                    onCreateNote = {
                        viewModel.navigateTo(Screen.Editor(noteId = null))
                    },
                    onCreateDailyNote = {
                        viewModel.openOrCreateDailyNote()
                    },
                    onTogglePin = { note ->
                        viewModel.togglePin(note)
                    },
                    onToggleArchive = { note ->
                        viewModel.toggleArchive(note)
                    },
                    onDeleteNote = { note ->
                        if (screen is Screen.Trash) {
                            viewModel.deletePermanently(note.id)
                        } else {
                            viewModel.moveToTrash(note)
                        }
                    },
                    onSearchChange = { query ->
                        viewModel.setSearchQuery(query)
                    },
                    searchScope = searchScope,
                    suggestions = searchSuggestions,
                    onScopeChange = { scope ->
                        viewModel.setSearchScope(scope)
                    },
                    onClearAllFilters = {
                        viewModel.clearAllFilters()
                    },
                    onSelectTag = { tag ->
                        viewModel.selectTag(tag)
                    },
                    onSelectColor = { color ->
                        viewModel.selectColor(color)
                    },
                    onToggleViewMode = {
                        viewModel.toggleViewMode()
                    },
                    onOpenGraph = {
                        viewModel.navigateTo(Screen.Graph())
                    },
                    onNavigateScreen = { targetScreen ->
                        viewModel.navigateTo(targetScreen)
                    },
                    onExportVault = {
                        viewModel.exportVaultJson()
                    },
                    onImportVault = { json ->
                        viewModel.importVaultJson(json)
                    },
                    currentUser = currentUser,
                    isSyncing = isSyncing,
                    lastSyncTime = lastSyncTime,
                    onSignInWithGoogle = { activity ->
                        viewModel.signInWithGoogle(activity) { errorMsg ->
                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    onSignOutGoogle = {
                        viewModel.signOutGoogle()
                    },
                    onSyncNow = {
                        viewModel.syncWithCloud { success, msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            is Screen.Editor -> {
                val note = remember(screen.noteId, allActiveNotes, archivedNotes, trashedNotes) {
                    if (screen.noteId != null) {
                        allActiveNotes.find { it.id == screen.noteId }
                            ?: archivedNotes.find { it.id == screen.noteId }
                            ?: trashedNotes.find { it.id == screen.noteId }
                    } else null
                }

                val backlinks = remember(note?.title, allActiveNotes) {
                    if (note != null && note.title.isNotBlank()) {
                        viewModel.getBacklinksFor(note.title)
                    } else emptyList()
                }

                NoteEditorScreen(
                    initialNote = note,
                    allNotes = allActiveNotes,
                    backlinks = backlinks,
                    onSaveNote = { noteToSave ->
                        viewModel.saveNote(noteToSave)
                    },
                    onDeleteNote = { noteToDelete ->
                        viewModel.moveToTrash(noteToDelete)
                    },
                    onTogglePin = { noteToToggle ->
                        viewModel.togglePin(noteToToggle)
                    },
                    onToggleArchive = { noteToToggle ->
                        viewModel.toggleArchive(noteToToggle)
                    },
                    onWikiLinkClick = { title ->
                        viewModel.handleWikiLinkClick(title)
                    },
                    onBack = {
                        viewModel.navigateBack()
                    },
                    onOpenInGraph = { noteId ->
                        viewModel.navigateTo(Screen.Graph(focusNoteId = noteId))
                    }
                )
            }

            is Screen.Graph -> {
                GraphViewScreen(
                    notes = allActiveNotes,
                    initialFocusNoteId = screen.focusNoteId,
                    onSelectNote = { noteId ->
                        viewModel.navigateTo(Screen.Editor(noteId = noteId))
                    },
                    onBack = {
                        viewModel.navigateBack()
                    }
                )
            }

            is Screen.TodoList -> {
                TodoListScreen(
                    tasks = filteredTasks,
                    allTasksCount = allTasks.size,
                    pendingTasksCount = pendingTasksCount,
                    currentFilter = taskFilter,
                    searchQuery = taskSearchQuery,
                    selectedTag = taskSelectedTag,
                    availableTags = availableTaskTags,
                    noteChecklists = noteChecklists,
                    allNotes = allActiveNotes,
                    onToggleTask = { task ->
                        viewModel.toggleTask(task)
                    },
                    onDeleteTask = { task ->
                        viewModel.deleteTask(task)
                    },
                    onClearCompleted = {
                        viewModel.clearCompletedTasks()
                    },
                    onAddTask = { title, priority, dueDate, tag, parentNoteId, parentNoteTitle ->
                        viewModel.addTask(title, priority, dueDate, tag, parentNoteId, parentNoteTitle)
                    },
                    onSetFilter = { filter ->
                        viewModel.setTaskFilter(filter)
                    },
                    onSearchChange = { query ->
                        viewModel.setTaskSearchQuery(query)
                    },
                    onSelectTag = { tag ->
                        viewModel.setTaskSelectedTag(tag)
                    },
                    onToggleNoteChecklist = { noteId, lineIndex ->
                        viewModel.toggleNoteChecklist(noteId, lineIndex)
                    },
                    onImportAllChecklists = {
                        viewModel.importAllNoteChecklists { count ->
                            android.widget.Toast.makeText(context, "Imported $count task(s) from notes", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenNote = { noteId ->
                        viewModel.navigateTo(Screen.Editor(noteId = noteId))
                    },
                    onOpenDrawer = {
                        viewModel.navigateTo(Screen.Home)
                    },
                    onBack = {
                        viewModel.navigateBack()
                    }
                )
            }
        }
    }
}
