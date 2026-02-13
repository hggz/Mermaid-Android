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
    )
) {
    companion object {
        val DEFAULT = LayoutConfig()
    }
}

// region Layout Result Types

data class PositionedNode(
    val node: FlowNode,
    val frame: RectF
)

data class PositionedEdge(
    val edge: FlowEdge,
    val fromPoint: PointF,
    val toPoint: PointF,
    val labelPosition: PointF?
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
                positioned[nodeId] = PositionedNode(node = node, frame = frame)
            }
        }

        // Position edges
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

            val labelPos = if (edge.label != null) {
                PointF(
                    (fromPoint.x + toPoint.x) / 2f,
                    (fromPoint.y + toPoint.y) / 2f - 10f
                )
            } else null

            PositionedEdge(edge = edge, fromPoint = fromPoint, toPoint = toPoint, labelPosition = labelPos)
        }

        // Calculate total size
        val maxX = positioned.values.maxOfOrNull { it.frame.right } ?: 0f
        val maxY = positioned.values.maxOfOrNull { it.frame.bottom } ?: 0f

        return FlowchartLayout(
            nodes = positioned.values.toList(),
            edges = positionedEdges,
            width = maxX + config.padding,
            height = maxY + config.padding
        )
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
}
