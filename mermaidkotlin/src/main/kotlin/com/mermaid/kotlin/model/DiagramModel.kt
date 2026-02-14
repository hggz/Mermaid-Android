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
    CLASS_DIAGRAM,
    STATE_DIAGRAM,
    GANTT,
    ER_DIAGRAM,
    UNKNOWN
}

// region Flowchart

data class FlowchartDiagram(
    val direction: FlowDirection,
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>,
    val subgraphs: List<Subgraph> = emptyList(),
    val classDefs: Map<String, NodeStyle> = emptyMap(),
    val nodeClassMap: Map<String, String> = emptyMap()  // nodeId -> className
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

data class Subgraph(
    val id: String,
    val label: String,
    val nodeIds: List<String> = emptyList()
)

data class NodeStyle(
    val fill: String? = null,
    val stroke: String? = null,
    val strokeWidth: Float? = null,
    val color: String? = null
)

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

// region Class Diagram

data class ClassDiagram(
    val classes: List<ClassDefinition> = emptyList(),
    val relationships: List<ClassRelationship> = emptyList()
) : Diagram {
    override val type = DiagramType.CLASS_DIAGRAM
}

data class ClassDefinition(
    val name: String,
    val properties: MutableList<ClassMember> = mutableListOf(),
    val methods: MutableList<ClassMember> = mutableListOf(),
    var annotation: String? = null
)

data class ClassMember(
    val visibility: Visibility = Visibility.PUBLIC,
    val name: String,
    val memberType: String? = null
) {
    enum class Visibility(val symbol: String) {
        PUBLIC("+"),
        PRIVATE("-"),
        PROTECTED("#"),
        PACKAGE_PRIVATE("~");

        companion object {
            fun fromChar(c: Char): Visibility? = when (c) {
                '+' -> PUBLIC
                '-' -> PRIVATE
                '#' -> PROTECTED
                '~' -> PACKAGE_PRIVATE
                else -> null
            }
        }
    }
}

data class ClassRelationship(
    val from: String,
    val to: String,
    val label: String? = null,
    val relationshipType: ClassRelationType,
    val fromCardinality: String? = null,
    val toCardinality: String? = null
) {
    enum class ClassRelationType {
        INHERITANCE,       // <|--
        COMPOSITION,       // *--
        AGGREGATION,       // o--
        ASSOCIATION,       // -->
        DEPENDENCY,        // ..>
        REALIZATION        // ..|>
    }
}

// endregion

// region State Diagram

data class StateDiagram(
    val states: List<StateNode> = emptyList(),
    val transitions: List<StateTransition> = emptyList()
) : Diagram {
    override val type = DiagramType.STATE_DIAGRAM
}

data class StateNode(
    val id: String,
    val label: String = id,
    val description: String? = null
)

data class StateTransition(
    val from: String,
    val to: String,
    val label: String? = null
)

// endregion

// region Gantt Chart

data class GanttDiagram(
    val title: String? = null,
    val dateFormat: String? = null,
    val sections: List<GanttSection> = emptyList()
) : Diagram {
    override val type = DiagramType.GANTT
}

data class GanttSection(
    val name: String,
    val tasks: MutableList<GanttTask> = mutableListOf()
)

data class GanttTask(
    val name: String,
    val id: String? = null,
    val status: TaskStatus = TaskStatus.NORMAL,
    val startDate: String? = null,
    val duration: String? = null,
    val afterId: String? = null
) {
    enum class TaskStatus {
        NORMAL,
        DONE,
        ACTIVE,
        CRITICAL,
        CRITICAL_DONE,
        CRITICAL_ACTIVE
    }
}

// endregion

// region ER Diagram

data class ERDiagram(
    val entities: List<EREntity> = emptyList(),
    val relationships: List<ERRelationship> = emptyList()
) : Diagram {
    override val type = DiagramType.ER_DIAGRAM
}

data class EREntity(
    val name: String,
    val attributes: MutableList<ERAttribute> = mutableListOf()
)

data class ERAttribute(
    val attributeType: String,
    val name: String,
    val key: AttributeKey? = null
) {
    enum class AttributeKey(val value: String) {
        PK("PK"),
        FK("FK"),
        UK("UK");

        companion object {
            fun fromString(s: String): AttributeKey? =
                entries.firstOrNull { it.value == s }
        }
    }
}

data class ERRelationship(
    val from: String,
    val to: String,
    val label: String,
    val fromCardinality: ERCardinality,
    val toCardinality: ERCardinality
) {
    enum class ERCardinality(val symbol: String) {
        EXACTLY_ONE("||"),
        ZERO_OR_ONE("|o"),
        ZERO_OR_MORE("o{"),
        ONE_OR_MORE("}|");
    }
}

// endregion
