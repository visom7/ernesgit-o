package com.ernesgito.ui.panels;

import com.ernesgito.model.StashEntry;
import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Stash management panel.
 * Operations: save, apply, pop, drop, show, stash → branch.
 */
public class StashPanel extends BasePanel {

    private VBox root;
    private ListView<StashEntry> stashList;
    private TextArea diffArea;
    private TextField messageField;
    private CheckBox untrackedCheck;

    public StashPanel(MainWindow window) {
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
        Label title = new Label("📦  Stash");
        title.getStyleClass().add("panel-title");
        header.getChildren().add(title);

        // ── Save stash ──
        VBox saveBox = new VBox(8);
        saveBox.getStyleClass().add("save-stash-box");
        saveBox.setPadding(new Insets(12, 20, 12, 20));

        Label saveLabel = new Label("SAVE CHANGES TO STASH");
        saveLabel.getStyleClass().add("list-section-label");

        HBox saveRow = new HBox(10);
        saveRow.setAlignment(Pos.CENTER_LEFT);

        messageField = new TextField();
        messageField.setPromptText("Stash description (optional)...");
        messageField.getStyleClass().add("search-field");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        untrackedCheck = new CheckBox("Include untracked (-u)");
        untrackedCheck.getStyleClass().add("check-dark");

        Button saveBtn = new Button("💾  Save Stash");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> saveStash());

        saveRow.getChildren().addAll(messageField, untrackedCheck, saveBtn);
        saveBox.getChildren().addAll(saveLabel, saveRow);

        // ── SplitPane: list left | diff right ──
        SplitPane split = new SplitPane();
        VBox.setVgrow(split, Priority.ALWAYS);

        // Stash list
        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(12));
        Label listLabel = new Label("SAVED STASHES");
        listLabel.getStyleClass().add("list-section-label");

        stashList = new ListView<>();
        stashList.getStyleClass().add("stash-list");
        stashList.setCellFactory(lv -> new StashCell());
        VBox.setVgrow(stashList, Priority.ALWAYS);
        stashList.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> { if (n != null) showStashDiff(n); });

        listBox.getChildren().addAll(listLabel, stashList);

        // Actions
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(12));

        Label actLabel = new Label("ACTIONS ON SELECTED");
        actLabel.getStyleClass().add("list-section-label");

        Button applyBtn = new Button("▶  Apply (keeps stash)");
        applyBtn.getStyleClass().add("btn-action");
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setOnAction(e -> applySelected(false));

        Button popBtn = new Button("▶✕  Pop (apply and delete)");
        popBtn.getStyleClass().add("btn-primary");
        popBtn.setMaxWidth(Double.MAX_VALUE);
        popBtn.setOnAction(e -> applySelected(true));

        Button dropBtn = new Button("🗑  Drop (delete)");
        dropBtn.getStyleClass().add("btn-danger");
        dropBtn.setMaxWidth(Double.MAX_VALUE);
        dropBtn.setOnAction(e -> dropSelected());

        Button branchBtn = new Button("🌿  Create branch from stash");
        branchBtn.getStyleClass().add("btn-secondary");
        branchBtn.setMaxWidth(Double.MAX_VALUE);
        branchBtn.setOnAction(e -> branchFromStash());

        Label diffLabel = new Label("PREVIEW");
        diffLabel.getStyleClass().add("list-section-label");
        diffLabel.setPadding(new Insets(12, 0, 4, 0));

        diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.getStyleClass().add("diff-area");
        diffArea.setWrapText(false);
        diffArea.setPromptText("Select a stash to view changes...");
        VBox.setVgrow(diffArea, Priority.ALWAYS);

        rightBox.getChildren().addAll(
            actLabel, applyBtn, popBtn, dropBtn, branchBtn, diffLabel, diffArea
        );
        VBox.setVgrow(rightBox, Priority.ALWAYS);

        split.getItems().addAll(listBox, rightBox);
        split.setDividerPositions(0.4);

        root.getChildren().addAll(header, saveBox, split);
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void saveStash() {
        String msg = messageField.getText();
        boolean untracked = untrackedCheck.isSelected();

        runAsync(
            () -> untracked ? git.stashSaveIncludeUntracked(msg) : git.stashSave(msg),
            () -> Platform.runLater(() -> {
                messageField.clear();
                refresh();
            }),
            err -> showError("Error saving stash:\n" + err)
        );
    }

    private void applySelected(boolean pop) {
        StashEntry entry = stashList.getSelectionModel().getSelectedItem();
        if (entry == null) { showError("Select a stash first."); return; }

        String action = pop ? "pop (apply and delete)" : "apply (apply and keep)";
        if (!confirm("Stash " + action, action + " the stash?\n" + entry.getMessage())) return;

        runAsync(
            () -> pop ? git.stashPop(entry.getIndex()) : git.stashApply(entry.getIndex()),
            () -> Platform.runLater(this::refresh),
            err -> showError("Error applying stash:\n" + err)
        );
    }

    private void dropSelected() {
        StashEntry entry = stashList.getSelectionModel().getSelectedItem();
        if (entry == null) { showError("Select a stash first."); return; }

        if (!confirm("Delete stash",
            "Permanently delete the stash?\n" + entry.getMessage())) return;

        runAsync(
            () -> git.stashDrop(entry.getIndex()),
            () -> Platform.runLater(this::refresh),
            err -> showError("Error deleting stash:\n" + err)
        );
    }

    private void branchFromStash() {
        StashEntry entry = stashList.getSelectionModel().getSelectedItem();
        if (entry == null) { showError("Select a stash first."); return; }

        String name = askInput("New branch from stash", "Name of the new branch:", "feature/");
        if (name == null || name.isBlank()) return;

        runAsync(
            () -> git.stashBranch(name, entry.getIndex()),
            () -> Platform.runLater(() -> {
                refresh();
                window.refreshBranchLabel();
            }),
            err -> showError("Error creating branch from stash:\n" + err)
        );
    }

    private void showStashDiff(StashEntry entry) {
        diffArea.setText("Loading...");
        new Thread(() -> {
            String diff = git.stashShow(entry.getIndex()).getOutputAsString();
            Platform.runLater(() -> diffArea.setText(diff));
        }).start();
    }

    // ─────────────────────────────────────────────────────
    // LOAD
    // ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        if (!git.hasRepo()) return;
        List<StashEntry> entries = git.getStashList();
        stashList.getItems().setAll(entries);
        diffArea.clear();
    }

    @Override
    public Node getRoot() { return root; }

    // ─────────────────────────────────────────────────────
    // CELL
    // ─────────────────────────────────────────────────────

    private static class StashCell extends ListCell<StashEntry> {
        @Override
        protected void updateItem(StashEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) { setText(null); setGraphic(null); return; }

            VBox box = new VBox(2);
            Label ref = new Label(entry.getRef());
            ref.getStyleClass().add("stash-ref");

            // Strip "On branch:" prefix from the message
            String msg = entry.getMessage()
                .replaceFirst("^WIP on [^:]+: \\w+ ", "")
                .replaceFirst("^On [^:]+: ", "");
            Label msgLbl = new Label(msg);
            msgLbl.getStyleClass().add("stash-msg");

            box.getChildren().addAll(ref, msgLbl);
            setGraphic(box);
            setText(null);
        }
    }
}
