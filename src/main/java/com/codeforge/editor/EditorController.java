package com.codeforge.editor;

import com.codeforge.runner.PythonRunner;
import com.codeforge.terminal.TerminalView;
import com.codeforge.file.FileManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class EditorController {

    private final CodeEditor editor;
    private final TerminalView terminal;
    private BufferedWriter inputWriter;

    public EditorController(CodeEditor editor, TerminalView terminal) {
        this.editor = editor;
        this.terminal = terminal;
    }

    public void runCode() {

        if (FileManager.getCurrentFile() == null) {
            terminal.print("⚠ Please save the file before running\n");
            return;
        }

        terminal.clear();
        terminal.print("▶ Running...\n");

        try {
            Process process = PythonRunner.run(editor.getCode());

            inputWriter = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream())
            );

            terminal.setInputHandler(input -> {
                try {
                    if (process.isAlive()) {
                        inputWriter.write(input);
                        inputWriter.newLine();
                        inputWriter.flush();
                    }
                } catch (Exception ex) {
                    terminal.print("[System] Input link broken.\n");
                }
            });

            Thread outputThread = new Thread(() -> {
                try (
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                    )
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String out = line;
                        javafx.application.Platform.runLater(() ->
                                terminal.print(out + "\n")
                        );
                    }

                    int exitCode = process.waitFor();
                    javafx.application.Platform.runLater(() ->
                        terminal.print("\n[Process finished with exit code "
                                + exitCode + "]\n")
                    );

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                        terminal.print("Error: " + e.getMessage() + "\n")
                    );
                }
            });

            outputThread.setDaemon(true);
            outputThread.start();

        } catch (Exception e) {
            terminal.print("Failed to run: " + e.getMessage() + "\n");
        }
    }
}
