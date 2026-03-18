package com.codeforge.terminal;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class TerminalView {

    private final TextArea terminal;
    private final TextField input;
    private final Button sendButton;
    private final VBox root;
    private Consumer<String> inputHandler;

    public TerminalView() {
        terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setWrapText(false);
        terminal.getStyleClass().add("terminal-output");
        VBox.setVgrow(terminal, Priority.ALWAYS);

        input = new TextField();
        input.setPromptText("Program input...");
        input.getStyleClass().add("terminal-input");
        input.setDisable(true);
        input.setOnAction(event -> submitInput());

        sendButton = new Button("Send");
        sendButton.getStyleClass().add("terminal-send");
        sendButton.setDisable(true);
        sendButton.setOnAction(event -> submitInput());

        HBox inputBar = new HBox(8, input, sendButton);
        inputBar.setPadding(new Insets(8, 0, 0, 0));
        HBox.setHgrow(input, Priority.ALWAYS);

        root = new VBox(terminal, inputBar);
        root.getStyleClass().add("terminal-shell");
        root.setPadding(new Insets(10));
        root.setSpacing(0);
    }

    public void print(String text) {
        Platform.runLater(() -> terminal.appendText(text));
    }

    public void clear() {
        Platform.runLater(terminal::clear);
    }

    public void echoInput(String text) {
        if (text == null || text.isBlank()) {
            print("\n");
        } else {
            print(text + "\n");
        }
    }

    public void setInputHandler(Consumer<String> handler) {
        inputHandler = handler;
        setInputEnabled(handler != null);
    }

    public void setInputEnabled(boolean enabled) {
        Platform.runLater(() -> {
            input.setDisable(!enabled);
            sendButton.setDisable(!enabled);
            if (enabled) {
                input.requestFocus();
            } else {
                input.clear();
            }
        });
    }

    public VBox getView() {
        return root;
    }

    private void submitInput() {
        String value = input.getText();
        if (inputHandler != null) {
            inputHandler.accept(value);
            input.clear();
        }
    }
}
