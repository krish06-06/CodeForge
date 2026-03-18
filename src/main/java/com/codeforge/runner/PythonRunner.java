package com.codeforge.runner;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PythonRunner {

    private PythonRunner() {
    }

    public static Process run(Path filePath, String code) throws Exception {
        Path fileToRun = filePath;

        if (fileToRun != null) {
            Files.writeString(fileToRun, code);
        } else {
            fileToRun = Files.createTempFile("codeforge_", ".py");
            Files.writeString(fileToRun, code);
            fileToRun.toFile().deleteOnExit();
        }

        ProcessBuilder builder = new ProcessBuilder("python", fileToRun.toAbsolutePath().toString());
        builder.redirectErrorStream(true);
        return builder.start();
    }
}
