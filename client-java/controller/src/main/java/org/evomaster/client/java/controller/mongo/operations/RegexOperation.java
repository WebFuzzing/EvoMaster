package org.evomaster.client.java.controller.mongo.operations;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Represent $regex operation.
 * Provides regular expression capabilities for pattern matching strings in queries.
 */
public class RegexOperation extends QueryOperation{
    private final Pattern pattern;
    private final List<Character> options;

    public RegexOperation(Pattern pattern, List<Character> options) {
        this.pattern = pattern;
        this.options = options;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<Character> getOptions() {
        return options;
    }
}