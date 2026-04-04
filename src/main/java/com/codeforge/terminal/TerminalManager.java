package com.codeforge.terminal;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.LinkedHashMap;
import java.util.Map;

public class TerminalManager {

    private final BorderPane root;
    private final TabPane tabPane;
    private final Map<Tab, TerminalSession> sessionsByTab = new LinkedHashMap<>();

    public TerminalManager() {
        this.tabPane = new TabPane();
        this.tabPane.getStyleClass().add("terminal-tabs");

        Button newTerminalButton = new Button("+");
        newTerminalButton.getStyleClass().add("terminal-add-button");
        newTerminalButton.setOnAction(event -> createTerminal());

        HBox headerActions = new HBox(newTerminalButton);
        headerActions.setPadding(new Insets(8, 10, 8, 10));
        headerActions.getStyleClass().add("terminal-toolbar");
        HBox.setHgrow(headerActions, Priority.ALWAYS);

        this.root = new BorderPane(tabPane);
        this.root.setBottom(headerActions);
        this.root.getStyleClass().add("terminal-manager");

        createTerminal();
    }

    public BorderPane getView() {
        return root;
    }

    public TerminalSession createTerminal() {
        TerminalSession session = new TerminalSession("Terminal");
        sessionsByTab.put(session.getTab(), session);
        tabPane.getTabs().add(session.getTab());
        tabPane.getSelectionModel().select(session.getTab());
        session.getTab().setOnClosed(event -> {
            session.dispose();
            sessionsByTab.remove(session.getTab());
            if (tabPane.getTabs().isEmpty()) {
                createTerminal();
            }
        });
        return session;
    }

    public TerminalSession getActiveSession() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        return selected == null ? null : sessionsByTab.get(selected);
    }
}
