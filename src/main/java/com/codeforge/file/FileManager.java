package com.codeforge.file;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public final class FileManager {

    private FileManager() {
    }

    public static Path chooseFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Python Files", "*.py"));
        var file = chooser.showOpenDialog(stage);
        return file == null ? null : file.toPath();
    }

    public static String readFile(Path path) throws IOException {
        return Files.readString(path);
    }

    public static Path saveFile(Stage stage, Path currentPath, String content) throws IOException {
        Path targetPath = currentPath;
        if (targetPath == null) {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Python Files", "*.py"));
            var file = chooser.showSaveDialog(stage);
            if (file == null) {
                return null;
            }
            targetPath = file.toPath();
        }

        Files.writeString(targetPath, content);
        return targetPath;
    }

    public static Path rename(Path source, String newName) throws IOException {
        Path target = source.resolveSibling(newName);
        return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void delete(Path source) throws IOException {
        if (Files.isDirectory(source)) {
            try (Stream<Path> stream = Files.walk(source)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (RuntimeException ex) {
                if (ex.getCause() instanceof IOException ioException) {
                    throw ioException;
                }
                throw ex;
            }
            return;
        }

        Files.deleteIfExists(source);
    }

    public static Path copy(Path source, Path destinationDirectory) throws IOException {
        if (source == null || destinationDirectory == null) {
            return null;
        }

        Path target = uniqueTarget(destinationDirectory.resolve(source.getFileName()));
        if (Files.isDirectory(source)) {
            copyDirectory(source, target);
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
        return target;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path current : (Iterable<Path>) stream::iterator) {
                Path relative = source.relativize(current);
                Path resolved = target.resolve(relative);
                if (Files.isDirectory(current)) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(current, resolved, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static Path uniqueTarget(Path desiredPath) throws IOException {
        if (!Files.exists(desiredPath)) {
            return desiredPath;
        }

        String fileName = desiredPath.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && !Files.isDirectory(desiredPath)) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (true) {
            Path candidate = desiredPath.resolveSibling(baseName + " Copy " + counter + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
            counter++;
        }
    }
}
