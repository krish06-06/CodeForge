package com.codeforge.editor;

import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class CodeEditor {

    private TextArea textArea;
    private BorderPane root;

    public CodeEditor() {
        textArea = new TextArea();
        textArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 14;");
        textArea.setWrapText(false);

        root = new BorderPane(textArea);
    }

    public BorderPane getView() {
        return root;
    }

    public String getCode() {
        return textArea.getText();
    }

    public void setCode(String code) {
        textArea.setText(code);
    }
}
