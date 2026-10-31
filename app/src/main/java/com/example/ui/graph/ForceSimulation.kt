package com.example.ui.graph

import com.example.data.NoteEntity
import com.example.model.GraphData
import com.example.model.GraphEdge
import com.example.model.GraphNode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PhysicsConfig(
    val repulsion: Float = 3500f,
    val springLength: Float = 140f,
    val springStiffness: Float = 0.045f,
    val centerGravity: Float = 0.012f,
    val damping: Float = 0.88f,
    val maxVelocity: Float = 25f
)

object ForceSimulation {

    /**
     * Builds GraphData from notes, analyzing bidirectional WikiLinks and tag metadata.
     */
    fun buildGraphData(notes: List<NoteEntity>): GraphData {
        val activeNotes = notes.filter { !it.isTrashed }
        val titleToNote = activeNotes.associateBy { it.title.trim().lowercase() }

        // Track raw directed links: (fromId, toId)
        val directedLinkSet = mutableSetOf<Pair<Long, Long>>()
        val inDegrees = mutableMapOf<Long, Int>()
        val outDegrees = mutableMapOf<Long, Int>()

        activeNotes.forEach { note ->
            val outgoingTitles = note.extractWikiLinks()
            var validOutCount = 0
            outgoingTitles.forEach { targetTitle ->
                val target = titleToNote[targetTitle.trim().lowercase()]
                if (target != null && target.id != note.id) {
                    directedLinkSet.add(Pair(note.id, target.id))
                    inDegrees[target.id] = (inDegrees[target.id] ?: 0) + 1
                    validOutCount++
                }
            }
            outDegrees[note.id] = validOutCount
        }

        // Build edges and check bidirectional status (A->B and B->A)
        val edges = mutableListOf<GraphEdge>()
        val seenEdgePairs = mutableSetOf<Pair<Long, Long>>()

        directedLinkSet.forEach { (fromId, toId) ->
            val fromNote = activeNotes.find { it.id == fromId }
            val toNote = activeNotes.find { it.id == toId }
            if (fromNote != null && toNote != null) {
                val isBidi = directedLinkSet.contains(Pair(toId, fromId))
                // For bidirectional edges, avoid creating duplicates in rendering
                val normalizedKey = if (fromId < toId) Pair(fromId, toId) else Pair(toId, fromId)
                if (!isBidi || seenEdgePairs.add(normalizedKey)) {
                    edges.add(
                        GraphEdge(
                            fromNodeId = fromId,
                            toNodeId = toId,
                            fromTitle = fromNote.title,
                            toTitle = toNote.title,
                            isBidirectional = isBidi
                        )
                    )
                }
            }
        }

        // Layout nodes radially / spiral initially
        val count = activeNotes.size.coerceAtLeast(1)
        val nodes = activeNotes.mapIndexed { index, note ->
            val angle = (index.toFloat() / count) * 2f * Math.PI.toFloat()
            // Stagger radius so nodes don't form a rigid single circle
            val dist = 160f + (index % 4) * 55f
            val inCount = inDegrees[note.id] ?: 0
            val outCount = outDegrees[note.id] ?: 0
            val total = inCount + outCount

            // Node radius grows with connections (hub effect)
            val radius = (20f + (total * 3.5f)).coerceIn(20f, 48f)

            GraphNode(
                id = note.id,
                title = note.title.ifBlank { "Untitled" },
                emoji = note.emoji.ifBlank { "📝" },
                colorHex = note.colorHex,
                tags = note.getTagList() + note.extractInlineTags(),
                x = cos(angle) * dist,
                y = sin(angle) * dist,
                inDegree = inCount,
                outDegree = outCount,
                totalConnections = total,
                radius = radius
            )
        }

        return GraphData(nodes = nodes, edges = edges)
    }

    /**
     * Performs one physics simulation tick with Coulomb repulsion, Hooke springs,
     * center gravitation, collision buffering, and velocity damping.
     * Returns the total kinetic energy of the system.
     */
    fun step(
        nodes: List<GraphNode>,
        edges: List<GraphEdge>,
        config: PhysicsConfig = PhysicsConfig()
    ): Float {
        if (nodes.isEmpty()) return 0f
        val nodeMap = nodes.associateBy { it.id }

        // 1. Repulsion between all node pairs
        val nodeCount = nodes.size
        for (i in 0 until nodeCount) {
            val n1 = nodes[i]
            for (j in i + 1 until nodeCount) {
                val n2 = nodes[j]
                val dx = n1.x - n2.x
                val dy = n1.y - n2.y
                val distSqr = dx * dx + dy * dy + 100f
                val dist = sqrt(distSqr)

                // Coulomb repulsion
                var force = config.repulsion / distSqr

                // Anti-overlap collision spring
                val minSeparation = n1.radius + n2.radius + 18f
                if (dist < minSeparation) {
                    val overlap = minSeparation - dist
                    force += overlap * 0.35f
                }

                val fx = (dx / dist) * force
                val fy = (dy / dist) * force

                if (!n1.isFixed) {
                    n1.vx += fx
                    n1.vy += fy
                }
                if (!n2.isFixed) {
                    n2.vx -= fx
                    n2.vy -= fy
                }
            }
        }

        // 2. Attraction along edges (Hooke's Law)
        edges.forEach { edge ->
            val from = nodeMap[edge.fromNodeId]
            val to = nodeMap[edge.toNodeId]
            if (from != null && to != null) {
                val dx = to.x - from.x
                val dy = to.y - from.y
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val targetLen = if (edge.isBidirectional) config.springLength * 0.9f else config.springLength
                val displacement = dist - targetLen
                val springMultiplier = if (edge.isBidirectional) config.springStiffness * 1.3f else config.springStiffness
                val force = displacement * springMultiplier

                val fx = (dx / dist) * force
                val fy = (dy / dist) * force

                if (!from.isFixed) {
                    from.vx += fx
                    from.vy += fy
                }
                if (!to.isFixed) {
                    to.vx -= fx
                    to.vy -= fy
                }
            }
        }

        // 3. Center gravity & velocity integration
        var totalKineticEnergy = 0f
        nodes.forEach { node ->
            if (node.isFixed) {
                node.vx = 0f
                node.vy = 0f
            } else {
                // Gentle gravitation to (0, 0)
                node.vx -= node.x * config.centerGravity
                node.vy -= node.y * config.centerGravity

                // Damping
                node.vx *= config.damping
                node.vy *= config.damping

                // Clamp velocity
                val speed = sqrt(node.vx * node.vx + node.vy * node.vy)
                if (speed > config.maxVelocity) {
                    node.vx = (node.vx / speed) * config.maxVelocity
                    node.vy = (node.vy / speed) * config.maxVelocity
                }

                node.x += node.vx
                node.y += node.vy

                totalKineticEnergy += node.vx * node.vx + node.vy * node.vy
            }
        }

        return totalKineticEnergy
    }
}
