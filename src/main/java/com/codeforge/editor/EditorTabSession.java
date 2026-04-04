package com.codeforge.editor;

import com.codeforge.language.Diagnostic;
import com.codeforge.snapshot.AISnapshotResult;

import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class EditorTabSession {

    private static int untitledCounter = 1;

    private final Tab tab;
    private final CodeEditor editor;
    private Path filePath;
    private final String untitledName;
    private List<Diagnostic> diagnostics = Collections.emptyList();
    private AISnapshotResult snapshotResult = AISnapshotResult.idle();

    public EditorTabSession() {
        this(null, "");
    }

    public EditorTabSession(Path filePath, String content) {
        this.filePath = filePath;
        this.untitledName = filePath == null ? "untitled-" + untitledCounter++ + ".py" : null;
        this.editor = new CodeEditor();
        this.tab = new Tab();
        this.tab.setClosable(true);
        this.tab.setContent(new StackPane(editor.getView()));
        this.editor.setCode(content);
        this.editor.markSaved();
        refreshTitle();
        this.editor.setDirtyChangeListener(dirty -> refreshTitle());
    }

    public Tab getTab() {
        return tab;
    }

    public CodeEditor getEditor() {
        return editor;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
        refreshTitle();
    }

    public String getDisplayName() {
        return filePath != null ? filePath.getFileName().toString() : untitledName;
    }

    public void refreshTitle() {
        String title = getDisplayName();
        if (editor.isDirty()) {
            title += " *";
        }
        tab.setText(title);
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics == null ? Collections.emptyList() : List.copyOf(diagnostics);
    }

    public AISnapshotResult getSnapshotResult() {
        return snapshotResult;
    }

    public void setSnapshotResult(AISnapshotResult snapshotResult) {
        this.snapshotResult = snapshotResult == null ? AISnapshotResult.idle() : snapshotResult;
    }
}
