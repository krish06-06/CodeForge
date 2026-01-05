package com.codeforge;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import com.codeforge.editor.CodeEditor;
import com.codeforge.editor.EditorController;
import com.codeforge.terminal.TerminalView;
import com.codeforge.file.FileManager;
import com.codeforge.file.WorkspaceView;
import javafx.scene.control.TreeView;


import java.io.File;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        CodeEditor editor = new CodeEditor();
        TerminalView terminal = new TerminalView();

        WorkspaceView workspace = new WorkspaceView();


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

        // 🔓 Open File
        Button openFileButton = new Button("📂 Open File");
        openFileButton.setOnAction(e -> {
            try {
                String code = FileManager.openFile(stage);
                if (code != null) {
                    editor.setCode(code);
                    editor.markSaved();
                }
            } catch (Exception ex) {
                terminal.print("Open failed: " + ex.getMessage() + "\n");
            }
        });

        // 📁 Open Folder (workspace base)
        Button openFolderButton = new Button("🗂 Open Folder");
        openFolderButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File folder = chooser.showDialog(stage);
            if (folder != null) {
                terminal.print("Opened folder: " + folder.getAbsolutePath() + "\n");
                // (Tree view comes next — this is the base like VS Code)
            }
        });

        // 💾 Save
        Button saveButton = new Button("💾 Save");
        saveButton.setOnAction(e -> {
            try {
                FileManager.saveFile(stage, editor.getCode());
                editor.markSaved();
            } catch (Exception ex) {
                terminal.print("Save failed: " + ex.getMessage() + "\n");
            }
        });

        // ▶ Run
        Button runButton = new Button("▶ Run");
        runButton.setOnAction(e -> controller.runCode());

        HBox topBar = new HBox(
                openFileButton,
                openFolderButton,
                saveButton,
                runButton
        );
        topBar.setSpacing(8);
        topBar.setStyle("-fx-padding: 6; -fx-background-color: #2b2b2b;");

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
