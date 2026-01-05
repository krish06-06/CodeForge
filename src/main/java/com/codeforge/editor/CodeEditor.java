package com.codeforge.editor;

import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class CodeEditor {

    private TextArea textArea;
    private BorderPane root;

    // Track unsaved changes
    private boolean dirty = false;

    public CodeEditor() {
        textArea = new TextArea();
        textArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 14;");
        textArea.setWrapText(false);

        // Mark dirty when user edits
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            dirty = true;
        });

        root = new BorderPane(textArea);
    }

    public BorderPane getView() {
        return root;
    }

    public String getCode() {
        return textArea.getText();
    }

    // Used when opening a file (should NOT mark dirty)
    public void setCode(String code) {
        textArea.setText(code);
        dirty = false; // 🔥 file just loaded
    }

    // Optional helpers (won't break anything)
    public boolean isDirty() {
        return dirty;
    }

    public void markSaved() {
        dirty = false;
    }
}
