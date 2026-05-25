package com.ernesgito.model;

/**
 * Represents a local or remote git branch.
 */
public class BranchInfo {
    private final String name;
    private final boolean isCurrent;
    private final boolean isRemote;
    private final String tracking;

    public BranchInfo(String name, boolean isCurrent, boolean isRemote, String tracking) {
        this.name = name;
        this.isCurrent = isCurrent;
        this.isRemote = isRemote;
        this.tracking = tracking;
    }

    public String getName()       { return name; }
    public boolean isCurrent()    { return isCurrent; }
    public boolean isRemote()     { return isRemote; }
    public String getTracking()   { return tracking; }

    /** Simplified name without the remote/ prefix */
    public String getShortName() {
        if (isRemote && name.contains("/")) {
            int idx = name.indexOf('/');
            return name.substring(idx + 1);
        }
        return name;
    }

    public String getRemoteName() {
        if (isRemote && name.contains("/")) {
            return name.substring(0, name.indexOf('/'));
        }
        return "";
    }

    @Override
    public String toString() { return name; }
}
