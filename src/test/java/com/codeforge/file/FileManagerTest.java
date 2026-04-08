package com.codeforge.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerTest {

    @Test
    void ensureFileExistsCreatesMissingFileAndParents() throws Exception {
        Path tempDir = Files.createTempDirectory("codeforge-file-manager");
        Path target = tempDir.resolve("nested").resolve("created.py");

        FileManager.ensureFileExists(target);

        assertTrue(Files.exists(target));
        assertEquals("", Files.readString(target));
    }

    @Test
    void writeFilePersistsContent() throws Exception {
        Path tempDir = Files.createTempDirectory("codeforge-file-manager");
        Path target = tempDir.resolve("written.py");

        FileManager.writeFile(target, "print('ok')");

        assertEquals("print('ok')", Files.readString(target));
    }
}
