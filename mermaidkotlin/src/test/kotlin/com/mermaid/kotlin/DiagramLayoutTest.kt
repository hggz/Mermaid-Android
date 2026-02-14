package com.mermaid.kotlin

import com.mermaid.kotlin.layout.DiagramLayout
import com.mermaid.kotlin.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagramLayoutTest {

    private val layout = DiagramLayout()

    // region Flowchart Layout

    @Test
    fun testFlowchartLayoutPositionsNodes() {
        val diagram = FlowchartDiagram(
            direction = FlowDirection.TOP_TO_BOTTOM,
            nodes = listOf(
                FlowNode(id = "A", label = "Start", shape = NodeShape.RECTANGLE),
                FlowNode(id = "B", label = "End", shape = NodeShape.RECTANGLE),
            ),
            edges = listOf(
                FlowEdge(from = "A", to = "B", label = null, style = EdgeStyle.SOLID)
            )
        )

        val result = layout.layoutFlowchart(diagram)

        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)

        // Node A should be above node B in TD layout
        val nodeA = result.nodes.first { it.node.id == "A" }
        val nodeB = result.nodes.first { it.node.id == "B" }
        assertTrue(nodeA.frame.top < nodeB.frame.top)
    }

    @Test
    fun testFlowchartLayoutLR() {
        val diagram = FlowchartDiagram(
            direction = FlowDirection.LEFT_TO_RIGHT,
            nodes = listOf(
                FlowNode(id = "A", label = "Start", shape = NodeShape.RECTANGLE),
                FlowNode(id = "B", label = "End", shape = NodeShape.RECTANGLE),
            ),
            edges = listOf(
                FlowEdge(from = "A", to = "B", label = null, style = EdgeStyle.SOLID)
            )
        )

        val result = layout.layoutFlowchart(diagram)

        val nodeA = result.nodes.first { it.node.id == "A" }
        val nodeB = result.nodes.first { it.node.id == "B" }
        // In LR, A should be left of B
        assertTrue(nodeA.frame.left < nodeB.frame.left)
    }

    @Test
    fun testFlowchartEdgeConnections() {
        val diagram = FlowchartDiagram(
            direction = FlowDirection.TOP_TO_BOTTOM,
            nodes = listOf(
                FlowNode(id = "A", label = "A", shape = NodeShape.RECTANGLE),
                FlowNode(id = "B", label = "B", shape = NodeShape.RECTANGLE),
            ),
            edges = listOf(
                FlowEdge(from = "A", to = "B", label = "Yes", style = EdgeStyle.SOLID)
            )
        )

        val result = layout.layoutFlowchart(diagram)
        val edge = result.edges[0]

        // From point should be below node A, to point above node B
        assertNotNull(edge.labelPosition)
        assertTrue(edge.points.first().y < edge.points.last().y) // TD: fromY < toY
    }

    @Test
    fun testFlowchartMultipleLayers() {
        val diagram = FlowchartDiagram(
            direction = FlowDirection.TOP_TO_BOTTOM,
            nodes = listOf(
                FlowNode(id = "A", label = "A", shape = NodeShape.RECTANGLE),
                FlowNode(id = "B", label = "B", shape = NodeShape.RECTANGLE),
                FlowNode(id = "C", label = "C", shape = NodeShape.RECTANGLE),
                FlowNode(id = "D", label = "D", shape = NodeShape.RECTANGLE),
            ),
            edges = listOf(
                FlowEdge(from = "A", to = "B", label = null, style = EdgeStyle.SOLID),
                FlowEdge(from = "A", to = "C", label = null, style = EdgeStyle.SOLID),
                FlowEdge(from = "B", to = "D", label = null, style = EdgeStyle.SOLID),
                FlowEdge(from = "C", to = "D", label = null, style = EdgeStyle.SOLID),
            )
        )

        val result = layout.layoutFlowchart(diagram)
        assertEquals(4, result.nodes.size)
        assertEquals(4, result.edges.size)

        // D should be in the bottom layer
        val nodeA = result.nodes.first { it.node.id == "A" }
        val nodeD = result.nodes.first { it.node.id == "D" }
        assertTrue(nodeA.frame.top < nodeD.frame.top)
    }

    // endregion

    // region Sequence Diagram Layout

    @Test
    fun testSequenceDiagramLayout() {
        val diagram = SequenceDiagram(
            participants = listOf(
                Participant(id = "Alice", label = "Alice"),
                Participant(id = "Bob", label = "Bob"),
            ),
            messages = listOf(
                Message(from = "Alice", to = "Bob", text = "Hello", style = MessageStyle.SOLID_ARROW),
                Message(from = "Bob", to = "Alice", text = "Hi", style = MessageStyle.DOTTED_ARROW),
            )
        )

        val result = layout.layoutSequenceDiagram(diagram)

        assertEquals(2, result.participants.size)
        assertEquals(2, result.messages.size)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)

        // Alice should be left of Bob
        val alice = result.participants[0]
        val bob = result.participants[1]
        assertTrue(alice.lifelineX < bob.lifelineX)

        // Messages should flow downward
        assertTrue(result.messages[0].y < result.messages[1].y)
    }

    @Test
    fun testSequenceLifelineExtends() {
        val diagram = SequenceDiagram(
            participants = listOf(Participant(id = "A", label = "A")),
            messages = listOf(
                Message(from = "A", to = "A", text = "Self", style = MessageStyle.SOLID_ARROW),
            )
        )

        val result = layout.layoutSequenceDiagram(diagram)
        val participant = result.participants[0]
        assertTrue(participant.lifelineTop < participant.lifelineBottom)
    }

    // endregion

    // region Pie Chart Layout

    @Test
    fun testPieChartLayout() {
        val diagram = PieChartDiagram(
            title = "Test",
            slices = listOf(
                PieSlice(label = "A", value = 30.0),
                PieSlice(label = "B", value = 70.0),
            )
        )

        val result = layout.layoutPieChart(diagram)

        assertEquals(2, result.slices.size)
        assertEquals("Test", result.title)
        assertTrue(result.radius > 0)

        // Check sweep angles sum to 360 degrees
        val totalSweep = result.slices.sumOf { it.sweepAngle.toDouble() }.toFloat()
        assertEquals(360f, totalSweep, 0.1f)

        // Check percentages
        assertEquals(30f, result.slices[0].percentage, 0.1f)
        assertEquals(70f, result.slices[1].percentage, 0.1f)
    }

    @Test
    fun testPieChartEmptyData() {
        val diagram = PieChartDiagram(title = null, slices = emptyList())
        val result = layout.layoutPieChart(diagram)
        assertTrue(result.slices.isEmpty())
    }

    @Test
    fun testPieChartSingleSlice() {
        val diagram = PieChartDiagram(
            title = null,
            slices = listOf(PieSlice(label = "Only", value = 100.0))
        )

        val result = layout.layoutPieChart(diagram)
        assertEquals(1, result.slices.size)
        assertEquals(100f, result.slices[0].percentage, 0.1f)
    }

    // endregion

    // region Class Diagram Layout

    @Test
    fun testClassDiagramLayout() {
        val diagram = ClassDiagram(
            classes = listOf(
                ClassDefinition(name = "Animal",
                    properties = mutableListOf(ClassMember(name = "name")),
                    methods = mutableListOf(ClassMember(name = "makeSound()"))),
                ClassDefinition(name = "Dog"),
            ),
            relationships = listOf(
                ClassRelationship(from = "Dog", to = "Animal",
                    relationshipType = ClassRelationship.ClassRelationType.INHERITANCE)
            )
        )

        val result = layout.layoutClassDiagram(diagram)
        assertEquals(2, result.classes.size)
        assertEquals(1, result.relationships.size)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }

    // endregion

    // region State Diagram Layout

    @Test
    fun testStateDiagramLayout() {
        val diagram = StateDiagram(
            states = listOf(
                StateNode(id = "[*]", label = "[*]"),
                StateNode(id = "Active", label = "Active"),
                StateNode(id = "Inactive", label = "Inactive"),
            ),
            transitions = listOf(
                StateTransition(from = "[*]", to = "Active"),
                StateTransition(from = "Active", to = "Inactive", label = "timeout"),
            )
        )

        val result = layout.layoutStateDiagram(diagram)
        assertEquals(3, result.states.size)
        assertEquals(2, result.transitions.size)
        assertTrue(result.width > 0)

        val startState = result.states.first { it.state.id == "[*]" }
        assertTrue(startState.isStartEnd)
    }

    // endregion

    // region Gantt Chart Layout

    @Test
    fun testGanttChartLayout() {
        val section = GanttSection(name = "Phase 1")
        section.tasks.add(GanttTask(name = "Design", status = GanttTask.TaskStatus.DONE))
        section.tasks.add(GanttTask(name = "Implement", status = GanttTask.TaskStatus.ACTIVE))

        val diagram = GanttDiagram(
            title = "Project",
            dateFormat = "YYYY-MM-DD",
            sections = listOf(section)
        )

        val result = layout.layoutGanttChart(diagram)
        assertEquals("Project", result.title)
        assertEquals(1, result.sections.size)
        assertEquals(2, result.tasks.size)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }

    // endregion

    // region ER Diagram Layout

    @Test
    fun testERDiagramLayout() {
        val diagram = ERDiagram(
            entities = listOf(
                EREntity(name = "CUSTOMER",
                    attributes = mutableListOf(
                        ERAttribute(attributeType = "int", name = "id", key = ERAttribute.AttributeKey.PK)
                    )),
                EREntity(name = "ORDER"),
            ),
            relationships = listOf(
                ERRelationship(from = "CUSTOMER", to = "ORDER",
                    label = "places",
                    fromCardinality = ERRelationship.ERCardinality.EXACTLY_ONE,
                    toCardinality = ERRelationship.ERCardinality.ZERO_OR_MORE)
            )
        )

        val result = layout.layoutERDiagram(diagram)
        assertEquals(2, result.entities.size)
        assertEquals(1, result.relationships.size)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }

    // endregion
}
