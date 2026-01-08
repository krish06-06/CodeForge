package com.codeforge.compiler;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CompilerMain {
    public static void main(String[] args) {
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        String filePath = "test.py";
        try {
            String source = Files.readString(Paths.get(filePath));
            PythonCompilerRunner.run(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}