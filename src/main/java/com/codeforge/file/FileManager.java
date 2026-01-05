package com.codeforge.file;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    // Holds the currently opened file path (VS Code style)
    private static Path currentFile;

    // OPEN FILE
    public static String openFile(Stage stage) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return null;

        currentFile = file.toPath();          // 🔥 remember file
        return Files.readString(currentFile); // return content (same as before)
    }

    // SAVE (Ctrl + S behavior)
    public static void saveFile(Stage stage, String content) throws Exception {

        // If file already exists → save directly
        if (currentFile != null) {
            Files.writeString(currentFile, content);
            return;
        }

        // Else → Save As
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        currentFile = file.toPath();           // 🔥 remember new file
        Files.writeString(currentFile, content);
    }

    // OPTIONAL: call when creating a new file
    public static void newFile() {
        currentFile = null;
    }

    // OPTIONAL: for future tab / title usage
    public static Path getCurrentFile() {
        return currentFile;
    }
}
