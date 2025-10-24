package org.evomaster.client.java.controller.opensearch.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;
import java.lang.NoSuchMethodException;
import org.evomaster.client.java.utils.SimpleLogger;

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
     * Extracts the field value from a query object.
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
     * Extracts the case_insensitive parameter from a term query object.
     */
    public static Boolean extractCaseInsensitive(Object query, String structure) {
        try {
            Object term = query.getClass().getMethod(structure).invoke(query);
            try {
                Object caseInsensitive = term.getClass().getMethod("caseInsensitive").invoke(term);
                if (caseInsensitive != null) {
                    return (Boolean) caseInsensitive;
                }
            } catch (NoSuchMethodException e) {
                // case_insensitive parameter is optional, return default value
                SimpleLogger.debug("[OpenSearch] case_insensitive parameter not found, using default (false)");
            }
            return false; // Default value as per OpenSearch documentation
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the terms array from a terms query object.
     */
    public static java.util.List<Object> extractTermsArray(Object query, String structure) {
        try {
            Object terms = query.getClass().getMethod(structure).invoke(query);
            
            // Get the terms array/list
            Object termsValue = terms.getClass().getMethod("terms").invoke(terms);
            if (termsValue instanceof java.util.List) {
                java.util.List<Object> result = new java.util.ArrayList<>();
                for (Object term : (java.util.List<?>) termsValue) {
                    result.add(extractTypedFieldValue(term));
                }
                return result;
            }
            return new java.util.ArrayList<>();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the boost parameter from a query object.
     */
    public static Float extractBoost(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object boost = queryObj.getClass().getMethod("boost").invoke(queryObj);
                if (boost != null) {
                    return ((Number) boost).floatValue();
                }
            } catch (NoSuchMethodException e) {
                // boost parameter is optional
                SimpleLogger.debug("[OpenSearch] boost parameter not found, using default");
            }
            return null; // Default value
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the _name parameter from a query object.
     */
    public static String extractQueryName(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object name = queryObj.getClass().getMethod("queryName").invoke(queryObj);
                if (name != null) {
                    return (String) name;
                }
            } catch (NoSuchMethodException e) {
                // _name parameter is optional
                SimpleLogger.debug("[OpenSearch] _name parameter not found");
            }
            return null; // Default value
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the value_type parameter from a terms query object.
     */
    public static String extractValueType(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object valueType = queryObj.getClass().getMethod("valueType").invoke(queryObj);
                if (valueType != null) {
                    return (String) valueType;
                }
            } catch (NoSuchMethodException e) {
                // value_type parameter is optional
                SimpleLogger.debug("[OpenSearch] value_type parameter not found, using default");
            }
            return "default"; // Default value as per OpenSearch documentation
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the minimum_should_match_field parameter from a terms_set query object.
     */
    public static String extractMinimumShouldMatchField(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object field = queryObj.getClass().getMethod("minimumShouldMatchField").invoke(queryObj);
                if (field != null) {
                    return (String) field;
                }
            } catch (NoSuchMethodException e) {
                // minimum_should_match_field parameter is optional
                SimpleLogger.debug("[OpenSearch] minimum_should_match_field parameter not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the minimum_should_match_script parameter from a terms_set query object.
     */
    public static String extractMinimumShouldMatchScript(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object script = queryObj.getClass().getMethod("minimumShouldMatchScript").invoke(queryObj);
                if (script != null) {
                    return (String) script;
                }
            } catch (NoSuchMethodException e) {
                // minimum_should_match_script parameter is optional
                SimpleLogger.debug("[OpenSearch] minimum_should_match_script parameter not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the values array from an ids query object.
     */
    public static java.util.List<String> extractIdsValues(Object query, String structure) {
        try {
            Object ids = query.getClass().getMethod(structure).invoke(query);
            Object valuesObj = ids.getClass().getMethod("values").invoke(ids);
            
            if (valuesObj instanceof java.util.List) {
                java.util.List<String> result = new java.util.ArrayList<>();
                for (Object value : (java.util.List<?>) valuesObj) {
                    result.add((String) value);
                }
                return result;
            }
            return new java.util.ArrayList<>();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts range parameters from a range query object.
     */
    public static Object extractRangeParameter(Object query, String structure, String parameterName) {
        try {
            Object range = query.getClass().getMethod(structure).invoke(query);
            String fieldName = extractFieldName(query, structure);
            
            // Get the field-specific range object
            Object fieldRange = range.getClass().getMethod(fieldName).invoke(range);
            
            try {
                Object param = fieldRange.getClass().getMethod(parameterName).invoke(fieldRange);
                if (param != null) {
                    return extractTypedFieldValue(param);
                }
            } catch (NoSuchMethodException e) {
                // Parameter is optional
                SimpleLogger.debug("[OpenSearch] Range parameter " + parameterName + " not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts string parameter from range query object.
     */
    public static String extractRangeStringParameter(Object query, String structure, String parameterName) {
        try {
            Object range = query.getClass().getMethod(structure).invoke(query);
            String fieldName = extractFieldName(query, structure);
            
            // Get the field-specific range object
            Object fieldRange = range.getClass().getMethod(fieldName).invoke(range);
            
            try {
                Object param = fieldRange.getClass().getMethod(parameterName).invoke(fieldRange);
                if (param != null) {
                    return (String) param;
                }
            } catch (NoSuchMethodException e) {
                // Parameter is optional
                SimpleLogger.debug("[OpenSearch] Range string parameter " + parameterName + " not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the rewrite parameter from a prefix query object.
     */
    public static String extractRewrite(Object query, String structure) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object rewrite = queryObj.getClass().getMethod("rewrite").invoke(queryObj);
                if (rewrite != null) {
                    return (String) rewrite;
                }
            } catch (NoSuchMethodException e) {
                // rewrite parameter is optional
                SimpleLogger.debug("[OpenSearch] rewrite parameter not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the field parameter from an exists query object.
     */
    public static String extractExistsField(Object query, String structure) {
        try {
            Object exists = query.getClass().getMethod(structure).invoke(query);
            Object field = exists.getClass().getMethod("field").invoke(exists);
            return (String) field;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts integer parameter from a query object.
     */
    public static Integer extractIntegerParameter(Object query, String structure, String parameterName) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object param = queryObj.getClass().getMethod(parameterName).invoke(queryObj);
                if (param != null) {
                    return ((Number) param).intValue();
                }
            } catch (NoSuchMethodException e) {
                SimpleLogger.debug("[OpenSearch] Integer parameter " + parameterName + " not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts string parameter from a query object.
     */
    public static String extractStringParameter(Object query, String structure, String parameterName) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object param = queryObj.getClass().getMethod(parameterName).invoke(queryObj);
                if (param != null) {
                    return (String) param;
                }
            } catch (NoSuchMethodException e) {
                SimpleLogger.debug("[OpenSearch] String parameter " + parameterName + " not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts boolean parameter from a query object.
     */
    public static Boolean extractBooleanParameter(Object query, String structure, String parameterName) {
        try {
            Object queryObj = query.getClass().getMethod(structure).invoke(query);
            try {
                Object param = queryObj.getClass().getMethod(parameterName).invoke(queryObj);
                if (param != null) {
                    return (Boolean) param;
                }
            } catch (NoSuchMethodException e) {
                SimpleLogger.debug("[OpenSearch] Boolean parameter " + parameterName + " not found");
            }
            return null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts nested query list from bool query clauses.
     */
    public static java.util.List<Object> extractBoolClause(Object query, String structure, String clauseName) {
        try {
            Object bool = query.getClass().getMethod(structure).invoke(query);
            try {
                Object clause = bool.getClass().getMethod(clauseName).invoke(bool);
                if (clause instanceof java.util.List) {
                    return (java.util.List<Object>) clause;
                }
            } catch (NoSuchMethodException e) {
                SimpleLogger.debug("[OpenSearch] Bool clause " + clauseName + " not found");
            }
            return new java.util.ArrayList<>();
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

            SimpleLogger.warn("[OpenSearch] [extractTypedFieldValue] found unknown kind: " + kindName);
            return value.getClass().getMethod("_get").invoke(value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

