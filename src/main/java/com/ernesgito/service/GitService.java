package com.ernesgito.service;

import com.ernesgito.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton service that encapsulates all git operations
 * by executing commands via ProcessBuilder.
 */
public class GitService {

    private static GitService instance;
    private String workDir;

    private GitService() {}

    public static GitService getInstance() {
        if (instance == null) instance = new GitService();
        return instance;
    }

    // ─────────────────────────────────────────────────────
    // REPOSITORY CONFIGURATION
    // ─────────────────────────────────────────────────────

    public void setWorkDir(String path) { this.workDir = path; }
    public String getWorkDir() { return workDir; }
    public boolean hasRepo() { return workDir != null && !workDir.isBlank(); }

    public boolean isValidRepo(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir");
            pb.directory(new File(path));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(OutputStream.nullOutputStream());
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────
    // EXECUTION ENGINE
    // ─────────────────────────────────────────────────────

    private GitResult exec(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.addAll(Arrays.asList(args));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(workDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            List<String> lines = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            ).lines().collect(Collectors.toList());

            int exitCode = process.waitFor();
            return new GitResult(exitCode, lines);
        } catch (Exception e) {
            return new GitResult(-1, List.of("ERROR: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────
    // REPOSITORY INFO
    // ─────────────────────────────────────────────────────

    public String getCurrentBranch() {
        GitResult r = exec("branch", "--show-current");
        return r.isSuccess() ? r.getFirstLine().trim() : "(unknown)";
    }

    public String getRepoName() {
        GitResult r = exec("rev-parse", "--show-toplevel");
        if (r.isSuccess()) {
            String path = r.getFirstLine().trim();
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return "repo";
    }

    public List<String[]> getStatus() {
        GitResult r = exec("status", "--porcelain");
        List<String[]> files = new ArrayList<>();
        for (String line : r.getOutput()) {
            if (line.length() >= 3) {
                String status = line.substring(0, 2).trim();
                String file   = line.substring(3).trim();
                files.add(new String[]{status, file});
            }
        }
        return files;
    }

    // ─────────────────────────────────────────────────────
    // BRANCHES
    // ─────────────────────────────────────────────────────

    public List<BranchInfo> getBranches() {
        // Format: * branch_name  ->  tracking   (if tracking is set)
        GitResult r = exec("branch", "-avv", "--format=%(HEAD)|%(refname:short)|%(upstream:short)|%(upstream:track)");
        List<BranchInfo> branches = new ArrayList<>();
        for (String line : r.getOutput()) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 2) continue;
            boolean isCurrent = "*".equals(parts[0].trim());
            String name       = parts[1].trim();
            String tracking   = parts.length > 2 ? parts[2].trim() : "";
            boolean isRemote  = name.startsWith("remotes/") || name.startsWith("origin/");

            // Strip "remotes/" prefix
            if (name.startsWith("remotes/")) name = name.substring(8);

            branches.add(new BranchInfo(name, isCurrent, isRemote, tracking));
        }
        return branches;
    }

    public GitResult checkoutBranch(String name) {
        return exec("checkout", name);
    }

    public GitResult createBranch(String name, boolean checkout) {
        return checkout
            ? exec("checkout", "-b", name)
            : exec("branch", name);
    }

    public GitResult createBranchFrom(String name, String from, boolean checkout) {
        return checkout
            ? exec("checkout", "-b", name, from)
            : exec("branch", name, from);
    }

    public GitResult deleteBranch(String name, boolean force) {
        return exec("branch", force ? "-D" : "-d", name);
    }

    public GitResult renameBranch(String oldName, String newName) {
        return exec("branch", "-m", oldName, newName);
    }

    public GitResult mergeBranch(String name) {
        return exec("merge", name);
    }

    public GitResult mergeBranchNoFF(String name) {
        return exec("merge", "--no-ff", name);
    }

    public GitResult abortMerge() {
        return exec("merge", "--abort");
    }

    // ─────────────────────────────────────────────────────
    // COMMIT HISTORY
    // ─────────────────────────────────────────────────────

    public List<CommitInfo> getLog(int limit) {
        // Pipe-separated format for safe parsing
        String fmt = "%H|%h|%s|%an|%ar|%D";
        GitResult r = exec("log", "--format=" + fmt, "-n", String.valueOf(limit));
        List<CommitInfo> commits = new ArrayList<>();
        for (String line : r.getOutput()) {
            String[] parts = line.split("\\|", 6);
            if (parts.length < 5) continue;
            String refs = parts.length > 5 ? parts[5].trim() : "";
            commits.add(new CommitInfo(
                parts[0].trim(), parts[1].trim(),
                parts[2].trim(), parts[3].trim(),
                parts[4].trim(), refs
            ));
        }
        return commits;
    }

    public List<CommitInfo> getLogForBranch(String branch, int limit) {
        String fmt = "%H|%h|%s|%an|%ar|%D";
        GitResult r = exec("log", "--format=" + fmt, "-n", String.valueOf(limit), branch);
        List<CommitInfo> commits = new ArrayList<>();
        for (String line : r.getOutput()) {
            String[] parts = line.split("\\|", 6);
            if (parts.length < 5) continue;
            String refs = parts.length > 5 ? parts[5].trim() : "";
            commits.add(new CommitInfo(
                parts[0].trim(), parts[1].trim(),
                parts[2].trim(), parts[3].trim(),
                parts[4].trim(), refs
            ));
        }
        return commits;
    }

    public GitResult getCommitDiff(String hash) {
        return exec("show", "--stat", "--format=", hash);
    }

    public GitResult getCommitFullDiff(String hash) {
        return exec("show", hash);
    }

    /** Removes the last commit (keeps changes staged) */
    public GitResult dropLastCommitSoft() {
        return exec("reset", "--soft", "HEAD~1");
    }

    /** Removes the last commit (keeps changes unstaged) */
    public GitResult dropLastCommitMixed() {
        return exec("reset", "--mixed", "HEAD~1");
    }

    /** Removes the last commit and discards all changes */
    public GitResult dropLastCommitHard() {
        return exec("reset", "--hard", "HEAD~1");
    }

    /** Edits the message of the last commit */
    public GitResult editLastCommitMessage(String newMessage) {
        return exec("commit", "--amend", "-m", newMessage);
    }

    /** Creates a revert commit for the given hash */
    public GitResult revertCommit(String hash) {
        return exec("revert", "--no-edit", hash);
    }

    // ─────────────────────────────────────────────────────
    // CHERRY-PICK
    // ─────────────────────────────────────────────────────

    public GitResult cherryPick(String hash) {
        return exec("cherry-pick", hash);
    }

    public GitResult cherryPickNoCommit(String hash) {
        return exec("cherry-pick", "-n", hash);
    }

    public GitResult cherryPickAbort() {
        return exec("cherry-pick", "--abort");
    }

    public GitResult cherryPickContinue() {
        return exec("cherry-pick", "--continue", "--no-edit");
    }

    // ─────────────────────────────────────────────────────
    // STASH
    // ─────────────────────────────────────────────────────

    public List<StashEntry> getStashList() {
        // Format: index|message|hash
        GitResult r = exec("stash", "list", "--format=%gd|%s|%H");
        List<StashEntry> entries = new ArrayList<>();
        int index = 0;
        for (String line : r.getOutput()) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", 3);
            String msg  = parts.length > 1 ? parts[1].trim() : line;
            String hash = parts.length > 2 ? parts[2].trim() : "";
            entries.add(new StashEntry(index++, msg, hash));
        }
        return entries;
    }

    public GitResult stashSave(String message) {
        if (message == null || message.isBlank()) {
            return exec("stash", "push");
        }
        return exec("stash", "push", "-m", message);
    }

    public GitResult stashSaveIncludeUntracked(String message) {
        if (message == null || message.isBlank()) {
            return exec("stash", "push", "-u");
        }
        return exec("stash", "push", "-u", "-m", message);
    }

    public GitResult stashApply(int index) {
        return exec("stash", "apply", "stash@{" + index + "}");
    }

    public GitResult stashPop(int index) {
        return exec("stash", "pop", "stash@{" + index + "}");
    }

    public GitResult stashDrop(int index) {
        return exec("stash", "drop", "stash@{" + index + "}");
    }

    public GitResult stashShow(int index) {
        return exec("stash", "show", "-p", "stash@{" + index + "}");
    }

    public GitResult stashBranch(String branchName, int index) {
        return exec("stash", "branch", branchName, "stash@{" + index + "}");
    }

    // ─────────────────────────────────────────────────────
    // REMOTES
    // ─────────────────────────────────────────────────────

    public List<String[]> getRemotes() {
        GitResult r = exec("remote", "-v");
        Map<String, String[]> map = new LinkedHashMap<>();
        for (String line : r.getOutput()) {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) continue;
            String name = parts[0];
            String url  = parts[1];
            String type = parts[2]; // (fetch) or (push)
            map.computeIfAbsent(name, k -> new String[]{k, "", ""});
            if (type.contains("fetch")) map.get(name)[1] = url;
            if (type.contains("push"))  map.get(name)[2] = url;
        }
        return new ArrayList<>(map.values());
    }

    public GitResult addRemote(String name, String url) {
        return exec("remote", "add", name, url);
    }

    public GitResult removeRemote(String name) {
        return exec("remote", "remove", name);
    }

    public GitResult setRemoteUrl(String name, String url) {
        return exec("remote", "set-url", name, url);
    }

    public GitResult fetch(String remote) {
        if (remote == null || remote.isBlank()) return exec("fetch", "--all");
        return exec("fetch", remote);
    }

    public GitResult pull(String remote, String branch) {
        return exec("pull", remote, branch);
    }

    public GitResult pullRebase(String remote, String branch) {
        return exec("pull", "--rebase", remote, branch);
    }

    /**
     * Push with advanced options.
     * @param force       use --force-with-lease (safer than --force)
     * @param setUpstream use -u to set upstream tracking
     */
    public GitResult push(String remote, String branch, boolean force, boolean setUpstream) {
        List<String> args = new ArrayList<>(List.of("push"));
        if (setUpstream) args.add("-u");
        if (force) args.add("--force-with-lease");
        args.add(remote);
        args.add(branch);
        return exec(args.toArray(new String[0]));
    }

    /** True force push (without lease verification) */
    public GitResult pushForce(String remote, String branch) {
        return exec("push", "--force", remote, branch);
    }

    /**
     * Syncs the current branch with upstream:
     * 1. fetch upstream
     * 2. merge upstream/upstreamBranch into the current branch
     * 3. push origin current_branch
     */
    public GitResult syncUpstream(String upstreamRemote, String upstreamBranch, String pushRemote) {
        GitResult fetch = exec("fetch", upstreamRemote);
        if (!fetch.isSuccess()) return fetch;

        GitResult merge = exec("merge", upstreamRemote + "/" + upstreamBranch);
        if (!merge.isSuccess()) return merge;

        return exec("push", pushRemote, getCurrentBranch());
    }

    // ─────────────────────────────────────────────────────
    // CONFLICTS
    // ─────────────────────────────────────────────────────

    public List<String> getConflictedFiles() {
        GitResult r = exec("diff", "--name-only", "--diff-filter=U");
        return r.getOutput().stream()
            .filter(l -> !l.isBlank())
            .collect(Collectors.toList());
    }

    public boolean hasConflicts() {
        return !getConflictedFiles().isEmpty();
    }

    public GitResult markResolved(String filePath) {
        return exec("add", filePath);
    }

    public GitResult continueMerge(String message) {
        if (message != null && !message.isBlank()) {
            return exec("commit", "-m", message);
        }
        return exec("commit", "--no-edit");
    }

    // ─────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────

    /** Returns the list of available branch names for selectors */
    public List<String> getBranchNames() {
        GitResult r = exec("branch", "-a", "--format=%(refname:short)");
        return r.getOutput().stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }

    /** Returns the names of available remotes */
    public List<String> getRemoteNames() {
        GitResult r = exec("remote");
        return r.getOutput().stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}
