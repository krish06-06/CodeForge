package com.codeforge.file;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class WorkspaceView {

    private final TreeView<File> treeView;
    private final BorderPane root;

    public WorkspaceView() {
        treeView = new TreeView<>();
        root = new BorderPane(treeView);

        treeView.setShowRoot(false);
        treeView.setCellFactory(view -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName().isBlank() ? item.getAbsolutePath() : item.getName());
                }
            }
        });
    }

    public void openFolder(File folder) {
        TreeItem<File> rootItem = createNode(folder);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
    }

    private TreeItem<File> createNode(File file) {
        TreeItem<File> item = new TreeItem<>(file);

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator
                    .comparing(File::isFile)
                    .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

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
