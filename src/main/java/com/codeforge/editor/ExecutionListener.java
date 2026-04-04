package com.codeforge.editor;

public interface ExecutionListener {

    void onRunStarted(EditorTabSession session, String terminalName);

    void onRunOutput(EditorTabSession session, String transcript);

    void onRunFinished(EditorTabSession session, int exitCode, String transcript);

    void onRunFailed(EditorTabSession session, String message);

    void onRunStreamError(EditorTabSession session, String transcript, String message);
}
