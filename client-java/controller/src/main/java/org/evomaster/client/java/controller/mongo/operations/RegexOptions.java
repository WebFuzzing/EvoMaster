package org.evomaster.client.java.controller.mongo.operations;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Options supported by MongoDB's {@code $regex} operator.
 */
public final class RegexOptions {

    private final boolean caseInsensitive;
    private final boolean multiline;
    private final boolean dotAll;
    private final boolean extended;
    private final boolean unicode;

    /**
     * Default constructor for creating a {@code RegexOptions} instance.
     * Initializes the regex options with all settings set to {@code false}.
     */
    public RegexOptions() {
        this(false, false, false, false, false);
    }

    /**
     * Constructs a new instance of {@code RegexOptions} with the specified options.
     *
     * @param caseInsensitive whether the regex evaluation should be case-insensitive.
     * @param multiline       whether the regex evaluation should recognize the start (^) and end ($)
     *                        anchors as beginning and end of each line, respectively.
     * @param dotAll          whether the dot (.) in the regex should match all characters, including line terminators.
     * @param extended        whether the regex should allow extended syntax, such as whitespace and comments.
     * @param unicode         whether the regex should handle Unicode-specific properties.
     */
    public RegexOptions(boolean caseInsensitive,
                        boolean multiline,
                        boolean dotAll,
                        boolean extended,
                        boolean unicode) {
        this.caseInsensitive = caseInsensitive;
        this.multiline = multiline;
        this.dotAll = dotAll;
        this.extended = extended;
        this.unicode = unicode;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public boolean isMultiline() {
        return multiline;
    }

    public boolean isDotAll() {
        return dotAll;
    }

    public boolean isExtended() {
        return extended;
    }

    public boolean isUnicode() {
        return unicode;
    }

    public boolean isEmpty() {
        return !caseInsensitive && !multiline && !dotAll && !extended && !unicode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RegexOptions)) {
            return false;
        }
        RegexOptions that = (RegexOptions) other;
        return caseInsensitive == that.caseInsensitive
                && multiline == that.multiline
                && dotAll == that.dotAll
                && extended == that.extended
                && unicode == that.unicode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseInsensitive, multiline, dotAll, extended, unicode);
    }

    public int toJavaRegexFlags() {
        int flags = 0;
        if (isCaseInsensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        if (isMultiline()) {
            flags |= Pattern.MULTILINE;
        }
        if (isDotAll()) {
            flags |= Pattern.DOTALL;
        }
        if (isExtended()) {
            flags |= Pattern.COMMENTS;
        }
        if (isUnicode()) {
            /**
             * MongoDB's 'u' option is intentionally mapped to no Java regex flag.
             * For MongoDB $regex, UTF mode is enabled by default, making 'u' redundant.
             * Java regex operates on Unicode strings, so no equivalent Pattern flag is required.
             */
        }
        return flags;
    }


}
