package com.example.model

data class GraphNode(
    val id: Long,
    val title: String,
    val emoji: String,
    val colorHex: String,
    val tags: List<String> = emptyList(),
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isFixed: Boolean = false,
    val inDegree: Int = 0,
    val outDegree: Int = 0,
    val totalConnections: Int = 0,
    val radius: Float = 24f
)

data class GraphEdge(
    val fromNodeId: Long,
    val toNodeId: Long,
    val fromTitle: String,
    val toTitle: String,
    val isBidirectional: Boolean = false
)

data class GraphData(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)
