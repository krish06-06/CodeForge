package com.codeforge.problems;

import com.codeforge.language.Diagnostic;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.function.Consumer;

public class ProblemsView {

    private final BorderPane root;
    private final ListView<Diagnostic> listView;
    private Consumer<Diagnostic> openDiagnosticListener;

    public ProblemsView() {
        this.listView = new ListView<>();
        this.listView.getStyleClass().add("problems-list");
        this.listView.setPlaceholder(new Label("No problems"));
        this.listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Diagnostic item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String location = item.getLine() > 0
                        ? "Line " + item.getLine() + ", Col " + item.getColumn()
                        : "Unknown location";
                    setText(item.getSeverity() + "  " + item.getMessage() + "  [" + location + "]");
                }
            }
        });
        this.listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Diagnostic selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && openDiagnosticListener != null) {
                    openDiagnosticListener.accept(selected);
                }
            }
        });

        Label title = new Label("Problems");
        title.getStyleClass().add("section-label");

        BorderPane.setMargin(listView, new Insets(6, 0, 0, 0));
        this.root = new BorderPane(listView);
        this.root.setTop(title);
        this.root.getStyleClass().add("problems-shell");
    }

    public BorderPane getView() {
        return root;
    }

    public void setDiagnostics(List<Diagnostic> diagnostics) {
        listView.setItems(FXCollections.observableArrayList(diagnostics));
    }

    public void setOpenDiagnosticListener(Consumer<Diagnostic> listener) {
        this.openDiagnosticListener = listener;
    }
}
