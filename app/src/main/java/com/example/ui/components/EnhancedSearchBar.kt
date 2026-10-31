package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.SearchScope
import com.example.ui.viewmodel.SearchSuggestion

@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchScope: SearchScope,
    onScopeChange: (SearchScope) -> Unit,
    suggestions: List<SearchSuggestion>,
    onSelectSuggestion: (Long) -> Unit,
    totalResults: Int,
    isFilterActive: Boolean,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var showScopeMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Main Search Bar Container (M3 Pill Shape)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            tonalElevation = if (isFocused) 4.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Search Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .padding(start = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isFocused || query.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = when (searchScope) {
                                SearchScope.ALL -> "Search notes, #tags, or [[links]]..."
                                SearchScope.TITLE_ONLY -> "Search note titles..."
                                SearchScope.CHECKLISTS -> "Search notes with to-do tasks..."
                                SearchScope.WIKILINKS -> "Search notes with [[wikilinks]]..."
                                SearchScope.CODE_BLOCKS -> "Search code snippets..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("home_search_input")
                )

                // Clear Query button
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                        },
                        modifier = Modifier.size(36.dp).testTag("clear_search_query")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Scope Selector Menu Button
                Box {
                    IconButton(
                        onClick = { showScopeMenu = true },
                        modifier = Modifier.size(38.dp).testTag("search_scope_menu_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (searchScope != SearchScope.ALL) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter Scope",
                                    tint = if (searchScope != SearchScope.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showScopeMenu,
                        onDismissRequest = { showScopeMenu = false }
                    ) {
                        SearchScope.values().forEach { scope ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(scope.iconEmoji, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                                        Text(
                                            text = scope.label,
                                            fontWeight = if (searchScope == scope) FontWeight.Bold else FontWeight.Normal,
                                            color = if (searchScope == scope) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    onScopeChange(scope)
                                    showScopeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Syntax Helper Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checklists shortcut
            SyntaxFilterPill(
                icon = "☑️",
                label = "To-Dos",
                isActive = searchScope == SearchScope.CHECKLISTS || query.startsWith("todo:"),
                onClick = {
                    if (searchScope == SearchScope.CHECKLISTS) onScopeChange(SearchScope.ALL)
                    else onScopeChange(SearchScope.CHECKLISTS)
                }
            )

            // WikiLinks shortcut
            SyntaxFilterPill(
                icon = "🔗",
                label = "[[Links]]",
                isActive = searchScope == SearchScope.WIKILINKS || query.startsWith("[["),
                onClick = {
                    if (query.startsWith("[[")) onQueryChange("")
                    else onQueryChange("[[")
                }
            )

            // Tags shortcut
            SyntaxFilterPill(
                icon = "#",
                label = "Hashtags",
                isActive = query.startsWith("#"),
                onClick = {
                    if (query.startsWith("#")) onQueryChange("")
                    else onQueryChange("#")
                }
            )

            // Code blocks shortcut
            SyntaxFilterPill(
                icon = "</>",
                label = "Code",
                isActive = searchScope == SearchScope.CODE_BLOCKS || query.startsWith("code:"),
                onClick = {
                    if (searchScope == SearchScope.CODE_BLOCKS) onScopeChange(SearchScope.ALL)
                    else onScopeChange(SearchScope.CODE_BLOCKS)
                }
            )

            // Titles shortcut
            SyntaxFilterPill(
                icon = "🔤",
                label = "Titles",
                isActive = searchScope == SearchScope.TITLE_ONLY,
                onClick = {
                    if (searchScope == SearchScope.TITLE_ONLY) onScopeChange(SearchScope.ALL)
                    else onScopeChange(SearchScope.TITLE_ONLY)
                }
            )
        }

        // Live Suggestions Overlay Card
        AnimatedVisibility(
            visible = isFocused && suggestions.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp)
                    .testTag("search_suggestions_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Notes",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    suggestions.forEach { suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    focusManager.clearFocus()
                                    onSelectSuggestion(suggestion.noteId)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("suggestion_item_${suggestion.noteId}")
                        ) {
                            Text(text = suggestion.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (suggestion.matchedSnippet.isNotEmpty()) {
                                    Text(
                                        text = suggestion.matchedSnippet,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Results indicator when search or filters are active
        if (isFilterActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Found $totalResults note${if (totalResults != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = onClearAllFilters,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Reset Search",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SyntaxFilterPill(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
