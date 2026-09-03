package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.operations.RegexOperation;
import org.evomaster.client.java.controller.mongo.operations.RegexOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Parses {@code { field: { $regex: pattern, $options: options } }} queries.
 */
public class RegexSelector extends QuerySelector {

    private static final String REGEX_OPERATOR = "$regex";
    private static final String OPTIONS_OPERATOR = "$options";

    private static final char MONGO_REGEX_OPTION_CASE_INSENSITIVE = 'i';
    private static final char MONGO_REGEX_OPTION_MULTILINE = 'm';
    private static final char MONGO_REGEX_OPTION_DOT_ALL = 's';
    private static final char MONGO_REGEX_OPTION_COMMENTS = 'x';
    private static final char MONGO_REGEX_OPTION_UNICODE = 'u';


    /**
     * A set of supported regex options used to modify the behavior of regular expression queries.
     * This set is immutable and contains predefined constants representing specific regex behavior.
     *
     * Available regex options in this set may include:
     * - Case insensitivity
     * - Multiline mode
     * - Dot-all mode, where the dot matches newline characters
     * - Extended mode, allowing for comments and whitespace in the pattern
     * - Unicode-aware matching
     *
     * This collection is primarily used in the context of query parsing and regex operation
     * construction within the `RegexSelector` class or other related selectors.
     */
    private static final Set<Character> REGEX_OPTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            MONGO_REGEX_OPTION_CASE_INSENSITIVE,
            MONGO_REGEX_OPTION_MULTILINE,
            MONGO_REGEX_OPTION_DOT_ALL,
            MONGO_REGEX_OPTION_COMMENTS,
            MONGO_REGEX_OPTION_UNICODE)));


    @Override
    public QueryOperation getOperation(Object query) {
        if (!isBsonDocument(query)) {
            return null;
        }

        Set<String> fields = documentKeys(query);
        if (fields == null || fields.size() != 1) {
            return null;
        }

        String fieldName = fields.iterator().next();
        Object innerDocument = getValue(query, fieldName);
        Object regexValue;
        Object explicitOptions = null;
        if (isBsonRegularExpression(innerDocument)) {
            // Mongo's Java driver serializes Filters.regex(...) as { field: /pattern/options }.
            regexValue = innerDocument;
        } else if (isBsonDocument(innerDocument) && documentContainsField(innerDocument, REGEX_OPERATOR)) {
            Set<String> operators = documentKeys(innerDocument);
            if (operators == null || operators.stream()
                    .anyMatch(key -> !REGEX_OPERATOR.equals(key) && !OPTIONS_OPERATOR.equals(key))) {
                return null;
            }
            regexValue = getValue(innerDocument, REGEX_OPERATOR);
            explicitOptions = getValue(innerDocument, OPTIONS_OPERATOR);
        } else {
            return null;
        }

        if (explicitOptions != null && !(explicitOptions instanceof String)) {
            return null;
        }

        String pattern;
        String options;
        if (regexValue instanceof String) {
            pattern = (String) regexValue;
            options = explicitOptions == null ? "" : (String) explicitOptions;
        } else if (regexValue instanceof Pattern) {
            Pattern javaPattern = (Pattern) regexValue;
            pattern = javaPattern.pattern();
            options = explicitOptions == null ? optionsFromFlags(javaPattern.flags()) : (String) explicitOptions;
        } else if (isBsonRegularExpression(regexValue)) {
            pattern = bsonRegexGetPattern(regexValue);
            options = explicitOptions == null
                    ? bsonRegexGetOptions(regexValue)
                    : (String) explicitOptions;
        } else {
            return null;
        }

        RegexOptions parsedOptions = parseOptions(options);
        if (parsedOptions == null) {
            return null;
        }

        final Pattern compile;
        try {
            final int flags = javaRegexFlagsFromOptions(parsedOptions);
            compile = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            return null;
        }
        return new RegexOperation(fieldName, compile, parsedOptions);
    }

    private RegexOptions parseOptions(String options) {

        for (char option : options.toCharArray()) {
            if (!REGEX_OPTIONS.contains(option)) {
                return null;
            }
        }
        return new RegexOptions(
                options.indexOf(MONGO_REGEX_OPTION_CASE_INSENSITIVE) >= 0,
                options.indexOf(MONGO_REGEX_OPTION_MULTILINE) >= 0,
                options.indexOf(MONGO_REGEX_OPTION_DOT_ALL) >= 0,
                options.indexOf(MONGO_REGEX_OPTION_COMMENTS) >= 0,
                options.indexOf(MONGO_REGEX_OPTION_UNICODE) >= 0);
    }

    private int javaRegexFlagsFromOptions(RegexOptions options) {
        int flags = 0;
        if (options.isCaseInsensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        if (options.isMultiline()) {
            flags |= Pattern.MULTILINE;
        }
        if (options.isDotAll()) {
            flags |= Pattern.DOTALL;
        }
        if (options.isExtended()) {
            flags |= Pattern.COMMENTS;
        }
        if (options.isUnicode()) {
            /**
             * MongoDB's 'u' option is intentionally mapped to no Java regex flag.
             * For MongoDB $regex, UTF mode is enabled by default, making 'u' redundant.
             * Java regex operates on Unicode strings, so no equivalent Pattern flag is required.
             */
        }
        return flags;
    }

    private String optionsFromFlags(int flags) {
        StringBuilder options = new StringBuilder();
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) options.append(MONGO_REGEX_OPTION_CASE_INSENSITIVE);
        if ((flags & Pattern.MULTILINE) != 0) options.append(MONGO_REGEX_OPTION_MULTILINE);
        if ((flags & Pattern.DOTALL) != 0) options.append(MONGO_REGEX_OPTION_DOT_ALL);
        if ((flags & Pattern.COMMENTS) != 0) options.append(MONGO_REGEX_OPTION_COMMENTS);
        if ((flags & Pattern.UNICODE_CASE) != 0) options.append(MONGO_REGEX_OPTION_UNICODE);
        return options.toString();
    }


    @Override
    protected String extractOperator(Object query) {
        if (!isBsonDocument(query)) {
            return null;
        }
        Set<String> fields = documentKeys(query);
        if (fields == null || fields.size() != 1) {
            return null;
        }
        Object innerDocument = getValue(query, fields.iterator().next());
        return isBsonRegularExpression(innerDocument)
                || isBsonDocument(innerDocument) && documentContainsField(innerDocument, REGEX_OPERATOR)
                ? REGEX_OPERATOR
                : null;
    }

    @Override
    protected String operator() {
        return REGEX_OPERATOR;
    }
}
