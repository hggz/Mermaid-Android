package com.mermaid.kotlin.parser

import com.mermaid.kotlin.model.*

/**
 * Parses Mermaid DSL strings into diagram model objects.
 *
 * Supports: flowchart, sequenceDiagram, pie, classDiagram, stateDiagram, gantt, erDiagram
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
            firstLine.startsWith("classDiagram") ->
                parseClassDiagram(lines.drop(1))
            firstLine.startsWith("stateDiagram") ->
                parseStateDiagram(lines.drop(1))
            firstLine.startsWith("gantt") ->
                parseGanttDiagram(lines.drop(1))
            firstLine.startsWith("erDiagram") ->
                parseERDiagram(lines.drop(1))
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
        val subgraphs = mutableListOf<Subgraph>()
        val classDefs = mutableMapOf<String, NodeStyle>()
        val nodeClassMap = mutableMapOf<String, String>()

        // Track subgraph nesting
        data class SubgraphEntry(val id: String, val label: String, val nodeIds: MutableList<String>)
        val subgraphStack = mutableListOf<SubgraphEntry>()

        for (line in lines.drop(1)) {
            // Skip end keyword
            if (line == "end") {
                subgraphStack.removeLastOrNull()?.let { current ->
                    subgraphs.add(Subgraph(id = current.id, label = current.label, nodeIds = current.nodeIds))
                }
                continue
            }

            // Subgraph start
            if (line.startsWith("subgraph ")) {
                val rest = line.removePrefix("subgraph ").trim()
                val subParts = rest.split(" ")
                val sgId: String
                val sgLabel: String
                if (subParts.size > 1 && rest.contains("[")) {
                    sgId = subParts[0]
                    val bracketStart = rest.indexOf('[')
                    val bracketEnd = rest.indexOf(']')
                    sgLabel = if (bracketStart >= 0 && bracketEnd > bracketStart) {
                        rest.substring(bracketStart + 1, bracketEnd)
                    } else {
                        subParts.drop(1).joinToString(" ")
                    }
                } else {
                    sgId = subParts[0]
                    sgLabel = rest
                }
                subgraphStack.add(SubgraphEntry(sgId, sgLabel, mutableListOf()))
                continue
            }

            // classDef directive
            if (line.startsWith("classDef ")) {
                parseClassDefDirective(line)?.let { (name, style) ->
                    classDefs[name] = style
                }
                continue
            }

            // class directive
            if (line.startsWith("class ") && !line.contains("-->") && !line.contains("---")) {
                parseClassDirective(line, nodeClassMap)
                continue
            }

            // style directive
            if (line.startsWith("style ")) {
                parseStyleDirective(line)?.let { (nodeId, style) ->
                    val uniqueName = "__style_$nodeId"
                    classDefs[uniqueName] = style
                    nodeClassMap[nodeId] = uniqueName
                }
                continue
            }

            // Try edge parse
            val parsedEdge = parseFlowchartEdge(line)
            if (parsedEdge != null) {
                for ((id, label, shape) in listOf(
                    Triple(parsedEdge.fromId, parsedEdge.fromLabel, parsedEdge.fromShape),
                    Triple(parsedEdge.toId, parsedEdge.toLabel, parsedEdge.toShape)
                )) {
                    if (id !in nodes) {
                        nodes[id] = FlowNode(id = id, label = label ?: id, shape = shape ?: NodeShape.RECTANGLE)
                    }
                    // Add to current subgraph
                    subgraphStack.lastOrNull()?.let { current ->
                        if (id !in current.nodeIds) current.nodeIds.add(id)
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
                    subgraphStack.lastOrNull()?.nodeIds?.add(node.id)
                }
            }

            // Handle :::className on nodes
            if (line.contains(":::")) {
                val classParts = line.split(":::")
                if (classParts.size == 2) {
                    val nodeRef = classParts[0].trim()
                    val className = classParts[1].trim().split("\\s+".toRegex()).first()
                    val (nodeId, _, _) = parseNodeRef(nodeRef)
                    nodeClassMap[nodeId] = className
                }
            }
        }

        return FlowchartDiagram(
            direction = direction,
            nodes = nodes.values.toList(),
            edges = edges,
            subgraphs = subgraphs,
            classDefs = classDefs,
            nodeClassMap = nodeClassMap
        )
    }

    private fun parseClassDefDirective(line: String): Pair<String, NodeStyle>? {
        val rest = line.removePrefix("classDef ").trim()
        val parts = rest.split(" ", limit = 2)
        if (parts.size < 2) return null
        val className = parts[0]
        val style = parseStyleString(parts[1])
        return className to style
    }

    private fun parseStyleDirective(line: String): Pair<String, NodeStyle>? {
        val rest = line.removePrefix("style ").trim()
        val parts = rest.split(" ", limit = 2)
        if (parts.size < 2) return null
        val nodeId = parts[0]
        val style = parseStyleString(parts[1])
        return nodeId to style
    }

    private fun parseClassDirective(line: String, map: MutableMap<String, String>) {
        val rest = line.removePrefix("class ").trim()
        val parts = rest.split(" ")
        if (parts.size < 2) return
        val className = parts.last()
        val nodeList = parts.dropLast(1).joinToString(" ")
        for (nodeId in nodeList.split(",")) {
            map[nodeId.trim()] = className
        }
    }

    private fun parseStyleString(str: String): NodeStyle {
        var fill: String? = null
        var stroke: String? = null
        var strokeWidth: Float? = null
        var color: String? = null

        for (prop in str.split(",")) {
            val kv = prop.split(":", limit = 2)
            if (kv.size != 2) continue
            val key = kv[0].trim()
            val value = kv[1].trim()
            when (key) {
                "fill" -> fill = value
                "stroke" -> stroke = value
                "stroke-width" -> strokeWidth = value.replace("px", "").toFloatOrNull() ?: 2f
                "color" -> color = value
            }
        }

        return NodeStyle(fill = fill, stroke = stroke, strokeWidth = strokeWidth, color = color)
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

            // Strip :::className from parts
            val cleanLeft = leftPart.split(":::").first()
            val cleanRight = rightPart.split(":::").first()

            val (fromId, fromLabel, fromShape) = parseNodeRef(cleanLeft)
            val (toId, toLabel, toShape) = parseNodeRef(cleanRight)

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

    // region Class Diagram Parser

    private fun parseClassDiagram(lines: List<String>): ClassDiagram {
        val classes = mutableMapOf<String, ClassDefinition>()
        val relationships = mutableListOf<ClassRelationship>()
        var currentClass: String? = null
        var inBlock = false

        for (line in lines) {
            // End of class block
            if (line == "}" && inBlock) {
                inBlock = false
                currentClass = null
                continue
            }

            // Inside class block — parse members
            if (inBlock && currentClass != null) {
                parseClassMember(line)?.let { member ->
                    if (member.name.contains("(")) {
                        classes[currentClass]?.methods?.add(member)
                    } else {
                        classes[currentClass]?.properties?.add(member)
                    }
                }
                continue
            }

            // Class block start: "class ClassName {"
            if (line.startsWith("class ") && line.endsWith("{")) {
                val name = line.removePrefix("class ").removeSuffix("{").trim()
                if (name !in classes) {
                    classes[name] = ClassDefinition(name = name)
                }
                currentClass = name
                inBlock = true
                continue
            }

            // Class declaration: "class ClassName"
            if (line.startsWith("class ") && !line.contains("{") && !line.contains(":")) {
                val name = line.removePrefix("class ").trim()
                if (name !in classes) {
                    classes[name] = ClassDefinition(name = name)
                }
                continue
            }

            // Annotation: <<interface>> ClassName
            if (line.startsWith("<<")) {
                val annotRegex = Regex("<<(.+?)>>\\s+(.+)")
                annotRegex.find(line)?.let { match ->
                    val annotation = match.groupValues[1]
                    val className = match.groupValues[2].trim()
                    if (className !in classes) {
                        classes[className] = ClassDefinition(name = className)
                    }
                    classes[className]?.annotation = annotation
                }
                continue
            }

            // Inline member: "ClassName : +method()"
            if (line.contains(" : ") && !line.contains("--") && !line.contains("..")) {
                val colonParts = line.split(" : ", limit = 2)
                if (colonParts.size >= 2) {
                    val className = colonParts[0].trim()
                    val memberStr = colonParts[1].trim()

                    if (className !in classes) {
                        classes[className] = ClassDefinition(name = className)
                    }

                    parseClassMember(memberStr)?.let { member ->
                        if (member.name.contains("(")) {
                            classes[className]?.methods?.add(member)
                        } else {
                            classes[className]?.properties?.add(member)
                        }
                    }
                }
                continue
            }

            // Relationship
            parseClassRelationship(line)?.let { rel ->
                for (name in listOf(rel.from, rel.to)) {
                    if (name !in classes) {
                        classes[name] = ClassDefinition(name = name)
                    }
                }
                relationships.add(rel)
                return@let
            }
        }

        return ClassDiagram(
            classes = classes.values.sortedBy { it.name },
            relationships = relationships
        )
    }

    private fun parseClassMember(line: String): ClassMember? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        var visibility = ClassMember.Visibility.PUBLIC
        var rest = trimmed

        rest.firstOrNull()?.let { first ->
            ClassMember.Visibility.fromChar(first)?.let { v ->
                visibility = v
                rest = rest.drop(1)
            }
        }

        rest = rest.trim()
        if (rest.isEmpty()) return null

        val parts = rest.split(" ", limit = 2)
        return if (rest.contains("(")) {
            val name = if (rest.contains(" ")) parts[0] else rest
            val memberType = if (parts.size > 1) parts[1] else null
            ClassMember(visibility = visibility, name = name, memberType = memberType)
        } else if (parts.size == 2) {
            ClassMember(visibility = visibility, name = parts[1], memberType = parts[0])
        } else {
            ClassMember(visibility = visibility, name = rest)
        }
    }

    private fun parseClassRelationship(line: String): ClassRelationship? {
        val relationPatterns = listOf(
            "..|>" to ClassRelationship.ClassRelationType.REALIZATION,
            "<|.." to ClassRelationship.ClassRelationType.REALIZATION,
            "<|--" to ClassRelationship.ClassRelationType.INHERITANCE,
            "--|>" to ClassRelationship.ClassRelationType.INHERITANCE,
            "*--" to ClassRelationship.ClassRelationType.COMPOSITION,
            "--*" to ClassRelationship.ClassRelationType.COMPOSITION,
            "o--" to ClassRelationship.ClassRelationType.AGGREGATION,
            "--o" to ClassRelationship.ClassRelationType.AGGREGATION,
            "..>" to ClassRelationship.ClassRelationType.DEPENDENCY,
            "<.." to ClassRelationship.ClassRelationType.DEPENDENCY,
            "-->" to ClassRelationship.ClassRelationType.ASSOCIATION,
            "<--" to ClassRelationship.ClassRelationType.ASSOCIATION,
            "--" to ClassRelationship.ClassRelationType.ASSOCIATION,
        )

        for ((pattern, relType) in relationPatterns) {
            val range = line.indexOf(pattern)
            if (range < 0) continue

            val leftPart = line.substring(0, range).trim()
            val rightPart = line.substring(range + pattern.length).trim()

            // Check for label after " : "
            var toName: String
            var label: String? = null
            if (rightPart.contains(" : ")) {
                val labelParts = rightPart.split(" : ", limit = 2)
                toName = labelParts[0].trim()
                label = labelParts[1].trim()
            } else {
                toName = rightPart
            }

            // Check for cardinality: "1" ClassName
            var fromName = leftPart
            var fromCard: String? = null
            var toCard: String? = null

            val fromCardRegex = Regex("^\"(.+?)\"\\s+(.+)$")
            fromCardRegex.matchEntire(fromName)?.let {
                fromCard = it.groupValues[1]
                fromName = it.groupValues[2]
            }
            val toCardRegex = Regex("^(.+?)\\s+\"(.+?)\"$")
            toCardRegex.matchEntire(toName)?.let {
                toName = it.groupValues[1]
                toCard = it.groupValues[2]
            }

            fromName = fromName.trim()
            toName = toName.trim()
            if (fromName.isEmpty() || toName.isEmpty()) continue

            val isReversed = pattern.startsWith("<")
            val from = if (isReversed) toName else fromName
            val to = if (isReversed) fromName else toName

            return ClassRelationship(
                from = from, to = to, label = label,
                relationshipType = relType,
                fromCardinality = if (isReversed) toCard else fromCard,
                toCardinality = if (isReversed) fromCard else toCard
            )
        }
        return null
    }

    // endregion

    // region State Diagram Parser

    private fun parseStateDiagram(lines: List<String>): StateDiagram {
        val states = mutableMapOf<String, StateNode>()
        val transitions = mutableListOf<StateTransition>()

        for (line in lines) {
            // State description: state "description" as s1
            if (line.startsWith("state ")) {
                val stateDescRegex = Regex("state\\s+\"(.+?)\"\\s+as\\s+(\\S+)")
                stateDescRegex.find(line)?.let { match ->
                    val desc = match.groupValues[1]
                    val id = match.groupValues[2]
                    states[id] = StateNode(id = id, label = id, description = desc)
                    return@let
                } ?: run {
                    val rest = line.removePrefix("state ").trim()
                    if (rest.isNotEmpty() && !rest.contains("-->")) {
                        val name = rest.replace("\"", "")
                        if (name !in states) {
                            states[name] = StateNode(id = name, label = name)
                        }
                    }
                }
                continue
            }

            // Transition: State1 --> State2 : label
            if (line.contains("-->")) {
                val parts = line.split("-->", limit = 2)
                if (parts.size < 2) continue

                val from = parts[0].trim()
                val rightPart = parts[1].trim()

                val to: String
                var label: String? = null

                if (rightPart.contains(":")) {
                    val colonParts = rightPart.split(":", limit = 2)
                    to = colonParts[0].trim()
                    label = colonParts[1].trim()
                } else {
                    to = rightPart
                }

                val fromId = from
                val toId = to

                if (fromId !in states) states[fromId] = StateNode(id = fromId, label = fromId)
                if (toId !in states) states[toId] = StateNode(id = toId, label = toId)

                transitions.add(StateTransition(from = fromId, to = toId, label = label))
            }
        }

        return StateDiagram(
            states = states.values.sortedBy { it.id },
            transitions = transitions
        )
    }

    // endregion

    // region Gantt Diagram Parser

    private fun parseGanttDiagram(lines: List<String>): GanttDiagram {
        var title: String? = null
        var dateFormat: String? = null
        val sections = mutableListOf<GanttSection>()
        var currentSection = GanttSection(name = "Default")
        var hasSection = false

        for (line in lines) {
            if (line.startsWith("title ")) {
                title = line.removePrefix("title ").trim()
                continue
            }
            if (line.startsWith("dateFormat ")) {
                dateFormat = line.removePrefix("dateFormat ").trim()
                continue
            }
            if (line.startsWith("section ")) {
                if (hasSection) sections.add(currentSection)
                val name = line.removePrefix("section ").trim()
                currentSection = GanttSection(name = name)
                hasSection = true
                continue
            }
            if (line.startsWith("axisFormat") || line.startsWith("todayMarker") ||
                line.startsWith("excludes") || line.startsWith("inclusiveEndDates")) {
                continue
            }

            // Task
            parseGanttTask(line)?.let { task ->
                if (!hasSection) hasSection = true
                currentSection.tasks.add(task)
            }
        }

        if (hasSection) sections.add(currentSection)

        return GanttDiagram(title = title, dateFormat = dateFormat, sections = sections)
    }

    private fun parseGanttTask(line: String): GanttTask? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val colonIdx = trimmed.indexOf(':')
        if (colonIdx < 0) return null

        val name = trimmed.substring(0, colonIdx).trim()
        val metaStr = trimmed.substring(colonIdx + 1).trim()
        if (name.isEmpty()) return null

        val tokens = metaStr.split(",").map { it.trim() }.toMutableList()

        var status = GanttTask.TaskStatus.NORMAL
        var id: String? = null
        var afterId: String? = null
        var startDate: String? = null
        var duration: String? = null

        // Parse status flags
        val statusFlags = mutableListOf<String>()
        while (tokens.isNotEmpty() && tokens.first() in listOf("done", "active", "crit")) {
            statusFlags.add(tokens.removeFirst())
        }

        status = when {
            "crit" in statusFlags && "done" in statusFlags -> GanttTask.TaskStatus.CRITICAL_DONE
            "crit" in statusFlags && "active" in statusFlags -> GanttTask.TaskStatus.CRITICAL_ACTIVE
            "crit" in statusFlags -> GanttTask.TaskStatus.CRITICAL
            "done" in statusFlags -> GanttTask.TaskStatus.DONE
            "active" in statusFlags -> GanttTask.TaskStatus.ACTIVE
            else -> GanttTask.TaskStatus.NORMAL
        }

        // Remaining tokens: [id], start|after, duration
        for (token in tokens) {
            when {
                token.startsWith("after ") -> afterId = token.removePrefix("after ").trim()
                token.endsWith("d") || token.endsWith("w") || token.endsWith("h") -> duration = token
                token.contains("-") && token.length >= 8 -> startDate = token
                else -> id = token
            }
        }

        return GanttTask(name = name, id = id, status = status,
            startDate = startDate, duration = duration, afterId = afterId)
    }

    // endregion

    // region ER Diagram Parser

    private fun parseERDiagram(lines: List<String>): ERDiagram {
        val entities = mutableMapOf<String, EREntity>()
        val relationships = mutableListOf<ERRelationship>()
        var currentEntity: String? = null
        var inBlock = false

        for (line in lines) {
            // End of entity block
            if (line == "}" && inBlock) {
                inBlock = false
                currentEntity = null
                continue
            }

            // Inside entity block — parse attributes
            if (inBlock && currentEntity != null) {
                parseERAttribute(line)?.let { attr ->
                    entities[currentEntity]?.attributes?.add(attr)
                }
                continue
            }

            // Entity block: "ENTITY_NAME {"
            if (line.endsWith("{") && !line.contains("|")) {
                val name = line.removeSuffix("{").trim()
                if (name !in entities) {
                    entities[name] = EREntity(name = name)
                }
                currentEntity = name
                inBlock = true
                continue
            }

            // Relationship
            parseERRelationship(line)?.let { rel ->
                for (name in listOf(rel.from, rel.to)) {
                    if (name !in entities) {
                        entities[name] = EREntity(name = name)
                    }
                }
                relationships.add(rel)
            }
        }

        return ERDiagram(
            entities = entities.values.sortedBy { it.name },
            relationships = relationships
        )
    }

    private fun parseERAttribute(line: String): ERAttribute? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(" ")
        if (parts.size < 2) return null

        val attrType = parts[0]
        val name = parts[1]
        val key = if (parts.size >= 3) ERAttribute.AttributeKey.fromString(parts[2]) else null

        return ERAttribute(attributeType = attrType, name = name, key = key)
    }

    private fun parseERRelationship(line: String): ERRelationship? {
        val cardinalityPatterns = listOf(
            "||--||" to (ERRelationship.ERCardinality.EXACTLY_ONE to ERRelationship.ERCardinality.EXACTLY_ONE),
            "||--o{" to (ERRelationship.ERCardinality.EXACTLY_ONE to ERRelationship.ERCardinality.ZERO_OR_MORE),
            "||--|{" to (ERRelationship.ERCardinality.EXACTLY_ONE to ERRelationship.ERCardinality.ONE_OR_MORE),
            "||--o|" to (ERRelationship.ERCardinality.EXACTLY_ONE to ERRelationship.ERCardinality.ZERO_OR_ONE),
            "}o--||" to (ERRelationship.ERCardinality.ZERO_OR_MORE to ERRelationship.ERCardinality.EXACTLY_ONE),
            "}|--||" to (ERRelationship.ERCardinality.ONE_OR_MORE to ERRelationship.ERCardinality.EXACTLY_ONE),
            "o|--||" to (ERRelationship.ERCardinality.ZERO_OR_ONE to ERRelationship.ERCardinality.EXACTLY_ONE),
            "o{--||" to (ERRelationship.ERCardinality.ZERO_OR_MORE to ERRelationship.ERCardinality.EXACTLY_ONE),
            "}o--o{" to (ERRelationship.ERCardinality.ZERO_OR_MORE to ERRelationship.ERCardinality.ZERO_OR_MORE),
            "}|--o{" to (ERRelationship.ERCardinality.ONE_OR_MORE to ERRelationship.ERCardinality.ZERO_OR_MORE),
            "o|--o{" to (ERRelationship.ERCardinality.ZERO_OR_ONE to ERRelationship.ERCardinality.ZERO_OR_MORE),
            "}|--|{" to (ERRelationship.ERCardinality.ONE_OR_MORE to ERRelationship.ERCardinality.ONE_OR_MORE),
        )

        for ((pattern, cards) in cardinalityPatterns) {
            val idx = line.indexOf(pattern)
            if (idx < 0) continue

            val leftPart = line.substring(0, idx).trim()
            val rightPart = line.substring(idx + pattern.length).trim()

            var entityName: String
            var label: String

            if (rightPart.contains(" : ")) {
                val colonParts = rightPart.split(" : ", limit = 2)
                entityName = colonParts[0].trim()
                label = colonParts[1].trim().replace("\"", "")
            } else {
                entityName = rightPart
                label = ""
            }

            if (leftPart.isEmpty() || entityName.isEmpty()) continue

            return ERRelationship(
                from = leftPart, to = entityName, label = label,
                fromCardinality = cards.first, toCardinality = cards.second
            )
        }
        return null
    }

    // endregion
}
