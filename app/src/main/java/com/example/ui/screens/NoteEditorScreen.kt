package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.markdown.MarkdownRenderer
import com.example.model.NoteColors
import com.example.ui.components.BacklinksPanel
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.EmojiPickerSheet
import com.example.ui.components.MarkdownToolbar
import com.example.ui.components.WikiLinkPickerDialog

enum class EditorTab(val label: String) {
    EDIT("Edit"),
    PREVIEW("Preview"),
    SPLIT("Live Split")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    allNotes: List<NoteEntity>,
    backlinks: List<com.example.data.BacklinkItem>,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onToggleArchive: (NoteEntity) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenInGraph: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var id by remember { mutableStateOf(initialNote?.id ?: 0L) }
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(initialNote?.content ?: ""))
    }
    var emoji by remember { mutableStateOf(initialNote?.emoji ?: "📝") }
    var colorHex by remember { mutableStateOf(initialNote?.colorHex ?: "#202124") }
    var bannerColor by remember { mutableStateOf(initialNote?.bannerColor) }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var tagsText by remember { mutableStateOf(initialNote?.tags ?: "") }

    var selectedTab by remember { mutableStateOf(EditorTab.EDIT) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showWikiLinkDialog by remember { mutableStateOf(false) }
    var showBacklinks by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val currentSurfaceColor = NoteColors.getColorForHex(colorHex, isDark)
    val accentColor = NoteColors.getAccentForHex(colorHex)

    // Auto-save debounced or on change
    fun persistCurrentState() {
        val noteToSave = NoteEntity(
            id = id,
            title = title.ifBlank { "Untitled" },
            content = contentValue.text,
            emoji = emoji,
            colorHex = colorHex,
            bannerColor = bannerColor,
            isPinned = isPinned,
            tags = tagsText,
            updatedAt = System.currentTimeMillis()
        )
        onSaveNote(noteToSave)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("note_editor_screen"),
        containerColor = currentSurfaceColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.size(10.dp)
                        ) {
                            Box(modifier = Modifier.background(Color(0xFF10B981), CircleShape))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            persistCurrentState()
                            onBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Pin toggle
                    IconButton(
                        onClick = {
                            isPinned = !isPinned
                            persistCurrentState()
                        },
                        modifier = Modifier.testTag("editor_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Color picker toggle
                    IconButton(
                        onClick = { showColorPicker = !showColorPicker },
                        modifier = Modifier.testTag("editor_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change color",
                            tint = accentColor
                        )
                    }
                    // Backlinks button
                    IconButton(
                        onClick = { showBacklinks = !showBacklinks },
                        modifier = Modifier.testTag("editor_backlinks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Obsidian Backlinks",
                            tint = if (backlinks.isNotEmpty()) Color(0xFF818CF8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Graph View button
                    if (onOpenInGraph != null && initialNote != null && initialNote.id != 0L) {
                        IconButton(
                            onClick = {
                                persistCurrentState()
                                onOpenInGraph(initialNote.id)
                            },
                            modifier = Modifier.testTag("editor_graph_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "View in Graph",
                                tint = Color(0xFF818CF8)
                            )
                        }
                    }
                    // Delete note
                    if (initialNote != null && initialNote.id != 0L) {
                        IconButton(
                            onClick = {
                                onDeleteNote(initialNote)
                                onBack()
                            },
                            modifier = Modifier.testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentSurfaceColor
                )
            )
        },
        bottomBar = {
            if (selectedTab != EditorTab.PREVIEW) {
                MarkdownToolbar(
                    onInsertText = { prefix, suffix ->
                        val text = contentValue.text
                        val selection = contentValue.selection
                        val selectedText = text.substring(selection.start, selection.end)
                        val newText = text.replaceRange(
                            selection.start,
                            selection.end,
                            "$prefix$selectedText$suffix"
                        )
                        val newCursor = selection.start + prefix.length + selectedText.length
                        contentValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        persistCurrentState()
                    },
                    onOpenWikiLinkPicker = {
                        showWikiLinkDialog = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Color picker expansion row
            if (showColorPicker) {
                ColorPickerRow(
                    selectedHex = colorHex,
                    onSelectColor = {
                        colorHex = it
                        persistCurrentState()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            // Mode Tabs: Edit | Preview | Split
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = currentSurfaceColor,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                EditorTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            persistCurrentState()
                        },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            when (tab) {
                                EditorTab.EDIT -> Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                EditorTab.PREVIEW -> Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                EditorTab.SPLIT -> Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Main Editor Body
            when (selectedTab) {
                EditorTab.EDIT -> {
                    EditContentView(
                        title = title,
                        onTitleChange = {
                            title = it
                            persistCurrentState()
                        },
                        emoji = emoji,
                        onEmojiClick = { showEmojiPicker = true },
                        tagsText = tagsText,
                        onTagsChange = {
                            tagsText = it
                            persistCurrentState()
                        },
                        contentValue = contentValue,
                        onContentChange = {
                            contentValue = it
                            persistCurrentState()
                        },
                        showBacklinks = showBacklinks,
                        backlinks = backlinks,
                        onSelectBacklink = { noteId ->
                            persistCurrentState()
                            onWikiLinkClick(allNotes.find { it.id == noteId }?.title ?: "")
                        }
                    )
                }
                EditorTab.PREVIEW -> {
                    PreviewContentView(
                        title = title,
                        emoji = emoji,
                        tagsText = tagsText,
                        content = contentValue.text,
                        onToggleTask = { lineIndex ->
                            val lines = contentValue.text.lines().toMutableList()
                            if (lineIndex in lines.indices) {
                                val line = lines[lineIndex]
                                val uncheckedRegex = Regex("""^(\s*[-*+]\s+)\[\s\]\s+(.*)$""")
                                val checkedRegex = Regex("""^(\s*[-*+]\s+)\[[xX]\]\s+(.*)$""")
                                val newLine = when {
                                    uncheckedRegex.matches(line) -> {
                                        val match = uncheckedRegex.find(line)!!
                                        "${match.groupValues[1]}[x] ${match.groupValues[2]}"
                                    }
                                    checkedRegex.matches(line) -> {
                                        val match = checkedRegex.find(line)!!
                                        "${match.groupValues[1]}[ ] ${match.groupValues[2]}"
                                    }
                                    else -> line
                                }
                                lines[lineIndex] = newLine
                                contentValue = TextFieldValue(lines.joinToString("\n"))
                                persistCurrentState()
                            }
                        },
                        onWikiLinkClick = { linkTitle ->
                            persistCurrentState()
                            onWikiLinkClick(linkTitle)
                        },
                        showBacklinks = showBacklinks,
                        backlinks = backlinks,
                        onSelectBacklink = { noteId ->
                            persistCurrentState()
                            onWikiLinkClick(allNotes.find { it.id == noteId }?.title ?: "")
                        }
                    )
                }
                EditorTab.SPLIT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditContentView(
                                title = title,
                                onTitleChange = { title = it; persistCurrentState() },
                                emoji = emoji,
                                onEmojiClick = { showEmojiPicker = true },
                                tagsText = tagsText,
                                onTagsChange = { tagsText = it; persistCurrentState() },
                                contentValue = contentValue,
                                onContentChange = { contentValue = it; persistCurrentState() },
                                showBacklinks = false,
                                backlinks = emptyList(),
                                onSelectBacklink = {}
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
                        Box(modifier = Modifier.weight(1f)) {
                            PreviewContentView(
                                title = title,
                                emoji = emoji,
                                tagsText = tagsText,
                                content = contentValue.text,
                                onToggleTask = { lineIndex ->
                                    val lines = contentValue.text.lines().toMutableList()
                                    if (lineIndex in lines.indices) {
                                        val line = lines[lineIndex]
                                        val unchecked = Regex("""^(\s*[-*+]\s+)\[\s\]\s+(.*)$""")
                                        val checked = Regex("""^(\s*[-*+]\s+)\[[xX]\]\s+(.*)$""")
                                        val newLine = when {
                                            unchecked.matches(line) -> "${unchecked.find(line)!!.groupValues[1]}[x] ${unchecked.find(line)!!.groupValues[2]}"
                                            checked.matches(line) -> "${checked.find(line)!!.groupValues[1]}[ ] ${checked.find(line)!!.groupValues[2]}"
                                            else -> line
                                        }
                                        lines[lineIndex] = newLine
                                        contentValue = TextFieldValue(lines.joinToString("\n"))
                                        persistCurrentState()
                                    }
                                },
                                onWikiLinkClick = { linkTitle ->
                                    persistCurrentState()
                                    onWikiLinkClick(linkTitle)
                                },
                                showBacklinks = showBacklinks,
                                backlinks = backlinks,
                                onSelectBacklink = { noteId ->
                                    persistCurrentState()
                                    onWikiLinkClick(allNotes.find { it.id == noteId }?.title ?: "")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Emoji Picker Sheet
    if (showEmojiPicker) {
        EmojiPickerSheet(
            onSelectEmoji = {
                emoji = it
                persistCurrentState()
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    // Modal WikiLink Picker Dialog
    if (showWikiLinkDialog) {
        WikiLinkPickerDialog(
            availableNotes = allNotes,
            onSelectNoteTitle = { targetTitle ->
                val linkTag = "[[$targetTitle]]"
                val text = contentValue.text
                val sel = contentValue.selection
                val newText = text.replaceRange(sel.start, sel.end, linkTag)
                contentValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(sel.start + linkTag.length)
                )
                persistCurrentState()
            },
            onDismiss = { showWikiLinkDialog = false }
        )
    }
}

@Composable
private fun EditContentView(
    title: String,
    onTitleChange: (String) -> Unit,
    emoji: String,
    onEmojiClick: () -> Unit,
    tagsText: String,
    onTagsChange: (String) -> Unit,
    contentValue: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    showBacklinks: Boolean,
    backlinks: List<com.example.data.BacklinkItem>,
    onSelectBacklink: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Notion Page Icon + Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("editor_emoji_button")
            ) {
                IconButton(onClick = onEmojiClick) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = {
                    Text(
                        "Note Title...",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("note_title_input")
            )
        }

        // Tags bar
        TextField(
            value = tagsText,
            onValueChange = onTagsChange,
            placeholder = { Text("Tags (e.g. work, ideas, obsidian)", fontSize = 13.sp) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("note_tags_input")
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))

        // Raw Markdown text editor
        TextField(
            value = contentValue,
            onValueChange = onContentChange,
            placeholder = {
                Text(
                    "Write in markdown...\nUse # for headers\n- [ ] for checklists\n[[Note Title]] for Obsidian links\n> 💡 for Notion callouts",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp,
                fontSize = 15.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .testTag("note_content_input")
        )

        if (showBacklinks) {
            Spacer(modifier = Modifier.height(24.dp))
            BacklinksPanel(
                currentNoteTitle = title,
                backlinks = backlinks,
                onSelectBacklink = onSelectBacklink
            )
        }
    }
}

@Composable
private fun PreviewContentView(
    title: String,
    emoji: String,
    tagsText: String,
    content: String,
    onToggleTask: (Int) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    showBacklinks: Boolean,
    backlinks: List<com.example.data.BacklinkItem>,
    onSelectBacklink: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("note_preview_view")
    ) {
        // Render Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (tagsText.isNotBlank()) {
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 12.dp))

        // Render Markdown Blocks
        MarkdownRenderer(
            content = content,
            onToggleTask = onToggleTask,
            onWikiLinkClick = onWikiLinkClick,
            onTagClick = { /* tag clicked in preview */ }
        )

        if (showBacklinks) {
            Spacer(modifier = Modifier.height(28.dp))
            BacklinksPanel(
                currentNoteTitle = title,
                backlinks = backlinks,
                onSelectBacklink = onSelectBacklink
            )
        }
    }
}
