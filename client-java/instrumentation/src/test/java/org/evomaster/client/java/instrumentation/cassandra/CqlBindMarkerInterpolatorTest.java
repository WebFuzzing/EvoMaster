package org.evomaster.client.java.instrumentation.cassandra;

import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CqlBindMarkerInterpolator}, covering the marker scanner and the rendering of
 * values as CQL literals. No Cassandra instance is needed: the values are rendered by the driver's
 * own {@link CodecRegistry#DEFAULT}, reached through a stand-in for the session, since the
 * interpolator only ever asks a session for {@code getContext().getCodecRegistry()}.
 */
public class CqlBindMarkerInterpolatorTest {

    /**
     * Stands in for a {@code CqlSession}. The interpolator resolves the codec registry by
     * reflection on whatever object it is handed, so it needs no driver type here.
     */
    public static class FakeSession {
        public FakeContext getContext() {
            return new FakeContext();
        }
    }

    public static class FakeContext {
        public CodecRegistry getCodecRegistry() {
            return CodecRegistry.DEFAULT;
        }
    }

    private static final Object SESSION = new FakeSession();

    private static String positional(String cql, Object... values) {
        return CqlBindMarkerInterpolator.forPositionalValues(SESSION, cql, values);
    }

    private static String named(String cql, Map<String, Object> values) {
        return CqlBindMarkerInterpolator.forNamedValues(SESSION, cql, values);
    }

    // ------------------------------------------------------------------ positional markers

    @Test
    public void testSinglePositionalMarker() {
        assertEquals("SELECT * FROM t WHERE a = 123",
                positional("SELECT * FROM t WHERE a = ?", 123));
    }

    @Test
    public void testSeveralPositionalMarkersAreSubstitutedInOrder() {
        assertEquals("SELECT * FROM t WHERE a = 1 AND b = 'two' AND c = true",
                positional("SELECT * FROM t WHERE a = ? AND b = ? AND c = ?", 1, "two", true));
    }

    @Test
    public void testStringValueIsQuotedAndEscapedByTheDriver() {
        assertEquals("SELECT * FROM t WHERE a = 'O''Brien'",
                positional("SELECT * FROM t WHERE a = ?", "O'Brien"));
    }

    @Test
    public void testNullValueRendersAsNull() {
        assertEquals("SELECT * FROM t WHERE a = NULL",
                positional("SELECT * FROM t WHERE a = ?", new Object[]{null}));
    }

    @Test
    public void testNoValuesLeavesTheQueryUntouched() {
        String cql = "SELECT * FROM t WHERE a = 1";
        assertEquals(cql, positional(cql));
    }

    // ------------------------------------------------------------------ named markers

    @Test
    public void testNamedMarkersAreSubstitutedByName() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", 1);
        values.put("second", "two");

        assertEquals("SELECT * FROM t WHERE a = 1 AND b = 'two'",
                named("SELECT * FROM t WHERE a = :first AND b = :second", values));
    }

    @Test
    public void testSameNamedMarkerUsedTwiceIsSubstitutedBothTimes() {
        Map<String, Object> values = new HashMap<>();
        values.put("v", 7);

        assertEquals("SELECT * FROM t WHERE a = 7 AND b = 7",
                named("SELECT * FROM t WHERE a = :v AND b = :v", values));
    }

    @Test
    public void testUnknownNamedMarkerFallsBack() {
        Map<String, Object> values = new HashMap<>();
        values.put("other", 1);

        String cql = "SELECT * FROM t WHERE a = :v";
        assertEquals(cql, named(cql, values));
    }

    // ------------------------------------------------------------------ markers that are not markers

    @Test
    public void testQuestionMarkInsideStringLiteralIsNotAMarker() {
        assertEquals("SELECT * FROM t WHERE a = 'is it? yes' AND b = 1",
                positional("SELECT * FROM t WHERE a = 'is it? yes' AND b = ?", 1));
    }

    @Test
    public void testEscapedQuoteInsideStringLiteralDoesNotEndIt() {
        assertEquals("SELECT * FROM t WHERE a = 'it''s a ? here' AND b = 1",
                positional("SELECT * FROM t WHERE a = 'it''s a ? here' AND b = ?", 1));
    }

    @Test
    public void testQuestionMarkInsideQuotedIdentifierIsNotAMarker() {
        assertEquals("SELECT \"we?ird\" FROM t WHERE a = 1",
                positional("SELECT \"we?ird\" FROM t WHERE a = ?", 1));
    }

    @Test
    public void testQuestionMarkInsideDollarQuotedStringIsNotAMarker() {
        assertEquals("SELECT * FROM t WHERE a = $$what? no$$ AND b = 1",
                positional("SELECT * FROM t WHERE a = $$what? no$$ AND b = ?", 1));
    }

    @Test
    public void testQuestionMarkInsideDashLineCommentIsNotAMarker() {
        assertEquals("SELECT * FROM t -- is this ?\nWHERE a = 1",
                positional("SELECT * FROM t -- is this ?\nWHERE a = ?", 1));
    }

    @Test
    public void testQuestionMarkInsideSlashLineCommentIsNotAMarker() {
        assertEquals("SELECT * FROM t // is this ?\nWHERE a = 1",
                positional("SELECT * FROM t // is this ?\nWHERE a = ?", 1));
    }

    @Test
    public void testQuestionMarkInsideBlockCommentIsNotAMarker() {
        assertEquals("SELECT /* a ? here */ * FROM t WHERE a = 1",
                positional("SELECT /* a ? here */ * FROM t WHERE a = ?", 1));
    }

    @Test
    public void testNamedMarkerInsideStringLiteralIsNotAMarker() {
        Map<String, Object> values = new HashMap<>();
        values.put("b", 2);

        assertEquals("SELECT * FROM t WHERE a = 'x:name y' AND b = 2",
                named("SELECT * FROM t WHERE a = 'x:name y' AND b = :b", values));
    }

    @Test
    public void testColonFollowedByNonIdentifierIsNotAMarker() {
        // the ':' of a map literal is not a bind marker, so the statement interpolates normally
        assertEquals("UPDATE t SET m = {'k':1} WHERE a = 5",
                positional("UPDATE t SET m = {'k':1} WHERE a = ?", 5));
    }

    // ------------------------------------------------------------------ arity mismatches fall back

    @Test
    public void testMoreMarkersThanValuesFallsBack() {
        String cql = "SELECT * FROM t WHERE a = ? AND b = ?";
        assertEquals(cql, positional(cql, 1));
    }

    @Test
    public void testMoreValuesThanMarkersFallsBack() {
        String cql = "SELECT * FROM t WHERE a = ?";
        assertEquals(cql, positional(cql, 1, 2));
    }

    @Test
    public void testPositionalValuesAgainstNamedMarkersFallBack() {
        String cql = "SELECT * FROM t WHERE a = :v";
        assertEquals(cql, positional(cql, 1));
    }
}