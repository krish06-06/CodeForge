package com.codeforge.editor;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorTabManagerTest {

    @BeforeAll
    static void startFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void createsUntitledScratchSessionByDefault() throws Exception {
        runOnFxThread(() -> {
            EditorTabManager manager = new EditorTabManager();
            EditorTabSession session = manager.getActiveSession();

            assertNull(session.getFilePath());
            assertTrue(session.getDisplayName().startsWith("untitled-"));
            return null;
        });
    }

    @Test
    void saveSessionToPathUpdatesSessionMetadata() throws Exception {
        Path target = Files.createTempDirectory("codeforge-editor-tabs").resolve("saved_file.py");

        runOnFxThread(() -> {
            EditorTabManager manager = new EditorTabManager();
            EditorTabSession session = manager.getActiveSession();

            session.getEditor().setCode("print('saved')");
            assertTrue(manager.saveSessionToPath(session, target));
            assertEquals(target.toAbsolutePath().normalize(), session.getFilePath());
            assertEquals("saved_file.py", session.getDisplayName());
            assertFalse(session.getEditor().isDirty());
            return null;
        });

        assertEquals("print('saved')", Files.readString(target));
    }

    @Test
    void openFileReusesExistingSessionForSamePath() throws Exception {
        Path target = Files.createTempFile("codeforge-editor-tabs", ".py");
        Files.writeString(target, "print('hello')");

        runOnFxThread(() -> {
            EditorTabManager manager = new EditorTabManager();
            EditorTabSession first = manager.openFile(target);
            EditorTabSession second = manager.openFile(target);

            assertSame(first, second);
            return null;
        });
    }

    private <T> T runOnFxThread(FxCallable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable::call);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }
}
