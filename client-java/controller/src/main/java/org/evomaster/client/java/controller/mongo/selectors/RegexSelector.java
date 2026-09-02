package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.operations.RegexOperation;
import org.evomaster.client.java.controller.mongo.operations.RegexOptions;

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
        if (!isBsonDocument(innerDocument) || !documentContainsField(innerDocument, REGEX_OPERATOR)) {
            return null;
        }

        Set<String> operators = documentKeys(innerDocument);
        if (operators == null || operators.stream()
                .anyMatch(key -> !REGEX_OPERATOR.equals(key) && !OPTIONS_OPERATOR.equals(key))) {
            return null;
        }

        Object regexValue = getValue(innerDocument, REGEX_OPERATOR);
        Object explicitOptions = getValue(innerDocument, OPTIONS_OPERATOR);
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
            final int flags = flagsFromOptions(parsedOptions);
            compile = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            return null;
        }
        return new RegexOperation(fieldName, compile, parsedOptions);
    }

    private RegexOptions parseOptions(String options) {
        for (char option : options.toCharArray()) {
            if ("imsxu".indexOf(option) < 0) {
                return null;
            }
        }
        return new RegexOptions(
                options.indexOf('i') >= 0,
                options.indexOf('m') >= 0,
                options.indexOf('s') >= 0,
                options.indexOf('x') >= 0,
                options.indexOf('u') >= 0);
    }

    private int flagsFromOptions(RegexOptions options) {
        int flags = 0;
        if (options.isCaseInsensitive()) flags |= Pattern.CASE_INSENSITIVE;
        if (options.isMultiline()) flags |= Pattern.MULTILINE;
        if (options.isDotAll()) flags |= Pattern.DOTALL;
        if (options.isExtended()) flags |= Pattern.COMMENTS;
        if (options.isUnicode()) flags |= Pattern.UNICODE_CASE;
        return flags;
    }

    private String optionsFromFlags(int flags) {
        StringBuilder options = new StringBuilder();
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) options.append('i');
        if ((flags & Pattern.MULTILINE) != 0) options.append('m');
        if ((flags & Pattern.DOTALL) != 0) options.append('s');
        if ((flags & Pattern.COMMENTS) != 0) options.append('x');
        if ((flags & Pattern.UNICODE_CASE) != 0) options.append('u');
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
        return isBsonDocument(innerDocument) && documentContainsField(innerDocument, REGEX_OPERATOR)
                ? REGEX_OPERATOR
                : null;
    }

    @Override
    protected String operator() {
        return REGEX_OPERATOR;
    }
}
