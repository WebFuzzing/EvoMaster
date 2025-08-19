package org.evomaster.client.java.controller.opensearch.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class OpenSearchQueryHelper {
    public static String extractQueryKind(Object query) {
        try {
            Object kind = query.getClass().getMethod("_kind").invoke(query);
            return (String) kind.getClass().getMethod("name").invoke(kind);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    public static String extractTermFieldName(Object query) {
        try {
            Object term = query.getClass().getMethod("term").invoke(query);
            return (String) term.getClass().getMethod("field").invoke(term);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object extractTermFieldValue(Object query) {
        try {
            Object term = query.getClass().getMethod("term").invoke(query);
            Object value = term.getClass().getMethod("value").invoke(term);
            return value.getClass().getMethod("_get").invoke(value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
