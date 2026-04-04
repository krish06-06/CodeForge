package com.codeforge.editor;

import com.codeforge.file.FileManager;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EditorTabManager {

    private final TabPane tabPane;
    private final Map<Tab, EditorTabSession> sessionsByTab = new LinkedHashMap<>();
    private final Map<Path, EditorTabSession> sessionsByPath = new LinkedHashMap<>();
    private Consumer<EditorTabSession> activeSessionListener;
    private Consumer<EditorTabSession> sessionOpenedListener;
    private Predicate<EditorTabSession> closeRequestHandler;

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

        saveSession(stage, session);
    }

    public boolean saveSession(Stage stage, EditorTabSession session) throws Exception {
        if (session == null) {
            return false;
        }

        Path previousPath = session.getFilePath();
        Path savedPath = FileManager.saveFile(stage, previousPath, session.getEditor().getCode());
        if (savedPath == null) {
            return false;
        }

        if (previousPath != null) {
            sessionsByPath.remove(previousPath);
        }

        session.setFilePath(savedPath);
        session.getEditor().markSaved();
        sessionsByPath.put(savedPath, session);
        return true;
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
        List<EditorTabSession> sessions = new ArrayList<>(getSessionsInside(path));
        for (EditorTabSession session : sessions) {
            closeSession(session, true);
        }

        if (tabPane.getTabs().isEmpty()) {
            openUntitled();
        }
    }

    public Collection<EditorTabSession> getSessions() {
        return List.copyOf(sessionsByTab.values());
    }

    public List<EditorTabSession> getSessionsInside(Path path) {
        List<EditorTabSession> sessions = new ArrayList<>();
        sessionsByPath.forEach((openPath, session) -> {
            if (openPath != null && openPath.startsWith(path)) {
                sessions.add(session);
            }
        });
        return sessions;
    }

    public boolean hasDirtySessions() {
        return sessionsByTab.values().stream().anyMatch(session -> session.getEditor().isDirty());
    }

    public List<EditorTabSession> getDirtySessionsInside(Path path) {
        return getSessionsInside(path).stream()
            .filter(session -> session.getEditor().isDirty())
            .toList();
    }

    public Optional<Path> getActivePath() {
        EditorTabSession session = getActiveSession();
        return session == null ? Optional.empty() : Optional.ofNullable(session.getFilePath());
    }

    public void setActiveSessionListener(Consumer<EditorTabSession> listener) {
        this.activeSessionListener = listener;
    }

    public void setSessionOpenedListener(Consumer<EditorTabSession> listener) {
        this.sessionOpenedListener = listener;
    }

    public void setCloseRequestHandler(Predicate<EditorTabSession> handler) {
        this.closeRequestHandler = handler;
    }

    private void registerSession(EditorTabSession session) {
        sessionsByTab.put(session.getTab(), session);
        tabPane.getTabs().add(session.getTab());
        if (sessionOpenedListener != null) {
            sessionOpenedListener.accept(session);
        }

        session.getTab().setOnCloseRequest(event -> {
            if (!closeSession(session, false)) {
                event.consume();
            }
        });
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

    private boolean closeSession(EditorTabSession session, boolean force) {
        if (session == null) {
            return true;
        }

        if (!force && closeRequestHandler != null && !closeRequestHandler.test(session)) {
            return false;
        }

        tabPane.getTabs().remove(session.getTab());
        sessionsByTab.remove(session.getTab());
        if (session.getFilePath() != null) {
            sessionsByPath.remove(session.getFilePath());
        }

        if (tabPane.getTabs().isEmpty()) {
            openUntitled();
        }

        return true;
    }
}
