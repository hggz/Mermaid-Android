package com.mermaid.kotlin.model

/**
 * A parsed diagram that can be laid out and rendered.
 */
interface Diagram {
    val type: DiagramType
}

enum class DiagramType {
    FLOWCHART,
    SEQUENCE_DIAGRAM,
    PIE,
    UNKNOWN
}

// region Flowchart

data class FlowchartDiagram(
    val direction: FlowDirection,
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>
) : Diagram {
    override val type = DiagramType.FLOWCHART
}

enum class FlowDirection(val value: String) {
    TOP_TO_BOTTOM("TD"),
    TOP_DOWN("TB"),
    BOTTOM_TO_TOP("BT"),
    LEFT_TO_RIGHT("LR"),
    RIGHT_TO_LEFT("RL");

    companion object {
        fun fromString(s: String): FlowDirection =
            entries.firstOrNull { it.value == s } ?: TOP_TO_BOTTOM
    }
}

data class FlowNode(
    val id: String,
    val label: String,
    val shape: NodeShape
)

enum class NodeShape {
    RECTANGLE,       // [text]
    ROUNDED_RECT,    // (text)
    STADIUM,         // ([text])
    DIAMOND,         // {text}
    HEXAGON,         // {{text}}
    CIRCLE,          // ((text))
    ASYMMETRIC       // >text]
}

data class FlowEdge(
    val from: String,
    val to: String,
    val label: String? = null,
    val style: EdgeStyle = EdgeStyle.SOLID
)

enum class EdgeStyle {
    SOLID,       // -->
    DOTTED,      // -.->
    THICK,       // ==>
    INVISIBLE    // ~~~
}

// endregion

// region Sequence Diagram

data class SequenceDiagram(
    val participants: List<Participant>,
    val messages: List<Message>
) : Diagram {
    override val type = DiagramType.SEQUENCE_DIAGRAM
}

data class Participant(
    val id: String,
    val label: String
)

data class Message(
    val from: String,
    val to: String,
    val text: String,
    val style: MessageStyle
)

enum class MessageStyle {
    SOLID_ARROW,     // ->>
    DOTTED_ARROW,    // -->>
    SOLID_LINE,      // ->
    DOTTED_LINE,     // -->
    SOLID_CROSS,     // -x
    DOTTED_CROSS     // --x
}

// endregion

// region Pie Chart

data class PieChartDiagram(
    val title: String? = null,
    val slices: List<PieSlice>
) : Diagram {
    override val type = DiagramType.PIE
}

data class PieSlice(
    val label: String,
    val value: Double
)

// endregion
