package org.evomaster.client.java.controller.mongo.operations;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Represent $regex operation.
 * Provides regular expression capabilities for pattern matching strings in queries.
 */
public class RegexOperation extends QueryOperation{
    private final Pattern pattern;
    private final ArrayList<Character> options;

    public RegexOperation(Pattern pattern, ArrayList<Character> options) {
        this.pattern = pattern;
        this.options = options;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public ArrayList<Character> getOptions() {
        return options;
    }
}