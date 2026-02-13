package com.mermaid.kotlin.renderer

import android.graphics.*
import com.mermaid.kotlin.layout.*
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

        // Draw lifelines
        for (p in layout.participants) {
            drawLifeline(canvas, p)
        }

        // Draw participant boxes
        for (p in layout.participants) {
            drawParticipantBox(canvas, p)
        }

        // Draw messages
        for (msg in layout.messages) {
            drawMessage(canvas, msg)
        }

        return bitmap
    }

    fun renderPieChart(layout: DiagramLayout.PieLayout): Bitmap {
        val bitmap = createBitmap(layout.width, layout.height)
        val canvas = Canvas(bitmap)
        fillBackground(canvas, layout.width, layout.height)

        // Title
        layout.title?.let { title ->
            drawText(canvas, title, layout.titlePosition,
                fontSize = config.titleFontSize, bold = true, alignment = TextAlignment.CENTER)
        }

        // Draw slices
        for (slice in layout.slices) {
            drawPieSlice(canvas, slice, layout.center, layout.radius)
        }

        // Draw legend
        drawPieLegend(canvas, layout.slices, layout.center, layout.radius)

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

    // region Flowchart Drawing

    private fun drawFlowNode(canvas: Canvas, node: PositionedNode) {
        val frame = node.frame
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeColor
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.nodeBorderColor
            style = Paint.Style.STROKE
            strokeWidth = config.lineWidth
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
            fontSize = config.fontSize, bold = false, alignment = TextAlignment.CENTER)
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

        canvas.drawLine(edge.fromPoint.x, edge.fromPoint.y,
            edge.toPoint.x, edge.toPoint.y, paint)

        // Draw arrowhead
        drawArrowhead(canvas, edge.fromPoint, edge.toPoint)

        // Edge label
        val label = edge.edge.label
        val pos = edge.labelPosition
        if (label != null && pos != null) {
            // White background for label
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

    // region Text Drawing

    enum class TextAlignment { LEFT, CENTER, RIGHT }

    private fun drawText(canvas: Canvas, text: String, at: PointF,
                         fontSize: Float, bold: Boolean, alignment: TextAlignment) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
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
