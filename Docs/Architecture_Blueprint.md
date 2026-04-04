# CodeForge Architecture Blueprint

## Current state

The IDE shell has now moved beyond the original single-editor prototype.
CodeForge currently has:

- multi-tab editor sessions
- multi-terminal sessions
- lazy-loaded workspace explorer
- file tree context actions
- a problems panel
- a language-service facade for validation and execution routing

The compiler is still a subset implementation, but the surrounding IDE architecture is now much better prepared for growth.

## Active architecture

```text
+--------------------------------------------------------------------------------------+
|                                       CodeForge                                      |
+--------------------------------------------------------------------------------------+
| Workbench UI                                                                         |
|  - MainApp                                                                           |
|  - Toolbar                                                                           |
|  - WorkspaceView                                                                     |
|  - Editor TabPane                                                                    |
|  - Bottom Panel (Terminals + Problems)                                               |
|  - Status Bar                                                                        |
+--------------------------------------------+-----------------------------------------+
                                             |
                                             v
+--------------------------------------------+-----------------------------------------+
|                                WorkbenchController                                   |
|  - Open/save/run commands                                                             |
|  - Dirty-tab close confirmation                                                       |
|  - Workspace rename/delete/copy/paste orchestration                                   |
|  - Active-session synchronization                                                     |
|  - Diagnostics refresh                                                                |
+----------------------------+---------------------------+------------------------------+
                             |                           |
                             v                           v
+----------------------------+--+         +--------------------------------------------+
| Editor Session Layer          |         | Terminal Session Layer                      |
|  - EditorTabManager           |         |  - TerminalManager                          |
|  - EditorTabSession           |         |  - TerminalSession                          |
|  - CodeEditor                 |         |  - TerminalView                             |
+-------------------------------+         +--------------------------------------------+
                             |
                             v
+--------------------------------------------------------------------------------------+
|                                  LanguageService                                     |
|  - validate(source)                                                                  |
|  - runExternally(path, source)                                                       |
|  Future: route supported syntax to the custom interpreter                            |
+--------------------------------------------+-----------------------------------------+
                                             |
                                             v
+--------------------------------------------------------------------------------------+
|                                  Compiler Pipeline                                   |
|  Lexer -> Tokens -> Parser -> AST -> VM                                              |
|  Current parser supports a starter statement/expression subset for IDE diagnostics    |
+--------------------------------------------------------------------------------------+
```

## Module responsibilities

### Workbench layer

- `MainApp.java`: layout composition and UI shell only
- `WorkbenchController.java`: application orchestration and shared workbench state

### Editor layer

- `EditorTabManager.java`: open tabs, active tab, close flow, save flow
- `EditorTabSession.java`: one document session, one editor, one file path, one diagnostics list
- `CodeEditor.java`: RichTextFX editor, syntax coloring, caret navigation, dirty/content events
- `EditorController.java`: run active editor buffer in the selected execution backend

### Workspace layer

- `WorkspaceView.java`: lazy tree UI and context menu dispatch
- `FileManager.java`: filesystem operations

### Terminal layer

- `TerminalManager.java`: multiple terminal sessions
- `TerminalSession.java`: process lifecycle, stdin/stdout bridge, cleanup on close
- `TerminalView.java`: terminal output and input controls

### Problems layer

- `ProblemsView.java`: diagnostics list for the active editor

### Language/compiler layer

- `LanguageService.java`: IDE-facing facade for validation and execution routing
- `compiler/lexer`: source to tokens with positions
- `compiler/parser`: tokens to AST for the supported grammar subset
- `compiler/ast`: immutable node models
- `compiler/vm`: tree-walking execution for the current subset

## Why this is better than the earlier structure

- `MainApp` no longer owns every interaction directly.
- File, editor, terminal, and diagnostics state each have a clearer home.
- The workspace tree no longer rebuilds the entire folder synchronously every time.
- The IDE now has a clean insertion point for the custom Python engine through `LanguageService`.

## Recommended next build phases

### Phase 1: IDE hardening

- Add save-all and close-all commands.
- Preserve expanded workspace nodes across refresh.
- Add process stop/restart controls per terminal.
- Add recent workspaces and session restore.

### Phase 2: better language tooling

- Expand parser support for blocks, comparisons, booleans, and function definitions.
- Add semantic analysis for variable binding and scope diagnostics.
- Surface diagnostics inline in the editor gutter, not only in the problems panel.

### Phase 3: custom execution path

- Run supported files through the custom interpreter first.
- Fallback to external Python only for unsupported syntax while the engine grows.
- Replace runtime printouts with structured runtime diagnostics for the IDE.
