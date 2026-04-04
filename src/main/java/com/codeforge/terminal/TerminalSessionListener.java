package com.codeforge.terminal;

public interface TerminalSessionListener {

    void onStarted(TerminalSession terminal);

    void onOutput(TerminalSession terminal, String chunk, String transcript);

    void onFinished(TerminalSession terminal, int exitCode, String transcript);

    void onError(TerminalSession terminal, String transcript, String message);
}
