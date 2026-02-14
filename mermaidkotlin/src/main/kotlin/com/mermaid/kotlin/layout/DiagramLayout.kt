package com.mermaid.kotlin.layout

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import com.mermaid.kotlin.model.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Layout configuration for diagram rendering.
 */
data class LayoutConfig(
    // General
    val padding: Float = 40f,
    val backgroundColor: Int = Color.WHITE,

    // Fonts
    val fontSize: Float = 14f,
    val titleFontSize: Float = 18f,
    val fontFamily: String = "sans-serif",

    // Colors
    val nodeColor: Int = Color.rgb(217, 235, 255),
    val nodeBorderColor: Int = Color.rgb(51, 102, 179),
    val edgeColor: Int = Color.rgb(77, 77, 77),
    val textColor: Int = Color.rgb(26, 26, 26),
    val arrowColor: Int = Color.rgb(77, 77, 77),

    // Flowchart
    val nodeWidth: Float = 150f,
    val nodeHeight: Float = 50f,
    val nodeCornerRadius: Float = 8f,
    val horizontalSpacing: Float = 60f,
    val verticalSpacing: Float = 60f,
    val lineWidth: Float = 2f,

    // Sequence diagram
    val participantWidth: Float = 120f,
    val participantHeight: Float = 40f,
    val participantSpacing: Float = 40f,
    val messageSpacing: Float = 50f,
    val lifelineColor: Int = Color.rgb(179, 179, 179),

    // Pie chart
    val pieRadius: Float = 120f,
    val pieLabelOffset: Float = 30f,
    val pieColors: List<Int> = listOf(
        Color.rgb(66, 133, 244),    // blue
        Color.rgb(234, 87, 87),     // red
        Color.rgb(77, 176, 80),     // green
        Color.rgb(255, 194, 8),     // yellow
        Color.rgb(156, 89, 182),    // purple
        Color.rgb(255, 153, 0),     // orange
        Color.rgb(0, 189, 212),     // teal
        Color.rgb(232, 120, 158),   // pink
    ),

    // Subgraph
    val subgraphPadding: Float = 20f,
    val subgraphLabelHeight: Float = 24f,
    val subgraphBorderColor: Int = Color.rgb(153, 153, 153),
    val subgraphFillColor: Int = Color.argb(128, 245, 247, 250),

    // Class diagram
    val classBoxWidth: Float = 200f,
    val classHeaderHeight: Float = 35f,
    val classMemberHeight: Float = 22f,
    val classSpacing: Float = 80f,

    // State diagram
    val stateWidth: Float = 140f,
    val stateHeight: Float = 45f,
    val stateCornerRadius: Float = 12f,
    val stateSpacing: Float = 70f,
    val startEndRadius: Float = 12f,

    // Gantt chart
    val ganttBarHeight: Float = 28f,
    val ganttBarSpacing: Float = 8f,
    val ganttSectionSpacing: Float = 12f,
    val ganttLabelWidth: Float = 180f,
    val ganttDayWidth: Float = 30f,
    val ganttColors: List<Int> = listOf(
        Color.rgb(66, 133, 244),
        Color.rgb(77, 176, 80),
        Color.rgb(234, 87, 87),
        Color.rgb(255, 194, 8),
    ),
    val ganttCriticalColor: Int = Color.rgb(234, 87, 87),
    val ganttDoneColor: Int = Color.rgb(179, 179, 179),
    val ganttActiveColor: Int = Color.rgb(66, 133, 244),

    // ER diagram
    val erEntityWidth: Float = 180f,
    val erEntityHeaderHeight: Float = 32f,
    val erAttributeHeight: Float = 22f,
    val erEntitySpacing: Float = 100f,
) {
    companion object {
        val DEFAULT = LayoutConfig()

        val DARK_MODE: LayoutConfig
            get() = LayoutConfig(
                backgroundColor = Color.rgb(31, 31, 36),
                textColor = Color.rgb(230, 230, 235),
                nodeColor = Color.rgb(51, 64, 89),
                nodeBorderColor = Color.rgb(102, 140, 204),
                edgeColor = Color.rgb(179, 179, 179),
                arrowColor = Color.rgb(179, 179, 179),
                lifelineColor = Color.rgb(128, 128, 128),
                subgraphBorderColor = Color.rgb(115, 128, 140),
                subgraphFillColor = Color.argb(153, 46, 51, 61),
            )
    }
}

// region Layout Result Types

data class PositionedNode(
    val node: FlowNode,
    val frame: RectF,
    val style: NodeStyle? = null
)

data class PositionedEdge(
    val edge: FlowEdge,
    val points: List<PointF>,  // Multi-point path for edge routing
    val labelPosition: PointF?
)

data class PositionedSubgraph(
    val subgraph: Subgraph,
    val frame: RectF,
    val labelPosition: PointF
)

data class PositionedParticipant(
    val participant: Participant,
    val headerFrame: RectF,
    val lifelineX: Float,
    val lifelineTop: Float,
    val lifelineBottom: Float
)

data class PositionedMessage(
    val message: Message,
    val fromX: Float,
    val toX: Float,
    val y: Float
)

data class PositionedPieSlice(
    val slice: PieSlice,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Int,
    val labelPosition: PointF,
    val percentage: Float
)

data class PositionedClassBox(
    val classDef: ClassDefinition,
    val frame: RectF,
    val headerFrame: RectF,
    val propertiesFrame: RectF,
    val methodsFrame: RectF
)

data class PositionedClassRelationship(
    val relationship: ClassRelationship,
    val fromPoint: PointF,
    val toPoint: PointF,
    val fromLabelPos: PointF?,
    val toLabelPos: PointF?,
    val labelPos: PointF?
)

data class PositionedState(
    val state: StateNode,
    val frame: RectF,
    val isStartEnd: Boolean
)

data class PositionedStateTransition(
    val transition: StateTransition,
    val points: List<PointF>,
    val labelPosition: PointF?
)

data class PositionedGanttTask(
    val task: GanttTask,
    val bar: RectF,
    val labelPosition: PointF,
    val color: Int
)

data class PositionedGanttSection(
    val name: String,
    val y: Float
)

data class PositionedEREntity(
    val entity: EREntity,
    val frame: RectF,
    val headerFrame: RectF,
    val attributeFrames: List<RectF>
)

data class PositionedERRelationship(
    val relationship: ERRelationship,
    val fromPoint: PointF,
    val toPoint: PointF,
    val labelPosition: PointF
)

// endregion

/**
 * Computes layout positions for diagram elements.
 */
class DiagramLayout(
    val config: LayoutConfig = LayoutConfig.DEFAULT
) {

    // region Flowchart Layout

    data class FlowchartLayout(
        val nodes: List<PositionedNode>,
        val edges: List<PositionedEdge>,
        val subgraphs: List<PositionedSubgraph>,
        val width: Float,
        val height: Float
    )

    fun layoutFlowchart(diagram: FlowchartDiagram): FlowchartLayout {
        val orderedIds = topologicalSort(diagram)
        val layers = assignLayers(orderedIds, diagram.edges)
        val nodeMap = diagram.nodes.associateBy { it.id }

        val isVertical = diagram.direction == FlowDirection.TOP_TO_BOTTOM ||
                diagram.direction == FlowDirection.TOP_DOWN ||
                diagram.direction == FlowDirection.BOTTOM_TO_TOP

        val positioned = mutableMapOf<String, PositionedNode>()

        for ((layerIndex, layer) in layers.withIndex()) {
            for ((nodeIndex, nodeId) in layer.withIndex()) {
                val node = nodeMap[nodeId] ?: continue

                val x: Float
                val y: Float

                if (isVertical) {
                    x = config.padding + nodeIndex * (config.nodeWidth + config.horizontalSpacing)
                    y = config.padding + layerIndex * (config.nodeHeight + config.verticalSpacing)
                } else {
                    x = config.padding + layerIndex * (config.nodeWidth + config.horizontalSpacing)
                    y = config.padding + nodeIndex * (config.nodeHeight + config.verticalSpacing)
                }

                val frame = RectF(x, y, x + config.nodeWidth, y + config.nodeHeight)
                val style = resolveNodeStyle(nodeId, diagram)
                positioned[nodeId] = PositionedNode(node = node, frame = frame, style = style)
            }
        }

        // Layout subgraphs (bounding boxes around their nodes)
        val positionedSubgraphs = diagram.subgraphs.mapNotNull { sg ->
            val nodeFrames = sg.nodeIds.mapNotNull { positioned[it]?.frame }
            if (nodeFrames.isEmpty()) return@mapNotNull null

            val minX = nodeFrames.minOf { it.left } - config.subgraphPadding
            val minY = nodeFrames.minOf { it.top } - config.subgraphPadding - config.subgraphLabelHeight
            val maxX = nodeFrames.maxOf { it.right } + config.subgraphPadding
            val maxY = nodeFrames.maxOf { it.bottom } + config.subgraphPadding

            val frame = RectF(minX, minY, maxX, maxY)
            val labelPos = PointF(minX + config.subgraphPadding, minY + config.subgraphLabelHeight / 2f + 4f)

            PositionedSubgraph(subgraph = sg, frame = frame, labelPosition = labelPos)
        }

        // Edge routing with obstacle avoidance
        val allNodeFrames = positioned.values.map { it.frame }
        val positionedEdges = diagram.edges.mapNotNull { edge ->
            val fromNode = positioned[edge.from] ?: return@mapNotNull null
            val toNode = positioned[edge.to] ?: return@mapNotNull null

            val fromPoint: PointF
            val toPoint: PointF

            if (isVertical) {
                fromPoint = PointF(fromNode.frame.centerX(), fromNode.frame.bottom)
                toPoint = PointF(toNode.frame.centerX(), toNode.frame.top)
            } else {
                fromPoint = PointF(fromNode.frame.right, fromNode.frame.centerY())
                toPoint = PointF(toNode.frame.left, toNode.frame.centerY())
            }

            val routedPoints = routeEdge(
                from = fromPoint, to = toPoint,
                obstacles = allNodeFrames,
                excludeIds = listOf(edge.from, edge.to),
                nodeFrames = positioned,
                isVertical = isVertical
            )

            val labelPos = if (edge.label != null) {
                PointF(
                    (fromPoint.x + toPoint.x) / 2f,
                    (fromPoint.y + toPoint.y) / 2f - 10f
                )
            } else null

            PositionedEdge(edge = edge, points = routedPoints, labelPosition = labelPos)
        }

        // Calculate total size, accounting for subgraphs
        val allFrames = positioned.values.map { it.frame } + positionedSubgraphs.map { it.frame }
        val maxX = (allFrames.maxOfOrNull { it.right } ?: 0f) + config.padding
        val maxY = (allFrames.maxOfOrNull { it.bottom } ?: 0f) + config.padding

        return FlowchartLayout(
            nodes = positioned.values.toList(),
            edges = positionedEdges,
            subgraphs = positionedSubgraphs,
            width = maxX,
            height = maxY
        )
    }

    private fun resolveNodeStyle(nodeId: String, diagram: FlowchartDiagram): NodeStyle? {
        val className = diagram.nodeClassMap[nodeId] ?: return null
        return diagram.classDefs[className]
    }

    // Edge Routing

    private fun routeEdge(
        from: PointF, to: PointF,
        obstacles: List<RectF>,
        excludeIds: List<String>,
        nodeFrames: Map<String, PositionedNode>,
        isVertical: Boolean
    ): List<PointF> {
        val excludeFrames = excludeIds.mapNotNull { nodeFrames[it]?.frame }
        val intermediateObstacles = obstacles.filter { rect ->
            !excludeFrames.any { it == rect } &&
            lineIntersectsRect(from, to, RectF(rect.left - 4, rect.top - 4, rect.right + 4, rect.bottom + 4))
        }

        if (intermediateObstacles.isEmpty()) {
            return listOf(from, to)
        }

        val offset = 20f

        return if (isVertical) {
            val obstacleMinX = intermediateObstacles.minOf { it.left }
            val obstacleMaxX = intermediateObstacles.maxOf { it.right }
            val bendX = if (Math.abs(from.x - obstacleMinX) < Math.abs(from.x - obstacleMaxX)) {
                obstacleMinX - offset
            } else {
                obstacleMaxX + offset
            }
            listOf(from, PointF(bendX, from.y), PointF(bendX, to.y), to)
        } else {
            val obstacleMinY = intermediateObstacles.minOf { it.top }
            val obstacleMaxY = intermediateObstacles.maxOf { it.bottom }
            val bendY = if (Math.abs(from.y - obstacleMinY) < Math.abs(from.y - obstacleMaxY)) {
                obstacleMinY - offset
            } else {
                obstacleMaxY + offset
            }
            listOf(from, PointF(from.x, bendY), PointF(to.x, bendY), to)
        }
    }

    private fun lineIntersectsRect(from: PointF, to: PointF, rect: RectF): Boolean {
        val dx = to.x - from.x
        val dy = to.y - from.y

        val edges = listOf(
            Pair(-dx, from.x - rect.left),
            Pair(dx, rect.right - from.x),
            Pair(-dy, from.y - rect.top),
            Pair(dy, rect.bottom - from.y),
        )

        var tMin = 0f
        var tMax = 1f

        for ((p, q) in edges) {
            if (Math.abs(p) < 0.001f) {
                if (q < 0) return false
            } else {
                val t = q / p
                if (p < 0) {
                    tMin = max(tMin, t)
                } else {
                    tMax = min(tMax, t)
                }
                if (tMin > tMax) return false
            }
        }

        return true
    }

    private fun topologicalSort(diagram: FlowchartDiagram): List<String> {
        val allIds = diagram.nodes.map { it.id }.toMutableSet()
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        allIds.forEach { inDegree[it] = 0 }

        for (edge in diagram.edges) {
            adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
            inDegree[edge.to] = (inDegree[edge.to] ?: 0) + 1
        }

        val queue = ArrayDeque(allIds.filter { (inDegree[it] ?: 0) == 0 }.sorted())
        val result = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)

            for (neighbor in adjacency[node] ?: emptyList()) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 0) - 1
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }

        // Add remaining (cycles)
        val remaining = allIds.minus(result.toSet()).sorted()
        result.addAll(remaining)

        return result
    }

    private fun assignLayers(sortedIds: List<String>, edges: List<FlowEdge>): List<List<String>> {
        val layerOf = mutableMapOf<String, Int>()

        for (id in sortedIds) {
            val incomingLayers = edges
                .filter { it.to == id }
                .mapNotNull { layerOf[it.from] }
            val layer = (incomingLayers.maxOrNull() ?: -1) + 1
            layerOf[id] = layer
        }

        val maxLayer = layerOf.values.maxOrNull() ?: 0
        val layers = MutableList(maxLayer + 1) { mutableListOf<String>() }
        for ((id, layer) in layerOf) {
            layers[layer].add(id)
        }

        // Sort within layers for stability
        for (layer in layers) {
            layer.sort()
        }

        return layers
    }

    // endregion

    // region Sequence Diagram Layout

    data class SequenceLayout(
        val participants: List<PositionedParticipant>,
        val messages: List<PositionedMessage>,
        val width: Float,
        val height: Float
    )

    fun layoutSequenceDiagram(diagram: SequenceDiagram): SequenceLayout {
        val totalWidth = config.padding * 2 +
                diagram.participants.size * config.participantWidth +
                max(0, diagram.participants.size - 1) * config.participantSpacing

        val headerY = config.padding
        val lifelineTop = headerY + config.participantHeight + 20f
        val lifelineBottom = lifelineTop + diagram.messages.size * config.messageSpacing + 40f

        val participantPositions = mutableListOf<PositionedParticipant>()
        val participantXMap = mutableMapOf<String, Float>()

        for ((i, p) in diagram.participants.withIndex()) {
            val x = config.padding + i * (config.participantWidth + config.participantSpacing)
            val centerX = x + config.participantWidth / 2f
            val headerFrame = RectF(x, headerY, x + config.participantWidth, headerY + config.participantHeight)

            participantPositions.add(
                PositionedParticipant(
                    participant = p,
                    headerFrame = headerFrame,
                    lifelineX = centerX,
                    lifelineTop = lifelineTop,
                    lifelineBottom = lifelineBottom
                )
            )
            participantXMap[p.id] = centerX
        }

        val messagePositions = diagram.messages.mapIndexed { i, msg ->
            val y = lifelineTop + (i + 1) * config.messageSpacing
            PositionedMessage(
                message = msg,
                fromX = participantXMap[msg.from] ?: 0f,
                toX = participantXMap[msg.to] ?: 0f,
                y = y
            )
        }

        val totalHeight = lifelineBottom + config.padding

        return SequenceLayout(
            participants = participantPositions,
            messages = messagePositions,
            width = totalWidth,
            height = totalHeight
        )
    }

    // endregion

    // region Pie Chart Layout

    data class PieLayout(
        val slices: List<PositionedPieSlice>,
        val center: PointF,
        val radius: Float,
        val title: String?,
        val titlePosition: PointF,
        val width: Float,
        val height: Float
    )

    fun layoutPieChart(diagram: PieChartDiagram): PieLayout {
        val total = diagram.slices.sumOf { it.value }
        if (total <= 0.0) {
            return PieLayout(
                slices = emptyList(),
                center = PointF(0f, 0f),
                radius = 0f,
                title = diagram.title,
                titlePosition = PointF(0f, 0f),
                width = 100f,
                height = 100f
            )
        }

        val legendWidth = 180f
        val canvasWidth = config.padding * 2 + config.pieRadius * 2 + legendWidth
        val canvasHeight = config.padding * 2 + config.pieRadius * 2 + (if (diagram.title != null) 30f else 0f)
        val titleY = config.padding + 10f
        val centerY = (if (diagram.title != null) titleY + 30f else config.padding) + config.pieRadius
        val centerX = config.padding + config.pieRadius
        val center = PointF(centerX, centerY)

        val slices = mutableListOf<PositionedPieSlice>()
        // Android uses degrees, start at top (-90)
        var currentAngle = -90f

        for ((i, slice) in diagram.slices.withIndex()) {
            val fraction = (slice.value / total).toFloat()
            val sweepAngle = fraction * 360f
            val midAngleDeg = currentAngle + sweepAngle / 2f
            val midAngleRad = Math.toRadians(midAngleDeg.toDouble())

            val labelR = config.pieRadius + config.pieLabelOffset
            val labelPos = PointF(
                center.x + labelR * cos(midAngleRad).toFloat(),
                center.y + labelR * sin(midAngleRad).toFloat()
            )

            slices.add(
                PositionedPieSlice(
                    slice = slice,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    color = config.pieColors[i % config.pieColors.size],
                    labelPosition = labelPos,
                    percentage = fraction * 100f
                )
            )

            currentAngle += sweepAngle
        }

        return PieLayout(
            slices = slices,
            center = center,
            radius = config.pieRadius,
            title = diagram.title,
            titlePosition = PointF(centerX, titleY),
            width = canvasWidth,
            height = canvasHeight
        )
    }

    // endregion

    // region Class Diagram Layout

    data class ClassDiagramLayout(
        val classes: List<PositionedClassBox>,
        val relationships: List<PositionedClassRelationship>,
        val width: Float,
        val height: Float
    )

    fun layoutClassDiagram(diagram: ClassDiagram): ClassDiagramLayout {
        val positioned = mutableMapOf<String, PositionedClassBox>()

        val cols = max(1, Math.ceil(Math.sqrt(diagram.classes.size.toDouble())).toInt())

        for ((i, cls) in diagram.classes.withIndex()) {
            val col = i % cols
            val row = i / cols

            val propCount = max(cls.properties.size, 1)
            val methCount = max(cls.methods.size, 1)
            val totalHeight = config.classHeaderHeight +
                propCount * config.classMemberHeight +
                methCount * config.classMemberHeight + 4f

            val x = config.padding + col * (config.classBoxWidth + config.classSpacing)
            val y = config.padding + row * (totalHeight + config.classSpacing)

            val frame = RectF(x, y, x + config.classBoxWidth, y + totalHeight)
            val headerFrame = RectF(x, y, x + config.classBoxWidth, y + config.classHeaderHeight)
            val propsY = y + config.classHeaderHeight
            val propsHeight = propCount * config.classMemberHeight
            val propsFrame = RectF(x, propsY, x + config.classBoxWidth, propsY + propsHeight)
            val methsY = propsY + propsHeight
            val methsHeight = methCount * config.classMemberHeight
            val methsFrame = RectF(x, methsY, x + config.classBoxWidth, methsY + methsHeight)

            positioned[cls.name] = PositionedClassBox(
                classDef = cls,
                frame = frame,
                headerFrame = headerFrame,
                propertiesFrame = propsFrame,
                methodsFrame = methsFrame
            )
        }

        val positionedRels = diagram.relationships.mapNotNull { rel ->
            val fromBox = positioned[rel.from] ?: return@mapNotNull null
            val toBox = positioned[rel.to] ?: return@mapNotNull null

            val (fromPt, toPt) = connectBoxes(fromBox.frame, toBox.frame)

            val midX = (fromPt.x + toPt.x) / 2f
            val midY = (fromPt.y + toPt.y) / 2f

            val labelPos = if (rel.label != null) PointF(midX, midY - 10f) else null
            val fromLabelPos = if (rel.fromCardinality != null)
                PointF(fromPt.x + if (toPt.x > fromPt.x) 15f else -15f, fromPt.y - 12f) else null
            val toLabelPos = if (rel.toCardinality != null)
                PointF(toPt.x + if (fromPt.x > toPt.x) 15f else -15f, toPt.y - 12f) else null

            PositionedClassRelationship(
                relationship = rel,
                fromPoint = fromPt, toPoint = toPt,
                fromLabelPos = fromLabelPos, toLabelPos = toLabelPos,
                labelPos = labelPos
            )
        }

        val allFrames = positioned.values.map { it.frame }
        val maxX = (allFrames.maxOfOrNull { it.right } ?: 0f) + config.padding
        val maxY = (allFrames.maxOfOrNull { it.bottom } ?: 0f) + config.padding

        return ClassDiagramLayout(
            classes = positioned.values.toList(),
            relationships = positionedRels,
            width = maxX, height = maxY
        )
    }

    private fun connectBoxes(from: RectF, to: RectF): Pair<PointF, PointF> {
        val fromCenterX = from.centerX()
        val fromCenterY = from.centerY()
        val toCenterX = to.centerX()
        val toCenterY = to.centerY()

        val dx = toCenterX - fromCenterX
        val dy = toCenterY - fromCenterY

        return if (Math.abs(dx) > Math.abs(dy)) {
            // Connect horizontally
            Pair(
                PointF(if (dx > 0) from.right else from.left, from.centerY()),
                PointF(if (dx > 0) to.left else to.right, to.centerY())
            )
        } else {
            // Connect vertically
            Pair(
                PointF(from.centerX(), if (dy > 0) from.bottom else from.top),
                PointF(to.centerX(), if (dy > 0) to.top else to.bottom)
            )
        }
    }

    // endregion

    // region State Diagram Layout

    data class StateDiagramLayout(
        val states: List<PositionedState>,
        val transitions: List<PositionedStateTransition>,
        val width: Float,
        val height: Float
    )

    fun layoutStateDiagram(diagram: StateDiagram): StateDiagramLayout {
        val allIds = diagram.states.map { it.id }
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()
        allIds.forEach { inDegree[it] = 0 }

        for (t in diagram.transitions) {
            adjacency.getOrPut(t.from) { mutableListOf() }.add(t.to)
            inDegree[t.to] = (inDegree[t.to] ?: 0) + 1
        }

        val queue = ArrayDeque(allIds.filter { (inDegree[it] ?: 0) == 0 }.sorted())
        val sorted = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            sorted.add(node)
            for (neighbor in adjacency[node] ?: emptyList()) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 0) - 1
                if (inDegree[neighbor] == 0) queue.add(neighbor)
            }
        }
        val remaining = allIds.toSet().minus(sorted.toSet()).sorted()
        sorted.addAll(remaining)

        // Assign layers
        val layerOf = mutableMapOf<String, Int>()
        for (id in sorted) {
            val incoming = diagram.transitions.filter { it.to == id }.mapNotNull { layerOf[it.from] }
            layerOf[id] = (incoming.maxOrNull() ?: -1) + 1
        }

        val maxLayer = layerOf.values.maxOrNull() ?: 0
        val layers = MutableList(maxLayer + 1) { mutableListOf<String>() }
        for ((id, layer) in layerOf) { layers[layer].add(id) }
        for (layer in layers) { layer.sort() }

        val stateMap = diagram.states.associateBy { it.id }
        val positioned = mutableMapOf<String, PositionedState>()

        for ((layerIndex, layer) in layers.withIndex()) {
            for ((nodeIndex, nodeId) in layer.withIndex()) {
                val state = stateMap[nodeId] ?: continue
                val isStartEnd = nodeId == "[*]"
                val w = if (isStartEnd) config.startEndRadius * 2 else config.stateWidth
                val h = if (isStartEnd) config.startEndRadius * 2 else config.stateHeight

                val x = config.padding + nodeIndex * (config.stateWidth + config.stateSpacing)
                val y = config.padding + layerIndex * (config.stateHeight + config.stateSpacing)

                val frame = RectF(x, y, x + w, y + h)
                positioned[nodeId] = PositionedState(state = state, frame = frame, isStartEnd = isStartEnd)
            }
        }

        val positionedTransitions = diagram.transitions.mapNotNull { t ->
            val fromState = positioned[t.from] ?: return@mapNotNull null
            val toState = positioned[t.to] ?: return@mapNotNull null

            val fromPt = PointF(fromState.frame.centerX(), fromState.frame.bottom)
            val toPt = PointF(toState.frame.centerX(), toState.frame.top)

            val labelPos = if (t.label != null)
                PointF((fromPt.x + toPt.x) / 2f, (fromPt.y + toPt.y) / 2f - 10f) else null

            PositionedStateTransition(
                transition = t,
                points = listOf(fromPt, toPt),
                labelPosition = labelPos
            )
        }

        val allFrames = positioned.values.map { it.frame }
        val maxX = (allFrames.maxOfOrNull { it.right } ?: 0f) + config.padding
        val maxY = (allFrames.maxOfOrNull { it.bottom } ?: 0f) + config.padding

        return StateDiagramLayout(
            states = positioned.values.toList(),
            transitions = positionedTransitions,
            width = maxX, height = maxY
        )
    }

    // endregion

    // region Gantt Chart Layout

    data class GanttLayout(
        val sections: List<PositionedGanttSection>,
        val tasks: List<PositionedGanttTask>,
        val title: String?,
        val titlePosition: PointF,
        val gridLines: List<Pair<Float, String>>,
        val width: Float,
        val height: Float
    )

    fun layoutGanttChart(diagram: GanttDiagram): GanttLayout {
        val titleHeight = if (diagram.title != null) 35f else 0f
        val headerHeight = 30f
        var currentY = config.padding + titleHeight + headerHeight

        val positionedSections = mutableListOf<PositionedGanttSection>()
        val positionedTasks = mutableListOf<PositionedGanttTask>()

        val allTasks = diagram.sections.flatMap { it.tasks }
        val totalDays = max(allTasks.size * 5, 20)
        val chartWidth = config.ganttLabelWidth + totalDays * config.ganttDayWidth

        var taskIndex = 0
        for ((sectionIdx, section) in diagram.sections.withIndex()) {
            positionedSections.add(PositionedGanttSection(name = section.name, y = currentY))
            currentY += config.ganttSectionSpacing

            for (task in section.tasks) {
                val barX = config.ganttLabelWidth + taskIndex * 3 * config.ganttDayWidth
                val barWidth = 5f * config.ganttDayWidth
                val bar = RectF(barX, currentY, barX + barWidth, currentY + config.ganttBarHeight)

                val color: Int = when (task.status) {
                    GanttTask.TaskStatus.CRITICAL, GanttTask.TaskStatus.CRITICAL_ACTIVE -> config.ganttCriticalColor
                    GanttTask.TaskStatus.DONE, GanttTask.TaskStatus.CRITICAL_DONE -> config.ganttDoneColor
                    GanttTask.TaskStatus.ACTIVE -> config.ganttActiveColor
                    GanttTask.TaskStatus.NORMAL -> config.ganttColors[sectionIdx % config.ganttColors.size]
                }

                val labelPos = PointF(config.padding + 10f, currentY + config.ganttBarHeight / 2f)
                positionedTasks.add(PositionedGanttTask(
                    task = task, bar = bar, labelPosition = labelPos, color = color
                ))

                currentY += config.ganttBarHeight + config.ganttBarSpacing
                taskIndex++
            }

            currentY += config.ganttSectionSpacing
        }

        val totalHeight = currentY + config.padding
        val totalWidth = max(chartWidth, 400f) + config.padding

        val gridLines = mutableListOf<Pair<Float, String>>()
        for (i in 0..(totalDays / 5)) {
            val x = config.ganttLabelWidth + i * 5 * config.ganttDayWidth
            gridLines.add(Pair(x, "Day ${i * 5}"))
        }

        val titlePos = PointF(totalWidth / 2f, config.padding + 15f)

        return GanttLayout(
            sections = positionedSections,
            tasks = positionedTasks,
            title = diagram.title,
            titlePosition = titlePos,
            gridLines = gridLines,
            width = totalWidth, height = totalHeight
        )
    }

    // endregion

    // region ER Diagram Layout

    data class ERDiagramLayout(
        val entities: List<PositionedEREntity>,
        val relationships: List<PositionedERRelationship>,
        val width: Float,
        val height: Float
    )

    fun layoutERDiagram(diagram: ERDiagram): ERDiagramLayout {
        val positioned = mutableMapOf<String, PositionedEREntity>()

        val cols = max(1, Math.ceil(Math.sqrt(diagram.entities.size.toDouble())).toInt())

        for ((i, entity) in diagram.entities.withIndex()) {
            val col = i % cols
            val row = i / cols

            val attrCount = max(entity.attributes.size, 1)
            val totalHeight = config.erEntityHeaderHeight + attrCount * config.erAttributeHeight + 4f

            val x = config.padding + col * (config.erEntityWidth + config.erEntitySpacing)
            val y = config.padding + row * (totalHeight + config.erEntitySpacing)

            val frame = RectF(x, y, x + config.erEntityWidth, y + totalHeight)
            val headerFrame = RectF(x, y, x + config.erEntityWidth, y + config.erEntityHeaderHeight)

            val attrFrames = mutableListOf<RectF>()
            for (j in 0 until entity.attributes.size) {
                val attrY = y + config.erEntityHeaderHeight + j * config.erAttributeHeight
                attrFrames.add(RectF(x, attrY, x + config.erEntityWidth, attrY + config.erAttributeHeight))
            }

            positioned[entity.name] = PositionedEREntity(
                entity = entity,
                frame = frame,
                headerFrame = headerFrame,
                attributeFrames = attrFrames
            )
        }

        val positionedRels = diagram.relationships.mapNotNull { rel ->
            val fromEntity = positioned[rel.from] ?: return@mapNotNull null
            val toEntity = positioned[rel.to] ?: return@mapNotNull null

            val (fromPt, toPt) = connectBoxes(fromEntity.frame, toEntity.frame)
            val labelPos = PointF((fromPt.x + toPt.x) / 2f, (fromPt.y + toPt.y) / 2f - 10f)

            PositionedERRelationship(
                relationship = rel,
                fromPoint = fromPt, toPoint = toPt,
                labelPosition = labelPos
            )
        }

        val allFrames = positioned.values.map { it.frame }
        val maxX = (allFrames.maxOfOrNull { it.right } ?: 0f) + config.padding
        val maxY = (allFrames.maxOfOrNull { it.bottom } ?: 0f) + config.padding

        return ERDiagramLayout(
            entities = positioned.values.toList(),
            relationships = positionedRels,
            width = maxX, height = maxY
        )
    }

    // endregion
}
