package com.mermaid.kotlin

import com.mermaid.kotlin.layout.LayoutConfig
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
    private val outputDir = "/tmp/Mermaid-Android/examples"

    private fun saveAndVerify(name: String, bitmap: android.graphics.Bitmap) {
        val pngBytes = DiagramRenderer.pngData(bitmap)
        val outFile = File("$outputDir/$name.png")
        outFile.parentFile.mkdirs()
        outFile.writeBytes(pngBytes)
        println("✓ examples/$name.png (${pngBytes.size} bytes, ${bitmap.width}x${bitmap.height})")
        assert(pngBytes.size > 100) { "PNG too small" }
        assert(pngBytes[0] == 0x89.toByte())
        assert(pngBytes[1] == 0x50.toByte())
    }

    @Test
    fun generateFlowchartExample() {
        saveAndVerify("flowchart", mermaid.render("""
            flowchart TD
                A[Rectangle] --> B(Rounded)
                B --> C([Stadium])
                C --> D{Diamond}
                D --> E((Circle))
                E --> F>Asymmetric]
        """.trimIndent()))
    }

    @Test
    fun generateSequenceExample() {
        saveAndVerify("sequence", mermaid.render("""
            sequenceDiagram
                participant A as Alice
                actor B as Bob
                A->>B: Solid arrow
                A-->>B: Dotted arrow
                A-)B: Async
                A--)B: Dotted async
        """.trimIndent()))
    }

    @Test
    fun generatePieExample() {
        saveAndVerify("pie", mermaid.render("""
            pie title Distribution
                "Category A" : 40
                "Category B" : 35
                "Category C" : 25
        """.trimIndent()))
    }

    @Test
    fun generateClassDiagramExample() {
        saveAndVerify("class", mermaid.render("""
            classDiagram
                class Animal {
                    +String name
                    +int age
                    +makeSound()
                    +move()
                }
                class Dog {
                    +String breed
                    +bark()
                }
                class Cat {
                    +bool indoor
                    +meow()
                }
                Animal <|-- Dog
                Animal <|-- Cat
        """.trimIndent()))
    }

    @Test
    fun generateStateDiagramExample() {
        saveAndVerify("state", mermaid.render("""
            stateDiagram-v2
                [*] --> Idle
                Idle --> Processing : submit
                Processing --> Complete : done
                Processing --> Error : fail
                Error --> Idle : retry
                Complete --> [*]
        """.trimIndent()))
    }

    @Test
    fun generateGanttExample() {
        saveAndVerify("gantt", mermaid.render("""
            gantt
                title Project Timeline
                dateFormat YYYY-MM-DD
                section Planning
                Requirements : done, req1, 2024-01-01, 5d
                Design       : active, des1, after req1, 7d
                section Development
                Backend      : dev1, after des1, 10d
                Frontend     : dev2, after des1, 12d
                section Testing
                QA           : crit, qa1, after dev2, 5d
        """.trimIndent()))
    }

    @Test
    fun generateERDiagramExample() {
        saveAndVerify("er", mermaid.render("""
            erDiagram
                CUSTOMER {
                    int id PK
                    string name
                    string email UK
                }
                ORDER {
                    int id PK
                    date created
                    float total
                }
                CUSTOMER ||--o{ ORDER : places
        """.trimIndent()))
    }

    @Test
    fun generateSubgraphExample() {
        saveAndVerify("subgraph", mermaid.render("""
            flowchart TD
                subgraph Frontend
                    A[React App] --> B[API Client]
                end
                subgraph Backend
                    C[Express Server] --> D[(Database)]
                end
                B --> C
        """.trimIndent()))
    }

    @Test
    fun generateStyleExample() {
        saveAndVerify("style", mermaid.render("""
            flowchart LR
                A[Normal]:::blue --> B[Warning]:::orange
                B --> C[Error]:::red
                classDef blue fill:#bbdefb,stroke:#1565c0,color:#0d47a1
                classDef orange fill:#ffe0b2,stroke:#e65100,color:#bf360c
                classDef red fill:#ffcdd2,stroke:#b71c1c,color:#b71c1c
        """.trimIndent()))
    }

    @Test
    fun generateDarkModeExample() {
        val darkMermaid = MermaidKotlin.darkMode()
        saveAndVerify("dark_mode", darkMermaid.render("""
            flowchart TD
                A[Start] --> B{Decision}
                B -->|Yes| C[Process]
                B -->|No| D[Skip]
                C --> E[End]
                D --> E
        """.trimIndent()))
    }
}
