package org.evomaster.client.java.controller.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

public class FieldPathResolver {

    static final String FIELD_PATH_SEPARATOR = ".";
    /**
     * Marker for a field path that does not reach any value in a document.
     */
    static final Object MISSING_FIELD = new Object();

    /**
     * Resolves a field name, possibly written in MongoDB's dot notation (e.g. "a.b.0.c"),
     * against a document. Following MongoDB's semantics, a path segment applied to an array
     * is resolved both as an index into the array (when the segment is a non-negative
     * integer) and as a field of each of its sub-documents. Hence, a path may reach several
     * values.
     *
     * @param document  the document the path is resolved against
     * @param fieldPath the (possibly dotted) field name
     * @return the values reached by the path, never empty. {@link #MISSING_FIELD} marks
     * a path that does not reach any value.
     */
    public static List<Object> getActualValues(Object document, String fieldPath) {
        List<Object> values = new ArrayList<>();
        getActualValues(document, splitFieldPath(fieldPath), 0, values);
        List<Object> actualValues = values.stream().map(FieldPathResolver::toActualValue).collect(Collectors.toList());
        return actualValues;
    }

    /**
     * Returns true if the field path is present in the document, false otherwise.
     * A missing field is evaluated as a null value.
     */
    public static boolean isFieldPathPresent(Object document, String fieldPath) {
        List<Object> values = new ArrayList<>();
        getActualValues(document, splitFieldPath(fieldPath), 0, values);
        values.removeIf(actualValue -> actualValue == FieldPathResolver.MISSING_FIELD);
        return (!values.isEmpty());
    }

    /**
     * A missing field is evaluated as a null value.
     */
    private static Object toActualValue(Object resolvedValue) {
        return resolvedValue == FieldPathResolver.MISSING_FIELD ? null : resolvedValue;
    }

    private static void getActualValues(Object current, String[] segments, int index, List<Object> actualValues) {
        if (index == segments.length) {
            actualValues.add(current);
            return;
        }
        final String segment = segments[index];
        if (isBsonDocument(current)) {
            if (documentContainsField(current, segment)) {
                getActualValues(getValue(current, segment), segments, index + 1, actualValues);
            } else {
                actualValues.add(MISSING_FIELD);
            }
        } else if (current instanceof List<?>) {
            // A segment applied to an array is ambiguous: a numeric segment such as "0" may denote
            // either the element at that position of the array, or a field literally named "0" of
            // the sub-documents held by the array. Both interpretations are followed, and the
            // values reached by each of them are collected.
            final int sizeBefore = actualValues.size();
            final List<?> list = (List<?>) current;
            OptionalInt arrayIndexOpt = parseAsArrayIndex(segment);
            if (arrayIndexOpt.isPresent()) {
                final int arrayIndex = arrayIndexOpt.getAsInt();
                if (arrayIndex >= 0 && arrayIndex < list.size()) {
                    // Array index interpretation: the segment is consumed by indexing into the array
                    getActualValues(list.get(arrayIndex), segments, index + 1, actualValues);
                }
            }
            for (Object element : list) {
                // Field name interpretation: the array is traversed implicitly, without consuming
                // the segment, which is then looked up in each sub-document of the array.
                // Only sub-documents are traversed, nested arrays are not implicitly unwound
                if (isBsonDocument(element)) {
                    getActualValues(element, segments, index, actualValues);
                }
            }
            if (actualValues.size() == sizeBefore) {
                actualValues.add(MISSING_FIELD);
            }
        } else {
            actualValues.add(MISSING_FIELD);
        }
    }

    static String[] splitFieldPath(String fieldPath) {
        return fieldPath.split(Pattern.quote(FIELD_PATH_SEPARATOR), -1);
    }


    /**
     * Parses a string as a non-negative integer, returning an empty OptionalInt if the string is not a valid representation of a non-negative integer.
     * Values such as "+1" or " 1" are not accepted. Leading zeros are also not accepted, except for the string "0".
     * @param text
     * @return
     */
    public static OptionalInt parseAsArrayIndex(String text) {
        if (!text.chars().allMatch(Character::isDigit)) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
