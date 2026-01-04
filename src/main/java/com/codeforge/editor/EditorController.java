package com.codeforge.editor;

import com.codeforge.runner.PythonRunner;
import com.codeforge.terminal.TerminalView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EditorController {

    private final CodeEditor editor;
    private final TerminalView terminal;

    public EditorController(CodeEditor editor, TerminalView terminal) {
        this.editor = editor;
        this.terminal = terminal;
    }

    public void runCode() {

        terminal.print("▶ Running...\n");

        try {
            Process process = PythonRunner.run(editor.getCode());

            // Output stream thread
            new Thread(() -> {
                try (BufferedReader reader =
                     new BufferedReader(
                         new InputStreamReader(process.getInputStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        terminal.print(line);
                    }

                } catch (Exception e) {
                    terminal.print("Error: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            terminal.print("Failed to run: " + e.getMessage());
        }
    }
}
