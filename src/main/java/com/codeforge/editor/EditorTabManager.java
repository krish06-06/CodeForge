package com.codeforge.editor;

import com.codeforge.file.FileManager;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class EditorTabManager {

    private final TabPane tabPane;
    private final Map<Tab, EditorTabSession> sessionsByTab = new LinkedHashMap<>();
    private final Map<Path, EditorTabSession> sessionsByPath = new LinkedHashMap<>();
    private Consumer<EditorTabSession> activeSessionListener;

    public EditorTabManager() {
        this.tabPane = new TabPane();
        this.tabPane.getStyleClass().add("editor-tabs");
        this.tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (activeSessionListener != null) {
                activeSessionListener.accept(getSession(newTab));
            }
        });
        openUntitled();
    }

    public TabPane getView() {
        return tabPane;
    }

    public EditorTabSession openUntitled() {
        EditorTabSession session = new EditorTabSession();
        registerSession(session);
        tabPane.getSelectionModel().select(session.getTab());
        return session;
    }

    public EditorTabSession openFile(Path path) throws Exception {
        EditorTabSession existing = sessionsByPath.get(path);
        if (existing != null) {
            tabPane.getSelectionModel().select(existing.getTab());
            return existing;
        }

        EditorTabSession session = new EditorTabSession(path, FileManager.readFile(path));
        registerSession(session);
        sessionsByPath.put(path, session);
        tabPane.getSelectionModel().select(session.getTab());
        return session;
    }

    public EditorTabSession getActiveSession() {
        return getSession(tabPane.getSelectionModel().getSelectedItem());
    }

    public void saveActive(Stage stage) throws Exception {
        EditorTabSession session = getActiveSession();
        if (session == null) {
            return;
        }

        Path previousPath = session.getFilePath();
        Path savedPath = FileManager.saveFile(stage, previousPath, session.getEditor().getCode());
        if (savedPath == null) {
            return;
        }

        if (previousPath != null) {
            sessionsByPath.remove(previousPath);
        }

        session.setFilePath(savedPath);
        session.getEditor().markSaved();
        sessionsByPath.put(savedPath, session);
    }

    public void renamePath(Path oldPath, Path newPath) {
        Map<Path, EditorTabSession> remapped = new LinkedHashMap<>();

        sessionsByPath.forEach((path, session) -> {
            if (path.equals(oldPath) || path.startsWith(oldPath)) {
                Path relative = oldPath.equals(path) ? Path.of("") : oldPath.relativize(path);
                Path updated = relative.toString().isEmpty() ? newPath : newPath.resolve(relative);
                session.setFilePath(updated);
                remapped.put(updated, session);
            } else {
                remapped.put(path, session);
            }
        });

        sessionsByPath.clear();
        sessionsByPath.putAll(remapped);
    }

    public void closePath(Path path) {
        EditorTabSession session = sessionsByPath.remove(path);
        if (session != null) {
            tabPane.getTabs().remove(session.getTab());
            sessionsByTab.remove(session.getTab());
        }

        if (tabPane.getTabs().isEmpty()) {
            openUntitled();
        }
    }

    public void closeSessionsInside(Path path) {
        sessionsByPath.entrySet().removeIf(entry -> {
            Path openPath = entry.getKey();
            if (openPath != null && openPath.startsWith(path)) {
                tabPane.getTabs().remove(entry.getValue().getTab());
                sessionsByTab.remove(entry.getValue().getTab());
                return true;
            }
            return false;
        });

        if (tabPane.getTabs().isEmpty()) {
            openUntitled();
        }
    }

    public Optional<Path> getActivePath() {
        EditorTabSession session = getActiveSession();
        return session == null ? Optional.empty() : Optional.ofNullable(session.getFilePath());
    }

    public void setActiveSessionListener(Consumer<EditorTabSession> listener) {
        this.activeSessionListener = listener;
    }

    private void registerSession(EditorTabSession session) {
        sessionsByTab.put(session.getTab(), session);
        tabPane.getTabs().add(session.getTab());

        session.getTab().setOnClosed(event -> {
            sessionsByTab.remove(session.getTab());
            if (session.getFilePath() != null) {
                sessionsByPath.remove(session.getFilePath());
            }

            if (tabPane.getTabs().isEmpty()) {
                openUntitled();
            }
        });
    }

    private EditorTabSession getSession(Tab tab) {
        return tab == null ? null : sessionsByTab.get(tab);
    }
}
