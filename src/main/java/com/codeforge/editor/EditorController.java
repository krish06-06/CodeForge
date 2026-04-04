package com.codeforge.editor;

import com.codeforge.language.LanguageService;
import com.codeforge.terminal.TerminalManager;
import com.codeforge.terminal.TerminalSession;
import com.codeforge.terminal.TerminalSessionListener;

public class EditorController {

    private final EditorTabManager editorTabs;
    private final TerminalManager terminalManager;
    private final LanguageService languageService;
    private ExecutionListener executionListener;

    public EditorController(EditorTabManager editorTabs, TerminalManager terminalManager, LanguageService languageService) {
        this.editorTabs = editorTabs;
        this.terminalManager = terminalManager;
        this.languageService = languageService;
    }

    public void runActiveFile() {
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null) {
            return;
        }

        try {
            TerminalSession terminal = terminalManager.getActiveSession();
            if (terminal == null || terminal.isRunning()) {
                terminal = terminalManager.createTerminal();
            }

            Process process = languageService.runExternally(session.getFilePath(), session.getEditor().getCode());
            terminal.attachProcess(process, "Running " + session.getDisplayName(), new TerminalSessionListener() {
                @Override
                public void onStarted(TerminalSession terminal) {
                    if (executionListener != null) {
                        executionListener.onRunStarted(session, terminal.getName());
                    }
                }

                @Override
                public void onOutput(TerminalSession terminal, String chunk, String transcript) {
                    if (executionListener != null) {
                        executionListener.onRunOutput(session, transcript);
                    }
                }

                @Override
                public void onFinished(TerminalSession terminal, int exitCode, String transcript) {
                    if (executionListener != null) {
                        executionListener.onRunFinished(session, exitCode, transcript);
                    }
                }

                @Override
                public void onError(TerminalSession terminal, String transcript, String message) {
                    if (executionListener != null) {
                        executionListener.onRunStreamError(session, transcript, message);
                    }
                }
            });
        } catch (Exception ex) {
            TerminalSession terminal = terminalManager.getActiveSession();
            if (terminal != null) {
                terminal.print("Failed to run: " + ex.getMessage() + "\n");
            }
            if (executionListener != null) {
                executionListener.onRunFailed(session, ex.getMessage());
            }
        }
    }

    public void setExecutionListener(ExecutionListener executionListener) {
        this.executionListener = executionListener;
    }
}
