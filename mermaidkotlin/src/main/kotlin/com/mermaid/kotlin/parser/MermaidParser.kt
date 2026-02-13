package com.mermaid.kotlin.parser

import com.mermaid.kotlin.model.*

/**
 * Parses Mermaid DSL strings into diagram model objects.
 *
 * Supports: flowchart, sequenceDiagram, pie
 */
class MermaidParser {

    sealed class ParseError(override val message: String) : Exception(message) {
        class EmptyInput : ParseError("Empty diagram input")
        class UnknownDiagramType(type: String) : ParseError("Unknown diagram type: $type")
        class InvalidSyntax(detail: String) : ParseError("Invalid syntax: $detail")
    }

    /**
     * Parse a Mermaid DSL string into a typed Diagram.
     */
    fun parse(input: String): Diagram {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) throw ParseError.EmptyInput()

        val lines = trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("%%") }

        val firstLine = lines.firstOrNull() ?: throw ParseError.EmptyInput()

        return when {
            firstLine.startsWith("flowchart") || firstLine.startsWith("graph") ->
                parseFlowchart(lines, firstLine)
            firstLine.startsWith("sequenceDiagram") ->
                parseSequenceDiagram(lines.drop(1))
            firstLine.startsWith("pie") ->
                parsePieChart(lines, firstLine)
            else ->
                throw ParseError.UnknownDiagramType(firstLine)
        }
    }

    // region Flowchart Parser

    private fun parseFlowchart(lines: List<String>, header: String): FlowchartDiagram {
        val parts = header.split(" ")
        val dirStr = if (parts.size > 1) parts[1] else "TD"
        val direction = FlowDirection.fromString(dirStr)

        val nodes = mutableMapOf<String, FlowNode>()
        val edges = mutableListOf<FlowEdge>()

        for (line in lines.drop(1)) {
            val parsedEdge = parseFlowchartEdge(line)
            if (parsedEdge != null) {
                // Auto-create nodes
                for ((id, label, shape) in listOf(
                    Triple(parsedEdge.fromId, parsedEdge.fromLabel, parsedEdge.fromShape),
                    Triple(parsedEdge.toId, parsedEdge.toLabel, parsedEdge.toShape)
                )) {
                    if (id !in nodes) {
                        nodes[id] = FlowNode(id = id, label = label ?: id, shape = shape ?: NodeShape.RECTANGLE)
                    }
                }
                edges.add(FlowEdge(
                    from = parsedEdge.fromId,
                    to = parsedEdge.toId,
                    label = parsedEdge.edgeLabel,
                    style = parsedEdge.edgeStyle
                ))
            } else {
                val node = parseFlowchartNode(line)
                if (node != null) {
                    nodes[node.id] = node
                }
            }
        }

        return FlowchartDiagram(
            direction = direction,
            nodes = nodes.values.toList(),
            edges = edges
        )
    }

    private data class ParsedEdge(
        val fromId: String,
        val fromLabel: String?,
        val fromShape: NodeShape?,
        val toId: String,
        val toLabel: String?,
        val toShape: NodeShape?,
        val edgeLabel: String?,
        val edgeStyle: EdgeStyle
    )

    private fun parseFlowchartEdge(line: String): ParsedEdge? {
        val edgePatterns = listOf(
            "==>" to EdgeStyle.THICK,
            "-.->".to(EdgeStyle.DOTTED),
            "~~~" to EdgeStyle.INVISIBLE,
            "-->" to EdgeStyle.SOLID,
        )

        for ((pattern, style) in edgePatterns) {
            val idx = line.indexOf(pattern)
            if (idx < 0) continue

            val leftPart = line.substring(0, idx).trim()
            var rightPart = line.substring(idx + pattern.length).trim()
            var edgeLabel: String? = null

            // Check for edge label: |label|
            if (rightPart.startsWith("|")) {
                val endPipe = rightPart.indexOf('|', 1)
                if (endPipe > 0) {
                    edgeLabel = rightPart.substring(1, endPipe)
                    rightPart = rightPart.substring(endPipe + 1).trim()
                }
            }

            val (fromId, fromLabel, fromShape) = parseNodeRef(leftPart)
            val (toId, toLabel, toShape) = parseNodeRef(rightPart)

            return ParsedEdge(
                fromId = fromId, fromLabel = fromLabel, fromShape = fromShape,
                toId = toId, toLabel = toLabel, toShape = toShape,
                edgeLabel = edgeLabel, edgeStyle = style
            )
        }
        return null
    }

    /**
     * Parse a node reference like `A`, `A[Label]`, `A(Label)`, `A{Label}`, etc.
     */
    internal fun parseNodeRef(ref: String): Triple<String, String?, NodeShape?> {
        val s = ref.trim()

        // ((label)) — circle
        val circleRegex = Regex("^([A-Za-z0-9_]+)\\(\\((.+)\\)\\)$")
        circleRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.CIRCLE)
        }

        // ([label]) — stadium
        val stadiumRegex = Regex("^([A-Za-z0-9_]+)\\(\\[(.+)]\\)$")
        stadiumRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.STADIUM)
        }

        // {{label}} — hexagon
        val hexagonRegex = Regex("^([A-Za-z0-9_]+)\\{\\{(.+)}}$")
        hexagonRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.HEXAGON)
        }

        // {label} — diamond
        val diamondRegex = Regex("^([A-Za-z0-9_]+)\\{(.+)}$")
        diamondRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.DIAMOND)
        }

        // >label] — asymmetric
        val asymmetricRegex = Regex("^([A-Za-z0-9_]+)>(.+)]$")
        asymmetricRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.ASYMMETRIC)
        }

        // (label) — rounded rect
        val roundedRegex = Regex("^([A-Za-z0-9_]+)\\((.+)\\)$")
        roundedRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.ROUNDED_RECT)
        }

        // [label] — rectangle
        val rectRegex = Regex("^([A-Za-z0-9_]+)\\[(.+)]$")
        rectRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], it.groupValues[2], NodeShape.RECTANGLE)
        }

        // Plain ID
        val plainRegex = Regex("^([A-Za-z0-9_]+)$")
        plainRegex.matchEntire(s)?.let {
            return Triple(it.groupValues[1], null, null)
        }

        return Triple(s, null, null)
    }

    private fun parseFlowchartNode(line: String): FlowNode? {
        val (id, label, shape) = parseNodeRef(line)
        if (label == null && shape == null) return null
        return FlowNode(id = id, label = label ?: id, shape = shape ?: NodeShape.RECTANGLE)
    }

    // endregion

    // region Sequence Diagram Parser

    private fun parseSequenceDiagram(lines: List<String>): SequenceDiagram {
        val participants = mutableListOf<Participant>()
        val participantSet = mutableSetOf<String>()
        val messages = mutableListOf<Message>()

        for (line in lines) {
            // participant declarations
            if (line.startsWith("participant ")) {
                val rest = line.removePrefix("participant ")
                val parts = rest.split(" as ")
                val id: String
                val label: String
                if (parts.size >= 2) {
                    id = parts[0].trim()
                    label = parts[1].trim()
                } else {
                    id = rest.trim()
                    label = id
                }
                if (id !in participantSet) {
                    participants.add(Participant(id = id, label = label))
                    participantSet.add(id)
                }
                continue
            }

            // actor declarations
            if (line.startsWith("actor ")) {
                val id = line.removePrefix("actor ").trim()
                if (id !in participantSet) {
                    participants.add(Participant(id = id, label = id))
                    participantSet.add(id)
                }
                continue
            }

            // Messages
            val msg = parseSequenceMessage(line)
            if (msg != null) {
                for (pid in listOf(msg.from, msg.to)) {
                    if (pid !in participantSet) {
                        participants.add(Participant(id = pid, label = pid))
                        participantSet.add(pid)
                    }
                }
                messages.add(msg)
            }
        }

        return SequenceDiagram(participants = participants, messages = messages)
    }

    private fun parseSequenceMessage(line: String): Message? {
        val arrowPatterns = listOf(
            "-->>" to MessageStyle.DOTTED_ARROW,
            "->>" to MessageStyle.SOLID_ARROW,
            "--x" to MessageStyle.DOTTED_CROSS,
            "-x" to MessageStyle.SOLID_CROSS,
            "--)" to MessageStyle.DOTTED_LINE,
            "-)" to MessageStyle.SOLID_LINE,
            "-->" to MessageStyle.DOTTED_LINE,
            "->" to MessageStyle.SOLID_LINE,
        )

        for ((pattern, style) in arrowPatterns) {
            val idx = line.indexOf(pattern)
            if (idx < 0) continue

            val from = line.substring(0, idx).trim()
            val afterArrow = line.substring(idx + pattern.length).trim()

            val colonIdx = afterArrow.indexOf(": ")
            if (colonIdx < 0) continue

            val to = afterArrow.substring(0, colonIdx).trim()
            val text = afterArrow.substring(colonIdx + 2).trim()

            if (from.isEmpty() || to.isEmpty()) continue

            return Message(from = from, to = to, text = text, style = style)
        }
        return null
    }

    // endregion

    // region Pie Chart Parser

    private fun parsePieChart(lines: List<String>, header: String): PieChartDiagram {
        var title: String? = null
        val slices = mutableListOf<PieSlice>()

        // "pie title My Title"
        val titleRegex = Regex("pie\\s+title\\s+(.+)")
        titleRegex.find(header)?.let {
            title = it.groupValues[1].trim()
        }

        for (line in lines.drop(1)) {
            // "title My Title" on its own line
            if (line.startsWith("title ") && title == null) {
                title = line.removePrefix("title ").trim()
                continue
            }

            // Match: "Label" : value
            val sliceRegex = Regex("^\\s*\"(.+?)\"\\s*:\\s*([0-9.]+)\\s*$")
            sliceRegex.matchEntire(line)?.let {
                val label = it.groupValues[1]
                val value = it.groupValues[2].toDoubleOrNull() ?: 0.0
                slices.add(PieSlice(label = label, value = value))
            }
        }

        return PieChartDiagram(title = title, slices = slices)
    }

    // endregion
}
