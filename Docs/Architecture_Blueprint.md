# CodeForge Architecture Blueprint

## Current assessment

The repository already has a useful split between `editor`, `terminal`, `file`, and `compiler`, which is the right direction.
The biggest architectural gap is that the compiler is still a proof of concept while the IDE shell is directly coupled to running external Python processes.

For the target product, the custom Python engine should become a first-class backend service inside the app, not just an experiment beside the editor.

## Recommended stack

- Desktop shell: JavaFX with RichTextFX
- Language engine: Pure Java compiler pipeline
- Execution model: Tree-walking interpreter first, bytecode VM second
- Testing: JUnit 5 for lexer/parser/semantic/runtime tests

JavaFX is a strong fit for this repo because it keeps the whole system in one language and one build toolchain.
That matters here because your hardest problem is the compiler, not IPC or web packaging.

## Target architecture

```text
+----------------------------------------------------------------------------------+
|                                   CodeForge IDE                                  |
+----------------------------------------------------------------------------------+
| Workbench Layer                                                                  |
|  - MainApp                                                                       |
|  - Toolbar / Command actions                                                     |
|  - WorkspaceView                                                                 |
|  - CodeEditor tabs                                                               |
|  - TerminalView / Problems panel                                                 |
+-----------------------------------------+----------------------------------------+
                                          |
                                          v
+-----------------------------------------+----------------------------------------+
|                             Language Service Facade                              |
|  - Open file                                                                        |
|  - Validate source                                                                  |
|  - Run source                                                                       |
|  - Surface diagnostics                                                              |
+-----------------------------------------+----------------------------------------+
                                          |
          +-------------------------------+-------------------------------+
          |                                                               |
          v                                                               v
+-----------------------------+                            +--------------------------------+
| Compiler Pipeline           |                            | Runtime                         |
|  Lexer                      |                            | Tree Interpreter (phase 1)      |
|  Parser                     |                            | or                              |
|  AST                        |                            | Bytecode Generator + VM (phase 2)|
|  Semantic Analyzer          |                            +--------------------------------+
|  Diagnostics                |
+-----------------------------+
          |
          v
+-----------------------------+
| Shared Core Models          |
|  Token                      |
|  AST Nodes                  |
|  Symbol Table               |
|  Source Span                |
|  Error Types                |
+-----------------------------+
```

## Module responsibilities

### IDE layer

- `editor/`: text buffer, syntax highlighting, cursor state, tab presentation
- `file/`: workspace tree, file open/save, path state
- `terminal/`: stdin/stdout bridge for the runtime
- `MainApp.java`: composition root only, minimal business logic

### Compiler layer

- `lexer/`: raw source to token stream, including indentation and source spans
- `parser/`: token stream to AST
- `semantic/`: symbol resolution, scope creation, binding checks, future type rules
- `vm/` or interpreter: AST execution or bytecode execution
- `token/`, `ast/`, `bytecode/`: shared immutable models

## Suggested roadmap

1. Finish lexer with indentation, comments, strings, numbers, keywords, and location-aware diagnostics.
2. Expand the parser to support statements, blocks, assignments, expressions, and function calls.
3. Add a semantic pass before execution so runtime behavior is not doing name resolution ad hoc.
4. Run the custom compiler from the IDE terminal instead of the external `python` command when the active file is a CodeForge-supported subset.
5. Add `src/test/java` with focused lexer and parser tests before increasing syntax coverage.

## Immediate repo improvements

- Keep `MainApp` focused on layout and event wiring.
- Stop letting parser/runtime depend on bare `RuntimeException`.
- Introduce position-rich tokens now so parser and diagnostics do not need a second refactor later.
- Prefer a tree-walking interpreter first. It is much faster to validate semantics and language design than jumping straight to bytecode.
