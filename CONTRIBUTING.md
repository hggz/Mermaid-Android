# Contributing to Mermaid-Android

Thank you for your interest in contributing! This is a pure Kotlin Android library for rendering Mermaid diagrams natively using Android Canvas — no WebView or JavaScript required.

## Getting Started

1. Fork the repository
2. Clone your fork
3. Open in Android Studio
4. Run tests: `./gradlew :mermaidkotlin:test`

## Architecture

The library follows a clean pipeline architecture:

```
Mermaid DSL String → Parser → Diagram Model → Layout Engine → Renderer → Bitmap/PNG
```

- **Model** (`model/`): Data classes representing diagram elements
- **Parser** (`parser/`): Regex-based Mermaid DSL parser
- **Layout** (`layout/`): Position calculation with topological sort
- **Renderer** (`renderer/`): Android Canvas-based drawing

## Adding a New Diagram Type

1. Add model classes in `model/DiagramModel.kt`
2. Add parsing logic in `parser/MermaidParser.kt`
3. Add layout calculation in `layout/DiagramLayout.kt`
4. Add rendering in `renderer/DiagramRenderer.kt`
5. Wire it up in `MermaidKotlin.kt`
6. Add tests for each layer

## Code Style

- Follow standard Kotlin conventions
- Use data classes for models
- Keep the parser, layout, and renderer layers independent
- Write unit tests for all new functionality

## Submitting Changes

1. Create a feature branch
2. Make your changes
3. Run all tests: `./gradlew :mermaidkotlin:test`
4. Submit a pull request

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
