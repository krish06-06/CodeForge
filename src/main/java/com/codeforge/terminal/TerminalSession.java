package com.codeforge.terminal;

import javafx.application.Platform;
import javafx.scene.control.Tab;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class TerminalSession {

    private final Tab tab;
    private final TerminalView view;
    private final String name;
    private BufferedWriter inputWriter;
    private Process process;

    public TerminalSession(String name) {
        this.name = name;
        this.view = new TerminalView();
        this.tab = new Tab(name, view.getView());
        this.tab.setClosable(true);
    }

    public Tab getTab() {
        return tab;
    }

    public TerminalView getView() {
        return view;
    }

    public String getName() {
        return name;
    }

    public void attachProcess(Process process, String header) {
        this.process = process;
        this.inputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        view.clear();
        view.print(header + "\n\n");
        view.setInputHandler(this::sendInput);

        Thread ioThread = new Thread(this::streamOutput, name + "-io");
        ioThread.setDaemon(true);
        ioThread.start();
    }

    public void print(String text) {
        view.print(text);
    }

    private void sendInput(String input) {
        try {
            if (process == null || !process.isAlive()) {
                view.print("[System] Process is no longer running.\n");
                view.setInputHandler(null);
                return;
            }

            inputWriter.write(input == null ? "" : input);
            inputWriter.newLine();
            inputWriter.flush();
            view.echoInput(input);
        } catch (Exception ex) {
            view.print("[System] Failed to send input: " + ex.getMessage() + "\n");
        }
    }

    private void streamOutput() {
        try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
            int nextChar;
            while ((nextChar = reader.read()) != -1) {
                view.print(String.valueOf((char) nextChar));
            }

            int exitCode = process.waitFor();
            Platform.runLater(() -> {
                view.setInputHandler(null);
                view.print("\n[Process finished with exit code " + exitCode + "]\n");
            });
        } catch (Exception ex) {
            Platform.runLater(() -> {
                view.setInputHandler(null);
                view.print("\n[Terminal error] " + ex.getMessage() + "\n");
            });
        }
    }
}
