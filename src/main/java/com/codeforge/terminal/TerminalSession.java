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
    private StringBuilder transcript = new StringBuilder();
    private TerminalSessionListener listener;

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

    public void attachProcess(Process process, String header, TerminalSessionListener listener) {
        stopProcess();
        this.process = process;
        this.listener = listener;
        this.transcript = new StringBuilder();
        this.inputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        view.clear();
        view.print(header + "\n\n");
        view.setInputHandler(this::sendInput);
        if (listener != null) {
            listener.onStarted(this);
        }

        Thread ioThread = new Thread(this::streamOutput, name + "-io");
        ioThread.setDaemon(true);
        ioThread.start();
    }

    public void print(String text) {
        view.print(text);
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void dispose() {
        stopProcess();
        view.setInputHandler(null);
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
            char[] buffer = new char[256];
            int readCount;
            while ((readCount = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, readCount);
                transcript.append(chunk);
                view.print(chunk);
                if (listener != null) {
                    listener.onOutput(this, chunk, transcript.toString());
                }
            }

            int exitCode = process.waitFor();
            Platform.runLater(() -> {
                view.setInputHandler(null);
                view.print("\n[Process finished with exit code " + exitCode + "]\n");
                if (listener != null) {
                    listener.onFinished(this, exitCode, transcript.toString());
                }
            });
        } catch (Exception ex) {
            Platform.runLater(() -> {
                view.setInputHandler(null);
                view.print("\n[Terminal error] " + ex.getMessage() + "\n");
                if (listener != null) {
                    listener.onError(this, transcript.toString(), ex.getMessage());
                }
            });
        }
    }

    private void stopProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
