package com.codeforge.snapshot;

import com.codeforge.editor.EditorTabSession;
import com.codeforge.language.Diagnostic;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AISnapshotService {

    private static final Pattern MODULE_NOT_FOUND_PATTERN = Pattern.compile(
        "ModuleNotFoundError:\\s+No module named ['\"]?([^'\"\\n]+)['\"]?",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NAME_ERROR_PATTERN = Pattern.compile(
        "NameError:\\s+name ['\"]?([^'\"\\n]+)['\"]? is not defined",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FILE_NOT_FOUND_PATTERN = Pattern.compile(
        "FileNotFoundError:\\s+\\[Errno 2\\]\\s+No such file or directory:\\s+['\"]([^'\"]+)['\"]",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IMPORT_ERROR_PATTERN = Pattern.compile(
        "ImportError:\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SYNTAX_ERROR_PATTERN = Pattern.compile(
        "^SyntaxError:\\s+(.+)$",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    private static final Pattern PROBLEM_PATTERN = Pattern.compile(
        "(?is)Problem:\\s*(.+?)(?=\\n\\s*Cause:|\\z)"
    );
    private static final Pattern CAUSE_PATTERN = Pattern.compile(
        "(?is)Cause:\\s*(.+?)(?=\\n\\s*Fix:|\\z)"
    );
    private static final Pattern FIX_PATTERN = Pattern.compile(
        "(?is)Fix:\\s*(.+?)(?=\\n\\s*Confidence:|\\z)"
    );
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile(
        "(?is)Confidence:\\s*(Low|Medium|High)"
    );
    private static final int MAX_LINES = 60;
    private static final int MAX_CHARS = 3200;

    private final LLMClient llmClient;
    private String lastAnalysisKey;
    private AISnapshotResult lastAnalysisResult;

    public AISnapshotService() {
        this(new GroqClient());
    }

    AISnapshotService(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public AISnapshotResult idleResult() {
        return AISnapshotResult.idle();
    }

    public AISnapshotResult summarizeValidation(EditorTabSession session) {
        if (session == null) {
            return idleResult();
        }

        List<Diagnostic> diagnostics = session.getDiagnostics();
        if (diagnostics.isEmpty()) {
            return new AISnapshotResult(
                "No pre-run issues detected",
                "CodeForge's lexer and parser do not see a syntax problem in the active file.",
                "Run the file to capture runtime output if the bug appears during execution.",
                "High"
            );
        }

        Diagnostic first = diagnostics.getFirst();
        String location = first.getLine() > 0
            ? "Line %d, Column %d.".formatted(first.getLine(), first.getColumn())
            : "The exact location was not reported.";
        return new AISnapshotResult(
            "Pre-run syntax issue",
            first.getMessage() + " " + location,
            suggestionFor(first),
            "High"
        );
    }

    public AISnapshotResult analyzeExecution(String transcript, int exitCode) {
        String preparedOutput = prepareOutput(transcript);
        if (preparedOutput.isBlank()) {
            return null;
        }

        String cacheKey = exitCode + "::" + preparedOutput;
        synchronized (this) {
            if (cacheKey.equals(lastAnalysisKey) && lastAnalysisResult != null) {
                return lastAnalysisResult;
            }
        }

        AISnapshotResult result = detectLocally(preparedOutput);
        if (result == null) {
            result = shouldUseLLM(preparedOutput, exitCode)
                ? analyzeWithLLM(preparedOutput, exitCode)
                : AISnapshotResult.noIssuesDetected();
        }

        synchronized (this) {
            lastAnalysisKey = cacheKey;
            lastAnalysisResult = result;
        }
        return result;
    }

    public AISnapshotResult summarizeRunFailure(String message) {
        return new AISnapshotResult(
            "Program failed to start",
            blankToFallback(message, "CodeForge could not launch the Python process."),
            "Verify that Python is installed and available on PATH, then run the file again.",
            "High"
        );
    }

    public AISnapshotResult summarizeTerminalError(String transcript, String message) {
        String preparedOutput = prepareOutput(transcript);
        if (!preparedOutput.isBlank()) {
            AISnapshotResult result = detectLocally(preparedOutput);
            if (result != null) {
                return result;
            }
        }

        return new AISnapshotResult(
            "Terminal stream error",
            blankToFallback(message, "CodeForge lost access to the process output stream."),
            "Run the file again. If it repeats, inspect the Python installation and terminal environment.",
            "Medium"
        );
    }

    public String buildExplainMorePrompt(String transcript, AISnapshotResult result) {
        return """
            Explain this CodeForge debugging snapshot in more depth.

            Problem: %s
            Cause: %s
            Fix: %s
            Confidence: %s

            Terminal output:
            %s
            """.formatted(
            result.getProblem(),
            result.getCause(),
            result.getFix(),
            result.getConfidence(),
            prepareOutput(transcript)
        );
    }

    private AISnapshotResult analyzeWithLLM(String preparedOutput, int exitCode) {
        if (!llmClient.isConfigured()) {
            return AISnapshotResult.unavailable(
                "AI Snapshot unavailable: missing GROQ_API_KEY",
                "Missing GROQ_API_KEY in the project .env file.",
                "Add GROQ_API_KEY to .env, then restart CodeForge."
            );
        }

        try {
            String response = llmClient.complete(systemPrompt(), userPrompt(preparedOutput, exitCode));
            return parseResponse(response);
        } catch (Exception ex) {
            return AISnapshotResult.unavailable(
                "AI Snapshot unavailable: request failed",
                "Request to Groq failed.",
                "Check network access and the GROQ_API_KEY value in .env, then run the program again."
            );
        }
    }

    private AISnapshotResult parseResponse(String response) {
        String problem = extractSection(PROBLEM_PATTERN, response);
        String cause = extractSection(CAUSE_PATTERN, response);
        String fix = extractSection(FIX_PATTERN, response);
        String confidence = extractSection(CONFIDENCE_PATTERN, response);

        if (problem == null || cause == null || fix == null) {
            return AISnapshotResult.unavailable(
                "AI Snapshot unavailable: invalid response",
                "Groq returned an unexpected response.",
                "Run the program again or inspect the terminal output directly."
            );
        }

        return new AISnapshotResult(problem, cause, fix, confidence);
    }

    private String extractSection(Pattern pattern, String response) {
        Matcher matcher = pattern.matcher(response == null ? "" : response.trim());
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private AISnapshotResult detectLocally(String preparedOutput) {
        Matcher moduleNotFound = MODULE_NOT_FOUND_PATTERN.matcher(preparedOutput);
        if (moduleNotFound.find()) {
            String moduleName = moduleNotFound.group(1).trim();
            return new AISnapshotResult(
                "Missing Python module",
                "Python could not import `%s` in the current environment.".formatted(moduleName),
                "Install `%s` in the active Python environment or correct the import name.".formatted(moduleName),
                "High"
            );
        }

        Matcher syntaxError = SYNTAX_ERROR_PATTERN.matcher(preparedOutput);
        if (syntaxError.find()) {
            return new AISnapshotResult(
                "Python syntax error",
                syntaxError.group(1).trim(),
                "Fix the highlighted statement in the file, save it, and run again.",
                "High"
            );
        }

        Matcher nameError = NAME_ERROR_PATTERN.matcher(preparedOutput);
        if (nameError.find()) {
            String name = nameError.group(1).trim();
            return new AISnapshotResult(
                "Undefined name",
                "The name `%s` is being used before it is defined or is out of scope.".formatted(name),
                "Define `%s` earlier, pass it into scope, or correct the spelling.".formatted(name),
                "High"
            );
        }

        Matcher fileNotFound = FILE_NOT_FOUND_PATTERN.matcher(preparedOutput);
        if (fileNotFound.find()) {
            String missingPath = fileNotFound.group(1).trim();
            return new AISnapshotResult(
                "Missing file path",
                "Python could not find `%s` from the current working context.".formatted(missingPath),
                "Check the file path, the working directory, and whether the file exists before rerunning.",
                "High"
            );
        }

        Matcher importError = IMPORT_ERROR_PATTERN.matcher(preparedOutput);
        if (importError.find()) {
            return new AISnapshotResult(
                "Import failed",
                importError.group(1).trim(),
                "Verify the imported symbol, file name, and interpreter environment.",
                "High"
            );
        }

        return null;
    }

    private boolean shouldUseLLM(String preparedOutput, int exitCode) {
        if (exitCode != 0) {
            return true;
        }

        String normalized = preparedOutput.toLowerCase();
        return normalized.contains("traceback")
            || normalized.contains("exception")
            || normalized.contains("error")
            || normalized.contains("failed");
    }

    private String prepareOutput(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return "";
        }

        String[] lines = transcript
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .strip()
            .split("\n");

        int start = Math.max(0, lines.length - MAX_LINES);
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < lines.length; index++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines[index].stripTrailing());
        }

        String prepared = builder.toString().trim();
        if (prepared.length() <= MAX_CHARS) {
            return prepared;
        }
        return prepared.substring(prepared.length() - MAX_CHARS).trim();
    }

    private String systemPrompt() {
        return """
            You are CodeForge AI Snapshot, an IDE-native debugging assistant.
            Analyze Python terminal output and respond using exactly this format:
            Problem: <one-line summary>
            Cause: <short explanation>
            Fix: <clear actionable step>
            Confidence:
            <Low | Medium | High>

            Keep every field concise.
            Do not add markdown, bullets, or extra sections.
            """;
    }

    private String userPrompt(String preparedOutput, int exitCode) {
        return """
            Exit code: %d

            Terminal output:
            %s
            """.formatted(exitCode, preparedOutput);
    }

    private String suggestionFor(Diagnostic diagnostic) {
        String source = diagnostic.getSource().toLowerCase();
        String message = diagnostic.getMessage().toLowerCase();

        if (source.contains("lexer")) {
            return "Check for an unexpected character, unmatched quote, or indentation issue near the flagged location.";
        }
        if (message.contains("expected ')'")) {
            return "Close the open parenthesis in the current statement and rerun.";
        }
        if (message.contains("expected '='")) {
            return "Review the assignment syntax around the reported variable name.";
        }
        return "Inspect the reported line against the syntax currently supported by CodeForge's parser.";
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
