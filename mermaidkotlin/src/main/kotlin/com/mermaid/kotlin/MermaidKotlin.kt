package com.mermaid.kotlin

import android.graphics.Bitmap
import com.mermaid.kotlin.layout.DiagramLayout
import com.mermaid.kotlin.layout.LayoutConfig
import com.mermaid.kotlin.model.*
import com.mermaid.kotlin.parser.MermaidParser
import com.mermaid.kotlin.renderer.DiagramRenderer

/**
 * Pure Kotlin Mermaid diagram renderer for Android.
 *
 * Parses Mermaid DSL → lays out diagram → renders to Bitmap via Android Canvas.
 * No WebView, no JavaScript dependencies.
 *
 * Usage:
 * ```kotlin
 * val mermaid = MermaidKotlin()
 * val bitmap = mermaid.render("flowchart TD\n  A[Start] --> B[End]")
 * val pngBytes = mermaid.renderToPNG("sequenceDiagram\n  Alice->>Bob: Hello")
 * ```
 */
class MermaidKotlin(
    config: LayoutConfig = LayoutConfig.DEFAULT
) {
    private val parser = MermaidParser()
    private val layoutEngine = DiagramLayout(config)
    private val renderer = DiagramRenderer(config)

    /**
     * Parse and render a Mermaid DSL string to a Bitmap.
     */
    fun render(input: String): Bitmap {
        val diagram = parser.parse(input)
        return renderDiagram(diagram)
    }

    /**
     * Parse and render a Mermaid DSL string to PNG byte array.
     */
    fun renderToPNG(input: String): ByteArray {
        val bitmap = render(input)
        return DiagramRenderer.pngData(bitmap)
    }

    /**
     * Parse only — returns the diagram model without rendering.
     */
    fun parse(input: String): Diagram {
        return parser.parse(input)
    }

    private fun renderDiagram(diagram: Diagram): Bitmap {
        return when (diagram) {
            is FlowchartDiagram -> {
                val layout = layoutEngine.layoutFlowchart(diagram)
                renderer.renderFlowchart(layout)
            }
            is SequenceDiagram -> {
                val layout = layoutEngine.layoutSequenceDiagram(diagram)
                renderer.renderSequenceDiagram(layout)
            }
            is PieChartDiagram -> {
                val layout = layoutEngine.layoutPieChart(diagram)
                renderer.renderPieChart(layout)
            }
            else -> throw IllegalArgumentException("Unsupported diagram type: ${diagram.type}")
        }
    }
}
