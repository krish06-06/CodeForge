package com.codeforge.snapshot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AISnapshotServiceTest {

    @Test
    void usesLocalDetectionForModuleNotFoundError() {
        StubClient client = new StubClient(true, """
            Problem: Should not be used
            Cause: Should not be used
            Fix: Should not be used
            Confidence:
            Low
            """);
        AISnapshotService service = new AISnapshotService(client);

        AISnapshotResult result = service.analyzeExecution("""
            Traceback (most recent call last):
              File "app.py", line 1, in <module>
                import requests
            ModuleNotFoundError: No module named 'requests'
            """, 1);

        assertEquals("Missing Python module", result.getProblem());
        assertEquals("High", result.getConfidence());
        assertEquals(0, client.callCount);
    }

    @Test
    void usesLlmForUnhandledTracebackAndCachesTheResult() {
        StubClient client = new StubClient(true, """
            Problem: Value parsing failed
            Cause: The program tried to convert non-numeric text to an integer.
            Fix: Validate the input before calling int() or handle the ValueError.
            Confidence:
            High
            """);
        AISnapshotService service = new AISnapshotService(client);
        String transcript = """
            Traceback (most recent call last):
              File "app.py", line 4, in <module>
                total = int(user_input)
            ValueError: invalid literal for int() with base 10: 'abc'
            """;

        AISnapshotResult first = service.analyzeExecution(transcript, 1);
        AISnapshotResult second = service.analyzeExecution(transcript, 1);

        assertEquals("Value parsing failed", first.getProblem());
        assertEquals("High", first.getConfidence());
        assertEquals(first.getProblem(), second.getProblem());
        assertEquals(1, client.callCount);
    }

    @Test
    void returnsUnavailableResultWhenClientIsNotConfigured() {
        StubClient client = new StubClient(false, "");
        AISnapshotService service = new AISnapshotService(client);

        AISnapshotResult result = service.analyzeExecution("""
            Traceback (most recent call last):
              File "app.py", line 1, in <module>
                raise RuntimeError("boom")
            RuntimeError: boom
            """, 1);

        assertEquals("AI Snapshot unavailable: missing GROQ_API_KEY", result.getProblem());
        assertEquals("Missing GROQ_API_KEY in the project .env file.", result.getCause());
        assertEquals(0, client.callCount);
    }

    @Test
    void returnsNullWhenTranscriptIsEmpty() {
        AISnapshotService service = new AISnapshotService(new StubClient(true, ""));

        assertNull(service.analyzeExecution("   ", 0));
    }

    private static final class StubClient implements LLMClient {
        private final boolean configured;
        private final String response;
        private int callCount;

        private StubClient(boolean configured, String response) {
            this.configured = configured;
            this.response = response;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            callCount++;
            return response;
        }
    }
}
