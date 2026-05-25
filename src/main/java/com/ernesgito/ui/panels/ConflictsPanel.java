package com.ernesgito.ui.panels;

import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * Merge / cherry-pick conflict resolution panel.
 * Displays conflicted files, allows marking them as resolved and completing the merge.
 */
public class ConflictsPanel extends BasePanel {

    private VBox root;
    private ListView<String> conflictList;
    private Label statusLabel;
    private TextArea commitMsgArea;

    public ConflictsPanel(MainWindow window) {
        super(window);
        buildUI();
    }

    private void buildUI() {
        root = new VBox();
        root.getStyleClass().add("panel");

        // ── Header ──
        HBox header = new HBox(12);
        header.getStyleClass().add("panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));

        Label title = new Label("⚡  Conflicts");
        title.getStyleClass().add("panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("↺  Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        header.getChildren().addAll(title, spacer, refreshBtn);

        // ── Status indicator ──
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(8, 20, 8, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");

        statusLabel = new Label("No active conflicts");
        statusLabel.getStyleClass().add("status-ok");
        statusBar.getChildren().add(statusLabel);

        // ── SplitPane ──
        SplitPane split = new SplitPane();
        VBox.setVgrow(split, Priority.ALWAYS);

        // Conflicted files list
        VBox listBox = new VBox(10);
        listBox.setPadding(new Insets(12));

        Label listTitle = new Label("CONFLICTED FILES");
        listTitle.getStyleClass().add("list-section-label");

        conflictList = new ListView<>();
        conflictList.getStyleClass().add("conflict-list");
        conflictList.setCellFactory(lv -> new ConflictCell());
        VBox.setVgrow(conflictList, Priority.ALWAYS);

        HBox listActions = new HBox(10);
        listActions.setAlignment(Pos.CENTER_LEFT);

        Button openBtn = new Button("📂  Open in editor");
        openBtn.getStyleClass().add("btn-action");
        openBtn.setOnAction(e -> openInEditor());

        Button resolveBtn = new Button("✓  Mark as resolved");
        resolveBtn.getStyleClass().add("btn-secondary");
        resolveBtn.setOnAction(e -> markResolved());

        listActions.getChildren().addAll(openBtn, resolveBtn);
        listBox.getChildren().addAll(listTitle, conflictList, listActions);

        // Merge actions panel
        VBox actionsBox = new VBox(12);
        actionsBox.setPadding(new Insets(12));

        Label actTitle = new Label("RESOLVE MERGE");
        actTitle.getStyleClass().add("list-section-label");

        Label msgLabel = new Label("Merge commit message:");
        msgLabel.getStyleClass().add("label-muted");

        commitMsgArea = new TextArea();
        commitMsgArea.setPromptText("Commit message (leave empty to use the automatic one)...");
        commitMsgArea.getStyleClass().add("diff-area");
        commitMsgArea.setPrefRowCount(3);
        commitMsgArea.setMaxHeight(100);

        Button continueBtn = new Button("✅  Continue merge");
        continueBtn.getStyleClass().add("btn-primary");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setOnAction(e -> continueMerge());

        Button abortBtn = new Button("✕  Abort merge");
        abortBtn.getStyleClass().add("btn-danger");
        abortBtn.setMaxWidth(Double.MAX_VALUE);
        abortBtn.setOnAction(e -> abortMerge());

        Separator sep = new Separator();

        Label helpLabel = new Label(
            "How to resolve conflicts:\n\n" +
            "1. Open the file in your editor\n" +
            "2. Look for the <<<<<<, ======= and >>>>>>> markers\n" +
            "3. Edit the content and remove the markers\n" +
            "4. Save the file\n" +
            "5. Click 'Mark as resolved'\n" +
            "6. When all are resolved, continue the merge"
        );
        helpLabel.getStyleClass().add("help-text");
        helpLabel.setWrapText(true);

        actionsBox.getChildren().addAll(
            actTitle, msgLabel, commitMsgArea,
            continueBtn, abortBtn, sep, helpLabel
        );

        split.getItems().addAll(listBox, actionsBox);
        split.setDividerPositions(0.55);

        root.getChildren().addAll(header, statusBar, split);
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void openInEditor() {
        String file = conflictList.getSelectionModel().getSelectedItem();
        if (file == null) { showError("Select a file first."); return; }

        try {
            File f = new File(git.getWorkDir(), file);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(f);
            } else {
                // Fallback: open with the system editor
                new ProcessBuilder("xdg-open", f.getAbsolutePath())
                    .start();
            }
        } catch (Exception ex) {
            showError("Could not open the editor:\n" + ex.getMessage());
        }
    }

    private void markResolved() {
        String file = conflictList.getSelectionModel().getSelectedItem();
        if (file == null) { showError("Select a file first."); return; }

        runAsync(
            () -> git.markResolved(file),
            () -> Platform.runLater(this::refresh),
            err -> showError("Error marking as resolved:\n" + err)
        );
    }

    private void continueMerge() {
        List<String> conflicts = git.getConflictedFiles();
        if (!conflicts.isEmpty()) {
            showError("There are still files with conflicts:\n" +
                String.join("\n", conflicts));
            return;
        }

        String msg = commitMsgArea.getText().trim();
        if (!confirm("Continue merge",
            "Complete the merge with a new commit?")) return;

        runAsync(
            () -> git.continueMerge(msg.isBlank() ? null : msg),
            () -> Platform.runLater(() -> {
                commitMsgArea.clear();
                refresh();
                window.refreshBranchLabel();
                showSuccess("Merge completed successfully!");
            }),
            err -> showError("Error continuing merge:\n" + err)
        );
    }

    private void abortMerge() {
        if (!confirm("Abort merge",
            "Abort the current merge? All resolution changes will be lost.")) return;

        runAsync(
            () -> git.abortMerge(),
            () -> Platform.runLater(() -> {
                refresh();
                showSuccess("Merge aborted. The repository returned to its previous state.");
            }),
            err -> showError("Error aborting merge:\n" + err)
        );
    }

    // ─────────────────────────────────────────────────────
    // LOAD
    // ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        if (!git.hasRepo()) return;

        List<String> conflicts = git.getConflictedFiles();
        conflictList.getItems().setAll(conflicts);

        if (conflicts.isEmpty()) {
            statusLabel.setText("✓  No active conflicts");
            statusLabel.getStyleClass().removeAll("status-conflict");
            statusLabel.getStyleClass().add("status-ok");
        } else {
            statusLabel.setText("⚡  " + conflicts.size() + " file(s) in conflict");
            statusLabel.getStyleClass().removeAll("status-ok");
            statusLabel.getStyleClass().add("status-conflict");
        }
    }

    @Override
    public Node getRoot() { return root; }

    // ─────────────────────────────────────────────────────
    // CELL
    // ─────────────────────────────────────────────────────

    private static class ConflictCell extends ListCell<String> {
        @Override
        protected void updateItem(String file, boolean empty) {
            super.updateItem(file, empty);
            if (empty || file == null) { setText(null); setGraphic(null); return; }

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label("⚡");
            icon.getStyleClass().add("conflict-icon");

            Label name = new Label(file);
            name.getStyleClass().add("conflict-file");

            row.getChildren().addAll(icon, name);
            setGraphic(row);
            setText(null);
        }
    }
}
