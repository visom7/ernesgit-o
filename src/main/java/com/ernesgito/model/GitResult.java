package com.ernesgito.model;

import java.util.List;

/**
 * Encapsulates the result of executing a git command.
 */
public class GitResult {
    private final int exitCode;
    private final List<String> output;

    public GitResult(int exitCode, List<String> output) {
        this.exitCode = exitCode;
        this.output = output;
    }

    public boolean isSuccess() { return exitCode == 0; }
    public int getExitCode() { return exitCode; }
    public List<String> getOutput() { return output; }

    public String getOutputAsString() {
        return String.join("\n", output);
    }

    public String getFirstLine() {
        return output.isEmpty() ? "" : output.get(0);
    }

    @Override
    public String toString() {
        return "GitResult{exitCode=" + exitCode + ", lines=" + output.size() + "}";
    }
}
