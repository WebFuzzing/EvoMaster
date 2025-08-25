package org.evomaster.client.java.controller.opensearch.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;
import java.lang.NoSuchMethodException;

/**
 * Helper class for working with OpenSearch queries.
 */
public class OpenSearchQueryHelper {
    /**
     * Extracts the kind of query (term, match, etc.) from a query object.
     */
    public static String extractQueryKind(Object query) {
        try {
            Object kind = query.getClass().getMethod("_kind").invoke(query);
            return (String) kind.getClass().getMethod("name").invoke(kind);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Extracts the field name from a query object.
     */
    public static String extractFieldName(Object query, String structure) {
        try {
            Object term = query.getClass().getMethod(structure).invoke(query);
            return (String) term.getClass().getMethod("field").invoke(term);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the field value from a query object with its t.
     */
    public static Object extractFieldValue(Object query, String structure) {
        try {
            Object term = query.getClass().getMethod(structure).invoke(query);
            Object value = term.getClass().getMethod("value").invoke(term);
            return extractTypedFieldValue(value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the value from a field by its type (Double, Long, Boolean, String, Null).
     */
    public static Object extractTypedFieldValue(Object value) {
        try {
            Object kind = value.getClass().getMethod("_kind").invoke(value);
            String kindName = (String) kind.getClass().getMethod("name").invoke(kind);

            switch (kindName) {
                case "Double":
                    if ((Boolean) value.getClass().getMethod("isDouble").invoke(value)) {
                        return value.getClass().getMethod("doubleValue").invoke(value);
                    }
                    break;
                case "Long":
                    if ((Boolean) value.getClass().getMethod("isLong").invoke(value)) {
                        return value.getClass().getMethod("longValue").invoke(value);
                    }
                    break;
                case "Boolean":
                    if ((Boolean) value.getClass().getMethod("isBoolean").invoke(value)) {
                        return value.getClass().getMethod("booleanValue").invoke(value);
                    }
                    break;
                case "String":
                    if ((Boolean) value.getClass().getMethod("isString").invoke(value)) {
                        return value.getClass().getMethod("stringValue").invoke(value);
                    }
                    break;
                case "Null":
                    return null;
            }

            return value.getClass().getMethod("_get").invoke(value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

