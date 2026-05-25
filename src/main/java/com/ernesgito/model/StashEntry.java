package com.ernesgito.model;

/**
 * Represents an entry in the git stash.
 */
public class StashEntry {
    private final int index;
    private final String message;
    private final String hash;

    public StashEntry(int index, String message, String hash) {
        this.index = index;
        this.message = message;
        this.hash = hash;
    }

    public int getIndex()     { return index; }
    public String getMessage() { return message; }
    public String getHash()   { return hash; }
    public String getRef()    { return "stash@{" + index + "}"; }

    @Override
    public String toString() { return getRef() + ": " + message; }
}
