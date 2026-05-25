package com.ernesgito.ui.panels;

import com.ernesgito.model.CommitInfo;
import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Commit history panel.
 * Operations: cherry-pick, drop commit (soft/mixed/hard), edit message, revert.
 */
public class HistoryPanel extends BasePanel {

    private VBox root;
    private TableView<CommitInfo> table;
    private TextArea diffArea;
    private Label statsLabel;
    private ComboBox<String> branchSelector;
    private ComboBox<Integer> limitSelector;

    public HistoryPanel(MainWindow window) {
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

        Label title = new Label("📜  Commit history");
        title.getStyleClass().add("panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label branchLbl = new Label("Branch:");
        branchLbl.getStyleClass().add("label-muted");
        branchSelector = new ComboBox<>();
        branchSelector.getStyleClass().add("combo-dark");
        branchSelector.setPrefWidth(180);
        branchSelector.setOnAction(e -> loadHistory());

        Label limitLbl = new Label("Limit:");
        limitLbl.getStyleClass().add("label-muted");
        limitSelector = new ComboBox<>();
        limitSelector.getItems().addAll(50, 100, 200, 500);
        limitSelector.setValue(100);
        limitSelector.getStyleClass().add("combo-dark");
        limitSelector.setOnAction(e -> loadHistory());

        header.getChildren().addAll(title, spacer, branchLbl, branchSelector,
                                    limitLbl, limitSelector);

        // ── Quick actions bar ──
        HBox quickActions = buildQuickActionsBar();

        // ── SplitPane: table on top, diff on bottom ──
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox.setVgrow(split, Priority.ALWAYS);

        table = buildTable();
        VBox tableBox = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Diff area
        VBox diffBox = new VBox(6);
        diffBox.setPadding(new Insets(8, 12, 8, 12));
        diffBox.getStyleClass().add("diff-container");

        HBox diffHeader = new HBox(8);
        diffHeader.setAlignment(Pos.CENTER_LEFT);
        Label diffTitle = new Label("Changes of the selected commit");
        diffTitle.getStyleClass().add("list-section-label");
        statsLabel = new Label();
        statsLabel.getStyleClass().add("label-muted");
        diffHeader.getChildren().addAll(diffTitle, statsLabel);

        diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.getStyleClass().add("diff-area");
        diffArea.setWrapText(false);
        VBox.setVgrow(diffArea, Priority.ALWAYS);

        diffBox.getChildren().addAll(diffHeader, diffArea);

        split.getItems().addAll(tableBox, diffBox);
        split.setDividerPositions(0.65);

        root.getChildren().addAll(header, quickActions, split);
    }

    @SuppressWarnings("unchecked")
    private TableView<CommitInfo> buildTable() {
        TableView<CommitInfo> tv = new TableView<>();
        tv.getStyleClass().add("commits-table");
        tv.setPlaceholder(new Label("No commits"));

        TableColumn<CommitInfo, String> hashCol = new TableColumn<>("Hash");
        hashCol.setCellValueFactory(new PropertyValueFactory<>("shortHash"));
        hashCol.setPrefWidth(70);
        hashCol.getStyleClass().add("col-hash");

        TableColumn<CommitInfo, String> refsCol = new TableColumn<>("Refs");
        refsCol.setCellValueFactory(new PropertyValueFactory<>("refs"));
        refsCol.setPrefWidth(160);
        refsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String refs, boolean empty) {
                super.updateItem(refs, empty);
                if (empty || refs == null || refs.isBlank()) {
                    setText(null); setGraphic(null); return;
                }
                HBox box = new HBox(4);
                for (String ref : refs.split(",")) {
                    ref = ref.trim();
                    if (ref.isBlank()) continue;
                    Label badge = new Label(ref);
                    badge.getStyleClass().add(ref.contains("HEAD") ? "ref-head"
                        : ref.contains("origin") ? "ref-remote" : "ref-local");
                    box.getChildren().add(badge);
                }
                setGraphic(box); setText(null);
            }
        });

        TableColumn<CommitInfo, String> subjectCol = new TableColumn<>("Message");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        subjectCol.setPrefWidth(380);

        TableColumn<CommitInfo, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(130);

        TableColumn<CommitInfo, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(110);

        tv.getColumns().addAll(hashCol, refsCol, subjectCol, authorCol, dateCol);

        // Selection → load diff
        tv.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadDiff(n);
        });

        // Context menu
        tv.setRowFactory(t -> {
            TableRow<CommitInfo> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) showContextMenu(row.getItem(), row, e.getScreenX(), e.getScreenY());
            });
            return row;
        });

        return tv;
    }

    private HBox buildQuickActionsBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("quick-actions-bar");
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Last commit:");
        lbl.getStyleClass().add("label-muted");

        Button softDrop = new Button("↩ Drop (soft)");
        softDrop.getStyleClass().add("btn-secondary");
        softDrop.setTooltip(new Tooltip("Removes the commit, keeps changes in staging"));
        softDrop.setOnAction(e -> dropLastCommit("soft"));

        Button mixedDrop = new Button("↩ Drop (mixed)");
        mixedDrop.getStyleClass().add("btn-secondary");
        mixedDrop.setTooltip(new Tooltip("Removes the commit, keeps changes unstaged"));
        mixedDrop.setOnAction(e -> dropLastCommit("mixed"));

        Button hardDrop = new Button("✕ Drop (hard)");
        hardDrop.getStyleClass().add("btn-danger");
        hardDrop.setTooltip(new Tooltip("Removes the commit AND the changes permanently"));
        hardDrop.setOnAction(e -> dropLastCommit("hard"));

        Button editMsg = new Button("✏ Edit message");
        editMsg.getStyleClass().add("btn-secondary");
        editMsg.setTooltip(new Tooltip("Edit the last commit message (amend)"));
        editMsg.setOnAction(e -> editLastMessage());

        bar.getChildren().addAll(lbl, softDrop, mixedDrop, hardDrop,
            new Separator(javafx.geometry.Orientation.VERTICAL), editMsg);
        return bar;
    }

    // ─────────────────────────────────────────────────────
    // CONTEXT MENU
    // ─────────────────────────────────────────────────────

    private void showContextMenu(CommitInfo commit, Node anchor,
                                 double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        MenuItem copyHash = new MenuItem("📋  Copy hash " + commit.getShortHash());
        copyHash.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(commit.getHash());
            cb.setContent(content);
        });

        MenuItem cherryPick = new MenuItem("🍒  Cherry-pick this commit");
        cherryPick.setOnAction(e -> cherryPick(commit));

        MenuItem revert = new MenuItem("↩  Revert (creates inverse commit)");
        revert.setOnAction(e -> revertCommit(commit));

        MenuItem newBranch = new MenuItem("🌿  New branch from here");
        newBranch.setOnAction(e -> branchFromCommit(commit));

        menu.getItems().addAll(copyHash, new SeparatorMenuItem(),
                               cherryPick, revert, new SeparatorMenuItem(), newBranch);
        menu.show(anchor, screenX, screenY);
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void dropLastCommit(String mode) {
        String modeDesc = switch (mode) {
            case "soft"  -> "soft — keeps changes in staging";
            case "mixed" -> "mixed — keeps changes unstaged";
            case "hard"  -> "hard — PERMANENTLY deletes the changes";
            default -> mode;
        };

        if (!confirm("Drop last commit",
            "Delete the last commit?\nMode: " + modeDesc)) return;

        runAsync(
            () -> switch (mode) {
                case "soft"  -> git.dropLastCommitSoft();
                case "mixed" -> git.dropLastCommitMixed();
                default      -> git.dropLastCommitHard();
            },
            () -> Platform.runLater(() -> { refresh(); window.refreshBranchLabel(); }),
            msg -> showError("Error deleting commit:\n" + msg)
        );
    }

    private void editLastMessage() {
        List<CommitInfo> commits = table.getItems();
        if (commits.isEmpty()) return;
        String current = commits.get(0).getSubject();

        String newMsg = askInput("Edit last commit message",
            "New message:", current);
        if (newMsg == null || newMsg.isBlank() || newMsg.equals(current)) return;

        runAsync(
            () -> git.editLastCommitMessage(newMsg),
            () -> Platform.runLater(this::refresh),
            msg -> showError("Error editing message:\n" + msg)
        );
    }

    private void cherryPick(CommitInfo commit) {
        if (!confirm("Cherry-pick",
            "Apply commit " + commit.getShortHash() + " onto the current branch?\n" +
            "\"" + commit.getSubject() + "\"")) return;

        runAsync(
            () -> git.cherryPick(commit.getHash()),
            () -> showSuccess("Cherry-pick applied: " + commit.getShortHash()),
            msg -> showError("Error during cherry-pick:\n" + msg)
        );
    }

    private void revertCommit(CommitInfo commit) {
        if (!confirm("Revert commit",
            "Create a commit that reverts " + commit.getShortHash() + "?\n" +
            "\"" + commit.getSubject() + "\"")) return;

        runAsync(
            () -> git.revertCommit(commit.getHash()),
            () -> Platform.runLater(this::refresh),
            msg -> showError("Error during revert:\n" + msg)
        );
    }

    private void branchFromCommit(CommitInfo commit) {
        String name = askInput("New branch from " + commit.getShortHash(),
            "Branch name:", "feature/");
        if (name == null || name.isBlank()) return;

        runAsync(
            () -> git.createBranchFrom(name, commit.getHash(), true),
            () -> Platform.runLater(() -> { refresh(); window.refreshBranchLabel(); }),
            msg -> showError("Error creating branch:\n" + msg)
        );
    }

    // ─────────────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────────────

    private void loadHistory() {
        if (!git.hasRepo()) return;
        String branch = branchSelector.getValue();
        int limit = limitSelector.getValue() != null ? limitSelector.getValue() : 100;

        List<CommitInfo> commits = (branch == null || branch.isBlank())
            ? git.getLog(limit)
            : git.getLogForBranch(branch, limit);

        table.getItems().setAll(commits);
        statsLabel.setText(commits.size() + " commits");
    }

    private void loadDiff(CommitInfo commit) {
        new Thread(() -> {
            String diff = git.getCommitDiff(commit.getHash()).getOutputAsString();
            Platform.runLater(() -> diffArea.setText(diff));
        }).start();
    }

    @Override
    public void refresh() {
        if (!git.hasRepo()) return;

        // Update branch selector
        List<String> branches = git.getBranchNames();
        String current = branchSelector.getValue();
        branchSelector.getItems().setAll(branches);
        branchSelector.setValue(current != null && branches.contains(current)
            ? current : git.getCurrentBranch());

        loadHistory();
    }

    @Override
    public Node getRoot() { return root; }
}
