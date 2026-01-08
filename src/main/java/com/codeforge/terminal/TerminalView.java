package com.codeforge.terminal;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class TerminalView {

    private TextArea terminal;
    private TextField input;
    private VBox root;

    private Consumer<String> inputHandler;

    public TerminalView() {
        terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setStyle("-fx-font-family: Consolas; -fx-font-size: 13;");

        input = new TextField();
        input.setPromptText("Enter input...");
        input.setOnAction(e -> {
            if (inputHandler != null) {
                inputHandler.accept(input.getText());
                input.clear();
            }
        });

        root = new VBox(terminal, input);
    }

    public void print(String text) {
        Platform.runLater(() -> terminal.appendText(text));
    }

    public void clear() {
        Platform.runLater(() -> terminal.clear());
    }

    public void setInputHandler(Consumer<String> handler) {
        this.inputHandler = handler;
    }

    public VBox getView() {
        return root;
    }
}
