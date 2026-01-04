package com.codeforge.file;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;

public class FileManager {

    public static String openFile(Stage stage) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return null;

        return Files.readString(file.toPath());
    }

    public static void saveFile(Stage stage, String content) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Python Files", "*.py")
        );

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        Files.writeString(file.toPath(), content);
    }
}
