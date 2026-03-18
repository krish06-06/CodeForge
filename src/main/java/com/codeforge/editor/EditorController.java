package com.codeforge.editor;

import com.codeforge.runner.PythonRunner;
import com.codeforge.terminal.TerminalManager;
import com.codeforge.terminal.TerminalSession;

public class EditorController {

    private final EditorTabManager editorTabs;
    private final TerminalManager terminalManager;

    public EditorController(EditorTabManager editorTabs, TerminalManager terminalManager) {
        this.editorTabs = editorTabs;
        this.terminalManager = terminalManager;
    }

    public void runActiveFile() {
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null) {
            return;
        }

        try {
            TerminalSession terminal = terminalManager.getActiveSession();
            if (terminal == null) {
                terminal = terminalManager.createTerminal();
            }

            Process process = PythonRunner.run(session.getFilePath(), session.getEditor().getCode());
            terminal.attachProcess(process, "Running " + session.getDisplayName());
        } catch (Exception ex) {
            TerminalSession terminal = terminalManager.getActiveSession();
            if (terminal != null) {
                terminal.print("Failed to run: " + ex.getMessage() + "\n");
            }
        }
    }
}
