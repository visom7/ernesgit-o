package com.ernesgito.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a git commit with its metadata.
 */
public class CommitInfo {
    private final StringProperty hash = new SimpleStringProperty();
    private final StringProperty shortHash = new SimpleStringProperty();
    private final StringProperty subject = new SimpleStringProperty();
    private final StringProperty author = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty refs = new SimpleStringProperty();

    public CommitInfo(String hash, String shortHash, String subject,
                      String author, String date, String refs) {
        this.hash.set(hash);
        this.shortHash.set(shortHash);
        this.subject.set(subject);
        this.author.set(author);
        this.date.set(date);
        this.refs.set(refs);
    }

    public String getHash()      { return hash.get(); }
    public String getShortHash() { return shortHash.get(); }
    public String getSubject()   { return subject.get(); }
    public String getAuthor()    { return author.get(); }
    public String getDate()      { return date.get(); }
    public String getRefs()      { return refs.get(); }

    public StringProperty hashProperty()      { return hash; }
    public StringProperty shortHashProperty() { return shortHash; }
    public StringProperty subjectProperty()   { return subject; }
    public StringProperty authorProperty()    { return author; }
    public StringProperty dateProperty()      { return date; }
    public StringProperty refsProperty()      { return refs; }

    public boolean hasRefs() { return refs.get() != null && !refs.get().isBlank(); }
}
