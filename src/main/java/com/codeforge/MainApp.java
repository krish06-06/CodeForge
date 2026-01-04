package com.codeforge;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import com.codeforge.editor.CodeEditor;
import com.codeforge.editor.EditorController;
import com.codeforge.terminal.TerminalView;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        // Core UI components
        CodeEditor editor = new CodeEditor();
        TerminalView terminal = new TerminalView();

        // Controller (THIS WAS MISSING)
        EditorController controller =
                new EditorController(editor, terminal);

        // Split editor + terminal
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(
                editor.getView(),
                terminal.getView()
        );
        splitPane.setDividerPositions(0.7);

        // Run button (VS Code style)
        Button runButton = new Button("▶ Run");
        runButton.setOnAction(e -> controller.runCode());

        HBox topBar = new HBox(runButton);
        topBar.setStyle("-fx-padding: 6; -fx-background-color: #2b2b2b;");

        // Root layout
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm()
        );

        stage.setTitle("CodeForge – Python Editor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
