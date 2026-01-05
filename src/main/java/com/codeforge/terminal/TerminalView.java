package com.codeforge.terminal;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class TerminalView {

    private TextArea terminal;
    private BorderPane root;

    public TerminalView() {
        terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setStyle("-fx-font-family: Consolas; -fx-font-size: 13;");
        terminal.setPrefHeight(200);

        root = new BorderPane(terminal);
    }

    // Existing method (unchanged signature)
    public void print(String text) {
        Platform.runLater(() -> terminal.appendText(text));
    }

    // 🔥 NEW method (does not break anything)
    public void clear() {
        Platform.runLater(() -> terminal.clear());
    }

    public BorderPane getView() {
        return root;
    }
}
