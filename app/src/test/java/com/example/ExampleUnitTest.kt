package com.example

import com.example.data.NoteEntity
import com.example.data.NoteLinkEntity
import com.example.markdown.MarkdownBlock
import com.example.markdown.MarkdownParser
import com.example.markdown.TokenType
import com.example.ui.viewmodel.SearchScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testWikiLinksAndTagsExtraction() {
        val note = NoteEntity(
            title = "Test Note",
            content = "This connects to [[Project Roadmap 2026]] and [[Idea Scratchpad]]. Tagged with #dev and #planning."
        )
        val links = note.extractWikiLinks()
        val tags = note.extractInlineTags()

        assertEquals(listOf("Project Roadmap 2026", "Idea Scratchpad"), links)
        assertEquals(listOf("dev", "planning"), tags)
    }

    @Test
    fun testBidirectionalLinkMetadataExtraction() {
        val note = NoteEntity(
            id = 42L,
            title = "Architecture Plan",
            content = """
                # Architecture Plan
                Check out [[Project Roadmap 2026|Roadmap]] for technical milestones.
                Also refer to [[Meeting Notes]] for the team feedback.
            """.trimIndent()
        )

        val metadataLinks = note.extractLinksWithMetadata()
        assertEquals(2, metadataLinks.size)

        val firstLink = metadataLinks[0]
        assertEquals("Project Roadmap 2026", firstLink.targetTitle)
        assertEquals("Roadmap", firstLink.alias)
        assertTrue(firstLink.contextSnippet.contains("Check out [[Project Roadmap 2026|Roadmap]]"))

        val secondLink = metadataLinks[1]
        assertEquals("Meeting Notes", secondLink.targetTitle)
        assertEquals(null, secondLink.alias)
        assertTrue(secondLink.contextSnippet.contains("Also refer to [[Meeting Notes]]"))

        // Create NoteLinkEntity instance to verify schema model initialization
        val linkEntity = NoteLinkEntity(
            sourceNoteId = note.id,
            targetTitle = firstLink.targetTitle,
            alias = firstLink.alias,
            contextSnippet = firstLink.contextSnippet
        )
        assertEquals(42L, linkEntity.sourceNoteId)
        assertEquals("Project Roadmap 2026", linkEntity.targetTitle)
        assertEquals("Roadmap", linkEntity.alias)
        assertNotNull(linkEntity.contextSnippet)
    }

    @Test
    fun testLinkedNotesByTitleResolution() {
        val sourceNoteA = NoteEntity(
            id = 1L,
            title = "Daily Journal",
            content = "Worked on [[Project Roadmap 2026]] today."
        )
        val sourceNoteB = NoteEntity(
            id = 2L,
            title = "Idea Scratchpad",
            content = "Check ideas in [[Project Roadmap 2026]]."
        )
        val targetNote = NoteEntity(
            id = 3L,
            title = "Project Roadmap 2026",
            content = "Roadmap details here."
        )

        val allNotes = listOf(sourceNoteA, sourceNoteB, targetNote)
        val targetTitle = "Project Roadmap 2026"

        val incomingNotes = allNotes.filter { note ->
            note.id != targetNote.id && note.extractWikiLinks().any { it.equals(targetTitle, ignoreCase = true) }
        }

        assertEquals(2, incomingNotes.size)
        assertTrue(incomingNotes.any { it.id == 1L })
        assertTrue(incomingNotes.any { it.id == 2L })
    }

    @Test
    fun testEnhancedSearchFiltering() {
        val noteWithTodo = NoteEntity(
            id = 1L,
            title = "Checklist Note",
            content = "Tasks:\n- [ ] Open milestone\n- [x] Finished item"
        )
        val noteWithLinks = NoteEntity(
            id = 2L,
            title = "Knowledge Hub",
            content = "Refer to [[Checklist Note]] for details."
        )
        val noteWithCode = NoteEntity(
            id = 3L,
            title = "Kotlin Snippet",
            content = "```kotlin\nval x = 42\n```"
        )

        val notes = listOf(noteWithTodo, noteWithLinks, noteWithCode)

        // Test CHECKLISTS scope
        val checklistNotes = notes.filter { it.content.contains("- [ ]") || it.content.contains("- [x]") }
        assertEquals(1, checklistNotes.size)
        assertEquals("Checklist Note", checklistNotes[0].title)

        // Test WIKILINKS scope
        val linkedNotes = notes.filter { it.extractWikiLinks().isNotEmpty() }
        assertEquals(1, linkedNotes.size)
        assertEquals("Knowledge Hub", linkedNotes[0].title)

        // Test CODE scope
        val codeNotes = notes.filter { it.content.contains("```") }
        assertEquals(1, codeNotes.size)
        assertEquals("Kotlin Snippet", codeNotes[0].title)
    }

    @Test
    fun testMarkdownParserBlocks() {
        val raw = """
# Header 1
## Header 2
> 💡 Notion Callout tip
- [ ] Task 1
- [x] Task 2
```kotlin
val a = 1
```
---
        """.trimIndent()

        val blocks = MarkdownParser.parse(raw)
        assertTrue(blocks.any { it is MarkdownBlock.Header && it.level == 1 })
        assertTrue(blocks.any { it is MarkdownBlock.CalloutBlock && it.emoji == "💡" })
        assertTrue(blocks.any { it is MarkdownBlock.TaskItem && !it.isChecked })
        assertTrue(blocks.any { it is MarkdownBlock.TaskItem && it.isChecked })
        assertTrue(blocks.any { it is MarkdownBlock.CodeBlock })
        assertTrue(blocks.any { it is MarkdownBlock.Divider })
    }

    @Test
    fun testInlineTokenizer() {
        val inline = "Check [[Obsidian Link]] with **bold** and `code`"
        val tokens = MarkdownParser.parseInline(inline)
        assertTrue(tokens.any { it.type == TokenType.WIKILINK && it.target == "Obsidian Link" })
        assertTrue(tokens.any { it.type == TokenType.BOLD && it.text == "bold" })
        assertTrue(tokens.any { it.type == TokenType.INLINE_CODE && it.text == "code" })
    }

    @Test
    fun testForceDirectedGraphDataAndBidirectionalLinks() {
        val noteA = NoteEntity(
            id = 1L,
            title = "Neural Networks",
            content = "Fundamentals of deep learning. See [[Deep Learning]] and [[Linear Algebra]].",
            tags = "ai,math"
        )
        val noteB = NoteEntity(
            id = 2L,
            title = "Deep Learning",
            content = "Advanced topics building on [[Neural Networks]].",
            tags = "ai"
        )
        val noteC = NoteEntity(
            id = 3L,
            title = "Linear Algebra",
            content = "Vector spaces and matrices.",
            tags = "math"
        )
        val orphanNote = NoteEntity(
            id = 4L,
            title = "Shopping List",
            content = "Apples, Milk, Bread",
            tags = "personal"
        )

        val notes = listOf(noteA, noteB, noteC, orphanNote)
        val graphData = com.example.ui.graph.ForceSimulation.buildGraphData(notes)

        // Verify nodes count
        assertEquals(4, graphData.nodes.size)

        val nodeA = graphData.nodes.find { it.id == 1L }!!
        val nodeB = graphData.nodes.find { it.id == 2L }!!
        val nodeC = graphData.nodes.find { it.id == 3L }!!
        val nodeOrphan = graphData.nodes.find { it.id == 4L }!!

        // Node A: links out to B and C (outDegree 2), gets backlink from B (inDegree 1)
        assertEquals(2, nodeA.outDegree)
        assertEquals(1, nodeA.inDegree)
        assertEquals(3, nodeA.totalConnections)

        // Node B: links out to A (outDegree 1), gets backlink from A (inDegree 1)
        assertEquals(1, nodeB.outDegree)
        assertEquals(1, nodeB.inDegree)
        assertEquals(2, nodeB.totalConnections)

        // Orphan node has 0 connections
        assertEquals(0, nodeOrphan.totalConnections)

        // Check bidirectional edge between A and B
        val bidiEdge = graphData.edges.find {
            (it.fromNodeId == 1L && it.toNodeId == 2L) || (it.fromNodeId == 2L && it.toNodeId == 1L)
        }
        assertNotNull(bidiEdge)
        assertTrue("Edge between Neural Networks and Deep Learning must be bidirectional", bidiEdge!!.isBidirectional)

        // Check unidirectional edge from A to C
        val uniEdge = graphData.edges.find { it.fromNodeId == 1L && it.toNodeId == 3L }
        assertNotNull(uniEdge)
        assertEquals(false, uniEdge!!.isBidirectional)

        // Test physics relaxation step does not produce NaN or infinite values
        val energy = com.example.ui.graph.ForceSimulation.step(graphData.nodes, graphData.edges)
        assertTrue(energy >= 0f)
        graphData.nodes.forEach { node ->
            assertTrue(!node.x.isNaN() && !node.x.isInfinite())
            assertTrue(!node.y.isNaN() && !node.y.isInfinite())
            assertTrue(!node.vx.isNaN() && !node.vx.isInfinite())
            assertTrue(!node.vy.isNaN() && !node.vy.isInfinite())
        }
    }

    @Test
    fun testTaskEntityAndToggling() {
        val task = com.example.data.TaskEntity(
            id = 101L,
            title = "Test To-Do Task",
            isCompleted = false,
            priority = com.example.data.TaskPriority.HIGH,
            tag = "work",
            parentNoteId = 5L,
            parentNoteTitle = "Project Roadmap"
        )

        assertEquals("Test To-Do Task", task.title)
        assertEquals(false, task.isCompleted)
        assertEquals(com.example.data.TaskPriority.HIGH, task.priority)
        assertEquals("work", task.tag)
        assertEquals(5L, task.parentNoteId)
        assertEquals("Project Roadmap", task.parentNoteTitle)

        // Toggle task
        val toggled = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
        assertTrue(toggled.isCompleted)
        assertNotNull(toggled.completedAt)
    }

    @Test
    fun testNoteChecklistExtractionAndFilter() {
        val noteA = NoteEntity(
            id = 1L,
            title = "Welcome Note",
            emoji = "🚀",
            content = """
                # Tasks
                - [x] Completed task A
                - [ ] Pending task B
                - [X] Completed task C with capital X
            """.trimIndent()
        )
        val noteB = NoteEntity(
            id = 2L,
            title = "Scratchpad",
            emoji = "💡",
            content = "No tasks here, just regular notes and [[Welcome Note]]."
        )

        val taskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])\]\s+(.*)$""")
        val items = mutableListOf<com.example.data.NoteChecklistItem>()

        listOf(noteA, noteB).forEach { note ->
            note.content.lines().forEachIndexed { idx, line ->
                taskRegex.find(line)?.let { match ->
                    val isChecked = match.groupValues[2].equals("x", ignoreCase = true)
                    val text = match.groupValues[3].trim()
                    items.add(
                        com.example.data.NoteChecklistItem(
                            noteId = note.id,
                            noteTitle = note.title,
                            noteEmoji = note.emoji,
                            lineIndex = idx,
                            isChecked = isChecked,
                            text = text
                        )
                    )
                }
            }
        }

        assertEquals(3, items.size)
        assertTrue(items[0].isChecked)
        assertEquals("Completed task A", items[0].text)
        assertEquals(false, items[1].isChecked)
        assertEquals("Pending task B", items[1].text)
        assertTrue(items[2].isChecked)
        assertEquals("Completed task C with capital X", items[2].text)

        // Verify pending checklist filter
        val pendingChecklists = items.filter { !it.isChecked }
        assertEquals(1, pendingChecklists.size)
        assertEquals("Pending task B", pendingChecklists[0].text)
    }
}
