package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteChecklistItem
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.ui.viewmodel.TaskFilter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    tasks: List<TaskEntity>,
    allTasksCount: Int,
    pendingTasksCount: Int,
    currentFilter: TaskFilter,
    searchQuery: String,
    selectedTag: String?,
    availableTags: List<String>,
    noteChecklists: List<NoteChecklistItem>,
    allNotes: List<NoteEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onClearCompleted: () -> Unit,
    onAddTask: (title: String, priority: TaskPriority, dueDate: Long?, tag: String, parentNoteId: Long?, parentNoteTitle: String?) -> Unit,
    onSetFilter: (TaskFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTag: (String?) -> Unit,
    onToggleNoteChecklist: (noteId: Long, lineIndex: Int) -> Unit,
    onImportAllChecklists: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val completedCount = allTasksCount - pendingTasksCount
    val completionPercentage = if (allTasksCount > 0) (completedCount.toFloat() / allTasksCount.toFloat()) else 0f

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("todo_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "To-Do List",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (pendingTasksCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.testTag("pending_count_badge")
                            ) {
                                Text(
                                    text = "$pendingTasksCount pending",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("todo_menu_drawer")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                },
                actions = {
                    // Import checklists from notes
                    IconButton(
                        onClick = onImportAllChecklists,
                        modifier = Modifier.testTag("import_checklists_button")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Import Checklists From Notes",
                            tint = Color(0xFF6366F1)
                        )
                    }

                    // Clear completed tasks
                    if (completedCount > 0) {
                        IconButton(
                            onClick = onClearCompleted,
                            modifier = Modifier.testTag("clear_completed_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear Completed Tasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Tabs: Vault Tasks vs Note Checklists
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vault Tasks ($allTasksCount)", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_vault_tasks")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Notes (${noteChecklists.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_note_checklists")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (selectedTab == 0) {
                    // Vault Tasks View
                    VaultTasksContent(
                        tasks = tasks,
                        allTasksCount = allTasksCount,
                        completedCount = completedCount,
                        completionPercentage = completionPercentage,
                        currentFilter = currentFilter,
                        searchQuery = searchQuery,
                        selectedTag = selectedTag,
                        availableTags = availableTags,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onSetFilter = onSetFilter,
                        onSearchChange = onSearchChange,
                        onSelectTag = onSelectTag,
                        onOpenNote = onOpenNote,
                        onAddNewTask = { showAddSheet = true }
                    )
                } else {
                    // Markdown Note Checklists Aggregated View
                    NoteChecklistsContent(
                        checklists = noteChecklists,
                        onToggleLine = onToggleNoteChecklist,
                        onOpenNote = onOpenNote,
                        onImportAll = onImportAllChecklists
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to create new task
    if (showAddSheet) {
        AddTaskBottomSheet(
            sheetState = sheetState,
            allNotes = allNotes,
            onDismiss = { showAddSheet = false },
            onSave = { title, priority, dueDate, tag, parentNoteId, parentNoteTitle ->
                onAddTask(title, priority, dueDate, tag, parentNoteId, parentNoteTitle)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showAddSheet = false
                }
            }
        )
    }
}

@Composable
private fun VaultTasksContent(
    tasks: List<TaskEntity>,
    allTasksCount: Int,
    completedCount: Int,
    completionPercentage: Float,
    currentFilter: TaskFilter,
    searchQuery: String,
    selectedTag: String?,
    availableTags: List<String>,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onSetFilter: (TaskFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTag: (String?) -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddNewTask: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_tasks_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stats & Progress Header Card
        item(key = "progress_card") {
            ProgressStatsCard(
                allTasksCount = allTasksCount,
                completedCount = completedCount,
                completionPercentage = completionPercentage
            )
        }

        // Search Input
        item(key = "search_box") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search tasks, tags, or [[linked notes]]...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // Filter Chips Row
        item(key = "filter_chips") {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskFilter.values().forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { onSetFilter(filter) },
                            label = { Text("${filter.iconEmoji} ${filter.label}") },
                            leadingIcon = if (currentFilter == filter) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }

                // Tag Filter Chips (if tags exist)
                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val isSelected = selectedTag.equals(tag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTag(tag) }
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tasks List
        if (tasks.isEmpty()) {
            item(key = "empty_state") {
                TaskEmptyState(
                    searchQuery = searchQuery,
                    onAddNewTask = onAddNewTask
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onToggle = { onToggleTask(task) },
                    onDelete = { onDeleteTask(task) },
                    onOpenNote = onOpenNote
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ProgressStatsCard(
    allTasksCount: Int,
    completedCount: Int,
    completionPercentage: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Task Progress",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (allTasksCount == 0) "No tasks yet" else "$completedCount of $allTasksCount completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${(completionPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val animatedProgress by animateFloatAsState(targetValue = completionPercentage, label = "progress")
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            if (allTasksCount > 0 && completionPercentage >= 1.0f) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎉 Outstanding! All tasks completed!",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpenNote: (Long) -> Unit
) {
    val isCompleted = task.isCompleted
    val animatedAlpha by animateFloatAsState(targetValue = if (isCompleted) 0.6f else 1f, label = "alpha")

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with test tag
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            ) {
                // Task title with strikethrough if completed
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Badges row: Priority, Due Date, Tag, Linked Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Priority Pill
                    val (priorityBg, priorityFg) = when (task.priority) {
                        TaskPriority.HIGH -> Pair(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFEF4444))
                        TaskPriority.MEDIUM -> Pair(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFF59E0B))
                        TaskPriority.LOW -> Pair(Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF3B82F6))
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityBg
                    ) {
                        Text(
                            text = task.priority.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityFg
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due date badge
                    if (task.dueDateMillis != null) {
                        val isOverdue = !isCompleted && task.dueDateMillis < System.currentTimeMillis() - 86400000L
                        val dateText = formatDueDate(task.dueDateMillis)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Tag badge
                    if (task.tag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "#${task.tag}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Obsidian WikiLink to Parent Note
                    if (task.parentNoteTitle != null && task.parentNoteId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onOpenNote(task.parentNoteId) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = Color(0xFF818CF8)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "[[${task.parentNoteTitle}]]",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF818CF8)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_task_${task.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun NoteChecklistsContent(
    checklists: List<NoteChecklistItem>,
    onToggleLine: (noteId: Long, lineIndex: Int) -> Unit,
    onOpenNote: (Long) -> Unit,
    onImportAll: () -> Unit
) {
    if (checklists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📝", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Checklist Items Found in Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add checklist lines like '- [ ] My task' to your markdown notes, and they will automatically show up here!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        // Group checklists by note
        val grouped = remember(checklists) { checklists.groupBy { it.noteId } }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("note_checklists_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "import_banner") {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Synced Note Checklists",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Toggling these checkboxes updates your markdown notes instantly in offline SQLite.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onImportAll,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("import_all_note_tasks_button")
                        ) {
                            Text("Import", fontSize = 12.sp)
                        }
                    }
                }
            }

            grouped.forEach { (noteId, items) ->
                item(key = "note_group_$noteId") {
                    val first = items.first()
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Note Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenNote(noteId) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = first.noteEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = first.noteTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open Note",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Checklist lines
                            items.forEach { checkItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkItem.isChecked,
                                        onCheckedChange = { onToggleLine(noteId, checkItem.lineIndex) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = checkItem.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (checkItem.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (checkItem.isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "note_bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun TaskEmptyState(
    searchQuery: String,
    onAddNewTask: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✅", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No matching tasks" else "No tasks in this filter",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Create a task or import action items from notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddNewTask,
                modifier = Modifier.testTag("empty_state_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Task")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    allNotes: List<NoteEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, priority: TaskPriority, dueDate: Long?, tag: String, parentNoteId: Long?, parentNoteTitle: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var tag by remember { mutableStateOf("") }
    var selectedDueDateOption by remember { mutableStateOf("None") }
    var selectedParentNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNoteDropdown by remember { mutableStateOf(false) }

    val dueDateMillis = remember(selectedDueDateOption) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        when (selectedDueDateOption) {
            "Today" -> now
            "Tomorrow" -> now + 86400000L
            "In 3 Days" -> now + (3 * 86400000L)
            "Next Week" -> now + (7 * 86400000L)
            else -> null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("add_task_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "New To-Do Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What needs to be done?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_task_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Priority Selection
            Column {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { p ->
                        val isSelected = priority == p
                        val btnColor = when (p) {
                            TaskPriority.HIGH -> Color(0xFFEF4444)
                            TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                            TaskPriority.LOW -> Color(0xFF3B82F6)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) btnColor else btnColor.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) btnColor else btnColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { priority = p }
                                .testTag("priority_btn_${p.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else btnColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick Due Date Selection Chips
            Column {
                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("None", "Today", "Tomorrow", "In 3 Days", "Next Week").forEach { option ->
                        val isSelected = selectedDueDateOption == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDueDateOption = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Tag & Parent Note Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tag Input
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    placeholder = { Text("Tag (e.g. work)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_task_tag_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Parent Note Link Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showNoteDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedParentNote != null) selectedParentNote!!.title else "Link Note",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showNoteDropdown,
                        onDismissRequest = { showNoteDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Standalone Task)") },
                            onClick = {
                                selectedParentNote = null
                                showNoteDropdown = false
                            }
                        )
                        allNotes.forEach { note ->
                            DropdownMenuItem(
                                text = { Text("${note.emoji} ${note.title}") },
                                onClick = {
                                    selectedParentNote = note
                                    showNoteDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            priority,
                            dueDateMillis,
                            tag.trim(),
                            selectedParentNote?.id,
                            selectedParentNote?.title
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

private fun formatDueDate(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    val todayStart = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val diffDays = ((timeMillis - todayStart) / 86400000L).toInt()
    return when (diffDays) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        in 2..6 -> "In $diffDays days"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMillis))
    }
}
