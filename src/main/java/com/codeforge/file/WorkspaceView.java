package com.codeforge.file;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class WorkspaceView {

    public interface WorkspaceListener {
        void onOpenFile(Path path);
        void onPathRenamed(Path oldPath, Path newPath);
        void onPathDeleted(Path path);
        void onMessage(String message);
    }

    private final TreeView<Path> treeView;
    private Path rootPath;
    private Path clipboardPath;
    private WorkspaceListener listener;

    public WorkspaceView() {
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setCellFactory(view -> createCell());
    }

    public void openFolder(Path folder) {
        rootPath = folder;
        refresh();
    }

    public void refresh() {
        if (rootPath == null) {
            treeView.setRoot(null);
            return;
        }

        TreeItem<Path> rootItem = createNode(rootPath);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
    }

    public TreeView<Path> getView() {
        return treeView;
    }

    public void setListener(WorkspaceListener listener) {
        this.listener = listener;
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
        rename.setOnAction(event -> renamePath(path));

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> deletePath(path));

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            clipboardPath = path;
            notifyMessage("Copied " + path.getFileName());
        });

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(event -> pasteInto(path));

        ContextMenu menu = new ContextMenu(rename, delete, copy, paste);
        menu.setOnShowing(event -> paste.setDisable(clipboardPath == null));
        return menu;
    }

    private void renamePath(Path source) {
        TextInputDialog dialog = new TextInputDialog(source.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + source.getFileName());
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String newName = result.get().trim();
        if (newName.isEmpty() || newName.equals(source.getFileName().toString())) {
            return;
        }

        try {
            Path renamed = FileManager.rename(source, newName);
            refresh();
            if (listener != null) {
                listener.onPathRenamed(source, renamed);
            }
            notifyMessage("Renamed to " + renamed.getFileName());
        } catch (Exception ex) {
            notifyMessage("Rename failed: " + ex.getMessage());
        }
    }

    private void deletePath(Path source) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete");
        confirmation.setHeaderText("Delete " + source.getFileName() + "?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            FileManager.delete(source);
            refresh();
            if (listener != null) {
                listener.onPathDeleted(source);
            }
            notifyMessage("Deleted " + source.getFileName());
        } catch (Exception ex) {
            notifyMessage("Delete failed: " + ex.getMessage());
        }
    }

    private void pasteInto(Path target) {
        if (clipboardPath == null) {
            return;
        }

        Path destinationDirectory = Files.isDirectory(target) ? target : target.getParent();
        try {
            Path pasted = FileManager.copy(clipboardPath, destinationDirectory);
            refresh();
            notifyMessage("Pasted " + pasted.getFileName());
        } catch (Exception ex) {
            notifyMessage("Paste failed: " + ex.getMessage());
        }
    }

    private TreeItem<Path> createNode(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);

        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                Path[] children = stream.toArray(Path[]::new);
                Arrays.sort(children, Comparator
                    .comparing((Path candidate) -> Files.isRegularFile(candidate))
                    .thenComparing(candidate -> candidate.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));

                for (Path child : children) {
                    item.getChildren().add(createNode(child));
                }
            } catch (IOException ex) {
                notifyMessage("Failed to read folder: " + ex.getMessage());
            }
        }

        return item;
    }

    private void notifyMessage(String message) {
        if (listener != null) {
            listener.onMessage(message);
        }
    }
}
