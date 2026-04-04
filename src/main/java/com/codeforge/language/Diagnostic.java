package com.codeforge.language;

public class Diagnostic {

    private final DiagnosticSeverity severity;
    private final String source;
    private final String message;
    private final int line;
    private final int column;

    public Diagnostic(DiagnosticSeverity severity, String source, String message, int line, int column) {
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        String location = line > 0 ? "Line " + line + ", Col " + column : "Unknown location";
        return severity + " [" + source + "] " + message + " (" + location + ")";
    }
}
