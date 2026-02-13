package com.mermaid.kotlin

import com.mermaid.kotlin.model.*
import com.mermaid.kotlin.parser.MermaidParser
import org.junit.Assert.*
import org.junit.Test

class MermaidParserTest {

    private val parser = MermaidParser()

    // region Error Cases

    @Test(expected = MermaidParser.ParseError.EmptyInput::class)
    fun testEmptyInput() {
        parser.parse("")
    }

    @Test(expected = MermaidParser.ParseError.EmptyInput::class)
    fun testWhitespaceOnlyInput() {
        parser.parse("   \n  \n  ")
    }

    @Test
    fun testUnknownDiagramType() {
        try {
            parser.parse("timeline\n  2024: Something")
            fail("Should throw")
        } catch (e: MermaidParser.ParseError.UnknownDiagramType) {
            assertTrue(e.message!!.contains("timeline"))
        }
    }

    // endregion

    // region Flowchart Parsing

    @Test
    fun testFlowchartBasic() {
        val diagram = parser.parse("""
            flowchart TD
                A[Start] --> B[End]
        """.trimIndent())

        assertTrue(diagram is FlowchartDiagram)
        val flowchart = diagram as FlowchartDiagram
        assertEquals(FlowDirection.TOP_TO_BOTTOM, flowchart.direction)
        assertEquals(2, flowchart.nodes.size)
        assertEquals(1, flowchart.edges.size)

        val nodeIds = flowchart.nodes.map { it.id }.toSet()
        assertTrue(nodeIds.contains("A"))
        assertTrue(nodeIds.contains("B"))

        val edge = flowchart.edges[0]
        assertEquals("A", edge.from)
        assertEquals("B", edge.to)
        assertEquals(EdgeStyle.SOLID, edge.style)
        assertNull(edge.label)
    }

    @Test
    fun testFlowchartDirections() {
        val cases = mapOf(
            "TD" to FlowDirection.TOP_TO_BOTTOM,
            "TB" to FlowDirection.TOP_DOWN,
            "BT" to FlowDirection.BOTTOM_TO_TOP,
            "LR" to FlowDirection.LEFT_TO_RIGHT,
            "RL" to FlowDirection.RIGHT_TO_LEFT
        )
        for ((dir, expected) in cases) {
            val diagram = parser.parse("flowchart $dir\n  A --> B")
            val flowchart = diagram as FlowchartDiagram
            assertEquals("Direction $dir should parse correctly", expected, flowchart.direction)
        }
    }

    @Test
    fun testFlowchartGraphKeyword() {
        val diagram = parser.parse("graph LR\n  A --> B")
        val flowchart = diagram as FlowchartDiagram
        assertEquals(FlowDirection.LEFT_TO_RIGHT, flowchart.direction)
    }

    @Test
    fun testFlowchartNodeShapes() {
        val diagram = parser.parse("""
            flowchart TD
                A[Rectangle] --> B(Rounded)
                B --> C{Diamond}
                C --> D([Stadium])
                D --> E((Circle))
                E --> F{{Hexagon}}
        """.trimIndent())

        val flowchart = diagram as FlowchartDiagram
        val nodeMap = flowchart.nodes.associateBy { it.id }

        assertEquals(NodeShape.RECTANGLE, nodeMap["A"]?.shape)
        assertEquals("Rectangle", nodeMap["A"]?.label)
        assertEquals(NodeShape.ROUNDED_RECT, nodeMap["B"]?.shape)
        assertEquals("Rounded", nodeMap["B"]?.label)
        assertEquals(NodeShape.DIAMOND, nodeMap["C"]?.shape)
        assertEquals(NodeShape.STADIUM, nodeMap["D"]?.shape)
        assertEquals(NodeShape.CIRCLE, nodeMap["E"]?.shape)
        assertEquals(NodeShape.HEXAGON, nodeMap["F"]?.shape)
    }

    @Test
    fun testFlowchartEdgeStyles() {
        val diagram = parser.parse("""
            flowchart TD
                A --> B
                B -.-> C
                C ==> D
        """.trimIndent())

        val flowchart = diagram as FlowchartDiagram
        assertEquals(3, flowchart.edges.size)
        assertEquals(EdgeStyle.SOLID, flowchart.edges[0].style)
        assertEquals(EdgeStyle.DOTTED, flowchart.edges[1].style)
        assertEquals(EdgeStyle.THICK, flowchart.edges[2].style)
    }

    @Test
    fun testFlowchartEdgeLabel() {
        val diagram = parser.parse("""
            flowchart TD
                A -->|Yes| B
                A -->|No| C
        """.trimIndent())

        val flowchart = diagram as FlowchartDiagram
        assertEquals("Yes", flowchart.edges[0].label)
        assertEquals("No", flowchart.edges[1].label)
    }

    @Test
    fun testFlowchartMultipleEdges() {
        val diagram = parser.parse("""
            flowchart TD
                A[Start] --> B{Decision}
                B -->|Yes| C[Do thing]
                B -->|No| D[Skip]
                C --> E[End]
                D --> E
        """.trimIndent())

        val flowchart = diagram as FlowchartDiagram
        assertEquals(5, flowchart.edges.size)
        assertEquals(5, flowchart.nodes.size)
    }

    @Test
    fun testFlowchartComments() {
        val diagram = parser.parse("""
            flowchart TD
                %% This is a comment
                A --> B
        """.trimIndent())

        val flowchart = diagram as FlowchartDiagram
        assertEquals(2, flowchart.nodes.size)
    }

    // endregion

    // region Sequence Diagram Parsing

    @Test
    fun testSequenceDiagramBasic() {
        val diagram = parser.parse("""
            sequenceDiagram
                Alice->>Bob: Hello Bob
                Bob-->>Alice: Hi Alice
        """.trimIndent())

        val seq = diagram as SequenceDiagram
        assertEquals(2, seq.participants.size)
        assertEquals(2, seq.messages.size)

        assertEquals("Alice", seq.participants[0].id)
        assertEquals("Bob", seq.participants[1].id)

        assertEquals("Alice", seq.messages[0].from)
        assertEquals("Bob", seq.messages[0].to)
        assertEquals("Hello Bob", seq.messages[0].text)
        assertEquals(MessageStyle.SOLID_ARROW, seq.messages[0].style)

        assertEquals(MessageStyle.DOTTED_ARROW, seq.messages[1].style)
    }

    @Test
    fun testSequenceDiagramExplicitParticipants() {
        val diagram = parser.parse("""
            sequenceDiagram
                participant A as Alice
                participant B as Bob
                A->>B: Hello
        """.trimIndent())

        val seq = diagram as SequenceDiagram
        assertEquals("A", seq.participants[0].id)
        assertEquals("Alice", seq.participants[0].label)
        assertEquals("B", seq.participants[1].id)
        assertEquals("Bob", seq.participants[1].label)
    }

    @Test
    fun testSequenceDiagramMessageStyles() {
        val diagram = parser.parse("""
            sequenceDiagram
                A->>B: solid arrow
                B-->>A: dotted arrow
                A->B: solid line
                B-->A: dotted line
                A-xB: cross
                B--xA: dotted cross
        """.trimIndent())

        val seq = diagram as SequenceDiagram
        assertEquals(6, seq.messages.size)
        assertEquals(MessageStyle.SOLID_ARROW, seq.messages[0].style)
        assertEquals(MessageStyle.DOTTED_ARROW, seq.messages[1].style)
        assertEquals(MessageStyle.SOLID_LINE, seq.messages[2].style)
        assertEquals(MessageStyle.DOTTED_LINE, seq.messages[3].style)
        assertEquals(MessageStyle.SOLID_CROSS, seq.messages[4].style)
        assertEquals(MessageStyle.DOTTED_CROSS, seq.messages[5].style)
    }

    @Test
    fun testSequenceDiagramAutoParticipants() {
        val diagram = parser.parse("""
            sequenceDiagram
                Server->>Database: Query
                Database-->>Server: Results
        """.trimIndent())

        val seq = diagram as SequenceDiagram
        assertEquals(2, seq.participants.size)
        val ids = seq.participants.map { it.id }
        assertTrue(ids.contains("Server"))
        assertTrue(ids.contains("Database"))
    }

    @Test
    fun testSequenceDiagramActor() {
        val diagram = parser.parse("""
            sequenceDiagram
                actor User
                User->>System: Login
        """.trimIndent())

        val seq = diagram as SequenceDiagram
        assertEquals(2, seq.participants.size)
        assertEquals("User", seq.participants[0].id)
    }

    // endregion

    // region Pie Chart Parsing

    @Test
    fun testPieChartBasic() {
        val diagram = parser.parse("""
            pie title Languages
                "Swift" : 45
                "Kotlin" : 30
                "Python" : 25
        """.trimIndent())

        val pie = diagram as PieChartDiagram
        assertEquals("Languages", pie.title)
        assertEquals(3, pie.slices.size)

        assertEquals("Swift", pie.slices[0].label)
        assertEquals(45.0, pie.slices[0].value, 0.01)

        assertEquals("Kotlin", pie.slices[1].label)
        assertEquals(30.0, pie.slices[1].value, 0.01)
    }

    @Test
    fun testPieChartNoTitle() {
        val diagram = parser.parse("""
            pie
                "A" : 50
                "B" : 50
        """.trimIndent())

        val pie = diagram as PieChartDiagram
        assertNull(pie.title)
        assertEquals(2, pie.slices.size)
    }

    @Test
    fun testPieChartDecimalValues() {
        val diagram = parser.parse("""
            pie title Test
                "Alpha" : 33.3
                "Beta" : 66.7
        """.trimIndent())

        val pie = diagram as PieChartDiagram
        assertEquals(33.3, pie.slices[0].value, 0.01)
        assertEquals(66.7, pie.slices[1].value, 0.01)
    }

    @Test
    fun testPieChartSeparateTitleLine() {
        val diagram = parser.parse("""
            pie
                title My Chart
                "A" : 100
        """.trimIndent())

        val pie = diagram as PieChartDiagram
        assertEquals("My Chart", pie.title)
    }

    // endregion

    // region Diagram Type Detection

    @Test
    fun testDiagramTypeDetection() {
        val flowchart = parser.parse("flowchart TD\n  A --> B")
        assertTrue(flowchart is FlowchartDiagram)

        val graph = parser.parse("graph LR\n  A --> B")
        assertTrue(graph is FlowchartDiagram)

        val seq = parser.parse("sequenceDiagram\n  A->>B: hi")
        assertTrue(seq is SequenceDiagram)

        val pie = parser.parse("pie\n  \"A\" : 1")
        assertTrue(pie is PieChartDiagram)
    }

    // endregion
}
