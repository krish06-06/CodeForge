package com.codeforge.file;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.File;

public class WorkspaceView {

    private TreeView<File> treeView;
    private BorderPane root;

    public WorkspaceView() {
        treeView = new TreeView<>();
        root = new BorderPane(treeView);

        treeView.setShowRoot(false);
    }

    public void openFolder(File folder) {
        TreeItem<File> rootItem = createNode(folder);
        treeView.setRoot(rootItem);
    }

    private TreeItem<File> createNode(File file) {
        TreeItem<File> item = new TreeItem<>(file);

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    item.getChildren().add(createNode(f));
                }
            }
        }
        return item;
    }

    public TreeView<File> getView() {
        return treeView;
    }
}
