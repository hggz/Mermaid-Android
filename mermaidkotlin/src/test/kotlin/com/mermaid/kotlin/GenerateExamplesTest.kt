package com.mermaid.kotlin

import com.mermaid.kotlin.renderer.DiagramRenderer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Generates example PNG files from Mermaid DSL for README screenshots.
 *
 * Run with: ./gradlew :mermaidkotlin:test --tests "*.GenerateExamplesTest"
 *
 * Output: /tmp/Mermaid-Android/examples/
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GenerateExamplesTest {

    private val mermaid = MermaidKotlin()

    @Test
    fun generateFlowchartExample() {
        val bitmap = mermaid.render("""
            flowchart TD
                A[Rectangle] --> B(Rounded)
                B --> C([Stadium])
                C --> D{Diamond}
                D --> E((Circle))
                E --> F>Asymmetric]
        """.trimIndent())

        val pngBytes = DiagramRenderer.pngData(bitmap)
        val outFile = File("/tmp/Mermaid-Android/examples/flowchart.png")
        outFile.parentFile.mkdirs()
        outFile.writeBytes(pngBytes)
        println("✓ examples/flowchart.png (${pngBytes.size} bytes, ${bitmap.width}x${bitmap.height})")
        assert(pngBytes.size > 100) { "PNG too small" }
        // Verify PNG magic bytes
        assert(pngBytes[0] == 0x89.toByte())
        assert(pngBytes[1] == 0x50.toByte()) // P
        assert(pngBytes[2] == 0x4E.toByte()) // N
        assert(pngBytes[3] == 0x47.toByte()) // G
    }

    @Test
    fun generateSequenceExample() {
        val bitmap = mermaid.render("""
            sequenceDiagram
                participant A as Alice
                actor B as Bob
                A->>B: Solid arrow
                A-->>B: Dotted arrow
                A-)B: Async
                A--)B: Dotted async
        """.trimIndent())

        val pngBytes = DiagramRenderer.pngData(bitmap)
        val outFile = File("/tmp/Mermaid-Android/examples/sequence.png")
        outFile.parentFile.mkdirs()
        outFile.writeBytes(pngBytes)
        println("✓ examples/sequence.png (${pngBytes.size} bytes, ${bitmap.width}x${bitmap.height})")
        assert(pngBytes.size > 100) { "PNG too small" }
    }

    @Test
    fun generatePieExample() {
        val bitmap = mermaid.render("""
            pie title Distribution
                "Category A" : 40
                "Category B" : 35
                "Category C" : 25
        """.trimIndent())

        val pngBytes = DiagramRenderer.pngData(bitmap)
        val outFile = File("/tmp/Mermaid-Android/examples/pie.png")
        outFile.parentFile.mkdirs()
        outFile.writeBytes(pngBytes)
        println("✓ examples/pie.png (${pngBytes.size} bytes, ${bitmap.width}x${bitmap.height})")
        assert(pngBytes.size > 100) { "PNG too small" }
    }
}
