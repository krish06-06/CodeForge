package com.codeforge.editor;

import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;

public class EditorTabSession {

    private static int untitledCounter = 1;

    private final Tab tab;
    private final CodeEditor editor;
    private Path filePath;
    private final String untitledName;

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
}
