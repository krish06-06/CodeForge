package com.codeforge.file;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class WorkspaceView {

    public interface WorkspaceListener {
        void onOpenFile(Path path);
        void onRenameRequested(Path path);
        void onDeleteRequested(Path path);
        void onCopyRequested(Path path);
        void onPasteRequested(Path path);
    }

    private final TreeView<Path> treeView;
    private Path rootPath;
    private WorkspaceListener listener;

    public WorkspaceView() {
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setCellFactory(view -> createCell());
    }

    public void openFolder(Path folder) {
        rootPath = folder;
        treeView.setRoot(folder == null ? null : new PathTreeItem(folder));
        if (treeView.getRoot() != null) {
            treeView.getRoot().setExpanded(true);
        }
    }

    public void refresh() {
        openFolder(rootPath);
    }

    public TreeView<Path> getView() {
        return treeView;
    }

    public void setListener(WorkspaceListener listener) {
        this.listener = listener;
    }

    public Path getRootPath() {
        return rootPath;
    }

    private TreeCell<Path> createCell() {
        TreeCell<Path> cell = new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.getFileName() == null ? item.toString() : item.getFileName().toString());
                    setContextMenu(createContextMenu(item));
                }
            }
        };

        cell.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2 || cell.isEmpty()) {
                return;
            }

            Path path = cell.getItem();
            if (path != null && Files.isRegularFile(path) && listener != null) {
                listener.onOpenFile(path);
            }
        });

        return cell;
    }

    private ContextMenu createContextMenu(Path path) {
        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(event -> {
            if (listener != null) {
                listener.onRenameRequested(path);
            }
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            if (listener != null) {
                listener.onDeleteRequested(path);
            }
        });

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            if (listener != null) {
                listener.onCopyRequested(path);
            }
        });

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(event -> {
            if (listener != null) {
                listener.onPasteRequested(path);
            }
        });

        return new ContextMenu(rename, delete, copy, paste);
    }

    private final class PathTreeItem extends TreeItem<Path> {
        private boolean childrenLoaded;
        private boolean loading;

        private PathTreeItem(Path path) {
            super(path);
            if (Files.isDirectory(path)) {
                getChildren().add(new TreeItem<>());
                expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                    if (isExpanded) {
                        loadChildrenAsync();
                    }
                });
            }
        }

        @Override
        public boolean isLeaf() {
            return !Files.isDirectory(getValue());
        }

        private void loadChildrenAsync() {
            if (childrenLoaded || loading || isLeaf()) {
                return;
            }

            loading = true;
            Task<List<PathTreeItem>> loadTask = new Task<>() {
                @Override
                protected List<PathTreeItem> call() throws Exception {
                    try (var stream = Files.list(PathTreeItem.this.getValue())) {
                        return stream
                            .sorted(Comparator
                                .comparing((Path candidate) -> Files.isRegularFile(candidate))
                                .thenComparing(candidate -> candidate.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                            .map(PathTreeItem::new)
                            .toList();
                    }
                }
            };

            loadTask.setOnSucceeded(event -> {
                childrenLoaded = true;
                loading = false;
                Platform.runLater(() -> getChildren().setAll(loadTask.getValue()));
            });

            loadTask.setOnFailed(event -> {
                childrenLoaded = true;
                loading = false;
                Platform.runLater(this::clearPlaceholder);
            });

            Thread loader = new Thread(loadTask, "workspace-loader-" + getValue().getFileName());
            loader.setDaemon(true);
            loader.start();
        }

        private void clearPlaceholder() {
            if (!getChildren().isEmpty() && getChildren().get(0).getValue() == null) {
                getChildren().clear();
            }
        }
    }
}
