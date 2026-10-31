package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.model.NoteColors
import com.example.ui.components.EnhancedSearchBar
import com.example.ui.components.KeepNoteCard
import com.example.ui.components.VaultSyncDialog
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SearchScope
import com.example.ui.viewmodel.SearchSuggestion
import com.example.ui.viewmodel.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<NoteEntity>,
    allActiveNotesCount: Int,
    archivedNotesCount: Int,
    trashedNotesCount: Int,
    pendingTasksCount: Int = 0,
    availableTags: List<String>,
    selectedTag: String?,
    selectedColor: String?,
    searchQuery: String,
    searchScope: SearchScope = SearchScope.ALL,
    suggestions: List<SearchSuggestion> = emptyList(),
    viewMode: ViewMode,
    currentScreen: Screen,
    onSelectNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onCreateDailyNote: () -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onToggleArchive: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onSearchChange: (String) -> Unit,
    onScopeChange: (SearchScope) -> Unit = {},
    onSelectTag: (String?) -> Unit,
    onSelectColor: (String?) -> Unit,
    onClearAllFilters: () -> Unit = {},
    onToggleViewMode: () -> Unit,
    onOpenGraph: () -> Unit,
    onNavigateScreen: (Screen) -> Unit,
    onExportVault: () -> String,
    onImportVault: (String) -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser? = null,
    isSyncing: Boolean = false,
    lastSyncTime: Long? = null,
    onSignInWithGoogle: (android.app.Activity) -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSyncDialog by remember { mutableStateOf(false) }

    // Split pinned vs unpinned notes
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val otherNotes = remember(notes) { notes.filter { !it.isPinned } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF6366F1), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧠", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "OmniNote",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Keep + Notion + Obsidian",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vault Notes
                NavigationDrawerItem(
                    label = { Text("Active Notes ($allActiveNotesCount)") },
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    selected = currentScreen is Screen.Home,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateScreen(Screen.Home)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_home")
                )

                // To-Do List / Tasks
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To-Do List")
                            if (pendingTasksCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$pendingTasksCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = Color(0xFF10B981)) },
                    selected = currentScreen is Screen.TodoList,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateScreen(Screen.TodoList)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_todo")
                )

                // Obsidian Graph View
                NavigationDrawerItem(
                    label = { Text("Graph View") },
                    icon = { Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF818CF8)) },
                    selected = currentScreen is Screen.Graph,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenGraph()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_graph")
                )

                // Daily Journal
                NavigationDrawerItem(
                    label = { Text("Today's Journal (Obsidian)") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF10B981)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCreateDailyNote()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_daily")
                )

                // Archive
                NavigationDrawerItem(
                    label = { Text("Archive ($archivedNotesCount)") },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    selected = currentScreen is Screen.Archive,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateScreen(Screen.Archive)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_archive")
                )

                // Trash
                NavigationDrawerItem(
                    label = { Text("Trash ($trashedNotesCount)") },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    selected = currentScreen is Screen.Trash,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateScreen(Screen.Trash)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_trash")
                )

                Spacer(modifier = Modifier.weight(1f))

                // Vault & Google Sync status in drawer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentUser != null) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            showSyncDialog = true
                        }
                        .testTag("drawer_sync_status")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentUser != null) "Google Synced" else "Offline Vault",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentUser?.displayName ?: currentUser?.email ?: "Tap to Sign in with Google",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().testTag("home_screen"),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                is Screen.Archive -> "Archived Notes"
                                is Screen.Trash -> "Trash Bin"
                                else -> "OmniNote"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Navigation drawer")
                        }
                    },
                    actions = {
                        // Cloud / Offline Sync indicator pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (currentUser != null) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (currentUser != null) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showSyncDialog = true }
                                .padding(end = 4.dp)
                                .testTag("top_sync_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(if (currentUser != null) Color(0xFF10B981) else Color(0xFF818CF8), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isSyncing) "Syncing..." else if (currentUser != null) "Synced" else "Sync",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        // To-Do List shortcut
                        IconButton(
                            onClick = { onNavigateScreen(Screen.TodoList) },
                            modifier = Modifier.testTag("top_todo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "To-Do List",
                                tint = Color(0xFF10B981)
                            )
                        }

                        // Obsidian Graph View shortcut
                        IconButton(
                            onClick = onOpenGraph,
                            modifier = Modifier.testTag("top_graph_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "Obsidian Graph View",
                                tint = Color(0xFF818CF8)
                            )
                        }

                        // Staggered grid vs Single list toggle (Google Keep style)
                        IconButton(
                            onClick = onToggleViewMode,
                            modifier = Modifier.testTag("toggle_view_mode")
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.STAGGERED_GRID) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List view"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Daily Note Action Button (Obsidian)
                    FloatingActionButton(
                        onClick = onCreateDailyNote,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("fab_daily_note")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = "Daily Note", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    // Main + Note FAB
                    FloatingActionButton(
                        onClick = onCreateNote,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_new_note")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create new note", modifier = Modifier.size(24.dp))
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Enhanced M3 Search Bar with live suggestions, syntax filter pills & scope menu
                EnhancedSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    searchScope = searchScope,
                    onScopeChange = onScopeChange,
                    suggestions = suggestions,
                    onSelectSuggestion = onSelectNote,
                    totalResults = notes.size,
                    isFilterActive = searchQuery.isNotEmpty() || selectedTag != null || selectedColor != null || searchScope != SearchScope.ALL,
                    onClearAllFilters = onClearAllFilters
                )

                // Tag Chips Filter (Horizontal Row)
                if (availableTags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterTagChip(
                                label = "All",
                                isSelected = selectedTag == null,
                                onClick = { onSelectTag(null) }
                            )
                        }
                        items(availableTags) { tag ->
                            FilterTagChip(
                                label = "#$tag",
                                isSelected = selectedTag == tag,
                                onClick = { onSelectTag(tag) }
                            )
                        }
                    }
                }

                // Keep Color Filter Bar (Optional)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset color
                    Surface(
                        shape = CircleShape,
                        color = if (selectedColor == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedColor == null) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onSelectColor(null) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("All", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    NoteColors.PALETTE.take(8).forEach { colorItem ->
                        val isSelected = selectedColor.equals(colorItem.hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorItem.accent)
                                .clickable { onSelectColor(if (isSelected) null else colorItem.hex) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Notes List / Grid
                if (notes.isEmpty()) {
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
                                text = if (searchQuery.isNotEmpty()) "No notes match \"$searchQuery\"" else "No notes in this view",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to create a markdown note with checklists & wikilinks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (viewMode == ViewMode.STAGGERED_GRID) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp
                        ) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = "PINNED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                                    )
                                }
                                items(pinnedNotes, key = { it.id }) { note ->
                                    KeepNoteCard(
                                        note = note,
                                        onClick = { onSelectNote(note.id) },
                                        onTogglePin = { onTogglePin(note) },
                                        onToggleArchive = { onToggleArchive(note) },
                                        onDelete = { onDeleteNote(note) }
                                    )
                                }
                            }

                            if (otherNotes.isNotEmpty()) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                                items(otherNotes, key = { it.id }) { note ->
                                    KeepNoteCard(
                                        note = note,
                                        onClick = { onSelectNote(note.id) },
                                        onTogglePin = { onTogglePin(note) },
                                        onToggleArchive = { onToggleArchive(note) },
                                        onDelete = { onDeleteNote(note) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Single Column List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pinnedNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "PINNED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(pinnedNotes, key = { it.id }) { note ->
                                    KeepNoteCard(
                                        note = note,
                                        onClick = { onSelectNote(note.id) },
                                        onTogglePin = { onTogglePin(note) },
                                        onToggleArchive = { onToggleArchive(note) },
                                        onDelete = { onDeleteNote(note) }
                                    )
                                }
                            }

                            if (otherNotes.isNotEmpty()) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                                items(otherNotes, key = { it.id }) { note ->
                                    KeepNoteCard(
                                        note = note,
                                        onClick = { onSelectNote(note.id) },
                                        onTogglePin = { onTogglePin(note) },
                                        onToggleArchive = { onToggleArchive(note) },
                                        onDelete = { onDeleteNote(note) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Vault & Cloud Sync Dialog
    if (showSyncDialog) {
        val totalLinks = remember(notes) {
            notes.sumOf { it.extractWikiLinks().size }
        }
        VaultSyncDialog(
            totalNotes = allActiveNotesCount,
            totalLinks = totalLinks,
            currentUser = currentUser,
            isSyncing = isSyncing,
            lastSyncTime = lastSyncTime,
            onSignInWithGoogle = onSignInWithGoogle,
            onSignOutGoogle = onSignOutGoogle,
            onSyncNow = onSyncNow,
            onExportVault = onExportVault,
            onImportVault = onImportVault,
            onDismiss = { showSyncDialog = false }
        )
    }
}

@Composable
private fun FilterTagChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
