package com.codeforge;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import com.codeforge.editor.CodeEditor;
import com.codeforge.editor.EditorController;
import com.codeforge.terminal.TerminalView;
import com.codeforge.file.FileManager;
import com.codeforge.file.WorkspaceView;

import java.io.File;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        CodeEditor editor = new CodeEditor();
        TerminalView terminal = new TerminalView();
        WorkspaceView workspace = new WorkspaceView();

        EditorController controller =
                new EditorController(editor, terminal);

        // 🔹 TreeView click → open file
        workspace.getView().setOnMouseClicked(e -> {
            TreeView<File> view = workspace.getView();
            TreeItem<File> item = view.getSelectionModel().getSelectedItem();
            if (item == null) return;

            File file = item.getValue();
            if (file.isFile() && file.getName().endsWith(".py")) {
                try {
                    String code = java.nio.file.Files.readString(file.toPath());
                    editor.setCode(code);
                    FileManager.setCurrentFile(file.toPath());
                    editor.markSaved();
                } catch (Exception ex) {
                    terminal.print("Open failed: " + ex.getMessage() + "\n");
                }
            }
        });

        // 🔹 Editor + Terminal (RIGHT SIDE)
        SplitPane editorSplit = new SplitPane(
                editor.getView(),
                terminal.getView()
        );
        editorSplit.setDividerPositions(0.7);

        // 🔹 Workspace + Editor (MAIN)
        SplitPane mainSplit = new SplitPane(
                workspace.getView(),
                editorSplit
        );
        mainSplit.setDividerPositions(0.25);

        // 🔹 Buttons
        Button openFileBtn = new Button("📂 Open File");
        openFileBtn.setOnAction(e -> {
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

        Button openFolderBtn = new Button("🗂 Open Folder");
        openFolderBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File folder = chooser.showDialog(stage);
            if (folder != null) {
                workspace.openFolder(folder);
                terminal.print("Workspace: " + folder.getAbsolutePath() + "\n");
            }
        });

        Button saveBtn = new Button("💾 Save");
        saveBtn.setOnAction(e -> {
            try {
                FileManager.saveFile(stage, editor.getCode());
                editor.markSaved();
            } catch (Exception ex) {
                terminal.print("Save failed: " + ex.getMessage() + "\n");
            }
        });

        Button runBtn = new Button("▶ Run");
        runBtn.setOnAction(e -> controller.runCode());

        HBox topBar = new HBox(
                openFileBtn,
                openFolderBtn,
                saveBtn,
                runBtn
        );
        topBar.setSpacing(8);
        topBar.setStyle("-fx-padding: 6; -fx-background-color: #2b2b2b;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(mainSplit);

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
