package org.evomaster.client.java.controller.mongo.operations;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represent $regex operation.
 * Provides regular expression capabilities for pattern matching strings in queries.
 */
public class RegexOperation extends QueryOperationWithField {
    private final Pattern pattern;
    private final RegexOptions options;

    /**
     * Creates a new RegexOperation with a default RegexOptions
     * (all options set to false)
     *
     * @param fieldname the fieldname
     * @param pattern the pattern
     */
    public RegexOperation(String fieldname, Pattern pattern) {
        this(fieldname, pattern, new RegexOptions());
    }

    public RegexOperation(String fieldName, Pattern pattern, RegexOptions options) {
        super(fieldName);
        Objects.requireNonNull(pattern);
        Objects.requireNonNull(options);
        this.pattern = pattern;
        this.options = options;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public RegexOptions getOptions() {
        return options;
    }
}
