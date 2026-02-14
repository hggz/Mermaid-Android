package com.mermaid.kotlin.renderer

import android.graphics.*
import com.mermaid.kotlin.layout.*
import com.mermaid.kotlin.model.*
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders laid-out diagrams to Bitmap using Android Canvas.
 */
class DiagramRenderer(
    val config: LayoutConfig = LayoutConfig.DEFAULT
) {

    // region Public API

    fun renderFlowchart(layout: DiagramLayout.FlowchartLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        // Draw subgraphs (behind everything)
        for (sg in layout.subgraphs) {
            drawSubgraph(canvas, sg)
        }

        // Draw edges first (behind nodes)
        for (edge in layout.edges) {
            drawEdge(canvas, edge)
        }

        // Draw nodes
        for (node in layout.nodes) {
            drawFlowNode(canvas, node)
        }

        return bitmap
    }

    fun renderSequenceDiagram(layout: DiagramLayout.SequenceLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        for (p in layout.participants) { drawLifeline(canvas, p) }
        for (p in layout.participants) { drawParticipantBox(canvas, p) }
        for (msg in layout.messages) { drawMessage(canvas, msg) }

        return bitmap
    }

    fun renderPieChart(layout: DiagramLayout.PieLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        layout.title?.let { title ->
            drawText(canvas, title, layout.titlePosition,
                fontSize = config.titleFontSize, bold = true, alignment = TextAlignment.CENTER)
        }

        for (slice in layout.slices) {
            drawPieSlice(canvas, slice, layout.center, layout.radius)
        }

        drawPieLegend(canvas, layout.slices, layout.center, layout.radius)

        return bitmap
    }

    fun renderClassDiagram(layout: DiagramLayout.ClassDiagramLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        for (rel in layout.relationships) { drawClassRelationship(canvas, rel) }
        for (cls in layout.classes) { drawClassBox(canvas, cls) }

        return bitmap
    }

    fun renderStateDiagram(layout: DiagramLayout.StateDiagramLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        for (t in layout.transitions) { drawStateTransition(canvas, t) }
        for (s in layout.states) { drawState(canvas, s) }

        return bitmap
    }

    fun renderGanttChart(layout: DiagramLayout.GanttLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        layout.title?.let { title ->
            drawText(canvas, title, layout.titlePosition,
                fontSize = config.titleFontSize, bold = true, alignment = TextAlignment.CENTER)
        }

        // Grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 217, 217)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        for ((x, label) in layout.gridLines) {
            canvas.drawLine(x, config.padding + 35f, x, layout.height - config.padding, gridPaint)
            drawText(canvas, label, PointF(x, config.padding + 45f),
                fontSize = config.fontSize - 3, bold = false, alignment = TextAlignment.CENTER)
        }

        // Section labels
        for (section in layout.sections) {
            drawText(canvas, section.name, PointF(config.padding + 10f, section.y + 6f),
                fontSize = config.fontSize - 1, bold = true, alignment = TextAlignment.LEFT)
        }

        // Task bars
        for (task in layout.tasks) { drawGanttTask(canvas, task) }

        return bitmap
    }

    fun renderERDiagram(layout: DiagramLayout.ERDiagramLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        for (rel in layout.relationships) { drawERRelationship(canvas, rel) }
        for (entity in layout.entities) { drawEREntity(canvas, entity) }

        return bitmap
    }

    // endregion

    // region Bitmap Creation

    private val scale = 2f

    private fun createBitmap(width: Float, height: Float): Bitmap {
        val w = (width * scale).toInt()
        val h = (height * scale).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.density = (160 * scale).toInt() // 320 DPI
        return bitmap
    }

    private fun fillBackground(canvas: Canvas, width: Float, height: Float) {
        canvas.scale(scale, scale)
        val paint = Paint().apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    // endregion

    // region Color Parsing

    private fun parseCSSColor(css: String): Int? {
        val hex = css.trim()
        if (!hex.startsWith("#")) return null
        val hexStr = hex.drop(1)

        return when (hexStr.length) {
            3 -> {
                val r = hexStr[0].digitToInt(16)
                val g = hexStr[1].digitToInt(16)
                val b = hexStr[2].digitToInt(16)
                Color.rgb(r * 17, g * 17, b * 17)
            }
            6 -> {
                val value = hexStr.toLongOrNull(16) ?: return null
                Color.rgb(
                    ((value shr 16) and 0xFF).toInt(),
                    ((value shr 8) and 0xFF).toInt(),
                    (value and 0xFF).toInt()
                )
            }
            else -> null
        }
    }

    // endregion

    // region Subgraph Drawing

    private fun drawSubgraph(canvas: Canvas, subgraph: PositionedSubgraph) {
        val frame = subgraph.frame

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.subgraphFillColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.subgraphBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(6f, 3f), 0f)
        }

        canvas.drawRoundRect(frame, 8f, 8f, fillPaint)
        canvas.drawRoundRect(frame, 8f, 8f, strokePaint)

        drawText(canvas, subgraph.subgraph.label, subgraph.labelPosition,
            fontSize = config.fontSize - 1, bold = true, alignment = TextAlignment.LEFT)
    }

    // endregion

    // region Flowchart Drawing

    private fun drawFlowNode(canvas: Canvas, node: PositionedNode) {
        val frame = node.frame

        // Apply custom style if present
        val fillColor = node.style?.fill?.let { parseCSSColor(it) } ?: config.nodeColor
        val strokeColor = node.style?.stroke?.let { parseCSSColor(it) } ?: config.nodeBorderColor
        val strokeWidth = node.style?.strokeWidth ?: config.lineWidth
        val textColor = node.style?.color?.let { parseCSSColor(it) } ?: config.textColor

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

        when (node.node.shape) {
            NodeShape.RECTANGLE -> {
                canvas.drawRect(frame, fillPaint)
                canvas.drawRect(frame, strokePaint)
            }

            NodeShape.ROUNDED_RECT -> {
                canvas.drawRoundRect(frame, config.nodeCornerRadius, config.nodeCornerRadius, fillPaint)
                canvas.drawRoundRect(frame, config.nodeCornerRadius, config.nodeCornerRadius, strokePaint)
            }

            NodeShape.STADIUM -> {
                val radius = frame.height() / 2f
                canvas.drawRoundRect(frame, radius, radius, fillPaint)
                canvas.drawRoundRect(frame, radius, radius, strokePaint)
            }

            NodeShape.DIAMOND -> {
                val path = Path().apply {
                    moveTo(frame.centerX(), frame.top)
                    lineTo(frame.right, frame.centerY())
                    lineTo(frame.centerX(), frame.bottom)
                    lineTo(frame.left, frame.centerY())
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }

            NodeShape.HEXAGON -> {
                val inset = 15f
                val path = Path().apply {
                    moveTo(frame.left + inset, frame.top)
                    lineTo(frame.right - inset, frame.top)
                    lineTo(frame.right, frame.centerY())
                    lineTo(frame.right - inset, frame.bottom)
                    lineTo(frame.left + inset, frame.bottom)
                    lineTo(frame.left, frame.centerY())
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }

            NodeShape.CIRCLE -> {
                val diameter = min(frame.width(), frame.height())
                val cx = frame.centerX()
                val cy = frame.centerY()
                canvas.drawCircle(cx, cy, diameter / 2f, fillPaint)
                canvas.drawCircle(cx, cy, diameter / 2f, strokePaint)
            }

            NodeShape.ASYMMETRIC -> {
                val inset = 15f
                val path = Path().apply {
                    moveTo(frame.left + inset, frame.top)
                    lineTo(frame.right, frame.top)
                    lineTo(frame.right, frame.bottom)
                    lineTo(frame.left + inset, frame.bottom)
                    lineTo(frame.left, frame.centerY())
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
        }

        // Draw label
        drawText(canvas, node.node.label,
            PointF(frame.centerX(), frame.centerY()),
            fontSize = config.fontSize, bold = false, alignment = TextAlignment.CENTER,
            textColor = textColor)
    }

    private fun drawEdge(canvas: Canvas, edge: PositionedEdge) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        when (edge.edge.style) {
            EdgeStyle.SOLID -> { /* default */ }
            EdgeStyle.DOTTED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            EdgeStyle.THICK -> {
                paint.strokeWidth = config.lineWidth * 2
            }
            EdgeStyle.INVISIBLE -> return
        }

        // Draw multi-point path
        if (edge.points.size < 2) return

        val path = Path().apply {
            moveTo(edge.points[0].x, edge.points[0].y)
            for (i in 1 until edge.points.size) {
                lineTo(edge.points[i].x, edge.points[i].y)
            }
        }
        canvas.drawPath(path, paint)

        // Draw arrowhead at the last segment
        if (edge.points.size >= 2) {
            val from = edge.points[edge.points.size - 2]
            val to = edge.points[edge.points.size - 1]
            drawArrowhead(canvas, from, to)
        }

        // Edge label
        val label = edge.edge.label
        val pos = edge.labelPosition
        if (label != null && pos != null) {
            val textSize = measureText(label, config.fontSize - 2)
            val bgRect = RectF(
                pos.x - textSize.x / 2f - 4f,
                pos.y - textSize.y / 2f - 2f,
                pos.x + textSize.x / 2f + 4f,
                pos.y + textSize.y / 2f + 2f
            )
            val bgPaint = Paint().apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(bgRect, bgPaint)

            drawText(canvas, label, pos,
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.CENTER)
        }
    }

    private fun drawArrowhead(canvas: Canvas, from: PointF, to: PointF) {
        val arrowLength = 10f
        val arrowWidth = 6f
        val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())

        val p1 = PointF(
            (to.x - arrowLength * cos(angle) + arrowWidth * sin(angle)).toFloat(),
            (to.y - arrowLength * sin(angle) - arrowWidth * cos(angle)).toFloat()
        )
        val p2 = PointF(
            (to.x - arrowLength * cos(angle) - arrowWidth * sin(angle)).toFloat(),
            (to.y - arrowLength * sin(angle) + arrowWidth * cos(angle)).toFloat()
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.arrowColor
            style = Paint.Style.FILL
        }

        val path = Path().apply {
            moveTo(to.x, to.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        canvas.drawPath(path, paint)
    }

    // endregion

    // region Sequence Diagram Drawing

    private fun drawLifeline(canvas: Canvas, participant: PositionedParticipant) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.lifelineColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        canvas.drawLine(participant.lifelineX, participant.lifelineTop,
            participant.lifelineX, participant.lifelineBottom, paint)
    }

    private fun drawParticipantBox(canvas: Canvas, participant: PositionedParticipant) {
        val frame = participant.headerFrame

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        canvas.drawRoundRect(frame, 6f, 6f, fillPaint)
        canvas.drawRoundRect(frame, 6f, 6f, strokePaint)

        drawText(canvas, participant.participant.label,
            PointF(frame.centerX(), frame.centerY()),
            fontSize = config.fontSize, bold = true, alignment = TextAlignment.CENTER)
    }

    private fun drawMessage(canvas: Canvas, message: PositionedMessage) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        when (message.message.style) {
            MessageStyle.DOTTED_ARROW, MessageStyle.DOTTED_LINE, MessageStyle.DOTTED_CROSS -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            else -> { /* solid */ }
        }

        val fromPt = PointF(message.fromX, message.y)
        val toPt = PointF(message.toX, message.y)

        canvas.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y, paint)

        // Arrow/cross at endpoint
        when (message.message.style) {
            MessageStyle.SOLID_ARROW, MessageStyle.DOTTED_ARROW -> {
                drawArrowhead(canvas, fromPt, toPt)
            }
            MessageStyle.SOLID_CROSS, MessageStyle.DOTTED_CROSS -> {
                drawCross(canvas, toPt)
            }
            else -> {
                drawArrowhead(canvas, fromPt, toPt)
            }
        }

        // Label
        val labelX = (message.fromX + message.toX) / 2f
        val labelY = message.y - 8f
        drawText(canvas, message.message.text,
            PointF(labelX, labelY),
            fontSize = config.fontSize - 1, bold = false, alignment = TextAlignment.CENTER)
    }

    private fun drawCross(canvas: Canvas, at: PointF) {
        val size = 6f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.arrowColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }
        canvas.drawLine(at.x - size, at.y - size, at.x + size, at.y + size, paint)
        canvas.drawLine(at.x + size, at.y - size, at.x - size, at.y + size, paint)
    }

    // endregion

    // region Pie Chart Drawing

    private fun drawPieSlice(canvas: Canvas, slice: PositionedPieSlice,
                             center: PointF, radius: Float) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = slice.color
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )

        canvas.drawArc(oval, slice.startAngle, slice.sweepAngle, true, fillPaint)
        canvas.drawArc(oval, slice.startAngle, slice.sweepAngle, true, strokePaint)
    }

    private fun drawPieLegend(canvas: Canvas, slices: List<PositionedPieSlice>,
                              center: PointF, radius: Float) {
        val legendX = center.x + radius + 40f
        var legendY = center.y - slices.size * 12f

        for (slice in slices) {
            // Color swatch
            val swatchRect = RectF(legendX, legendY - 6f, legendX + 12f, legendY + 6f)
            val swatchPaint = Paint().apply {
                color = slice.color
                style = Paint.Style.FILL
            }
            canvas.drawRect(swatchRect, swatchPaint)

            // Label
            val text = String.format("%s (%.1f%%)", slice.slice.label, slice.percentage)
            drawText(canvas, text,
                PointF(legendX + 20f, legendY),
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.LEFT)

            legendY += 24f
        }
    }

    // endregion

    // region Class Diagram Drawing

    private fun drawClassBox(canvas: Canvas, classBox: PositionedClassBox) {
        val cls = classBox.classDef

        // Full box background + border
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }
        canvas.drawRect(classBox.frame, bgPaint)
        canvas.drawRect(classBox.frame, borderPaint)

        // Header background
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(classBox.headerFrame, headerPaint)

        // Header text
        var headerY = classBox.headerFrame.centerY()
        cls.annotation?.let { annotation ->
            headerY -= 6f
            drawText(canvas, "<<$annotation>>",
                PointF(classBox.headerFrame.centerX(), headerY - 2f),
                fontSize = config.fontSize - 3, bold = false, alignment = TextAlignment.CENTER)
            headerY += 12f
        }
        drawText(canvas, cls.name,
            PointF(classBox.headerFrame.centerX(), headerY),
            fontSize = config.fontSize, bold = true, alignment = TextAlignment.CENTER)

        // Separator line under header
        val linePaint = Paint().apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(classBox.frame.left, classBox.headerFrame.bottom,
            classBox.frame.right, classBox.headerFrame.bottom, linePaint)

        // Properties
        val propStartY = classBox.propertiesFrame.top + 4f
        for ((i, prop) in cls.properties.withIndex()) {
            val y = propStartY + i * config.classMemberHeight + config.classMemberHeight / 2f
            val typeStr = if (prop.memberType != null) "${prop.memberType} " else ""
            val text = "${prop.visibility.symbol}$typeStr${prop.name}"
            drawText(canvas, text,
                PointF(classBox.frame.left + 10f, y),
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.LEFT)
        }

        // Separator between properties and methods
        if (cls.properties.isNotEmpty() || cls.methods.isNotEmpty()) {
            linePaint.strokeWidth = 0.5f
            canvas.drawLine(classBox.frame.left, classBox.methodsFrame.top,
                classBox.frame.right, classBox.methodsFrame.top, linePaint)
        }

        // Methods
        val methStartY = classBox.methodsFrame.top + 4f
        for ((i, meth) in cls.methods.withIndex()) {
            val y = methStartY + i * config.classMemberHeight + config.classMemberHeight / 2f
            val returnType = if (meth.memberType != null) " ${meth.memberType}" else ""
            val text = "${meth.visibility.symbol}${meth.name}$returnType"
            drawText(canvas, text,
                PointF(classBox.frame.left + 10f, y),
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.LEFT)
        }
    }

    private fun drawClassRelationship(canvas: Canvas, rel: PositionedClassRelationship) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        when (rel.relationship.relationshipType) {
            ClassRelationship.ClassRelationType.DEPENDENCY,
            ClassRelationship.ClassRelationType.REALIZATION -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            else -> {}
        }

        canvas.drawLine(rel.fromPoint.x, rel.fromPoint.y, rel.toPoint.x, rel.toPoint.y, paint)

        // Draw relationship markers
        drawRelationshipMarker(canvas, rel.relationship.relationshipType,
            rel.fromPoint, rel.toPoint)

        // Labels
        rel.relationship.label?.let { label ->
            rel.labelPos?.let { pos ->
                drawText(canvas, label, pos,
                    fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.CENTER)
            }
        }
        rel.relationship.fromCardinality?.let { card ->
            rel.fromLabelPos?.let { pos ->
                drawText(canvas, card, pos,
                    fontSize = config.fontSize - 3, bold = false, alignment = TextAlignment.CENTER)
            }
        }
        rel.relationship.toCardinality?.let { card ->
            rel.toLabelPos?.let { pos ->
                drawText(canvas, card, pos,
                    fontSize = config.fontSize - 3, bold = false, alignment = TextAlignment.CENTER)
            }
        }
    }

    private fun drawRelationshipMarker(canvas: Canvas, type: ClassRelationship.ClassRelationType,
                                       from: PointF, to: PointF) {
        val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()).toFloat()
        val size = 12f

        when (type) {
            ClassRelationship.ClassRelationType.INHERITANCE,
            ClassRelationship.ClassRelationType.REALIZATION -> {
                drawTriangleArrow(canvas, to, angle, size, filled = false)
            }
            ClassRelationship.ClassRelationType.COMPOSITION -> {
                drawDiamond(canvas, from, angle + Math.PI.toFloat(), size, filled = true)
            }
            ClassRelationship.ClassRelationType.AGGREGATION -> {
                drawDiamond(canvas, from, angle + Math.PI.toFloat(), size, filled = false)
            }
            ClassRelationship.ClassRelationType.ASSOCIATION,
            ClassRelationship.ClassRelationType.DEPENDENCY -> {
                drawArrowhead(canvas, from, to)
            }
        }
    }

    private fun drawTriangleArrow(canvas: Canvas, at: PointF, angle: Float,
                                  size: Float, filled: Boolean) {
        val halfWidth = size / 2.5f
        val p1 = PointF(
            (at.x - size * cos(angle.toDouble()) + halfWidth * sin(angle.toDouble())).toFloat(),
            (at.y - size * sin(angle.toDouble()) - halfWidth * cos(angle.toDouble())).toFloat()
        )
        val p2 = PointF(
            (at.x - size * cos(angle.toDouble()) - halfWidth * sin(angle.toDouble())).toFloat(),
            (at.y - size * sin(angle.toDouble()) + halfWidth * cos(angle.toDouble())).toFloat()
        )

        val path = Path().apply {
            moveTo(at.x, at.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }

        if (filled) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, paint)
        } else {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.STROKE
                strokeWidth = config.lineWidth
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawDiamond(canvas: Canvas, at: PointF, angle: Float,
                            size: Float, filled: Boolean) {
        val halfWidth = size / 3f
        val a = angle.toDouble()

        val tip = at
        val left = PointF(
            (tip.x + (size / 2) * cos(a) + halfWidth * sin(a)).toFloat(),
            (tip.y + (size / 2) * sin(a) - halfWidth * cos(a)).toFloat()
        )
        val back = PointF(
            (tip.x + size * cos(a)).toFloat(),
            (tip.y + size * sin(a)).toFloat()
        )
        val right = PointF(
            (tip.x + (size / 2) * cos(a) - halfWidth * sin(a)).toFloat(),
            (tip.y + (size / 2) * sin(a) + halfWidth * cos(a)).toFloat()
        )

        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(back.x, back.y)
            lineTo(right.x, right.y)
            close()
        }

        if (filled) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, paint)
        } else {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.STROKE
                strokeWidth = config.lineWidth
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }

    // endregion

    // region State Diagram Drawing

    private fun drawState(canvas: Canvas, state: PositionedState) {
        val frame = state.frame

        if (state.isStartEnd) {
            // Start/end marker: filled circle
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.FILL
            }
            canvas.drawOval(frame, paint)
            return
        }

        // Regular state: rounded rectangle
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        canvas.drawRoundRect(frame, config.stateCornerRadius, config.stateCornerRadius, fillPaint)
        canvas.drawRoundRect(frame, config.stateCornerRadius, config.stateCornerRadius, strokePaint)

        // State label
        var labelY = frame.centerY()
        val desc = state.state.description
        if (desc != null) {
            labelY -= 8f
            drawText(canvas, state.state.label,
                PointF(frame.centerX(), labelY),
                fontSize = config.fontSize, bold = true, alignment = TextAlignment.CENTER)
            drawText(canvas, desc,
                PointF(frame.centerX(), labelY + 16f),
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.CENTER)
        } else {
            drawText(canvas, state.state.label,
                PointF(frame.centerX(), labelY),
                fontSize = config.fontSize, bold = false, alignment = TextAlignment.CENTER)
        }
    }

    private fun drawStateTransition(canvas: Canvas, transition: PositionedStateTransition) {
        if (transition.points.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }

        val path = Path().apply {
            moveTo(transition.points[0].x, transition.points[0].y)
            for (i in 1 until transition.points.size) {
                lineTo(transition.points[i].x, transition.points[i].y)
            }
        }
        canvas.drawPath(path, paint)

        if (transition.points.size >= 2) {
            val from = transition.points[transition.points.size - 2]
            val to = transition.points[transition.points.size - 1]
            drawArrowhead(canvas, from, to)
        }

        // Label
        transition.transition.label?.let { label ->
            transition.labelPosition?.let { pos ->
                val textSize = measureText(label, config.fontSize - 2)
                val bgRect = RectF(
                    pos.x - textSize.x / 2f - 4f,
                    pos.y - textSize.y / 2f - 2f,
                    pos.x + textSize.x / 2f + 4f,
                    pos.y + textSize.y / 2f + 2f
                )
                val bgPaint = Paint().apply {
                    color = config.backgroundColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(bgRect, bgPaint)

                drawText(canvas, label, pos,
                    fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.CENTER)
            }
        }
    }

    // endregion

    // region Gantt Chart Drawing

    private fun drawGanttTask(canvas: Canvas, task: PositionedGanttTask) {
        val bar = task.bar

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = task.color
            style = Paint.Style.FILL
        }

        // Rounded bar
        canvas.drawRoundRect(bar, 4f, 4f, fillPaint)

        // Border for active/critical tasks
        if (task.task.status == GanttTask.TaskStatus.ACTIVE ||
            task.task.status == GanttTask.TaskStatus.CRITICAL ||
            task.task.status == GanttTask.TaskStatus.CRITICAL_ACTIVE) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.edgeColor
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(bar, 4f, 4f, borderPaint)
        }

        // Stripe pattern for done tasks
        if (task.task.status == GanttTask.TaskStatus.DONE ||
            task.task.status == GanttTask.TaskStatus.CRITICAL_DONE) {
            val stripePaint = Paint().apply {
                color = Color.argb(77, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            var x = bar.left + 4f
            while (x < bar.right) {
                canvas.drawLine(x, bar.top, x - 8f, bar.bottom, stripePaint)
                x += 6f
            }
        }

        // Task name label (left side)
        drawText(canvas, task.task.name, task.labelPosition,
            fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.LEFT)

        // Task name on bar (white text)
        drawText(canvas, task.task.name,
            PointF(bar.centerX(), bar.centerY()),
            fontSize = config.fontSize - 3, bold = false, alignment = TextAlignment.CENTER,
            textColor = Color.WHITE)
    }

    // endregion

    // region ER Diagram Drawing

    private fun drawEREntity(canvas: Canvas, entity: PositionedEREntity) {
        // Full box
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }
        canvas.drawRect(entity.frame, bgPaint)
        canvas.drawRect(entity.frame, borderPaint)

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(entity.headerFrame, headerPaint)

        drawText(canvas, entity.entity.name,
            PointF(entity.headerFrame.centerX(), entity.headerFrame.centerY()),
            fontSize = config.fontSize, bold = true, alignment = TextAlignment.CENTER)

        // Separator
        val linePaint = Paint().apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawLine(entity.frame.left, entity.headerFrame.bottom,
            entity.frame.right, entity.headerFrame.bottom, linePaint)

        // Attributes
        for ((i, attr) in entity.entity.attributes.withIndex()) {
            if (i >= entity.attributeFrames.size) break
            val attrFrame = entity.attributeFrames[i]
            val keyStr = if (attr.key != null) " ${attr.key.value}" else ""
            val text = "${attr.attributeType} ${attr.name}$keyStr"
            drawText(canvas, text,
                PointF(entity.frame.left + 10f, attrFrame.centerY()),
                fontSize = config.fontSize - 2, bold = attr.key != null, alignment = TextAlignment.LEFT)
        }
    }

    private fun drawERRelationship(canvas: Canvas, rel: PositionedERRelationship) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
        }
        canvas.drawLine(rel.fromPoint.x, rel.fromPoint.y, rel.toPoint.x, rel.toPoint.y, paint)

        // Draw cardinality markers
        val angle = atan2((rel.toPoint.y - rel.fromPoint.y).toDouble(),
            (rel.toPoint.x - rel.fromPoint.x).toDouble()).toFloat()
        drawERCardinality(canvas, rel.relationship.fromCardinality, rel.fromPoint, angle)
        drawERCardinality(canvas, rel.relationship.toCardinality, rel.toPoint,
            angle + Math.PI.toFloat())

        // Label
        if (rel.relationship.label.isNotEmpty()) {
            val textSize = measureText(rel.relationship.label, config.fontSize - 2)
            val bgRect = RectF(
                rel.labelPosition.x - textSize.x / 2f - 4f,
                rel.labelPosition.y - textSize.y / 2f - 2f,
                rel.labelPosition.x + textSize.x / 2f + 4f,
                rel.labelPosition.y + textSize.y / 2f + 2f
            )
            val bgPaint = Paint().apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(bgRect, bgPaint)

            drawText(canvas, rel.relationship.label, rel.labelPosition,
                fontSize = config.fontSize - 2, bold = false, alignment = TextAlignment.CENTER)
        }
    }

    private fun drawERCardinality(canvas: Canvas, cardinality: ERRelationship.ERCardinality,
                                  at: PointF, angle: Float) {
        val offset = 15f
        val a = angle.toDouble()
        val markerPoint = PointF(
            (at.x + offset * cos(a)).toFloat(),
            (at.y + offset * sin(a)).toFloat()
        )

        val perpAngle = a + Math.PI / 2
        val lineLen = 8f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        when (cardinality) {
            ERRelationship.ERCardinality.EXACTLY_ONE -> {
                // Two vertical lines ||
                for (d in listOf(-3f, 3f)) {
                    val p = PointF(
                        (markerPoint.x + d * cos(a)).toFloat(),
                        (markerPoint.y + d * sin(a)).toFloat()
                    )
                    canvas.drawLine(
                        (p.x - lineLen / 2 * cos(perpAngle)).toFloat(),
                        (p.y - lineLen / 2 * sin(perpAngle)).toFloat(),
                        (p.x + lineLen / 2 * cos(perpAngle)).toFloat(),
                        (p.y + lineLen / 2 * sin(perpAngle)).toFloat(),
                        paint
                    )
                }
            }
            ERRelationship.ERCardinality.ZERO_OR_ONE -> {
                // Line and circle |o
                canvas.drawLine(
                    (markerPoint.x - lineLen / 2 * cos(perpAngle)).toFloat(),
                    (markerPoint.y - lineLen / 2 * sin(perpAngle)).toFloat(),
                    (markerPoint.x + lineLen / 2 * cos(perpAngle)).toFloat(),
                    (markerPoint.y + lineLen / 2 * sin(perpAngle)).toFloat(),
                    paint
                )
                val circleCenter = PointF(
                    (markerPoint.x + 8 * cos(a)).toFloat(),
                    (markerPoint.y + 8 * sin(a)).toFloat()
                )
                canvas.drawCircle(circleCenter.x, circleCenter.y, 4f, paint)
            }
            ERRelationship.ERCardinality.ZERO_OR_MORE -> {
                // Circle and crow's foot o{
                val circleCenter = PointF(
                    (markerPoint.x - 4 * cos(a)).toFloat(),
                    (markerPoint.y - 4 * sin(a)).toFloat()
                )
                canvas.drawCircle(circleCenter.x, circleCenter.y, 4f, paint)
                drawCrowsFoot(canvas, PointF(
                    (markerPoint.x + 6 * cos(a)).toFloat(),
                    (markerPoint.y + 6 * sin(a)).toFloat()
                ), angle, lineLen)
            }
            ERRelationship.ERCardinality.ONE_OR_MORE -> {
                // Line and crow's foot }|
                canvas.drawLine(
                    (markerPoint.x - lineLen / 2 * cos(perpAngle)).toFloat(),
                    (markerPoint.y - lineLen / 2 * sin(perpAngle)).toFloat(),
                    (markerPoint.x + lineLen / 2 * cos(perpAngle)).toFloat(),
                    (markerPoint.y + lineLen / 2 * sin(perpAngle)).toFloat(),
                    paint
                )
                drawCrowsFoot(canvas, PointF(
                    (markerPoint.x + 8 * cos(a)).toFloat(),
                    (markerPoint.y + 8 * sin(a)).toFloat()
                ), angle, lineLen)
            }
        }
    }

    private fun drawCrowsFoot(canvas: Canvas, at: PointF, angle: Float, size: Float) {
        val a = angle.toDouble()
        val perpAngle = a + Math.PI / 2
        val forkLen = size / 1.5f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.edgeColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val tip = PointF(
            (at.x + forkLen * cos(a)).toFloat(),
            (at.y + forkLen * sin(a)).toFloat()
        )
        canvas.drawLine(tip.x, tip.y,
            (at.x + size / 2 * cos(perpAngle)).toFloat(),
            (at.y + size / 2 * sin(perpAngle)).toFloat(), paint)
        canvas.drawLine(tip.x, tip.y,
            (at.x - size / 2 * cos(perpAngle)).toFloat(),
            (at.y - size / 2 * sin(perpAngle)).toFloat(), paint)
        canvas.drawLine(tip.x, tip.y, at.x, at.y, paint)
    }

    // endregion

    // region Text Drawing

    enum class TextAlignment { LEFT, CENTER, RIGHT }

    private fun drawText(canvas: Canvas, text: String, at: PointF,
                         fontSize: Float, bold: Boolean, alignment: TextAlignment,
                         textColor: Int? = null) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor ?: config.textColor
            textSize = fontSize
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = when (alignment) {
                TextAlignment.LEFT -> Paint.Align.LEFT
                TextAlignment.CENTER -> Paint.Align.CENTER
                TextAlignment.RIGHT -> Paint.Align.RIGHT
            }
        }

        // Vertically center the text
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val drawY = at.y - metrics.ascent - textHeight / 2f

        canvas.drawText(text, at.x, drawY, paint)
    }

    internal fun measureText(text: String, fontSize: Float): PointF {
        val paint = Paint().apply {
            textSize = fontSize
            typeface = Typeface.DEFAULT
        }
        val width = paint.measureText(text)
        val metrics = paint.fontMetrics
        val height = metrics.descent - metrics.ascent
        return PointF(width, height)
    }

    // endregion

    companion object {
        /**
         * Convert a Bitmap to PNG byte array.
         */
        fun pngData(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }
    }
}
