package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

/** Independent snapshots keep server insertion and one test invocation from mutating another. */
final class MongoQueryCase {
    private static final JsonWriterSettings SNAPSHOT_FORMAT = JsonWriterSettings.builder()
            .outputMode(JsonMode.EXTENDED).build();
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
        // Relaxed JSON would silently turn a small Int64 into Int32 on the next parse.
        this.query = query.toJson(SNAPSHOT_FORMAT);
        this.document = document.toJson(SNAPSHOT_FORMAT);
        this.matches = matches;
        this.index = index == null ? null : index.toJson(SNAPSHOT_FORMAT);
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
