package com.example.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    onToggleTask: (lineIndex: Int) -> Unit = {},
    onWikiLinkClick: (title: String) -> Unit = {},
    onTagClick: (tag: String) -> Unit = {}
) {
    val blocks = remember(content) {
        MarkdownParser.parse(content)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(
                text = "Empty note. Start typing markdown, add checklists `- [ ]` or links `[[page]]`...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic
            )
        }

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    RenderHeader(block)
                }
                is MarkdownBlock.TaskItem -> {
                    RenderTaskItem(block, onToggleTask, onWikiLinkClick, onTagClick)
                }
                is MarkdownBlock.BulletItem -> {
                    RenderBulletItem(block, onWikiLinkClick, onTagClick)
                }
                is MarkdownBlock.NumberedItem -> {
                    RenderNumberedItem(block, onWikiLinkClick, onTagClick)
                }
                is MarkdownBlock.CodeBlock -> {
                    RenderCodeBlock(block)
                }
                is MarkdownBlock.QuoteBlock -> {
                    RenderQuoteBlock(block, onWikiLinkClick, onTagClick)
                }
                is MarkdownBlock.CalloutBlock -> {
                    RenderCalloutBlock(block, onWikiLinkClick, onTagClick)
                }
                is MarkdownBlock.TableBlock -> {
                    RenderTableBlock(block)
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    RenderFormattedParagraph(block.text, onWikiLinkClick, onTagClick)
                }
            }
        }
    }
}

@Composable
private fun RenderHeader(header: MarkdownBlock.Header) {
    val style = when (header.level) {
        1 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
    }
    Text(
        text = header.text,
        style = style,
        modifier = Modifier.padding(top = if (header.level == 1) 8.dp else 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun RenderTaskItem(
    item: MarkdownBlock.TaskItem,
    onToggle: (Int) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(item.lineIndex) }
            .padding(vertical = 2.dp)
            .testTag("task_item_${item.lineIndex}")
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggle(item.lineIndex) },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF10B981),
                uncheckedColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.size(28.dp).padding(end = 4.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        RenderInlineFormattedText(
            text = item.text,
            isStrikethrough = item.isChecked,
            onWikiLinkClick = onWikiLinkClick,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun RenderBulletItem(
    bullet: MarkdownBlock.BulletItem,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (bullet.indentLevel * 16).dp, top = 2.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
        RenderInlineFormattedText(
            text = bullet.text,
            onWikiLinkClick = onWikiLinkClick,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun RenderNumberedItem(
    item: MarkdownBlock.NumberedItem,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "${item.number}.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(end = 8.dp)
        )
        RenderInlineFormattedText(
            text = item.text,
            onWikiLinkClick = onWikiLinkClick,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun RenderCodeBlock(block: MarkdownBlock.CodeBlock) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161822),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.language.ifEmpty { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("code", block.code))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF282B3C), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            val scrollState = rememberScrollState()
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.horizontalScroll(scrollState)
            )
        }
    }
}

@Composable
private fun RenderQuoteBlock(
    quote: MarkdownBlock.QuoteBlock,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .background(MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(2.dp))
                .height(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        RenderInlineFormattedText(
            text = quote.text,
            isItalic = true,
            onWikiLinkClick = onWikiLinkClick,
            onTagClick = onTagClick
        )
    }
}

@Composable
private fun RenderCalloutBlock(
    callout: MarkdownBlock.CalloutBlock,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = callout.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (callout.title.isNotEmpty()) {
                    Text(
                        text = callout.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                RenderInlineFormattedText(
                    text = callout.text,
                    onWikiLinkClick = onWikiLinkClick,
                    onTagClick = onTagClick
                )
            }
        }
    }
}

@Composable
private fun RenderTableBlock(table: MarkdownBlock.TableBlock) {
    val scrollState = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.horizontalScroll(scrollState).padding(8.dp)) {
            // Header
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                table.headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                    )
                }
            }
            // Rows
            table.rows.forEach { row ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderFormattedParagraph(
    text: String,
    onWikiLinkClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    RenderInlineFormattedText(text = text, onWikiLinkClick = onWikiLinkClick, onTagClick = onTagClick)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderInlineFormattedText(
    text: String,
    modifier: Modifier = Modifier,
    isItalic: Boolean = false,
    isStrikethrough: Boolean = false,
    onWikiLinkClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {}
) {
    val tokens = remember(text) { MarkdownParser.parseInline(text) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.Center
    ) {
        tokens.forEach { token ->
            when (token.type) {
                TokenType.WIKILINK -> {
                    // Obsidian-style WikiLink pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        modifier = Modifier
                            .clickable { onWikiLinkClick(token.target) }
                            .padding(vertical = 2.dp)
                            .testTag("wikilink_${token.target}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "[[${token.target}]]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF818CF8)
                                )
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                TokenType.HASHTAG -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onTagClick(token.target) }
                            .padding(vertical = 2.dp)
                            .testTag("hashtag_${token.target}")
                    ) {
                        Text(
                            text = token.text,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF59E0B)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                TokenType.INLINE_CODE -> {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2230),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = token.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                TokenType.BOLD -> {
                    Text(
                        text = token.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                }
                TokenType.ITALIC -> {
                    Text(
                        text = token.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                }
                TokenType.STRIKE -> {
                    Text(
                        text = token.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                TokenType.LINK -> {
                    Text(
                        text = token.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                TokenType.PLAIN -> {
                    Text(
                        text = token.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isStrikethrough) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                }
            }
        }
    }
}
