package com.example.markdown

object MarkdownParser {

    fun parse(content: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Code block (fenced)
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                i++
                continue
            }

            // Divider
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Divider)
                i++
                continue
            }

            // Table check: starts with | and contains |
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("""^\|[\s\-:|]+\|$"""))) {
                val headerRow = splitTableRow(trimmed)
                i += 2 // skip header and delimiter row
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    rows.add(splitTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(MarkdownBlock.TableBlock(headerRow, rows))
                continue
            }

            // Callout: Notion style (> 💡 ... or > [!NOTE] ...)
            val calloutRegex = Regex("""^>\s*([💡🚀📌⚡🧠🎯⚠️💬✅🔥]|\[![A-Za-z]+\])\s*(.*)$""")
            if (calloutRegex.matches(trimmed)) {
                val match = calloutRegex.find(trimmed)!!
                val rawIcon = match.groupValues[1]
                val rest = match.groupValues[2]
                val emoji = if (rawIcon.startsWith("[!")) "💡" else rawIcon
                val title = if (rawIcon.startsWith("[!")) rawIcon.removePrefix("[!").removeSuffix("]") else ""
                
                // Collect any following quote lines that belong to this callout
                val bodyLines = mutableListOf(rest)
                i++
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    bodyLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownBlock.CalloutBlock(emoji, title, bodyLines.joinToString(" ").trim()))
                continue
            }

            // Regular Quote (> text)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf(trimmed.removePrefix(">").trim())
                i++
                while (i < lines.size && lines[i].trim().startsWith(">") && !calloutRegex.matches(lines[i].trim())) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownBlock.QuoteBlock(quoteLines.joinToString(" ")))
                continue
            }

            // Task item (Google Keep / Obsidian checklist)
            val taskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])\]\s+(.*)$""")
            val taskMatch = taskRegex.find(line)
            if (taskMatch != null) {
                val isChecked = taskMatch.groupValues[2].equals("x", ignoreCase = true)
                val taskText = taskMatch.groupValues[3]
                blocks.add(MarkdownBlock.TaskItem(lineIndex = i, isChecked = isChecked, text = taskText))
                i++
                continue
            }

            // Headers
            if (trimmed.startsWith("### ")) {
                blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ")))
                i++
                continue
            }
            if (trimmed.startsWith("## ")) {
                blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ")))
                i++
                continue
            }
            if (trimmed.startsWith("# ")) {
                blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ")))
                i++
                continue
            }

            // Bullet items
            val bulletRegex = Regex("""^(\s*)[-*+]\s+(.*)$""")
            val bulletMatch = bulletRegex.find(line)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1].length / 2
                blocks.add(MarkdownBlock.BulletItem(indent, bulletMatch.groupValues[2]))
                i++
                continue
            }

            // Numbered items
            val numberedRegex = Regex("""^(\s*)(\d+)\.\s+(.*)$""")
            val numMatch = numberedRegex.find(line)
            if (numMatch != null) {
                blocks.add(MarkdownBlock.NumberedItem(numMatch.groupValues[2], numMatch.groupValues[3]))
                i++
                continue
            }

            // Regular paragraph
            if (trimmed.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
            i++
        }

        return blocks
    }

    private fun splitTableRow(row: String): List<String> {
        val trimmed = row.trim().removeSurrounding("|")
        return trimmed.split("|").map { it.trim() }
    }

    /**
     * Parses inline formatting:
     * - [[WikiLink]] -> Obsidian note link
     * - #tag -> Tag
     * - **bold**
     * - *italic*
     * - ~~strike~~
     * - `code`
     * - [label](url)
     */
    fun parseInline(text: String): List<InlineToken> {
        val tokens = mutableListOf<InlineToken>()
        var cursor = 0
        val length = text.length

        // Combined regex for inline elements
        val pattern = Regex("""(\[\[(.*?)\]\])|(#([a-zA-Z0-9_-]+))|(\*\*(.*?)\*\*)|(\*(.*?)\*)|(~~(.*?)~~)|(`(.*?)`)|(\[(.*?)\]\((.*?)\))""")

        for (match in pattern.findAll(text)) {
            val range = match.range
            if (range.first > cursor) {
                tokens.add(InlineToken(TokenType.PLAIN, text.substring(cursor, range.first)))
            }

            when {
                // [[WikiLink]]
                match.value.startsWith("[[") && match.value.endsWith("]]") -> {
                    val target = match.value.removePrefix("[[").removeSuffix("]]").trim()
                    tokens.add(InlineToken(TokenType.WIKILINK, target, target = target))
                }
                // #Hashtag
                match.value.startsWith("#") -> {
                    val tag = match.value.removePrefix("#")
                    tokens.add(InlineToken(TokenType.HASHTAG, "#$tag", target = tag))
                }
                // **Bold**
                match.value.startsWith("**") && match.value.endsWith("**") -> {
                    val inner = match.value.removePrefix("**").removeSuffix("**")
                    tokens.add(InlineToken(TokenType.BOLD, inner))
                }
                // *Italic*
                match.value.startsWith("*") && match.value.endsWith("*") -> {
                    val inner = match.value.removePrefix("*").removeSuffix("*")
                    tokens.add(InlineToken(TokenType.ITALIC, inner))
                }
                // ~~Strike~~
                match.value.startsWith("~~") && match.value.endsWith("~~") -> {
                    val inner = match.value.removePrefix("~~").removeSuffix("~~")
                    tokens.add(InlineToken(TokenType.STRIKE, inner))
                }
                // `Code`
                match.value.startsWith("`") && match.value.endsWith("`") -> {
                    val inner = match.value.removePrefix("`").removeSuffix("`")
                    tokens.add(InlineToken(TokenType.INLINE_CODE, inner))
                }
                // [Label](URL)
                match.value.startsWith("[") && match.value.contains("](") -> {
                    val labelMatch = Regex("""\[(.*?)\]\((.*?)\)""").find(match.value)
                    if (labelMatch != null) {
                        tokens.add(InlineToken(TokenType.LINK, labelMatch.groupValues[1], target = labelMatch.groupValues[2]))
                    } else {
                        tokens.add(InlineToken(TokenType.PLAIN, match.value))
                    }
                }
                else -> {
                    tokens.add(InlineToken(TokenType.PLAIN, match.value))
                }
            }
            cursor = range.last + 1
        }

        if (cursor < length) {
            tokens.add(InlineToken(TokenType.PLAIN, text.substring(cursor)))
        }

        if (tokens.isEmpty()) {
            tokens.add(InlineToken(TokenType.PLAIN, text))
        }

        return tokens
    }
}
