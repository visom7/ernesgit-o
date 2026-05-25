package com.ernesgito.ui.panels;

import com.ernesgito.model.BranchInfo;
import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Branch management panel: create, delete, checkout, merge, compare.
 */
public class BranchesPanel extends BasePanel {

    private VBox root;
    private ListView<BranchInfo> localList;
    private ListView<BranchInfo> remoteList;
    private Label currentBranchLabel;
    private TextField searchField;

    private List<BranchInfo> allBranches;

    public BranchesPanel(MainWindow window) {
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

        Label title = new Label("🌿  Branches");
        title.getStyleClass().add("panel-title");

        currentBranchLabel = new Label();
        currentBranchLabel.getStyleClass().add("badge-current");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBranchBtn = new Button("＋  New branch");
        newBranchBtn.getStyleClass().add("btn-primary");
        newBranchBtn.setOnAction(e -> createBranch());

        header.getChildren().addAll(title, currentBranchLabel, spacer, newBranchBtn);

        // ── Search bar ──
        searchField = new TextField();
        searchField.setPromptText("🔍  Filter branches...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> filterBranches(n));
        HBox searchBar = new HBox(searchField);
        searchBar.setPadding(new Insets(8, 20, 4, 20));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // ── Local / remote lists ──
        SplitPane split = new SplitPane();
        split.getStyleClass().add("branches-split");
        VBox.setVgrow(split, Priority.ALWAYS);

        // Local
        VBox localBox = new VBox(6);
        localBox.setPadding(new Insets(12, 12, 12, 12));
        Label localLabel = new Label("LOCAL");
        localLabel.getStyleClass().add("list-section-label");
        localList = new ListView<>();
        localList.getStyleClass().add("branch-list");
        localList.setCellFactory(lv -> new BranchCell());
        VBox.setVgrow(localList, Priority.ALWAYS);
        localBox.getChildren().addAll(localLabel, localList);

        // Remote
        VBox remoteBox = new VBox(6);
        remoteBox.setPadding(new Insets(12, 12, 12, 12));
        Label remoteLabel = new Label("REMOTE");
        remoteLabel.getStyleClass().add("list-section-label");
        remoteList = new ListView<>();
        remoteList.getStyleClass().add("branch-list");
        remoteList.setCellFactory(lv -> new BranchCell());
        VBox.setVgrow(remoteList, Priority.ALWAYS);
        remoteBox.getChildren().addAll(remoteLabel, remoteList);

        split.getItems().addAll(localBox, remoteBox);

        // ── Actions panel (appears on selection) ──
        HBox actions = buildActionsBar();

        root.getChildren().addAll(header, searchBar, split, actions);

        // Selection listeners
        localList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) remoteList.getSelectionModel().clearSelection();
        });
        remoteList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) localList.getSelectionModel().clearSelection();
        });
    }

    private HBox buildActionsBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("actions-bar");
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);

        Button checkoutBtn = new Button("⎇  Checkout");
        checkoutBtn.getStyleClass().add("btn-action");
        checkoutBtn.setOnAction(e -> checkoutSelected());

        Button mergeBtn = new Button("⤸  Merge into current");
        mergeBtn.getStyleClass().add("btn-action");
        mergeBtn.setOnAction(e -> mergeSelected());

        Button newFromBtn = new Button("⎇  New from here");
        newFromBtn.getStyleClass().add("btn-secondary");
        newFromBtn.setOnAction(e -> newBranchFromSelected());

        Button renameBtn = new Button("✏  Rename");
        renameBtn.getStyleClass().add("btn-secondary");
        renameBtn.setOnAction(e -> renameSelected());

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteSelected());

        bar.getChildren().addAll(checkoutBtn, mergeBtn, newFromBtn, renameBtn, deleteBtn);
        return bar;
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void createBranch() {
        String name = askInput("New branch", "Name of the new branch:", "feature/");
        if (name == null || name.isBlank()) return;

        boolean checkout = confirm("Auto checkout",
            "Check out the new branch '" + name + "'?");

        runAsync(
            () -> git.createBranch(name, checkout),
            () -> Platform.runLater(() -> { refresh(); window.refreshBranchLabel(); }),
            msg -> showError("Error creating branch:\n" + msg)
        );
    }

    private void checkoutSelected() {
        BranchInfo branch = getSelected();
        if (branch == null) return;
        if (branch.isCurrent()) { showError("You are already on this branch."); return; }

        runAsync(
            () -> git.checkoutBranch(branch.getName()),
            () -> Platform.runLater(() -> { refresh(); window.refreshBranchLabel(); }),
            msg -> showError("Error during checkout:\n" + msg)
        );
    }

    private void mergeSelected() {
        BranchInfo branch = getSelected();
        if (branch == null) return;

        String current = git.getCurrentBranch();
        if (!confirm("Merge", "Merge '" + branch.getName() + "' into '" + current + "'?")) return;

        runAsync(
            () -> git.mergeBranch(branch.getName()),
            () -> showSuccess("Merge completed successfully."),
            msg -> showError("Error during merge:\n" + msg)
        );
    }

    private void newBranchFromSelected() {
        BranchInfo branch = getSelected();
        if (branch == null) return;

        String name = askInput("New branch from " + branch.getName(),
            "Name of the new branch:", "feature/");
        if (name == null || name.isBlank()) return;

        runAsync(
            () -> git.createBranchFrom(name, branch.getName(), true),
            () -> Platform.runLater(() -> { refresh(); window.refreshBranchLabel(); }),
            msg -> showError("Error creating branch:\n" + msg)
        );
    }

    private void renameSelected() {
        BranchInfo branch = getSelected();
        if (branch == null || branch.isRemote()) return;

        String newName = askInput("Rename branch", "New name:", branch.getName());
        if (newName == null || newName.isBlank() || newName.equals(branch.getName())) return;

        runAsync(
            () -> git.renameBranch(branch.getName(), newName),
            () -> Platform.runLater(this::refresh),
            msg -> showError("Error renaming branch:\n" + msg)
        );
    }

    private void deleteSelected() {
        BranchInfo branch = getSelected();
        if (branch == null || branch.isRemote()) return;
        if (branch.isCurrent()) { showError("You cannot delete the current branch."); return; }

        if (!confirm("Delete branch",
            "Delete branch '" + branch.getName() + "'?\nThis action cannot be undone.")) return;

        boolean force = confirm("Force delete?",
            "Use -D (force delete) even if it has unmerged commits?");

        runAsync(
            () -> git.deleteBranch(branch.getName(), force),
            () -> Platform.runLater(this::refresh),
            msg -> showError("Error deleting branch:\n" + msg)
        );
    }

    private BranchInfo getSelected() {
        BranchInfo local  = localList.getSelectionModel().getSelectedItem();
        BranchInfo remote = remoteList.getSelectionModel().getSelectedItem();
        return local != null ? local : remote;
    }

    // ─────────────────────────────────────────────────────
    // FILTER AND LOAD
    // ─────────────────────────────────────────────────────

    private void filterBranches(String query) {
        if (allBranches == null) return;
        String q = query.toLowerCase().trim();
        List<BranchInfo> local  = allBranches.stream()
            .filter(b -> !b.isRemote() && (q.isBlank() || b.getName().toLowerCase().contains(q)))
            .collect(Collectors.toList());
        List<BranchInfo> remote = allBranches.stream()
            .filter(b -> b.isRemote() && (q.isBlank() || b.getName().toLowerCase().contains(q)))
            .collect(Collectors.toList());
        localList.getItems().setAll(local);
        remoteList.getItems().setAll(remote);
    }

    @Override
    public void refresh() {
        if (!git.hasRepo()) return;
        String current = git.getCurrentBranch();
        currentBranchLabel.setText("current branch: " + current);

        allBranches = git.getBranches();
        filterBranches(searchField.getText());
    }

    @Override
    public Node getRoot() { return root; }

    // ─────────────────────────────────────────────────────
    // CUSTOM CELL
    // ─────────────────────────────────────────────────────

    private static class BranchCell extends ListCell<BranchInfo> {
        @Override
        protected void updateItem(BranchInfo branch, boolean empty) {
            super.updateItem(branch, empty);
            if (empty || branch == null) {
                setText(null); setGraphic(null);
                getStyleClass().removeAll("branch-cell-current");
                return;
            }
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            String icon = branch.isCurrent() ? "✦" : (branch.isRemote() ? "◎" : "○");
            Label iconLbl = new Label(icon);
            iconLbl.getStyleClass().add(branch.isCurrent() ? "branch-icon-current" : "branch-icon");

            Label nameLbl = new Label(branch.getName());
            nameLbl.getStyleClass().add(branch.isCurrent() ? "branch-name-current" : "branch-name");

            row.getChildren().addAll(iconLbl, nameLbl);

            if (branch.isCurrent()) {
                Label badge = new Label("CURRENT");
                badge.getStyleClass().add("badge-current-small");
                row.getChildren().add(badge);
            }

            if (!branch.getTracking().isBlank()) {
                Label tracking = new Label("→ " + branch.getTracking());
                tracking.getStyleClass().add("branch-tracking");
                row.getChildren().add(tracking);
            }

            setText(null);
            setGraphic(row);
        }
    }
}
