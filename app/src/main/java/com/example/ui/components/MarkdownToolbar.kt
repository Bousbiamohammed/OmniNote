package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownToolbar(
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onOpenWikiLinkPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // H1
            ToolbarTextButton(
                text = "H1",
                tag = "format_h1",
                onClick = { onInsertText("\n# ", "") }
            )
            // H2
            ToolbarTextButton(
                text = "H2",
                tag = "format_h2",
                onClick = { onInsertText("\n## ", "") }
            )
            // H3
            ToolbarTextButton(
                text = "H3",
                tag = "format_h3",
                onClick = { onInsertText("\n### ", "") }
            )

            // Bold
            ToolbarIconButton(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                tag = "format_bold",
                onClick = { onInsertText("**", "**") }
            )

            // Italic
            ToolbarIconButton(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                tag = "format_italic",
                onClick = { onInsertText("*", "*") }
            )

            // Checklist
            ToolbarIconButton(
                icon = Icons.Default.CheckBox,
                description = "To-do Checkbox",
                tag = "format_checkbox",
                onClick = { onInsertText("\n- [ ] ", "") }
            )

            // Bullet list
            ToolbarIconButton(
                icon = Icons.Default.FormatListBulleted,
                description = "Bullet list",
                tag = "format_bullet",
                onClick = { onInsertText("\n- ", "") }
            )

            // Numbered list
            ToolbarIconButton(
                icon = Icons.Default.FormatListNumbered,
                description = "Numbered list",
                tag = "format_numbered",
                onClick = { onInsertText("\n1. ", "") }
            )

            // WikiLink [[Note]]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                IconButton(
                    onClick = onOpenWikiLinkPicker,
                    modifier = Modifier.size(38.dp).testTag("format_wikilink")
                ) {
                    Text(
                        text = "[[ ]]",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    )
                }
            }

            // Quote
            ToolbarIconButton(
                icon = Icons.Default.FormatQuote,
                description = "Quote",
                tag = "format_quote",
                onClick = { onInsertText("\n> ", "") }
            )

            // Notion Callout
            ToolbarIconButton(
                icon = Icons.Default.Lightbulb,
                description = "Callout",
                tag = "format_callout",
                onClick = { onInsertText("\n> 💡 **Note:** ", "") }
            )

            // Code block
            ToolbarIconButton(
                icon = Icons.Default.Code,
                description = "Code Block",
                tag = "format_code",
                onClick = { onInsertText("\n```kotlin\n", "\n```\n") }
            )

            // Markdown Table
            ToolbarIconButton(
                icon = Icons.Default.TableChart,
                description = "Table",
                tag = "format_table",
                onClick = {
                    onInsertText(
                        "\n| Column 1 | Column 2 |\n| :--- | :--- |\n| Item A | Value 1 |\n| Item B | Value 2 |\n",
                        ""
                    )
                }
            )

            // Divider
            ToolbarIconButton(
                icon = Icons.Default.HorizontalRule,
                description = "Divider",
                tag = "format_divider",
                onClick = { onInsertText("\n---\n", "") }
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp).testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    text: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp).testTag(tag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
