package com.codeforge;

import com.codeforge.editor.CodeEditor;
import com.codeforge.editor.EditorController;
import com.codeforge.file.FileManager;
import com.codeforge.file.WorkspaceView;
import com.codeforge.terminal.TerminalView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        CodeEditor editor = new CodeEditor();
        TerminalView terminal = new TerminalView();
        WorkspaceView workspace = new WorkspaceView();
        EditorController controller = new EditorController(editor, terminal);

        TabPane editorTabs = createEditorTabs(editor);
        Label statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-text");

        workspace.getView().setOnMouseClicked(event -> openSelectedFile(workspace.getView(), editorTabs, editor, terminal, statusLabel));

        Button openFileButton = new Button("Open File");
        openFileButton.setOnAction(event -> {
            try {
                String code = FileManager.openFile(stage);
                if (code != null) {
                    editor.setCode(code);
                    editor.markSaved();
                    renameActiveTab(editorTabs, FileManager.getCurrentFile().getFileName().toString());
                    statusLabel.setText("Opened " + FileManager.getCurrentFile());
                }
            } catch (Exception ex) {
                terminal.print("Open failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Open failed");
            }
        });

        Button openFolderButton = new Button("Open Folder");
        openFolderButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File folder = chooser.showDialog(stage);
            if (folder != null) {
                workspace.openFolder(folder);
                statusLabel.setText("Workspace " + folder.getAbsolutePath());
                terminal.print("Workspace: " + folder.getAbsolutePath() + "\n");
            }
        });

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            try {
                FileManager.saveFile(stage, editor.getCode());
                editor.markSaved();
                if (FileManager.getCurrentFile() != null) {
                    renameActiveTab(editorTabs, FileManager.getCurrentFile().getFileName().toString());
                }
                statusLabel.setText("Saved");
            } catch (Exception ex) {
                terminal.print("Save failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Save failed");
            }
        });

        Button runButton = new Button("Run");
        runButton.getStyleClass().add("accent-button");
        runButton.setOnAction(event -> {
            controller.runCode();
            statusLabel.setText("Running active file");
        });

        HBox toolbar = new HBox(10, openFileButton, openFolderButton, new Separator(), saveButton, runButton);
        toolbar.setPadding(new Insets(10, 12, 10, 12));
        toolbar.getStyleClass().add("toolbar");

        BorderPane explorerPane = new BorderPane(workspace.getView());
        explorerPane.getStyleClass().add("panel");
        explorerPane.setTop(sectionLabel("Explorer"));

        BorderPane terminalPane = new BorderPane(terminal.getView());
        terminalPane.getStyleClass().add("panel");
        terminalPane.setTop(sectionLabel("Terminal"));

        SplitPane editorTerminalSplit = new SplitPane(editorTabs, terminalPane);
        editorTerminalSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        editorTerminalSplit.setDividerPositions(0.72);

        SplitPane mainSplit = new SplitPane(explorerPane, editorTerminalSplit);
        mainSplit.setDividerPositions(0.23);

        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(6, 12, 6, 12));
        statusBar.getStyleClass().add("status-bar");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(toolbar);
        root.setCenter(mainSplit);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1380, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("CodeForge");
        stage.setScene(scene);
        stage.show();
    }

    private TabPane createEditorTabs(CodeEditor editor) {
        TabPane editorTabs = new TabPane();
        editorTabs.getStyleClass().add("editor-tabs");

        Tab primaryTab = new Tab("untitled.py");
        primaryTab.setClosable(false);
        primaryTab.setContent(new StackPane(editor.getView()));

        editorTabs.getTabs().add(primaryTab);
        HBox.setHgrow(editorTabs, Priority.ALWAYS);
        return editorTabs;
    }

    private void openSelectedFile(
        TreeView<File> treeView,
        TabPane editorTabs,
        CodeEditor editor,
        TerminalView terminal,
        Label statusLabel
    ) {
        TreeItem<File> item = treeView.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }

        File file = item.getValue();
        if (!file.isFile() || !file.getName().endsWith(".py")) {
            return;
        }

        try {
            String code = Files.readString(file.toPath());
            editor.setCode(code);
            FileManager.setCurrentFile(file.toPath());
            editor.markSaved();
            renameActiveTab(editorTabs, file.getName());
            statusLabel.setText("Opened " + file.getAbsolutePath());
        } catch (Exception ex) {
            terminal.print("Open failed: " + ex.getMessage() + "\n");
            statusLabel.setText("Open failed");
        }
    }

    private void renameActiveTab(TabPane editorTabs, String name) {
        Tab selected = editorTabs.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setText(name);
        }
    }

    private HBox sectionLabel(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("section-label");

        HBox container = new HBox(label);
        container.setPadding(new Insets(10, 12, 10, 12));
        container.getStyleClass().add("section-header");
        return container;
    }

    public static void main(String[] args) {
        launch();
    }
}
