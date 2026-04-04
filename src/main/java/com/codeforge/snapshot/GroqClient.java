package com.codeforge.snapshot;

import com.codeforge.config.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqClient implements LLMClient {

    private static final URI API_URI = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final String MODEL = "llama-3.1-8b-instant";
    private static final Pattern CONTENT_PATTERN = Pattern.compile(
        "\"message\"\\s*:\\s*\\{[^{}]*?\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
        Pattern.DOTALL
    );

    private final HttpClient httpClient;
    private final String apiKey;

    public GroqClient() {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build(), ConfigManager.get("GROQ_API_KEY"));
    }

    GroqClient(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Missing GROQ_API_KEY");
        }

        String requestBody = """
            {
              "model": "%s",
              "temperature": 0.2,
              "max_tokens": 180,
              "messages": [
                {
                  "role": "system",
                  "content": "%s"
                },
                {
                  "role": "user",
                  "content": "%s"
                }
              ]
            }
            """.formatted(
            escapeJson(MODEL),
            escapeJson(systemPrompt),
            escapeJson(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder(API_URI)
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Groq request failed with HTTP " + response.statusCode());
        }

        return extractContent(response.body());
    }

    private String extractContent(String responseBody) {
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody == null ? "" : responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Groq response did not include assistant content");
        }
        return unescapeJson(matcher.group(1));
    }

    private static String escapeJson(String value) {
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

    private static String unescapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index + 1 >= value.length()) {
                builder.append(current);
                continue;
            }

            char next = value.charAt(++index);
            switch (next) {
                case '\\' -> builder.append('\\');
                case '"' -> builder.append('"');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        throw new IllegalStateException("Invalid unicode escape in Groq response");
                    }
                    String hex = value.substring(index + 1, index + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                }
                default -> builder.append(next);
            }
        }
        return builder.toString();
    }
}
