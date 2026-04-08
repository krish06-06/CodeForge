package com.codeforge;

import com.codeforge.editor.EditorController;
import com.codeforge.editor.EditorTabManager;
import com.codeforge.editor.EditorTabSession;
import com.codeforge.editor.ExecutionListener;
import com.codeforge.file.FileManager;
import com.codeforge.file.WorkspaceView;
import com.codeforge.language.Diagnostic;
import com.codeforge.language.LanguageService;
import com.codeforge.problems.ProblemsView;
import com.codeforge.snapshot.AISnapshotResult;
import com.codeforge.snapshot.AISnapshotService;
import com.codeforge.snapshot.AISnapshotView;
import com.codeforge.terminal.TerminalManager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkbenchController {

    private final Stage stage;
    private final EditorTabManager editorTabs;
    private final TerminalManager terminalManager;
    private final WorkspaceView workspaceView;
    private final ProblemsView problemsView;
    private final AISnapshotView aiSnapshotView;
    private final LanguageService languageService;
    private final AISnapshotService aiSnapshotService;
    private final EditorController editorController;
    private final ExecutorService snapshotExecutor;
    private final StringProperty status = new SimpleStringProperty("Ready");
    private Path clipboardPath;

    public WorkbenchController(Stage stage) {
        this.stage = stage;
        this.editorTabs = new EditorTabManager();
        this.terminalManager = new TerminalManager();
        this.workspaceView = new WorkspaceView();
        this.problemsView = new ProblemsView();
        this.aiSnapshotView = new AISnapshotView();
        this.languageService = new LanguageService();
        this.aiSnapshotService = new AISnapshotService();
        this.snapshotExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-snapshot-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.editorController = new EditorController(editorTabs, terminalManager, languageService);
        this.editorController.setExecutionListener(new ExecutionListener() {
            @Override
            public void onRunStarted(EditorTabSession session, String terminalName) {
            }

            @Override
            public void onRunOutput(EditorTabSession session, String transcript) {
            }

            @Override
            public void onRunFinished(EditorTabSession session, int exitCode, String transcript) {
                analyzeExecutionAsync(session, transcript, exitCode);
            }

            @Override
            public void onRunFailed(EditorTabSession session, String message) {
                applySnapshotResult(session, aiSnapshotService.summarizeRunFailure(message));
            }

            @Override
            public void onRunStreamError(EditorTabSession session, String transcript, String message) {
                applySnapshotResult(session, aiSnapshotService.summarizeTerminalError(transcript, message));
            }
        });

        editorTabs.setSessionOpenedListener(this::configureSession);
        editorTabs.setActiveSessionListener(this::onActiveSessionChanged);
        editorTabs.setCloseRequestHandler(this::confirmClose);

        workspaceView.setListener(new WorkspaceView.WorkspaceListener() {
            @Override
            public void onOpenFile(Path path) {
                openFile(path);
            }

            @Override
            public void onRenameRequested(Path path) {
                renamePath(path);
            }

            @Override
            public void onDeleteRequested(Path path) {
                deletePath(path);
            }

            @Override
            public void onCopyRequested(Path path) {
                clipboardPath = path;
                setStatus("Copied " + path.getFileName());
            }

            @Override
            public void onPasteRequested(Path path) {
                pasteInto(path);
            }
        });

        problemsView.setOpenDiagnosticListener(this::openDiagnostic);

        EditorTabSession initial = editorTabs.getActiveSession();
        if (initial != null) {
            configureSession(initial);
            onActiveSessionChanged(initial);
        }
    }

    public EditorTabManager getEditorTabs() {
        return editorTabs;
    }

    public TerminalManager getTerminalManager() {
        return terminalManager;
    }

    public WorkspaceView getWorkspaceView() {
        return workspaceView;
    }

    public ProblemsView getProblemsView() {
        return problemsView;
    }

    public AISnapshotView getAiSnapshotView() {
        return aiSnapshotView;
    }

    public ReadOnlyStringProperty statusProperty() {
        return status;
    }

    public void newFile() {
        newScratchFile();
    }

    public EditorTabSession newScratchFile() {
        EditorTabSession session = editorTabs.openUntitled();
        setStatus("Created " + session.getDisplayName());
        return session;
    }

    public void createFile() {
        try {
            Path targetPath = chooseCreatePath();
            if (targetPath == null) {
                return;
            }

            boolean existed = Files.exists(targetPath);
            EditorTabSession session = createOrOpenFile(targetPath);
            if (session != null) {
                setStatus((existed ? "Opened " : "Created ") + session.getDisplayName());
            }
        } catch (Exception ex) {
            setStatus("Create failed: " + ex.getMessage());
        }
    }

    public void openFileChooser() {
        Path path = FileManager.chooseFile(stage);
        if (path != null) {
            openFile(path);
        }
    }

    public void openFolderChooser() {
        DirectoryChooser chooser = new DirectoryChooser();
        var folder = chooser.showDialog(stage);
        if (folder != null) {
            workspaceView.openFolder(folder.toPath());
            setStatus("Workspace " + folder.getAbsolutePath());
        }
    }

    public void saveActive() {
        try {
            EditorTabSession session = editorTabs.getActiveSession();
            if (session == null) {
                return;
            }

            boolean saved = editorTabs.saveSession(stage, session);
            if (saved) {
                validateSession(session);
                setStatus("Saved " + session.getDisplayName());
                updateSnapshotForSession(session);
            }
        } catch (Exception ex) {
            setStatus("Save failed: " + ex.getMessage());
        }
    }

    public EditorTabSession saveActiveAs(Path targetPath) throws Exception {
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null) {
            return null;
        }

        boolean saved = editorTabs.saveSessionToPath(session, targetPath);
        if (!saved) {
            return null;
        }

        validateSession(session);
        updateSnapshotForSession(session);
        setStatus("Saved " + session.getDisplayName());
        return session;
    }

    public void runActive() {
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null) {
            return;
        }

        editorController.runActiveFile();
        setStatus("Running " + session.getDisplayName());
    }

    public void newTerminal() {
        var session = terminalManager.createTerminal();
        setStatus("Opened " + session.getName());
    }

    public EditorTabSession createOrOpenFile(Path targetPath) throws Exception {
        Path resolvedPath = FileManager.ensureFileExists(targetPath).toAbsolutePath().normalize();
        EditorTabSession session = editorTabs.openFile(resolvedPath);
        setStatus("Opened " + resolvedPath);
        return session;
    }

    private void openFile(Path path) {
        try {
            createOrOpenFile(path);
        } catch (Exception ex) {
            setStatus("Open failed: " + ex.getMessage());
        }
    }

    private void renamePath(Path source) {
        TextInputDialog dialog = new TextInputDialog(source.getFileName().toString());
        dialog.initOwner(stage);
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + source.getFileName());
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String newName = result.get().trim();
        if (newName.isEmpty() || newName.equals(source.getFileName().toString())) {
            return;
        }

        try {
            Path renamed = FileManager.rename(source, newName);
            editorTabs.renamePath(source, renamed);
            if (source.equals(workspaceView.getRootPath())) {
                workspaceView.openFolder(renamed);
            } else {
                workspaceView.refresh();
            }
            setStatus("Renamed to " + renamed.getFileName());
        } catch (Exception ex) {
            setStatus("Rename failed: " + ex.getMessage());
        }
    }

    private void deletePath(Path source) {
        List<EditorTabSession> dirtySessions = editorTabs.getDirtySessionsInside(source);
        if (!dirtySessions.isEmpty() && !confirmDiscardForDelete(dirtySessions.size(), source)) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(stage);
        confirmation.setTitle("Delete");
        confirmation.setHeaderText("Delete " + source.getFileName() + "?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            FileManager.delete(source);
            editorTabs.closeSessionsInside(source);
            workspaceView.refresh();
            setStatus("Deleted " + source.getFileName());
        } catch (Exception ex) {
            setStatus("Delete failed: " + ex.getMessage());
        }
    }

    private void pasteInto(Path target) {
        if (clipboardPath == null) {
            setStatus("Nothing to paste");
            return;
        }

        Path destination = Files.isDirectory(target) ? target : target.getParent();
        try {
            Path pasted = FileManager.copy(clipboardPath, destination);
            workspaceView.refresh();
            setStatus("Pasted " + pasted.getFileName());
        } catch (Exception ex) {
            setStatus("Paste failed: " + ex.getMessage());
        }
    }

    private void configureSession(EditorTabSession session) {
        if (session == null) {
            return;
        }

        session.getEditor().setContentChangeListener(text -> {
            validateSession(session);
            session.setSnapshotResult(aiSnapshotService.summarizeValidation(session));
            if (session == editorTabs.getActiveSession()) {
                problemsView.setDiagnostics(session.getDiagnostics());
                updateSnapshotForSession(session);
            }
        });
        validateSession(session);
        session.setSnapshotResult(aiSnapshotService.summarizeValidation(session));
    }

    private void validateSession(EditorTabSession session) {
        List<Diagnostic> diagnostics = languageService.validate(session.getEditor().getCode());
        session.setDiagnostics(diagnostics);
    }

    private void onActiveSessionChanged(EditorTabSession session) {
        if (session == null) {
            problemsView.setDiagnostics(List.of());
            aiSnapshotView.setResult(aiSnapshotService.idleResult());
            setStatus("No active file");
            return;
        }

        problemsView.setDiagnostics(session.getDiagnostics());
        updateSnapshotForSession(session);
        setStatus(session.getFilePath() != null
            ? "Active file: " + session.getFilePath()
            : "Active file: " + session.getDisplayName());
    }

    private boolean confirmClose(EditorTabSession session) {
        if (!session.getEditor().isDirty()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("Save changes to " + session.getDisplayName() + "?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType save = new ButtonType("Save");
        ButtonType discard = new ButtonType("Discard");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == cancel) {
            return false;
        }

        if (choice.get() == save) {
            try {
                boolean saved = editorTabs.saveSession(stage, session);
                if (!saved) {
                    return false;
                }
            } catch (Exception ex) {
                setStatus("Save failed: " + ex.getMessage());
                return false;
            }
        }

        return true;
    }

    private boolean confirmDiscardForDelete(int dirtyCount, Path path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("Delete " + path.getFileName() + " with " + dirtyCount + " unsaved file(s)?");
        alert.setContentText("Open unsaved editor changes inside this path will be discarded.");

        ButtonType delete = new ButtonType("Delete");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(delete, cancel);

        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == delete;
    }

    private void openDiagnostic(Diagnostic diagnostic) {
        EditorTabSession session = editorTabs.getActiveSession();
        if (session == null || diagnostic.getLine() <= 0) {
            return;
        }

        session.getEditor().moveTo(diagnostic.getLine(), diagnostic.getColumn());
    }

    private void setStatus(String message) {
        status.set(message);
    }

    private Path chooseCreatePath() {
        Path preferredDirectory = resolvePreferredCreateDirectory();
        if (preferredDirectory != null) {
            String fileName = promptForFileName(preferredDirectory);
            if (fileName == null) {
                return null;
            }
            return preferredDirectory.resolve(fileName);
        }

        return FileManager.chooseSavePath(stage, editorTabs.getActivePath().orElse(null));
    }

    private Path resolvePreferredCreateDirectory() {
        Path workspaceRoot = workspaceView.getRootPath();
        if (workspaceRoot != null && Files.isDirectory(workspaceRoot)) {
            return workspaceRoot;
        }

        Path activePath = editorTabs.getActivePath().orElse(null);
        if (activePath != null) {
            Path parent = activePath.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent;
            }
        }
        return null;
    }

    private String promptForFileName(Path directory) {
        TextInputDialog dialog = new TextInputDialog("new_file.py");
        dialog.initOwner(stage);
        dialog.setTitle("Create File");
        dialog.setHeaderText("Create file in " + directory);
        dialog.setContentText("File name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }

        String fileName = result.get().trim();
        if (fileName.isEmpty()) {
            setStatus("Create failed: file name is required");
            return null;
        }
        if (fileName.contains("/") || fileName.contains("\\") || ".".equals(fileName) || "..".equals(fileName)) {
            setStatus("Create failed: enter a file name, not a path");
            return null;
        }
        return fileName;
    }

    private void updateSnapshotForSession(EditorTabSession session) {
        if (session == null) {
            return;
        }
        if (session.getSnapshotResult() == null) {
            session.setSnapshotResult(aiSnapshotService.summarizeValidation(session));
        }
        aiSnapshotView.setResult(session.getSnapshotResult());
    }

    private void analyzeExecutionAsync(EditorTabSession session, String transcript, int exitCode) {
        if (session == null || transcript == null || transcript.isBlank()) {
            return;
        }

        snapshotExecutor.submit(() -> {
            AISnapshotResult result = aiSnapshotService.analyzeExecution(transcript, exitCode);
            if (result != null) {
                applySnapshotResult(session, result);
            }
        });
    }

    private void applySnapshotResult(EditorTabSession session, AISnapshotResult result) {
        if (session == null || result == null) {
            return;
        }

        session.setSnapshotResult(result);
        Platform.runLater(() -> {
            if (session == editorTabs.getActiveSession()) {
                aiSnapshotView.setResult(result);
            }
        });
    }
}
