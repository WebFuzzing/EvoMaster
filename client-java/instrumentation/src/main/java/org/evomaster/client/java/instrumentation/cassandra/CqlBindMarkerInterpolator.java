package org.evomaster.client.java.instrumentation.cassandra;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Replaces the bind markers of an executed CQL statement with the values it was executed with, so
 * that the heuristics compute a distance against real literals instead of a placeholder.
 * <p>
 * Without this, a SUT that binds its queries (which is what Spring Data and any use of
 * {@code PreparedStatement} do) only ever reports {@code WHERE col = ?}. The WHERE clause then
 * carries no value to measure a row against, so every candidate scores the same and the search has
 * no gradient to follow. The same problem was solved the same way for SQL, in
 * {@code PreparedStatementClassReplacement.interpolateSqlStringWithJSqlParser}.
 * <p>
 * <b>None of the entry points here ever throws, and none ever returns null.</b> This runs inside the
 * SUT's own process, on the path of every intercepted query, so a statement that cannot be
 * interpolated for any reason (unexpected driver shape, a value with no codec, a marker/value count
 * mismatch) falls back to the parameterised text. That is exactly what was recorded before this
 * class existed, so the worst case is no improvement rather than a broken SUT. It is a deliberate
 * departure from the project's "public preconditions throw" rule, for that reason; the
 * {@code requireNonNull} checks that remain guard against bugs in our own callers, not against SUT
 * data.
 * <p>
 * The driver is on the SUT's classpath, not this module's, so everything is reached by reflection,
 * as in {@link CassandraSchemaTracer}.
 */
public class CqlBindMarkerInterpolator {

    /*
     * Driver types reached by reflection. The interfaces are used, rather than the concrete classes
     * of the objects at hand, so that the looked-up methods are always publicly accessible.
     */
    private static final String CODEC_REGISTRY_CLASS = "com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry";
    private static final String TYPE_CODEC_CLASS = "com.datastax.oss.driver.api.core.type.codec.TypeCodec";

    /*
     * Names of the driver methods invoked via reflection throughout this class.
     */
    private static final String METHOD_GET_CONTEXT = "getContext";
    private static final String METHOD_GET_CODEC_REGISTRY = "getCodecRegistry";
    private static final String METHOD_CODEC_FOR = "codecFor";
    private static final String METHOD_FORMAT = "format";
    private static final String METHOD_GET_PREPARED_STATEMENT = "getPreparedStatement";
    private static final String METHOD_SIZE = "size";
    private static final String METHOD_GET_OBJECT = "getObject";
    private static final String METHOD_GET_POSITIONAL_VALUES = "getPositionalValues";
    private static final String METHOD_GET_NAMED_VALUES = "getNamedValues";
    private static final String METHOD_AS_INTERNAL = "asInternal";

    /** How a null bound value is written in CQL; no codec is consulted for it. */
    private static final String NULL_LITERAL = "NULL";

    private static final String DOLLAR_QUOTE = "$$";
    private static final String BLOCK_COMMENT_END = "*/";

    private CqlBindMarkerInterpolator() {
    }

    /**
     * Resolves the literal that a bind marker is replaced with.
     */
    private interface MarkerResolver {
        /**
         * @param name            the marker's name, for a named marker ({@code :name}), or
         *                        {@code null} for a positional one ({@code ?})
         * @param positionalIndex the 0-based position of a positional marker among the positional
         *                        markers of the statement, or -1 for a named one
         * @return the CQL literal to write in the marker's place, or {@code null} if it cannot be
         *         resolved, which aborts the whole interpolation
         */
        String literalFor(String name, int positionalIndex);
    }

    /**
     * Interpolates a {@code Statement} handed to {@code CqlSession.execute(Statement)}: a
     * {@code BoundStatement}, or a {@code SimpleStatement} carrying positional or named values.
     *
     * @param cqlSession   a live {@code com.datastax.oss.driver.api.core.CqlSession}
     * @param statement    the statement being executed
     * @param fallbackCql  the statement's parameterised text, as already extracted by the caller,
     *                     returned unchanged when there is nothing to interpolate or when
     *                     interpolating fails
     * @return the CQL with its bind markers replaced by literals, or {@code fallbackCql}
     */
    public static String forStatement(Object cqlSession, Object statement, String fallbackCql) {
        Objects.requireNonNull(cqlSession);
        Objects.requireNonNull(statement);
        Objects.requireNonNull(fallbackCql);

        try {
            Object codecRegistry = codecRegistryOf(cqlSession);

            List<String> positional = boundLiterals(statement, codecRegistry);
            if (positional == null) {
                positional = simpleStatementPositionalLiterals(statement, codecRegistry);
            }
            if (positional != null) {
                return interpolatePositional(fallbackCql, positional, fallbackCql);
            }

            Map<String, String> named = simpleStatementNamedLiterals(statement, codecRegistry);
            if (named != null) {
                return interpolateNamed(fallbackCql, named, fallbackCql);
            }

            // no values bound: the text already carries its literals
            return fallbackCql;
        } catch (Exception e) {
            warnFallback(fallbackCql, e);
            return fallbackCql;
        }
    }

    /**
     * Interpolates the positional values of {@code CqlSession.execute(String, Object...)}.
     *
     * @param cqlSession a live {@code com.datastax.oss.driver.api.core.CqlSession}
     * @param cql        the parameterised CQL, with one {@code ?} per value
     * @param values     the values bound to it, in order
     * @return the CQL with its markers replaced by literals, or {@code cql} unchanged on any problem
     */
    public static String forPositionalValues(Object cqlSession, String cql, Object[] values) {
        Objects.requireNonNull(cqlSession);
        Objects.requireNonNull(cql);

        if (values == null || values.length == 0) {
            return cql;
        }

        try {
            Object codecRegistry = codecRegistryOf(cqlSession);
            List<String> literals = new ArrayList<>(values.length);
            for (Object value : values) {
                literals.add(formatValue(codecRegistry, value));
            }
            return interpolatePositional(cql, literals, cql);
        } catch (Exception e) {
            warnFallback(cql, e);
            return cql;
        }
    }

    /**
     * Interpolates the named values of {@code CqlSession.execute(String, Map)}.
     *
     * @param cqlSession a live {@code com.datastax.oss.driver.api.core.CqlSession}
     * @param cql        the parameterised CQL, with a {@code :name} marker per entry
     * @param values     the values bound to it, by marker name
     * @return the CQL with its markers replaced by literals, or {@code cql} unchanged on any problem
     */
    public static String forNamedValues(Object cqlSession, String cql, Map<String, Object> values) {
        Objects.requireNonNull(cqlSession);
        Objects.requireNonNull(cql);

        if (values == null || values.isEmpty()) {
            return cql;
        }

        try {
            Object codecRegistry = codecRegistryOf(cqlSession);
            Map<String, String> literals = new HashMap<>();
            for (Map.Entry<String, Object> e : values.entrySet()) {
                literals.put(e.getKey(), formatValue(codecRegistry, e.getValue()));
            }
            return interpolateNamed(cql, literals, cql);
        } catch (Exception e) {
            warnFallback(cql, e);
            return cql;
        }
    }

    /**
     * @return the bound values of a {@code BoundStatement}, already rendered as CQL literals, or
     *         {@code null} if {@code statement} is not a {@code BoundStatement}
     */
    private static List<String> boundLiterals(Object statement, Object codecRegistry) throws ReflectiveOperationException {
        if (!hasMethod(statement, METHOD_GET_PREPARED_STATEMENT)) {
            return null;
        }

        int size = (int) invoke(statement, METHOD_SIZE);
        Method getObject = statement.getClass().getMethod(METHOD_GET_OBJECT, int.class);

        List<String> literals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            literals.add(formatValue(codecRegistry, getObject.invoke(statement, i)));
        }
        return literals;
    }

    /**
     * @return the positional values of a {@code SimpleStatement}, already rendered as CQL literals,
     *         or {@code null} if there are none
     */
    private static List<String> simpleStatementPositionalLiterals(Object statement, Object codecRegistry) throws ReflectiveOperationException {
        if (!hasMethod(statement, METHOD_GET_POSITIONAL_VALUES)) {
            return null;
        }

        List<?> values = (List<?>) invoke(statement, METHOD_GET_POSITIONAL_VALUES);
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<String> literals = new ArrayList<>(values.size());
        for (Object value : values) {
            literals.add(formatValue(codecRegistry, value));
        }
        return literals;
    }

    /**
     * @return the named values of a {@code SimpleStatement}, already rendered as CQL literals and
     *         keyed by marker name, or {@code null} if there are none
     */
    private static Map<String, String> simpleStatementNamedLiterals(Object statement, Object codecRegistry) throws ReflectiveOperationException {
        if (!hasMethod(statement, METHOD_GET_NAMED_VALUES)) {
            return null;
        }

        Map<?, ?> values = (Map<?, ?>) invoke(statement, METHOD_GET_NAMED_VALUES);
        if (values == null || values.isEmpty()) {
            return null;
        }

        Map<String, String> literals = new HashMap<>();
        for (Map.Entry<?, ?> e : values.entrySet()) {
            // the keys are CqlIdentifier, whose internal form is the marker's name as written
            String name = (String) invoke(e.getKey(), METHOD_AS_INTERNAL);
            literals.put(name, formatValue(codecRegistry, e.getValue()));
        }
        return literals;
    }

    /**
     * Substitutes each {@code ?} of {@code cql}, in order, with the corresponding entry of
     * {@code literals}. Falls back when the two counts differ, since the statement is then not the
     * one we think it is and a partial substitution would report a query that was never executed.
     */
    private static String interpolatePositional(String cql, List<String> literals, String fallbackCql) {
        int[] consumed = new int[1];
        String interpolated = substitute(cql, (name, positionalIndex) -> {
            if (name != null || positionalIndex >= literals.size()) {
                return null;
            }
            consumed[0] = positionalIndex + 1;
            return literals.get(positionalIndex);
        });

        if (interpolated == null || consumed[0] != literals.size()) {
            return fallbackCql;
        }
        return interpolated;
    }

    /**
     * Substitutes each {@code :name} of {@code cql} with the matching entry of {@code literals}.
     * Entries of {@code literals} that no marker refers to are tolerated; a marker with no entry
     * falls back.
     */
    private static String interpolateNamed(String cql, Map<String, String> literals, String fallbackCql) {
        String interpolated = substitute(cql, (name, positionalIndex) ->
                name != null ? literals.get(name) : null);

        return interpolated != null ? interpolated : fallbackCql;
    }

    /**
     * Walks {@code cql} once and replaces every bind marker found in code position with the literal
     * {@code resolver} gives for it.
     * <p>
     * A {@code ?} or {@code :name} inside a string literal, a quoted identifier, a dollar-quoted
     * string or a comment is not a bind marker, so those regions are copied over verbatim. A naive
     * {@code replaceFirst("\\?", ...)} gets this wrong, which is why the SQL equivalent that did so
     * ({@code PreparedStatementClassReplacement.interpolateSqlString}) is deprecated.
     *
     * @return the interpolated CQL, or {@code null} if any marker could not be resolved
     */
    private static String substitute(String cql, MarkerResolver resolver) {
        StringBuilder out = new StringBuilder(cql.length() + 32);
        int n = cql.length();
        int i = 0;
        int positionalIndex = 0;

        while (i < n) {
            char c = cql.charAt(i);

            if (c == '\'' || c == '"') {
                i = appendQuoted(cql, i, c, out);
            } else if (c == '$' && next(cql, i) == '$') {
                i = appendUntil(cql, i, DOLLAR_QUOTE, out);
            } else if ((c == '-' && next(cql, i) == '-') || (c == '/' && next(cql, i) == '/')) {
                i = appendLineComment(cql, i, out);
            } else if (c == '/' && next(cql, i) == '*') {
                i = appendUntil(cql, i, BLOCK_COMMENT_END, out);
            } else if (c == '?') {
                String literal = resolver.literalFor(null, positionalIndex++);
                if (literal == null) {
                    return null;
                }
                out.append(literal);
                i++;
            } else if (c == ':' && isMarkerNameStart(next(cql, i))) {
                int end = i + 1;
                while (end < n && isMarkerNamePart(cql.charAt(end))) {
                    end++;
                }
                String literal = resolver.literalFor(cql.substring(i + 1, end), -1);
                if (literal == null) {
                    return null;
                }
                out.append(literal);
                i = end;
            } else {
                out.append(c);
                i++;
            }
        }

        return out.toString();
    }

    /**
     * Copies a quoted region verbatim, from its opening quote through its closing one. A doubled
     * quote inside is an escaped quote rather than a terminator, as CQL defines it for both
     * {@code '...'} literals and {@code "..."} identifiers.
     *
     * @return the index just past the closing quote, or the end of the text if it is unterminated
     */
    private static int appendQuoted(String cql, int start, char quote, StringBuilder out) {
        int n = cql.length();
        out.append(quote);

        int i = start + 1;
        while (i < n) {
            char c = cql.charAt(i);
            if (c == quote) {
                if (i + 1 < n && cql.charAt(i + 1) == quote) {
                    out.append(quote).append(quote);
                    i += 2;
                    continue;
                }
                out.append(quote);
                return i + 1;
            }
            out.append(c);
            i++;
        }
        return i;
    }

    /**
     * Copies a region verbatim, from {@code start} through the first occurrence of {@code terminator}
     * after it, or to the end of the text if it never occurs.
     *
     * @return the index just past the terminator, or the end of the text
     */
    private static int appendUntil(String cql, int start, String terminator, StringBuilder out) {
        int end = cql.indexOf(terminator, start + terminator.length());
        int stop = end < 0 ? cql.length() : end + terminator.length();
        out.append(cql, start, stop);
        return stop;
    }

    /**
     * Copies a line comment verbatim, up to but not including the newline that ends it, which the
     * caller's loop then handles as ordinary text.
     *
     * @return the index of that newline, or the end of the text
     */
    private static int appendLineComment(String cql, int start, StringBuilder out) {
        int end = start;
        int n = cql.length();
        while (end < n && cql.charAt(end) != '\n') {
            end++;
        }
        out.append(cql, start, end);
        return end;
    }

    private static char next(String cql, int i) {
        return i + 1 < cql.length() ? cql.charAt(i + 1) : '\0';
    }

    /*
     * A named marker is ':' followed by an unquoted CQL identifier. Requiring a letter or underscore
     * first keeps the ':' of a map literal such as {'k':1} from looking like a marker.
     */
    private static boolean isMarkerNameStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isMarkerNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Renders a bound value as the CQL literal the driver itself would write for it, by asking the
     * session's codec registry for the value's codec. {@code TypeCodec.format} already quotes and
     * escapes, so no literal rendering is done here.
     */
    private static String formatValue(Object codecRegistry, Object value) throws ReflectiveOperationException {
        if (value == null) {
            return NULL_LITERAL;
        }

        Method codecFor = Class.forName(CODEC_REGISTRY_CLASS).getMethod(METHOD_CODEC_FOR, Object.class);
        Object codec = codecFor.invoke(codecRegistry, value);

        Method format = Class.forName(TYPE_CODEC_CLASS).getMethod(METHOD_FORMAT, Object.class);
        return (String) format.invoke(codec, value);
    }

    private static Object codecRegistryOf(Object cqlSession) throws ReflectiveOperationException {
        Object context = invoke(cqlSession, METHOD_GET_CONTEXT);
        return invoke(context, METHOD_GET_CODEC_REGISTRY);
    }

    private static boolean hasMethod(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    /**
     * Reports, at most once per distinct message, that a statement was recorded with its markers
     * still in place. Worth knowing when reading a run's heuristics, since such a query gets no
     * gradient, but not worth one line per execution.
     */
    private static void warnFallback(String cql, Exception e) {
        SimpleLogger.uniqueWarn("Failed to interpolate the bound values of a CQL statement, so it is"
                + " recorded with its bind markers: " + cql + ". Cause: " + e);
    }
}