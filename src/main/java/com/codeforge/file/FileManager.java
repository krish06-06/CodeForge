package com.codeforge.file;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    private static Path currentFile;

    public static String openFile(Stage stage) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return null;

        currentFile = file.toPath();
        return Files.readString(currentFile);
    }

    public static void saveFile(Stage stage, String content) throws Exception {

        if (currentFile != null) {
            Files.writeString(currentFile, content);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        currentFile = file.toPath();
        Files.writeString(currentFile, content);
    }

    public static void newFile() {
        currentFile = null;
    }

    public static Path getCurrentFile() {
        return currentFile;
    }

    // 🔥 REQUIRED FOR TREEVIEW
    public static void setCurrentFile(Path path) {
        currentFile = path;
    }
}
