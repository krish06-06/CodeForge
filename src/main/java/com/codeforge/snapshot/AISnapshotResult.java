package com.codeforge.snapshot;

public final class AISnapshotResult {

    private final String problem;
    private final String cause;
    private final String fix;
    private final String confidence;

    public AISnapshotResult(String problem, String cause, String fix, String confidence) {
        this.problem = clean(problem);
        this.cause = clean(cause);
        this.fix = clean(fix);
        this.confidence = normalizeConfidence(confidence);
    }

    public String getProblem() {
        return problem;
    }

    public String getCause() {
        return cause;
    }

    public String getFix() {
        return fix;
    }

    public String getConfidence() {
        return confidence;
    }

    public static AISnapshotResult idle() {
        return new AISnapshotResult(
            "Run code to analyze terminal output",
            "AI Snapshot watches completed terminal runs and extracts the likely issue.",
            "Run the active file, then check this panel for a short debugging summary.",
            "Medium"
        );
    }

    public static AISnapshotResult noIssuesDetected() {
        return new AISnapshotResult(
            "No issues detected",
            "The latest terminal output does not show a clear Python error or traceback.",
            "If the program is still wrong, reproduce the failure and rerun so CodeForge can inspect the output.",
            "Medium"
        );
    }

    public static AISnapshotResult unavailable(String problem, String cause, String fix) {
        return new AISnapshotResult(
            problem,
            cause,
            fix,
            "High"
        );
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "Unavailable";
        }
        return value.trim();
    }

    private static String normalizeConfidence(String value) {
        if (value == null) {
            return "Medium";
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "low" -> "Low";
            case "high" -> "High";
            default -> "Medium";
        };
    }
}
