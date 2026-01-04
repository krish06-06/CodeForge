package com.codeforge.runner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PythonRunner {

    public static Process run(String code) throws Exception {

        Path tempFile = Files.createTempFile("codeforge_", ".py");
        Files.writeString(tempFile, code);

        ProcessBuilder builder = new ProcessBuilder(
                "python", tempFile.toAbsolutePath().toString()
        );

        builder.redirectErrorStream(true);
        return builder.start();
    }
}
