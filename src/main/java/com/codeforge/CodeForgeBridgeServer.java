package com.codeforge;

import com.codeforge.editor.EditorTabManager;
import com.codeforge.editor.EditorTabSession;
import com.codeforge.snapshot.AISnapshotResult;
import com.codeforge.terminal.TerminalManager;
import com.codeforge.terminal.TerminalSession;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class CodeForgeBridgeServer {

    private static final String HOST = "localhost";
    private static final int PORT = 8765;

    private final WorkbenchController workbench;
    private HttpServer server;
    private ExecutorService executor;

    public CodeForgeBridgeServer(WorkbenchController workbench) {
        this.workbench = workbench;
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
            executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "codeforge-bridge");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/new-file", exchange -> handle(exchange, this::newFile));
            server.createContext("/open-file", exchange -> handle(exchange, this::openFile));
            server.createContext("/create-file", exchange -> handle(exchange, this::createFile));
            server.createContext("/get-code", exchange -> handle(exchange, this::getCode));
            server.createContext("/set-code", exchange -> handle(exchange, this::setCode));
            server.createContext("/save-as", exchange -> handle(exchange, this::saveAs));
            server.createContext("/run", exchange -> handle(exchange, this::runActive));
            server.createContext("/output", exchange -> handle(exchange, this::getOutput));
            server.createContext("/snapshot", exchange -> handle(exchange, this::getSnapshot));
            server.start();
            System.out.println("[CodeForge] Bridge server listening on http://" + HOST + ":" + PORT);
        } catch (IOException ex) {
            System.err.println("[CodeForge] Failed to start bridge server: " + ex.getMessage());
            if (server != null) {
                server.stop(0);
                server = null;
            }
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private Response newFile(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        EditorTabSession session = runOnFxThread(workbench::newScratchFile);
        return Response.ok("""
            {
              "ok": true,
              "name": "%s"
            }
            """.formatted(escapeJson(session.getDisplayName())));
    }

    private Response openFile(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        String body = readBody(exchange);
        String rawPath = requireStringField(body, "path");
        EditorTabSession session;
        try {
            Path path = Path.of(rawPath);
            session = runOnFxThread(() -> workbench.getEditorTabs().openFile(path));
        } catch (InvalidPathException ex) {
            throw new BadRequestException("Invalid path");
        }

        return Response.ok("""
            {
              "ok": true,
              "path": "%s",
              "displayName": "%s"
            }
            """.formatted(
            escapeJson(rawPath),
            escapeJson(session.getDisplayName())
        ));
    }

    private Response createFile(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        String body = readBody(exchange);
        String rawPath = requireStringField(body, "path");
        EditorTabSession session;
        try {
            Path path = Path.of(rawPath);
            session = runOnFxThread(() -> workbench.createOrOpenFile(path));
        } catch (InvalidPathException ex) {
            throw new BadRequestException("Invalid path");
        }

        return Response.ok("""
            {
              "ok": true,
              "path": "%s",
              "display_name": "%s"
            }
            """.formatted(
            escapeJson(session.getFilePath() == null ? rawPath : session.getFilePath().toString()),
            escapeJson(session.getDisplayName())
        ));
    }

    private Response getCode(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET");
        String code = runOnFxThread(() -> {
            EditorTabSession session = requireActiveSession();
            return session.getEditor().getCode();
        });

        return Response.ok("""
            {
              "ok": true,
              "code": "%s"
            }
            """.formatted(escapeJson(code)));
    }

    private Response setCode(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        String body = readBody(exchange);
        String code = requireStringField(body, "code");
        EditorTabSession session = runOnFxThread(() -> {
            EditorTabSession activeSession = requireActiveSession();
            activeSession.getEditor().setCode(code);
            return activeSession;
        });

        return Response.ok("""
            {
              "ok": true,
              "displayName": "%s"
            }
            """.formatted(escapeJson(session.getDisplayName())));
    }

    private Response saveAs(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        String body = readBody(exchange);
        String rawPath = requireStringField(body, "path");
        EditorTabSession session;
        try {
            Path path = Path.of(rawPath);
            session = runOnFxThread(() -> workbench.saveActiveAs(path));
        } catch (InvalidPathException ex) {
            throw new BadRequestException("Invalid path");
        }

        if (session == null || session.getFilePath() == null) {
            throw new BadRequestException("No active editor session");
        }

        return Response.ok("""
            {
              "ok": true,
              "path": "%s",
              "display_name": "%s"
            }
            """.formatted(
            escapeJson(session.getFilePath().toString()),
            escapeJson(session.getDisplayName())
        ));
    }

    private Response runActive(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "POST");
        String displayName = runOnFxThread(() -> {
            EditorTabSession session = requireActiveSession();
            workbench.runActive();
            return session.getDisplayName();
        });

        return Response.ok("""
            {
              "ok": true,
              "status": "running",
              "displayName": "%s"
            }
            """.formatted(escapeJson(displayName)));
    }

    private Response getOutput(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET");
        String transcript = runOnFxThread(() -> {
            TerminalManager terminalManager = workbench.getTerminalManager();
            TerminalSession terminal = terminalManager.getActiveSession();
            return terminal == null ? "" : terminal.getTranscript();
        });

        return Response.ok("""
            {
              "ok": true,
              "output": "%s"
            }
            """.formatted(escapeJson(transcript)));
    }

    private Response getSnapshot(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET");
        AISnapshotResult snapshot = runOnFxThread(() -> {
            EditorTabSession session = workbench.getEditorTabs().getActiveSession();
            return session == null ? AISnapshotResult.idle() : session.getSnapshotResult();
        });

        return Response.ok("""
            {
              "ok": true,
              "snapshot": {
                "problem": "%s",
                "cause": "%s",
                "fix": "%s",
                "confidence": "%s"
              }
            }
            """.formatted(
            escapeJson(snapshot.getProblem()),
            escapeJson(snapshot.getCause()),
            escapeJson(snapshot.getFix()),
            escapeJson(snapshot.getConfidence())
        ));
    }

    private void handle(HttpExchange exchange, BridgeAction action) throws IOException {
        try {
            Response response = action.execute(exchange);
            writeResponse(exchange, response);
        } catch (BadRequestException ex) {
            writeResponse(exchange, Response.badRequest(ex.getMessage()));
        } catch (MethodNotAllowedException ex) {
            writeResponse(exchange, Response.methodNotAllowed(ex.getMessage()));
        } catch (Exception ex) {
            writeResponse(exchange, Response.serverError(ex.getMessage() == null ? "Request failed" : ex.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private EditorTabSession requireActiveSession() throws BadRequestException {
        EditorTabManager editorTabs = workbench.getEditorTabs();
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null) {
            throw new BadRequestException("No active editor session");
        }
        return session;
    }

    private <T> T runOnFxThread(FxCallable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        FutureTask<T> task = new FutureTask<>(action::call);
        Platform.runLater(task);

        try {
            return task.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for JavaFX", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IOException("JavaFX execution failed", cause);
        }
    }

    private void requireMethod(HttpExchange exchange, String method) throws MethodNotAllowedException {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new MethodNotAllowedException("Expected " + method + " request");
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String requireStringField(String json, String key) throws BadRequestException {
        String value = findStringField(json, key);
        if (value == null) {
            throw new BadRequestException("Missing field: " + key);
        }
        return value;
    }

    private String findStringField(String json, String key) throws BadRequestException {
        if (json == null) {
            return null;
        }

        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + token.length());
        if (colonIndex < 0) {
            throw new BadRequestException("Malformed JSON body");
        }

        int valueIndex = nextNonWhitespace(json, colonIndex + 1);
        if (valueIndex < 0 || json.charAt(valueIndex) != '"') {
            throw new BadRequestException("Expected string value for " + key);
        }

        return readJsonString(json, valueIndex);
    }

    private int nextNonWhitespace(String text, int startIndex) {
        for (int index = startIndex; index < text.length(); index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private String readJsonString(String text, int openingQuoteIndex) throws BadRequestException {
        StringBuilder builder = new StringBuilder();
        for (int index = openingQuoteIndex + 1; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                return builder.toString();
            }
            if (current != '\\') {
                builder.append(current);
                continue;
            }
            if (index + 1 >= text.length()) {
                throw new BadRequestException("Invalid JSON escape");
            }

            char next = text.charAt(++index);
            switch (next) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (index + 4 >= text.length()) {
                        throw new BadRequestException("Invalid unicode escape");
                    }
                    String hex = text.substring(index + 1, index + 5);
                    try {
                        builder.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ex) {
                        throw new BadRequestException("Invalid unicode escape");
                    }
                    index += 4;
                }
                default -> throw new BadRequestException("Invalid JSON escape");
            }
        }

        throw new BadRequestException("Unterminated JSON string");
    }

    private void writeResponse(HttpExchange exchange, Response response) throws IOException {
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(response.statusCode(), body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (char current : value.toCharArray()) {
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }

    @FunctionalInterface
    private interface BridgeAction {
        Response execute(HttpExchange exchange) throws Exception;
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }

    private record Response(int statusCode, String body) {
        private static Response ok(String body) {
            return new Response(200, body);
        }

        private static Response badRequest(String message) {
            return error(400, message);
        }

        private static Response methodNotAllowed(String message) {
            return error(405, message);
        }

        private static Response serverError(String message) {
            return error(500, message);
        }

        private static Response error(int statusCode, String message) {
            return new Response(statusCode, """
                {
                  "ok": false,
                  "error": "%s"
                }
                """.formatted(escapeJson(message)));
        }
    }

    private static final class BadRequestException extends Exception {
        private BadRequestException(String message) {
            super(message);
        }
    }

    private static final class MethodNotAllowedException extends Exception {
        private MethodNotAllowedException(String message) {
            super(message);
        }
    }
}
