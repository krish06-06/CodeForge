package com.codeforge.editor;

import com.codeforge.file.FileManager;
import com.codeforge.runner.PythonRunner;
import com.codeforge.terminal.TerminalView;

import javafx.application.Platform;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

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
            terminal.print("Please save the file before running.\n");
            return;
        }

        terminal.clear();
        terminal.print("Running...\n\n");
        terminal.setInputEnabled(false);

        try {
            Process process = PythonRunner.run(editor.getCode());
            inputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

            terminal.setInputHandler(input -> {
                try {
                    if (!process.isAlive()) {
                        terminal.print("[System] Process is no longer running.\n");
                        terminal.setInputHandler(null);
                        return;
                    }

                    inputWriter.write(input == null ? "" : input);
                    inputWriter.newLine();
                    inputWriter.flush();
                    terminal.echoInput(input);
                } catch (Exception ex) {
                    terminal.print("[System] Failed to send input: " + ex.getMessage() + "\n");
                }
            });

            Thread ioThread = new Thread(() -> streamProcessOutput(process), "codeforge-terminal-io");
            ioThread.setDaemon(true);
            ioThread.start();
        } catch (Exception ex) {
            terminal.setInputHandler(null);
            terminal.print("Failed to run: " + ex.getMessage() + "\n");
        }
    }

    private void streamProcessOutput(Process process) {
        try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
            int nextChar;
            while ((nextChar = reader.read()) != -1) {
                char output = (char) nextChar;
                terminal.print(String.valueOf(output));
            }

            int exitCode = process.waitFor();
            Platform.runLater(() -> {
                terminal.setInputHandler(null);
                terminal.print("\n[Process finished with exit code " + exitCode + "]\n");
            });
        } catch (Exception ex) {
            Platform.runLater(() -> {
                terminal.setInputHandler(null);
                terminal.print("\n[Terminal error] " + ex.getMessage() + "\n");
            });
        }
    }
}
