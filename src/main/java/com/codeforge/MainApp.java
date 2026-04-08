package com.codeforge;

import com.codeforge.config.ConfigManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    private boolean adjustingStageBounds;
    private CodeForgeBridgeServer bridgeServer;

    @Override
    public void start(Stage stage) {
        WorkbenchController workbench = new WorkbenchController(stage);

        Button newScratchButton = createToolbarButton("+", "New Scratch", false);
        newScratchButton.setOnAction(event -> workbench.newScratchFile());

        Button createFileButton = createToolbarButton("+", "Create File", false);
        createFileButton.setOnAction(event -> workbench.createFile());

        Button openFileButton = createToolbarButton("O", "Open File", false);
        openFileButton.setOnAction(event -> workbench.openFileChooser());

        Button openFolderButton = createToolbarButton("F", "Open Folder", false);
        openFolderButton.setOnAction(event -> workbench.openFolderChooser());

        Button saveButton = createToolbarButton("S", "Save", false);
        saveButton.setOnAction(event -> workbench.saveActive());

        Button runButton = createToolbarButton(">", "Run", true);
        runButton.setOnAction(event -> workbench.runActive());

        Button newTerminalButton = createToolbarButton("T", "New Terminal", false);
        newTerminalButton.setOnAction(event -> workbench.newTerminal());

        HBox toolbar = new HBox(
            10,
            newScratchButton,
            createFileButton,
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

        BorderPane explorerPane = new BorderPane(workbench.getWorkspaceView().getView());
        explorerPane.getStyleClass().add("panel");
        explorerPane.setTop(sectionLabel("Explorer"));

        TabPane bottomTabs = new TabPane();
        bottomTabs.getStyleClass().add("bottom-tabs");
        Tab terminalsTab = new Tab("Terminals", workbench.getTerminalManager().getView());
        terminalsTab.setClosable(false);
        Tab problemsTab = new Tab("Problems", workbench.getProblemsView().getView());
        problemsTab.setClosable(false);
        Tab snapshotTab = new Tab("AI Snapshot", workbench.getAiSnapshotView().getView());
        snapshotTab.setClosable(false);
        bottomTabs.getTabs().addAll(terminalsTab, problemsTab, snapshotTab);

        BorderPane bottomPane = new BorderPane(bottomTabs);
        bottomPane.getStyleClass().add("panel");

        SplitPane editorBottomSplit = new SplitPane(workbench.getEditorTabs().getView(), bottomPane);
        editorBottomSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        editorBottomSplit.setDividerPositions(0.7);

        SplitPane mainSplit = new SplitPane(explorerPane, editorBottomSplit);
        mainSplit.setDividerPositions(0.23);

        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(6, 12, 6, 12));
        statusBar.getStyleClass().add("status-bar");
        var statusLabel = new javafx.scene.control.Label();
        statusLabel.getStyleClass().add("status-text");
        statusLabel.textProperty().bind(workbench.statusProperty());
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        statusBar.getChildren().add(statusLabel);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(toolbar);
        root.setCenter(mainSplit);
        root.setBottom(statusBar);

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double sceneWidth = Math.min(1480, Math.max(1100, visualBounds.getWidth() - 80));
        double sceneHeight = Math.min(920, Math.max(760, visualBounds.getHeight() - 80));

        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());

        stage.setTitle("CodeForge");
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
        bridgeServer = new CodeForgeBridgeServer(workbench);
        bridgeServer.start();
        Platform.runLater(() -> positionStageSafely(stage));
        stage.xProperty().addListener((obs, oldValue, newValue) -> keepStageWithinVisibleBounds(stage));
        stage.yProperty().addListener((obs, oldValue, newValue) -> keepStageWithinVisibleBounds(stage));
        stage.widthProperty().addListener((obs, oldValue, newValue) -> keepStageWithinVisibleBounds(stage));
        stage.heightProperty().addListener((obs, oldValue, newValue) -> keepStageWithinVisibleBounds(stage));
    }

    private HBox sectionLabel(String title) {
        var label = new javafx.scene.control.Label(title);
        label.getStyleClass().add("section-label");

        HBox container = new HBox(label);
        container.setPadding(new Insets(10, 12, 10, 12));
        container.getStyleClass().add("section-header");
        return container;
    }

    public static void main(String[] args) {
        ConfigManager.load();
        launch();
    }

    @Override
    public void stop() {
        if (bridgeServer != null) {
            bridgeServer.stop();
        }
    }

    private Button createToolbarButton(String icon, String text, boolean accent) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("toolbar-icon");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("toolbar-button-label");

        HBox content = new HBox(8, iconLabel, textLabel);
        Button button = new Button();
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("toolbar-button");
        if (accent) {
            button.getStyleClass().add("accent-button");
        }
        return button;
    }

    private void positionStageSafely(Stage stage) {
        Rectangle2D bounds = findBestScreenBounds(stage);
        double horizontalMargin = 24;
        double topMargin = 24;
        double bottomMargin = 24;

        double width = Math.min(stage.getWidth(), bounds.getWidth() - (horizontalMargin * 2));
        double height = Math.min(stage.getHeight(), bounds.getHeight() - topMargin - bottomMargin);
        double x = Math.max(bounds.getMinX() + horizontalMargin, bounds.getMinX() + ((bounds.getWidth() - width) / 2));
        double y = Math.max(bounds.getMinY() + topMargin, bounds.getMinY() + ((bounds.getHeight() - height) / 2));

        adjustingStageBounds = true;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(x);
        stage.setY(y);
        adjustingStageBounds = false;
    }

    private void keepStageWithinVisibleBounds(Stage stage) {
        if (adjustingStageBounds) {
            return;
        }

        Rectangle2D bounds = findBestScreenBounds(stage);
        double horizontalMargin = 8;
        double topMargin = 8;
        double bottomMargin = 8;

        double width = Math.min(stage.getWidth(), bounds.getWidth() - (horizontalMargin * 2));
        double height = Math.min(stage.getHeight(), bounds.getHeight() - topMargin - bottomMargin);
        double x = Math.max(bounds.getMinX() + horizontalMargin, Math.min(stage.getX(), bounds.getMaxX() - width - horizontalMargin));
        double y = Math.max(bounds.getMinY() + topMargin, Math.min(stage.getY(), bounds.getMaxY() - height - bottomMargin));

        adjustingStageBounds = true;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(x);
        stage.setY(y);
        adjustingStageBounds = false;
    }

    private Rectangle2D findBestScreenBounds(Stage stage) {
        double width = Math.max(stage.getWidth(), 1);
        double height = Math.max(stage.getHeight(), 1);
        return Screen.getScreensForRectangle(stage.getX(), stage.getY(), width, height)
            .stream()
            .findFirst()
            .orElse(Screen.getPrimary())
            .getVisualBounds();
    }
}
