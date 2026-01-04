package com.codeforge.terminal;

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

    public void print(String text) {
        terminal.appendText(text + "\n");
    }

    public BorderPane getView() {
        return root;
    }
}
