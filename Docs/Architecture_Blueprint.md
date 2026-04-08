# CodeForge Architecture Blueprint

## Current State

CodeForge is now a small but structured JavaFX IDE rather than a single-editor prototype.
The current application includes:

- multi-tab editor sessions
- quick scratch tabs and direct named file creation
- multi-terminal sessions
- lazy-loaded workspace explorer
- file tree context actions
- a problems panel
- an AI Snapshot panel
- a configuration loader for `.env` values
- a language-service facade for validation and execution routing

The compiler remains a subset implementation, but the IDE shell around it is organized for continued growth.

## Active Architecture

```text
+--------------------------------------------------------------------------------------+
|                                       CodeForge                                      |
+--------------------------------------------------------------------------------------+
| Workbench UI                                                                         |
|  - MainApp                                                                           |
|  - Toolbar                                                                           |
|  - WorkspaceView                                                                     |
|  - Editor TabPane                                                                    |
|  - Bottom Panel (Terminals + Problems + AI Snapshot)                                 |
|  - Status Bar                                                                        |
+--------------------------------------------+-----------------------------------------+
                                             |
                                             v
+--------------------------------------------+-----------------------------------------+
|                                WorkbenchController                                   |
|  - scratch-file creation                                                              |
|  - direct file creation                                                               |
|  - open/save/run commands                                                             |
|  - active-session synchronization                                                     |
|  - diagnostics refresh                                                                |
|  - AI Snapshot refresh                                                                |
+----------------------------+---------------------------+------------------------------+
                             |                           | 
                             v                           v
+----------------------------+--+         +--------------------------------------------+
| Editor Session Layer          |         | Terminal Session Layer                      |
|  - EditorTabManager           |         |  - TerminalManager                          |
|  - EditorTabSession           |         |  - TerminalSession                          |
|  - CodeEditor                 |         |  - TerminalView                             |
|  - EditorController           |         |  - TerminalSessionListener                  |
+-------------------------------+         +--------------------------------------------+
                             |
                             v
+--------------------------------------------------------------------------------------+
|                                  LanguageService                                     |
|  - validate(source)                                                                  |
|  - runExternally(path, source)                                                       |
+--------------------------------------------+-----------------------------------------+
                                             |
                    +------------------------+------------------------+
                    |                                                 |
                    v                                                 v
+--------------------------------------------+         +--------------------------------+
|                                  Compiler Pipeline           |         | AI Snapshot Layer             |
|  Lexer -> Tokens -> Parser -> AST -> VM                     |         | AISnapshotService             |
|  Current parser supports a starter statement subset         |         | AISnapshotView                |
+-------------------------------------------------------------+         | GroqClient / LLMClient       |
                                                                        +--------------------------------+
```

## Module Responsibilities

### Workbench Layer

- `MainApp.java`: application startup, configuration load, JavaFX shell composition
- `WorkbenchController.java`: high-level orchestration for editor, file, terminal, diagnostics, and snapshot state

### Editor Layer

- `EditorTabManager.java`: tab/session ownership, active tab, save flow, and file-backed session updates
- `EditorTabSession.java`: one document session, one editor, one file path, one diagnostics list, one snapshot result
- `CodeEditor.java`: RichTextFX editor, syntax coloring, caret navigation, dirty/content listeners
- `EditorController.java`: runs the active editor buffer through the selected execution path

### Workspace Layer

- `WorkspaceView.java`: lazy tree UI and file action dispatch
- `FileManager.java`: file chooser, file read/write, rename, delete, copy, and file-creation helpers

### Terminal Layer

- `TerminalManager.java`: multiple terminal tabs and active terminal lookup
- `TerminalSession.java`: process lifecycle, stdin/stdout bridge, transcript capture, cleanup on close
- `TerminalView.java`: terminal output and input controls

### Problems Layer

- `ProblemsView.java`: diagnostics list for the active editor

### Snapshot Layer

- `AISnapshotView.java`: structured panel showing problem, cause, fix, and confidence
- `AISnapshotService.java`: validation summary, runtime output analysis, local error detection, and model fallback
- `AISnapshotResult.java`: structured snapshot result model

### Config Layer

- `ConfigManager.java`: loads `.env` once and provides configuration values to the app

### Language / Compiler Layer

- `LanguageService.java`: IDE-facing validation and runtime routing facade
- `compiler/lexer`: source to tokens with positions
- `compiler/parser`: tokens to AST for the supported grammar subset
- `compiler/ast`: immutable node models
- `compiler/vm`: current VM runtime work for the subset compiler

## Current File Creation And Save Flow

CodeForge now supports two complementary file flows:

### Scratch Flow

- `New Scratch` opens a new untitled editor tab
- the tab keeps in-memory content until the user saves it
- the existing untitled workflow remains intact for quick experiments

### Named File Flow

- `Create File` creates or opens a real file on disk immediately
- when a workspace is open, the workspace root is the preferred target directory
- otherwise the active file’s parent folder is used when available
- if no clean target directory is available, the app falls back to the normal save dialog flow

### Save Flow

- file-backed tabs save directly to their current path
- untitled scratch tabs still prompt for a real path
- path changes are tracked through `EditorTabManager`, so tab titles and session metadata stay aligned

## Current Runtime Flow

1. The user edits code inside `CodeEditor`.
2. `WorkbenchController` revalidates the active session through `LanguageService.validate(...)`.
3. Diagnostics are pushed into `ProblemsView`.
4. The per-session snapshot is updated with a validation summary.
5. `Run` triggers `WorkbenchController.runActive()`.
6. `EditorController` asks `LanguageService.runExternally(...)` for a process.
7. `PythonRunner` writes the current code to a real or temporary file and starts `python`.
8. `TerminalSession` streams the process output into `TerminalView` and stores a transcript.
9. When the process completes, `WorkbenchController` sends the transcript to `AISnapshotService`.
10. `AISnapshotView` updates with a short structured summary.

## Why This Structure Works Well

- `MainApp` stays focused on startup and layout.
- `WorkbenchController` remains the main orchestration point for user-facing behavior.
- File-backed tabs and scratch tabs share the same editor/session model.
- Terminal handling remains isolated from editor logic.
- Validation and runtime analysis each have a clear home.
- AI Snapshot stays separate from terminal rendering and editor state management.

## Near-Term Areas For Improvement

- add richer editor actions such as save all and close all
- preserve expanded workspace state across refreshes
- expand parser support beyond the current subset
- improve runtime diagnostics and process controls
- deepen AI Snapshot analysis while keeping the panel concise
