package com.example.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.model.GraphData
import com.example.model.GraphEdge
import com.example.model.GraphNode
import com.example.model.NoteColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GraphViewScreen(
    notes: List<NoteEntity>,
    onSelectNote: (Long) -> Unit,
    onBack: () -> Unit,
    initialFocusNoteId: Long? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var hideOrphans by remember { mutableStateOf(false) }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var isFilterDialogVisible by remember { mutableStateOf(false) }

    // Simulation toggle: running vs paused
    var isSimulating by remember { mutableStateOf(true) }

    // Canvas transformations: pan and zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Selected node state
    var selectedNodeId by remember { mutableStateOf<Long?>(initialFocusNoteId) }
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }

    // Build raw graph data from notes
    val fullGraphData = remember(notes) {
        ForceSimulation.buildGraphData(notes)
    }

    // Filtered nodes and edges according to HUD settings
    val filteredGraphData = remember(fullGraphData, hideOrphans, selectedTagFilter) {
        var visibleNodes = fullGraphData.nodes

        if (hideOrphans) {
            visibleNodes = visibleNodes.filter { it.totalConnections > 0 }
        }

        if (selectedTagFilter != null) {
            visibleNodes = visibleNodes.filter { node ->
                node.tags.any { it.equals(selectedTagFilter, ignoreCase = true) }
            }
        }

        val visibleIds = visibleNodes.map { it.id }.toSet()
        val visibleEdges = fullGraphData.edges.filter {
            visibleIds.contains(it.fromNodeId) && visibleIds.contains(it.toNodeId)
        }

        GraphData(nodes = visibleNodes, edges = visibleEdges)
    }

    // Dynamic mutable nodes for 60fps physics simulation
    val dynamicNodes = remember(filteredGraphData.nodes) {
        filteredGraphData.nodes.map { it.copy() }
    }

    // Center on initial focus note if provided
    LaunchedEffect(initialFocusNoteId, dynamicNodes) {
        if (initialFocusNoteId != null) {
            val target = dynamicNodes.find { it.id == initialFocusNoteId }
            if (target != null) {
                panX = -target.x * scale
                panY = -target.y * scale
                selectedNodeId = target.id
            }
        }
    }

    // Physics animation loop running with frame callbacks
    LaunchedEffect(isSimulating, dynamicNodes, filteredGraphData.edges) {
        if (!isSimulating) return@LaunchedEffect

        // Warm up physics initially
        repeat(30) {
            ForceSimulation.step(dynamicNodes, filteredGraphData.edges)
        }

        var lowEnergyFrames = 0
        while (isSimulating) {
            withFrameNanos {
                val energy = ForceSimulation.step(dynamicNodes, filteredGraphData.edges)
                if (draggedNodeId == null && energy < 0.1f) {
                    lowEnergyFrames++
                    if (lowEnergyFrames > 120) {
                        // Graph has settled, temporarily pause loop until user interacts
                        isSimulating = false
                    }
                } else {
                    lowEnergyFrames = 0
                }
            }
        }
    }

    // Selected node details
    val selectedNode = remember(selectedNodeId, dynamicNodes) {
        dynamicNodes.find { it.id == selectedNodeId }
    }

    // Connected neighbor IDs for highlighting
    val connectedNodeIds = remember(selectedNodeId, filteredGraphData.edges) {
        if (selectedNodeId == null) emptySet()
        else {
            val ids = mutableSetOf<Long>()
            filteredGraphData.edges.forEach { edge ->
                if (edge.fromNodeId == selectedNodeId) ids.add(edge.toNodeId)
                if (edge.toNodeId == selectedNodeId) ids.add(edge.fromNodeId)
            }
            ids
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .testTag("graph_view_screen")
    ) {
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("graph_canvas")
                // Pan & Pinch-to-zoom gestures
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.25f, 4.0f)
                        panX += pan.x
                        panY += pan.y
                    }
                }
                // Tap and Drag gestures for individual nodes and canvas
                .pointerInput(dynamicNodes, scale, panX, panY) {
                    detectTapGestures { tapOffset ->
                        val virtualX = (tapOffset.x - size.width / 2f - panX) / scale
                        val virtualY = (tapOffset.y - size.height / 2f - panY) / scale

                        val hit = dynamicNodes.find { node ->
                            val dx = node.x - virtualX
                            val dy = node.y - virtualY
                            sqrt(dx * dx + dy * dy) <= (node.radius + 18f)
                        }

                        selectedNodeId = hit?.id
                    }
                }
                .pointerInput(dynamicNodes, scale, panX, panY) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val virtualX = (startOffset.x - size.width / 2f - panX) / scale
                            val virtualY = (startOffset.y - size.height / 2f - panY) / scale

                            val hit = dynamicNodes.find { node ->
                                val dx = node.x - virtualX
                                val dy = node.y - virtualY
                                sqrt(dx * dx + dy * dy) <= (node.radius + 18f)
                            }

                            if (hit != null) {
                                draggedNodeId = hit.id
                                hit.isFixed = true
                                isSimulating = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val draggedId = draggedNodeId
                            if (draggedId != null) {
                                val node = dynamicNodes.find { it.id == draggedId }
                                if (node != null) {
                                    node.x += dragAmount.x / scale
                                    node.y += dragAmount.y / scale
                                    isSimulating = true
                                }
                            } else {
                                // Background pan drag
                                panX += dragAmount.x
                                panY += dragAmount.y
                            }
                        },
                        onDragEnd = {
                            val draggedId = draggedNodeId
                            if (draggedId != null) {
                                val node = dynamicNodes.find { it.id == draggedId }
                                node?.isFixed = false
                                draggedNodeId = null
                            }
                        },
                        onDragCancel = {
                            val draggedId = draggedNodeId
                            if (draggedId != null) {
                                val node = dynamicNodes.find { it.id == draggedId }
                                node?.isFixed = false
                                draggedNodeId = null
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f + panX, size.height / 2f + panY)

            // Draw Obsidian-style subtle dot grid
            drawDotGrid(scale, panX, panY)

            // 1. Draw Edges (Bidirectional links & Citations)
            val nodeMap = dynamicNodes.associateBy { it.id }
            val hasSelection = selectedNodeId != null

            filteredGraphData.edges.forEach { edge ->
                val from = nodeMap[edge.fromNodeId]
                val to = nodeMap[edge.toNodeId]
                if (from != null && to != null) {
                    val p1 = Offset(center.x + from.x * scale, center.y + from.y * scale)
                    val p2 = Offset(center.x + to.x * scale, center.y + to.y * scale)

                    val isConnectedToSelected = selectedNodeId == from.id || selectedNodeId == to.id

                    val edgeColor = when {
                        isConnectedToSelected -> if (edge.isBidirectional) Color(0xFF34D399) else Color(0xFF818CF8)
                        hasSelection -> Color(0x1594A3B8) // Dimmed when focusing another node
                        edge.isBidirectional -> Color(0x5534D399) // Distinct green tint for mutual backlinks
                        else -> Color(0x3564748B) // Default obsidian link
                    }

                    val strokeWidth = when {
                        isConnectedToSelected -> 3.5f * scale
                        edge.isBidirectional -> 2.2f * scale
                        else -> 1.5f * scale
                    }.coerceIn(1f, 8f)

                    // Draw connection line
                    drawLine(
                        color = edgeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidth
                    )

                    // Draw link direction indicator (arrowhead towards target node)
                    if (scale >= 0.55f) {
                        drawDirectionalArrow(
                            from = p1,
                            to = p2,
                            targetRadius = to.radius * scale,
                            color = edgeColor,
                            scale = scale,
                            isBidirectional = edge.isBidirectional
                        )
                    }
                }
            }

            // 2. Draw Nodes
            dynamicNodes.forEach { node ->
                val p = Offset(center.x + node.x * scale, center.y + node.y * scale)
                val isSelected = selectedNodeId == node.id
                val isConnected = connectedNodeIds.contains(node.id)
                val matchesSearch = searchQuery.isNotBlank() && node.title.contains(searchQuery, ignoreCase = true)
                val isDimmed = hasSelection && !isSelected && !isConnected

                val baseColor = NoteColors.getAccentForHex(node.colorHex)
                val nodeAlpha = if (isDimmed) 0.22f else 1f

                // Outer Halo Glow for selected node or search match
                if (isSelected || matchesSearch) {
                    val haloColor = if (matchesSearch) Color(0xFFF59E0B) else Color(0xFF818CF8)
                    drawCircle(
                        color = haloColor.copy(alpha = 0.25f),
                        radius = (node.radius + 14f) * scale,
                        center = p
                    )
                    drawCircle(
                        color = haloColor.copy(alpha = 0.45f),
                        radius = (node.radius + 6f) * scale,
                        center = p
                    )
                }

                // Core Node Circle
                drawCircle(
                    color = (if (matchesSearch) Color(0xFFF59E0B) else baseColor).copy(alpha = nodeAlpha),
                    radius = node.radius * scale,
                    center = p
                )

                // High-contrast Node Border
                val borderColor = when {
                    isSelected -> Color.White
                    isConnected -> Color(0xFF818CF8)
                    matchesSearch -> Color(0xFFFDE68A)
                    else -> Color.White.copy(alpha = if (isDimmed) 0.15f else 0.45f)
                }
                drawCircle(
                    color = borderColor,
                    radius = node.radius * scale,
                    center = p,
                    style = Stroke(width = (if (isSelected) 2.5f else 1.5f) * scale)
                )

                // Emoji inside node
                if (scale >= 0.65f && !isDimmed) {
                    drawContext.canvas.nativeCanvas.apply {
                        val emojiPaint = android.graphics.Paint().apply {
                            textSize = (node.radius * 0.95f * scale).coerceIn(12f, 32f)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        // Center vertically in node circle
                        val fontMetrics = emojiPaint.fontMetrics
                        val baselineOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
                        drawText(node.emoji, p.x, p.y - baselineOffset, emojiPaint)
                    }
                }

                // Node Title Label
                val shouldShowLabel = scale >= 0.75f || isSelected || isConnected || matchesSearch
                if (shouldShowLabel && !isDimmed) {
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = (13f * scale).coerceIn(11f, 22f)
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                            )
                        }

                        val displayTitle = if (node.title.length > 20) node.title.take(18) + "…" else node.title
                        val textY = p.y + (node.radius + 16f) * scale

                        // Draw neat dark background pill for label
                        val textWidth = textPaint.measureText(displayTitle)
                        val pillPaddingH = 8f * scale
                        val pillPaddingV = 4f * scale
                        val bgPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(210, 15, 23, 42)
                            isAntiAlias = true
                        }
                        val pillRect = android.graphics.RectF(
                            p.x - textWidth / 2f - pillPaddingH,
                            textY + textPaint.fontMetrics.ascent - pillPaddingV,
                            p.x + textWidth / 2f + pillPaddingH,
                            textY + textPaint.fontMetrics.descent + pillPaddingV
                        )
                        drawRoundRect(pillRect, 8f * scale, 8f * scale, bgPaint)

                        // Draw label text
                        drawText(displayTitle, p.x, textY, textPaint)
                    }
                }
            }
        }

        // Top Control Overlay
        Surface(
            color = Color(0xFF131B2E).copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF263352)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("close_graph_view")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Graph View",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${filteredGraphData.nodes.size} notes • ${filteredGraphData.edges.size} links",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Play/Pause simulation toggle
                        IconButton(
                            onClick = { isSimulating = !isSimulating },
                            modifier = Modifier.testTag("toggle_physics_simulation")
                        ) {
                            Icon(
                                imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isSimulating) "Pause Physics" else "Resume Physics",
                                tint = if (isSimulating) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }

                        // Filter Menu
                        IconButton(
                            onClick = { isFilterDialogVisible = !isFilterDialogVisible },
                            modifier = Modifier.testTag("graph_filter_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters",
                                tint = if (hideOrphans || selectedTagFilter != null) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        // Search
                        IconButton(
                            onClick = { isSearchVisible = !isSearchVisible },
                            modifier = Modifier.testTag("graph_search_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search nodes",
                                tint = if (searchQuery.isNotEmpty()) Color(0xFFF59E0B) else Color.White
                            )
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search nodes in graph...", color = Color(0xFF64748B)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("graph_search_input")
                    )
                }

                // Expandable Filter Panel
                AnimatedVisibility(visible = isFilterDialogVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hide orphan notes (0 links)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Switch(
                                checked = hideOrphans,
                                onCheckedChange = { hideOrphans = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF818CF8),
                                    checkedTrackColor = Color(0xFF312E81)
                                ),
                                modifier = Modifier.testTag("hide_orphans_switch")
                            )
                        }

                        // All Tags in Graph
                        val allTags = remember(fullGraphData.nodes) {
                            fullGraphData.nodes.flatMap { it.tags }.distinct().sorted()
                        }

                        if (allTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Filter by Tag:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTagFilter == null,
                                    onClick = { selectedTagFilter = null },
                                    label = { Text("All Notes") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF818CF8),
                                        selectedLabelColor = Color.White
                                    )
                                )
                                allTags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedTagFilter == tag,
                                        onClick = {
                                            selectedTagFilter = if (selectedTagFilter == tag) null else tag
                                        },
                                        label = { Text("#$tag") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF818CF8),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Zoom and Reset Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedNode != null) 180.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale * 1.3f).coerceAtMost(4f) },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = { scale = (scale / 1.3f).coerceAtLeast(0.25f) },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = {
                    scale = 1f
                    panX = 0f
                    panY = 0f
                    selectedNodeId = null
                    isSimulating = true
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("reset_graph_zoom")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center View", modifier = Modifier.size(20.dp))
            }
        }

        // Bottom Selected Node Detail Card
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            selectedNode?.let { node ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_node_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF334155), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = node.emoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = node.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${node.inDegree} backlinks • ${node.outDegree} outgoing links",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedNodeId = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                            }
                        }

                        // Connected Notes Quick Chips
                        val connectedNotes = remember(node.id, filteredGraphData.edges, dynamicNodes) {
                            val nodeMap = dynamicNodes.associateBy { it.id }
                            val neighbors = mutableListOf<GraphNode>()
                            filteredGraphData.edges.forEach { edge ->
                                if (edge.fromNodeId == node.id) {
                                    nodeMap[edge.toNodeId]?.let { neighbors.add(it) }
                                } else if (edge.toNodeId == node.id) {
                                    nodeMap[edge.fromNodeId]?.let { neighbors.add(it) }
                                }
                            }
                            neighbors.distinctBy { it.id }
                        }

                        if (connectedNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Connected Notes:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                connectedNotes.forEach { neighbor ->
                                    Surface(
                                        color = Color(0xFF334155),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable {
                                            selectedNodeId = neighbor.id
                                            panX = -neighbor.x * scale
                                            panY = -neighbor.y * scale
                                            isSimulating = true
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(neighbor.emoji, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                neighbor.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Open Note Action
                        Surface(
                            onClick = { onSelectNote(node.id) },
                            color = Color(0xFF6366F1),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("open_note_from_graph")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Open Note in Editor",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a subtle dot grid on the canvas that moves with pan & zoom
 */
private fun DrawScope.drawDotGrid(scale: Float, panX: Float, panY: Float) {
    val step = (45f * scale).coerceAtLeast(20f)
    val startX = (panX % step)
    val startY = (panY % step)

    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(
                color = Color(0x15FFFFFF),
                radius = 1.3f,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}

/**
 * Draws a directional arrow on edge line from source to target
 */
private fun DrawScope.drawDirectionalArrow(
    from: Offset,
    to: Offset,
    targetRadius: Float,
    color: Color,
    scale: Float,
    isBidirectional: Boolean
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist <= targetRadius + 10f) return

    val angle = atan2(dy, dx)
    // Place arrow tip just before target node boundary
    val tipDistance = dist - targetRadius - 4f
    val tipX = from.x + cos(angle) * tipDistance
    val tipY = from.y + sin(angle) * tipDistance

    val arrowLength = (9f * scale).coerceIn(6f, 16f)
    val arrowAngle = Math.PI / 6 // 30 degrees

    val leftX = tipX - cos(angle - arrowAngle).toFloat() * arrowLength
    val leftY = tipY - sin(angle - arrowAngle).toFloat() * arrowLength

    val rightX = tipX - cos(angle + arrowAngle).toFloat() * arrowLength
    val rightY = tipY - sin(angle + arrowAngle).toFloat() * arrowLength

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }

    drawPath(path = path, color = color)

    // For bidirectional links, also draw arrow pointing back towards source
    if (isBidirectional) {
        val srcTipDistance = targetRadius + 4f
        val srcTipX = from.x + cos(angle) * srcTipDistance
        val srcTipY = from.y + sin(angle) * srcTipDistance

        val srcLeftX = srcTipX + cos(angle - arrowAngle).toFloat() * arrowLength
        val srcLeftY = srcTipY + sin(angle - arrowAngle).toFloat() * arrowLength

        val srcRightX = srcTipX + cos(angle + arrowAngle).toFloat() * arrowLength
        val srcRightY = srcTipY + sin(angle + arrowAngle).toFloat() * arrowLength

        val srcPath = Path().apply {
            moveTo(srcTipX, srcTipY)
            lineTo(srcLeftX, srcLeftY)
            lineTo(srcRightX, srcRightY)
            close()
        }
        drawPath(path = srcPath, color = color)
    }
}
