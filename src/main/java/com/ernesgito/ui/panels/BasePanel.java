package com.ernesgito.ui.panels;

import com.ernesgito.service.GitService;
import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

/**
 * Base class for all ErnesGit-O panels.
 * Provides access to GitService, the main window, and UI utilities.
 */
public abstract class BasePanel {

    protected final GitService git = GitService.getInstance();
    protected final MainWindow window;

    public BasePanel(MainWindow window) {
        this.window = window;
    }

    /** Returns the root node of the panel to display it in the contentArea */
    public abstract Node getRoot();

    /** Reloads the panel data from the repository */
    public abstract void refresh();

    // ─────────────────────────────────────────────────────
    // BACKGROUND OPERATIONS
    // ─────────────────────────────────────────────────────

    /**
     * Executes a git operation on a background thread and then updates the UI.
     * @param operation  Supplier that returns the result of the operation
     * @param onSuccess  Runnable to execute on the JavaFX thread on success
     * @param onFailure  Consumer<String> with the error message on failure
     */
    protected void runAsync(GitOperation operation,
                            Runnable onSuccess,
                            java.util.function.Consumer<String> onFailure) {
        Task<com.ernesgito.model.GitResult> task = new Task<>() {
            @Override
            protected com.ernesgito.model.GitResult call() throws Exception {
                return operation.run();
            }
        };

        task.setOnSucceeded(e -> {
            com.ernesgito.model.GitResult result = task.getValue();
            if (result.isSuccess()) {
                onSuccess.run();
            } else {
                onFailure.accept(result.getOutputAsString());
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            onFailure.accept(ex != null ? ex.getMessage() : "Unknown error");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** Functional interface for git operations that may throw exceptions */
    @FunctionalInterface
    protected interface GitOperation {
        com.ernesgito.model.GitResult run() throws Exception;
    }

    // ─────────────────────────────────────────────────────
    // UI UTILITIES
    // ─────────────────────────────────────────────────────

    protected void showSuccess(String message) {
        Platform.runLater(() ->
            MainWindow.showAlert(Alert.AlertType.INFORMATION, "Success", message)
        );
    }

    protected void showError(String message) {
        Platform.runLater(() ->
            MainWindow.showAlert(Alert.AlertType.ERROR, "Error", message)
        );
    }

    protected boolean confirm(String title, String message) {
        return MainWindow.confirm(title, message);
    }

    protected String askInput(String title, String label, String defaultVal) {
        return MainWindow.inputDialog(title, label, defaultVal)
            .showAndWait()
            .orElse(null);
    }
}
