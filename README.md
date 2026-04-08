# CodeForge

CodeForge is a JavaFX desktop IDE focused on Python editing, execution, and lightweight runtime feedback.

## Current Features

- Multi-tab code editor built on RichTextFX
- Quick untitled scratch tabs for experiments
- Direct named file creation for normal file-based work
- Workspace explorer with lazy folder loading
- Multiple terminal tabs for program execution
- Problems panel backed by the internal lexer and parser
- AI Snapshot panel for short validation and runtime summaries
- External Python execution through the system `python` command
- Project configuration loaded from `.env` through `ConfigManager`

## Current Workflow

1. Open a folder or create a scratch tab.
2. Edit Python code in the active editor tab.
3. Use `Create File` for a real file on disk or `New Scratch` for temporary work.
4. Run the active file to stream output into the terminal panel.
5. Review pre-run diagnostics in Problems and runtime summaries in AI Snapshot.

## Run The Project

Requirements:

- Java 24 toolchain
- Python available on `PATH`

Commands:

```powershell
./gradlew run
./gradlew test
```

If AI Snapshot should use the Groq-backed runtime analysis path, add the required key to your local `.env` file before launching the app.

## Project Layout

- `src/main/java/com/codeforge`: JavaFX application code
- `src/main/java/com/codeforge/editor`: editor and tab-session layer
- `src/main/java/com/codeforge/file`: file and workspace helpers
- `src/main/java/com/codeforge/terminal`: terminal sessions and terminal UI
- `src/main/java/com/codeforge/language`: validation facade and diagnostics
- `src/main/java/com/codeforge/runner`: external Python execution
- `src/main/java/com/codeforge/snapshot`: AI Snapshot result, service, and provider classes
- `src/main/java/com/codeforge/config`: configuration loading
- `src/main/java/com/codeforge/compiler`: current lexer, parser, AST, and VM work
- `Docs`: architecture and project reference notes

## Notes

- The internal compiler currently powers IDE diagnostics more than full runtime execution.
- Runtime execution still goes through `PythonRunner`.
- Scratch tabs and real file-backed tabs are both first-class parts of the editor flow.
