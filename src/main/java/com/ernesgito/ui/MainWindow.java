package com.ernesgito.ui;

import com.ernesgito.service.GitService;
import com.ernesgito.ui.panels.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Main window of ErnesGit-O.
 * Structure: Top toolbar | Left sidebar | Center content
 */
public class MainWindow {

    private final Stage stage;
    private final GitService git = GitService.getInstance();

    private BorderPane root;
    private Label repoLabel;
    private Label branchLabel;
    private StackPane contentArea;

    // Panels
    private BranchesPanel  branchesPanel;
    private HistoryPanel   historyPanel;
    private StashPanel     stashPanel;
    private RemotePanel    remotePanel;
    private ConflictsPanel conflictsPanel;

    private ToggleGroup navGroup;
    private ToggleButton activeNav;
    private BasePanel activePanel;

    public MainWindow(Stage stage) {
        this.stage = stage;
        buildUI();
    }

    // ─────────────────────────────────────────────────────
    // UI CONSTRUCTION
    // ─────────────────────────────────────────────────────

    private void buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-root");

        root.setTop(buildToolbar());
        root.setLeft(buildSidebar());

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        root.setCenter(contentArea);

        // Initialize panels (data not loaded yet)
        branchesPanel  = new BranchesPanel(this);
        historyPanel   = new HistoryPanel(this);
        stashPanel     = new StashPanel(this);
        remotePanel    = new RemotePanel(this);
        conflictsPanel = new ConflictsPanel(this);

        showNoRepo();
    }

    // ── TOOLBAR ──────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));

        // Logo/title
        Label logo = new Label("⑂  ErnesGit-O");
        logo.getStyleClass().add("toolbar-logo");

        // Open repo button
        Button openBtn = new Button("📂  Open repo");
        openBtn.getStyleClass().add("btn-primary");
        openBtn.setOnAction(e -> openRepo());

        // Current repo
        repoLabel = new Label("No repository");
        repoLabel.getStyleClass().add("toolbar-repo");

        // Current branch
        branchLabel = new Label();
        branchLabel.getStyleClass().add("toolbar-branch");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Refresh button
        Button refreshBtn = new Button("↺  Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshCurrentPanel());
        refreshBtn.setDisable(true);
        refreshBtn.setId("refreshBtn");

        bar.getChildren().addAll(logo, openBtn, repoLabel, branchLabel, spacer, refreshBtn);
        return bar;
    }

    // ── SIDEBAR ──────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16, 8, 16, 8));
        sidebar.setPrefWidth(180);

        navGroup = new ToggleGroup();

        ToggleButton btnBranches  = navBtn("🌿", "Branches",    "nav-ramas");
        ToggleButton btnHistory   = navBtn("📜", "History",     "nav-historial");
        ToggleButton btnStash     = navBtn("📦", "Stash",       "nav-stash");
        ToggleButton btnRemotes   = navBtn("🔗", "Remotes",     "nav-remotos");
        ToggleButton btnConflicts = navBtn("⚡", "Conflicts",   "nav-conflictos");

        btnBranches.setOnAction(e  -> showPanel(branchesPanel,  btnBranches));
        btnHistory.setOnAction(e   -> showPanel(historyPanel,   btnHistory));
        btnStash.setOnAction(e     -> showPanel(stashPanel,     btnStash));
        btnRemotes.setOnAction(e   -> showPanel(remotePanel,    btnRemotes));
        btnConflicts.setOnAction(e -> showPanel(conflictsPanel, btnConflicts));

        // Visual separator
        Separator sep = new Separator();
        sep.getStyleClass().add("sidebar-sep");

        sidebar.getChildren().addAll(
            btnBranches, btnHistory, btnStash, btnRemotes, sep, btnConflicts
        );
        return sidebar;
    }

    private ToggleButton navBtn(String icon, String text, String styleClass) {
        ToggleButton btn = new ToggleButton(icon + "  " + text);
        btn.setToggleGroup(navGroup);
        btn.getStyleClass().addAll("nav-btn", styleClass);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setDisable(true);
        btn.setId(styleClass);
        return btn;
    }

    // ─────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────

    private void showPanel(BasePanel panel, ToggleButton navBtn) {
        if (!git.hasRepo()) return;
        activeNav = navBtn;
        activePanel = panel;
        panel.refresh();
        contentArea.getChildren().setAll(panel.getRoot());
    }

    private void showNoRepo() {
        VBox placeholder = new VBox(20);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getStyleClass().add("no-repo");

        Label icon = new Label("⑂");
        icon.getStyleClass().add("no-repo-icon");

        Label msg  = new Label("Open a git repository to get started");
        msg.getStyleClass().add("no-repo-msg");

        Button openBtn = new Button("📂  Open repository");
        openBtn.getStyleClass().add("btn-primary");
        openBtn.setOnAction(e -> openRepo());

        placeholder.getChildren().addAll(icon, msg, openBtn);
        contentArea.getChildren().setAll(placeholder);
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────

    private void openRepo() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Git repository");
        File dir = chooser.showDialog(stage);
        if (dir == null) return;

        if (!git.isValidRepo(dir.getAbsolutePath())) {
            showAlert(Alert.AlertType.ERROR,
                "Not a Git repository",
                "The selected directory does not contain a valid git repository.");
            return;
        }

        git.setWorkDir(dir.getAbsolutePath());
        updateToolbarInfo();
        enableNavButtons();

        // Show branches panel by default
        ToggleButton btnBranches = (ToggleButton) root.getLeft().lookup("#nav-ramas");
        if (btnBranches != null) {
            btnBranches.setSelected(true);
            showPanel(branchesPanel, btnBranches);
        }
    }

    private void updateToolbarInfo() {
        String path = git.getWorkDir();
        String name = git.getRepoName();
        repoLabel.setText("📁 " + name + "  (" + path + ")");
        branchLabel.setText("🌿 " + git.getCurrentBranch());
    }

    public void refreshBranchLabel() {
        if (git.hasRepo()) branchLabel.setText("🌿 " + git.getCurrentBranch());
    }

    private void enableNavButtons() {
        root.getLeft().lookupAll(".nav-btn")
            .forEach(n -> n.setDisable(false));
        root.getTop().lookup("#refreshBtn").setDisable(false);
    }

    private void refreshCurrentPanel() {
        if (activePanel == null || !git.hasRepo()) return;
        activePanel.refresh();
    }

    // ─────────────────────────────────────────────────────
    // SHARED UTILITIES
    // ─────────────────────────────────────────────────────

    public static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        return alert.showAndWait()
            .filter(b -> b == ButtonType.OK)
            .isPresent();
    }

    public static TextInputDialog inputDialog(String title, String label, String defaultVal) {
        TextInputDialog dialog = new TextInputDialog(defaultVal);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(label);
        return dialog;
    }

    public BorderPane getRoot() { return root; }
}
