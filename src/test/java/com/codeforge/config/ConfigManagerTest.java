package com.codeforge.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigManagerTest {

    @AfterEach
    void tearDown() {
        ConfigManager.resetForTests();
    }

    @Test
    void loadsValuesFromDotEnvFile() throws Exception {
        Path tempDir = Files.createTempDirectory("codeforge-config");
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            GROQ_API_KEY=test-key
            QUOTED_VALUE="hello world"
            export SIMPLE_FLAG=true
            """);

        ConfigManager.loadFromPath(envFile);

        assertEquals("test-key", ConfigManager.get("GROQ_API_KEY"));
        assertEquals("hello world", ConfigManager.get("QUOTED_VALUE"));
        assertEquals("true", ConfigManager.get("SIMPLE_FLAG"));
    }

    @Test
    void returnsNullForMissingValue() {
        ConfigManager.loadFromPath(null);

        assertNull(ConfigManager.get("MISSING_KEY"));
    }
}
