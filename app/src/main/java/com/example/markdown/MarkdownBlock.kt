package com.example.markdown

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class TaskItem(val lineIndex: Int, val isChecked: Boolean, val text: String) : MarkdownBlock()
    data class BulletItem(val indentLevel: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class QuoteBlock(val text: String) : MarkdownBlock()
    data class CalloutBlock(val emoji: String, val title: String, val text: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

data class InlineToken(
    val type: TokenType,
    val text: String,
    val target: String = "" // For WikiLink note title or url
)

enum class TokenType {
    PLAIN,
    BOLD,
    ITALIC,
    STRIKE,
    INLINE_CODE,
    WIKILINK,
    HASHTAG,
    LINK
}
