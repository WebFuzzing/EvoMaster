package org.evomaster.client.java.controller.mongo;

import org.bson.Document;

/** Independent snapshots keep server insertion and one test invocation from mutating another. */
final class MongoQueryCase {
    private final String name;
    private final String query;
    private final String document;
    private final String index;
    final boolean matches;

    MongoQueryCase(String name, Document query, Document document, boolean matches) {
        this(name, query, document, matches, null);
    }

    MongoQueryCase(String name, Document query, Document document, boolean matches, Document index) {
        this.name = name;
        this.query = query.toJson();
        this.document = document.toJson();
        this.matches = matches;
        this.index = index == null ? null : index.toJson();
    }

    Document query() {
        return Document.parse(query);
    }

    Document document() {
        return Document.parse(document);
    }

    Document index() {
        return index == null ? null : Document.parse(index);
    }

    @Override
    public String toString() {
        return name;
    }
}
