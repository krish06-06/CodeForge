package com.codeforge.runner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import com.codeforge.file.FileManager;

public class PythonRunner {

    public static Process run(String code) throws Exception {

        Path fileToRun;

        // If user opened/saved a file → run it
        if (FileManager.getCurrentFile() != null) {
            fileToRun = FileManager.getCurrentFile();

            // Ensure latest code is saved before running
            Files.writeString(fileToRun, code);
        }
        // Else → fallback to temp file (unsaved code)
        else {
            fileToRun = Files.createTempFile("codeforge_", ".py");
            Files.writeString(fileToRun, code);
            fileToRun.toFile().deleteOnExit();
        }

        ProcessBuilder builder = new ProcessBuilder(
                "python", fileToRun.toAbsolutePath().toString()
        );

        builder.redirectErrorStream(true);
        return builder.start();
    }
}
