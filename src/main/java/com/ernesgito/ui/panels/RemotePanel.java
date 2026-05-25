package com.ernesgito.ui.panels;

import com.ernesgito.ui.MainWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Remote management panel.
 * Operations: view/add/remove remotes, push (force / set-upstream),
 * pull, fetch, and full sync with upstream.
 */
public class RemotePanel extends BasePanel {

    private VBox root;
    private ListView<String> remoteList;
    private ComboBox<String> pushRemote, pushBranch;
    private ComboBox<String> pullRemote, pullBranch;
    private CheckBox forceCheck, setUpstreamCheck;
    private ComboBox<String> upstreamRemote, upstreamBranch, syncPushRemote;
    private TextArea outputArea;

    public RemotePanel(MainWindow window) {
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
        Label title = new Label("🔗  Remotes");
        title.getStyleClass().add("panel-title");
        header.getChildren().add(title);

        // ── Scrollable content ──
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-dark");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(0);
        content.getChildren().addAll(
            buildRemotesSection(),
            buildPushSection(),
            buildPullFetchSection(),
            buildSyncSection(),
            buildOutputSection()
        );

        scroll.setContent(content);
        root.getChildren().addAll(header, scroll);
    }

    // ─────────────────────────────────────────────────────
    // SECTIONS
    // ─────────────────────────────────────────────────────

    private VBox buildRemotesSection() {
        VBox box = sectionBox("CONFIGURED REMOTES");

        remoteList = new ListView<>();
        remoteList.getStyleClass().add("remote-list");
        remoteList.setPrefHeight(120);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("＋  Add remote");
        addBtn.getStyleClass().add("btn-secondary");
        addBtn.setOnAction(e -> addRemote());

        Button removeBtn = new Button("🗑  Remove selected");
        removeBtn.getStyleClass().add("btn-danger");
        removeBtn.setOnAction(e -> removeRemote());

        Button fetchAllBtn = new Button("⬇  Fetch All");
        fetchAllBtn.getStyleClass().add("btn-action");
        fetchAllBtn.setOnAction(e -> fetchAll());

        btnRow.getChildren().addAll(addBtn, removeBtn, fetchAllBtn);
        box.getChildren().addAll(remoteList, btnRow);
        return box;
    }

    private VBox buildPushSection() {
        VBox box = sectionBox("PUSH");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(4, 0, 8, 0));

        pushRemote = new ComboBox<>();
        pushRemote.getStyleClass().add("combo-dark");
        pushRemote.setPrefWidth(160);

        pushBranch = new ComboBox<>();
        pushBranch.getStyleClass().add("combo-dark");
        pushBranch.setPrefWidth(180);
        pushBranch.setEditable(true);

        forceCheck       = new CheckBox("--force-with-lease  (recommended)");
        CheckBox forceTrueCheck = new CheckBox("--force  (no verification)");
        setUpstreamCheck = new CheckBox("-u  (set upstream tracking)");

        forceCheck.getStyleClass().add("check-dark");
        forceTrueCheck.getStyleClass().add("check-dark");
        setUpstreamCheck.getStyleClass().add("check-dark");

        // Mutually exclusive
        forceCheck.selectedProperty().addListener((obs, o, n) -> { if (n) forceTrueCheck.setSelected(false); });
        forceTrueCheck.selectedProperty().addListener((obs, o, n) -> { if (n) forceCheck.setSelected(false); });

        grid.add(label("Remote:"),   0, 0); grid.add(pushRemote,      1, 0);
        grid.add(label("Branch:"),   0, 1); grid.add(pushBranch,      1, 1);
        grid.add(forceCheck,         0, 2, 2, 1);
        grid.add(forceTrueCheck,     0, 3, 2, 1);
        grid.add(setUpstreamCheck,   0, 4, 2, 1);

        HBox btns = new HBox(10);
        Button pushBtn = new Button("⬆  Push");
        pushBtn.getStyleClass().add("btn-primary");
        pushBtn.setOnAction(e -> doPush(forceTrueCheck.isSelected()));

        btns.getChildren().add(pushBtn);

        box.getChildren().addAll(grid, btns);
        return box;
    }

    private VBox buildPullFetchSection() {
        VBox box = sectionBox("PULL / FETCH");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(4, 0, 8, 0));

        pullRemote = new ComboBox<>();
        pullRemote.getStyleClass().add("combo-dark");
        pullRemote.setPrefWidth(160);

        pullBranch = new ComboBox<>();
        pullBranch.getStyleClass().add("combo-dark");
        pullBranch.setPrefWidth(180);
        pullBranch.setEditable(true);

        CheckBox rebaseCheck = new CheckBox("--rebase");
        rebaseCheck.getStyleClass().add("check-dark");

        grid.add(label("Remote:"), 0, 0); grid.add(pullRemote,  1, 0);
        grid.add(label("Branch:"), 0, 1); grid.add(pullBranch,  1, 1);
        grid.add(rebaseCheck,      0, 2, 2, 1);

        HBox btns = new HBox(10);
        Button pullBtn = new Button("⬇  Pull");
        pullBtn.getStyleClass().add("btn-primary");
        pullBtn.setOnAction(e -> doPull(rebaseCheck.isSelected()));

        Button fetchBtn = new Button("⬇  Fetch (download only)");
        fetchBtn.getStyleClass().add("btn-secondary");
        fetchBtn.setOnAction(e -> doFetch());

        btns.getChildren().addAll(pullBtn, fetchBtn);
        box.getChildren().addAll(grid, btns);
        return box;
    }

    private VBox buildSyncSection() {
        VBox box = sectionBox("SYNC UPSTREAM → ORIGIN");

        Label desc = new Label(
            "Fetches from upstream, merges into the current branch and pushes to origin.\n" +
            "Ideal for keeping a fork in sync with the original repository."
        );
        desc.getStyleClass().add("label-muted");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 8, 0));

        upstreamRemote = new ComboBox<>();
        upstreamRemote.getStyleClass().add("combo-dark");
        upstreamRemote.setPrefWidth(160);
        upstreamRemote.setEditable(true);

        upstreamBranch = new ComboBox<>();
        upstreamBranch.getStyleClass().add("combo-dark");
        upstreamBranch.setPrefWidth(180);
        upstreamBranch.setEditable(true);

        syncPushRemote = new ComboBox<>();
        syncPushRemote.getStyleClass().add("combo-dark");
        syncPushRemote.setPrefWidth(160);

        grid.add(label("Upstream remote:"), 0, 0); grid.add(upstreamRemote, 1, 0);
        grid.add(label("Upstream branch:"), 0, 1); grid.add(upstreamBranch, 1, 1);
        grid.add(label("Push to remote:"),  0, 2); grid.add(syncPushRemote, 1, 2);

        Button syncBtn = new Button("🔄  Sync Upstream");
        syncBtn.getStyleClass().add("btn-primary");
        syncBtn.setOnAction(e -> doSync());

        box.getChildren().addAll(desc, grid, syncBtn);
        return box;
    }

    private VBox buildOutputSection() {
        VBox box = sectionBox("OUTPUT");
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.getStyleClass().add("diff-area");
        outputArea.setPromptText("Operation results will appear here...");
        outputArea.setPrefHeight(140);
        box.getChildren().add(outputArea);
        return box;
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void addRemote() {
        String name = askInput("Add remote", "Remote name (e.g. upstream):", "upstream");
        if (name == null || name.isBlank()) return;
        String url = askInput("Add remote", "Remote URL:", "https://github.com/");
        if (url == null || url.isBlank()) return;

        runAsync(
            () -> git.addRemote(name, url),
            () -> Platform.runLater(this::refresh),
            err -> showError("Error adding remote:\n" + err)
        );
    }

    private void removeRemote() {
        String selected = remoteList.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select a remote first."); return; }

        String name = selected.split("\\s")[0];
        if (!confirm("Remove remote", "Remove remote '" + name + "'?")) return;

        runAsync(
            () -> git.removeRemote(name),
            () -> Platform.runLater(this::refresh),
            err -> showError("Error removing remote:\n" + err)
        );
    }

    private void fetchAll() {
        log("Running: git fetch --all ...");
        runAsync(
            () -> git.fetch(null),
            () -> log("✓ Fetch completed."),
            err -> log("✗ Error: " + err)
        );
    }

    private void doPush(boolean forceFull) {
        String remote = pushRemote.getValue();
        String branch = pushBranch.getValue();
        if (remote == null || branch == null) { showError("Select a remote and branch."); return; }

        boolean force = forceFull || forceCheck.isSelected();
        boolean upstream = setUpstreamCheck.isSelected();

        String opts = (upstream ? "-u " : "") + (forceFull ? "--force" : force ? "--force-with-lease" : "");
        log("Running: git push " + opts + " " + remote + " " + branch + " ...");

        if (forceFull && !confirm("⚠ Force push",
            "Are you sure? --force will overwrite the remote history on " + remote + "/" + branch)) return;

        runAsync(
            () -> forceFull ? git.pushForce(remote, branch)
                            : git.push(remote, branch, force, upstream),
            () -> log("✓ Push completed to " + remote + "/" + branch),
            err -> log("✗ Error on push:\n" + err)
        );
    }

    private void doPull(boolean rebase) {
        String remote = pullRemote.getValue();
        String branch = pullBranch.getValue();
        if (remote == null || branch == null) { showError("Select a remote and branch."); return; }

        log("Running: git pull" + (rebase ? " --rebase" : "") + " " + remote + " " + branch + " ...");
        runAsync(
            () -> rebase ? git.pullRebase(remote, branch) : git.pull(remote, branch),
            () -> log("✓ Pull completed from " + remote + "/" + branch),
            err -> log("✗ Error on pull:\n" + err)
        );
    }

    private void doFetch() {
        String remote = pullRemote.getValue();
        log("Running: git fetch " + (remote != null ? remote : "--all") + " ...");
        runAsync(
            () -> git.fetch(remote),
            () -> log("✓ Fetch completed."),
            err -> log("✗ Error on fetch:\n" + err)
        );
    }

    private void doSync() {
        String uRemote = upstreamRemote.getValue();
        String uBranch = upstreamBranch.getValue();
        String pRemote = syncPushRemote.getValue();

        if (uRemote == null || uBranch == null || pRemote == null) {
            showError("Configure upstream remote, branch and push remote."); return;
        }

        log("Sync: fetch " + uRemote + " → merge " + uRemote + "/" + uBranch + " → push " + pRemote + " ...");
        runAsync(
            () -> git.syncUpstream(uRemote, uBranch, pRemote),
            () -> log("✓ Sync completed successfully."),
            err -> log("✗ Error on sync:\n" + err)
        );
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            outputArea.appendText(msg + "\n");
        });
    }

    // ─────────────────────────────────────────────────────
    // LOAD
    // ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        if (!git.hasRepo()) return;

        List<String[]> remotes = git.getRemotes();
        List<String> remoteNames = git.getRemoteNames();
        List<String> branches = git.getBranchNames();
        String currentBranch = git.getCurrentBranch();

        // Populate visual remote list
        remoteList.getItems().setAll(
            remotes.stream()
                .map(r -> r[0] + "   fetch: " + r[1])
                .toList()
        );

        // Update combos
        updateCombo(pushRemote,    remoteNames, "origin");
        updateCombo(pullRemote,    remoteNames, "origin");
        updateCombo(syncPushRemote, remoteNames, "origin");
        updateCombo(upstreamRemote, remoteNames, "upstream");

        List<String> localBranches = branches.stream()
            .filter(b -> !b.startsWith("origin/") && !b.startsWith("upstream/"))
            .toList();
        updateCombo(pushBranch,    localBranches, currentBranch);
        updateCombo(pullBranch,    localBranches, currentBranch);
        updateCombo(upstreamBranch, List.of("main", "master", "dev"), "main");
    }

    private void updateCombo(ComboBox<String> combo, List<String> items, String preferred) {
        String current = combo.getValue();
        combo.getItems().setAll(items);
        if (current != null && items.contains(current)) combo.setValue(current);
        else if (items.contains(preferred)) combo.setValue(preferred);
        else if (!items.isEmpty()) combo.setValue(items.get(0));
    }

    @Override
    public Node getRoot() { return root; }

    // ─────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────

    private VBox sectionBox(String labelText) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 20, 14, 20));
        box.getStyleClass().add("section-box");

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("list-section-label");
        box.getChildren().add(lbl);

        Separator sep = new Separator();
        sep.getStyleClass().add("section-sep");
        box.getChildren().add(sep);

        return box;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("label-muted");
        return l;
    }
}
