package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $text operation.
 * Performs a text search on the content of the fields indexed with a text index.
 */
public class TextOperation extends QueryOperation{
    private final String search;
    private final String language;
    private final Boolean caseSensitive;
    private final Boolean diacriticSensitive;

    public TextOperation(String search, String language, Boolean caseSensitive, Boolean diacriticSensitive) {
        this.search = search;
        this.language = language;
        this.caseSensitive = caseSensitive;
        this.diacriticSensitive = diacriticSensitive;
    }

    public String getSearch() {
        return search;
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    public Boolean getDiacriticSensitive() {
        return diacriticSensitive;
    }
}