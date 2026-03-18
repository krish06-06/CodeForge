package com.codeforge;

import com.codeforge.editor.EditorController;
import com.codeforge.editor.EditorTabManager;
import com.codeforge.editor.EditorTabSession;
import com.codeforge.file.FileManager;
import com.codeforge.file.WorkspaceView;
import com.codeforge.terminal.TerminalManager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Path;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        EditorTabManager editorTabs = new EditorTabManager();
        TerminalManager terminalManager = new TerminalManager();
        WorkspaceView workspace = new WorkspaceView();
        EditorController controller = new EditorController(editorTabs, terminalManager);

        Label statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-text");

        workspace.setListener(new WorkspaceView.WorkspaceListener() {
            @Override
            public void onOpenFile(Path path) {
                openFile(path, editorTabs, statusLabel, terminalManager);
            }

            @Override
            public void onPathRenamed(Path oldPath, Path newPath) {
                editorTabs.renamePath(oldPath, newPath);
                statusLabel.setText("Renamed " + oldPath.getFileName() + " to " + newPath.getFileName());
            }

            @Override
            public void onPathDeleted(Path path) {
                editorTabs.closeSessionsInside(path);
                statusLabel.setText("Deleted " + path.getFileName());
            }

            @Override
            public void onMessage(String message) {
                statusLabel.setText(message);
                if (terminalManager.getActiveSession() != null) {
                    terminalManager.getActiveSession().print(message + "\n");
                }
            }
        });

        editorTabs.setActiveSessionListener(session -> {
            if (session == null) {
                statusLabel.setText("No active file");
            } else if (session.getFilePath() != null) {
                statusLabel.setText("Active file: " + session.getFilePath());
            } else {
                statusLabel.setText("Active file: " + session.getDisplayName());
            }
        });

        Button newFileButton = new Button("New File");
        newFileButton.setOnAction(event -> {
            EditorTabSession session = editorTabs.openUntitled();
            statusLabel.setText("Created " + session.getDisplayName());
        });

        Button openFileButton = new Button("Open File");
        openFileButton.setOnAction(event -> {
            Path path = FileManager.chooseFile(stage);
            if (path != null) {
                openFile(path, editorTabs, statusLabel, terminalManager);
            }
        });

        Button openFolderButton = new Button("Open Folder");
        openFolderButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            var folder = chooser.showDialog(stage);
            if (folder != null) {
                workspace.openFolder(folder.toPath());
                statusLabel.setText("Workspace " + folder.getAbsolutePath());
            }
        });

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            try {
                editorTabs.saveActive(stage);
                statusLabel.setText("Saved " + editorTabs.getActiveSession().getDisplayName());
            } catch (Exception ex) {
                statusLabel.setText("Save failed");
                if (terminalManager.getActiveSession() != null) {
                    terminalManager.getActiveSession().print("Save failed: " + ex.getMessage() + "\n");
                }
            }
        });

        Button newTerminalButton = new Button("New Terminal");
        newTerminalButton.setOnAction(event -> {
            var session = terminalManager.createTerminal();
            statusLabel.setText("Opened " + session.getName());
        });

        Button runButton = new Button("Run");
        runButton.getStyleClass().add("accent-button");
        runButton.setOnAction(event -> {
            controller.runActiveFile();
            EditorTabSession active = editorTabs.getActiveSession();
            statusLabel.setText(active == null ? "Run requested" : "Running " + active.getDisplayName());
        });

        HBox toolbar = new HBox(
            10,
            newFileButton,
            openFileButton,
            openFolderButton,
            new Separator(),
            saveButton,
            runButton,
            new Separator(),
            newTerminalButton
        );
        toolbar.setPadding(new Insets(10, 12, 10, 12));
        toolbar.getStyleClass().add("toolbar");

        BorderPane explorerPane = new BorderPane(workspace.getView());
        explorerPane.getStyleClass().add("panel");
        explorerPane.setTop(sectionLabel("Explorer"));

        BorderPane terminalPane = new BorderPane(terminalManager.getView());
        terminalPane.getStyleClass().add("panel");
        terminalPane.setTop(sectionLabel("Terminals"));

        SplitPane editorTerminalSplit = new SplitPane(editorTabs.getView(), terminalPane);
        editorTerminalSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        editorTerminalSplit.setDividerPositions(0.7);

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

        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("CodeForge");
        stage.setScene(scene);
        stage.show();
    }

    private void openFile(Path path, EditorTabManager editorTabs, Label statusLabel, TerminalManager terminalManager) {
        try {
            editorTabs.openFile(path);
            statusLabel.setText("Opened " + path);
        } catch (Exception ex) {
            statusLabel.setText("Open failed");
            if (terminalManager.getActiveSession() != null) {
                terminalManager.getActiveSession().print("Open failed: " + ex.getMessage() + "\n");
            }
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
