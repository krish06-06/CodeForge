package com.codeforge.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ConfigManager {

    private static final String ENV_FILE_NAME = ".env";
    private static final Map<String, String> values = new HashMap<>();
    private static final Set<String> warnedMissingKeys = new HashSet<>();
    private static boolean loaded;

    private ConfigManager() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loadFromPath(findEnvFile());
    }

    public static synchronized String get(String key) {
        if (!loaded) {
            load();
        }

        String value = values.get(key);
        if ((value == null || value.isBlank()) && key != null && warnedMissingKeys.add(key)) {
            System.err.println("[CodeForge] Missing config value: " + key);
        }
        return value;
    }

    static synchronized void loadFromPath(Path envPath) {
        values.clear();
        warnedMissingKeys.clear();
        loaded = true;

        if (envPath == null) {
            System.err.println("[CodeForge] .env file not found. Configuration values will be unavailable.");
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                parseLine(rawLine);
            }
        } catch (IOException ex) {
            System.err.println("[CodeForge] Failed to load " + envPath + ": " + ex.getMessage());
        }
    }

    static synchronized void resetForTests() {
        values.clear();
        warnedMissingKeys.clear();
        loaded = false;
    }

    private static Path findEnvFile() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void parseLine(String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();
        if (key.isEmpty()) {
            return;
        }

        values.put(key, stripQuotes(value));
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
