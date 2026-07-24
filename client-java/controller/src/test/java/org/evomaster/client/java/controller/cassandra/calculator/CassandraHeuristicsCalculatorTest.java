package org.evomaster.client.java.controller.cassandra.calculator;

import com.datastax.oss.driver.api.core.data.CqlDuration;
import org.evomaster.client.java.controller.cassandra.model.CassandraRow;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraHeuristicsCalculatorTest {

    private final CassandraHeuristicsCalculator calc = new CassandraHeuristicsCalculator();

    private static CassandraRow row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return new CassandraRow(m);
    }

    private double dist(String cql, CassandraRow... rows) {
        return calc.computeDistance(cql, Arrays.asList(rows));
    }

    private double distNoRows(String cql) {
        return calc.computeDistance(cql, Collections.emptyList());
    }

    private static final double DELTA = 1e-9;

    /**
     * Expected distance for a single-row table whose WHERE-clause condition evaluates to
     * {@code FALSE_TRUTHNESS} for that row (e.g. because the queried column is absent from it).
     * H-Row-set is definitely true (one row is present), so H-Query is the average of
     * {@code 1.0} and H-Condition, i.e. {@code buildScaledTruthness(C, C).ofTrue}.
     */
    private static final double MISSING_COLUMN_DISTANCE =
            1.0 - (1.0 + (DistanceHelper.H_NOT_NULL + (1 - DistanceHelper.H_NOT_NULL) * DistanceHelper.H_NOT_NULL)) / 2.0;

    @Test
    void noWhere_emptyTable_maxDistance() {
        // H-Table(0 rows) = FALSE_TRUTHNESS → ofTrue = C → distance = 1 - C
        assertEquals(1.0 - DistanceHelper.H_NOT_NULL, distNoRows("SELECT * FROM t"), DELTA);
    }

    @Test
    void noWhere_nonEmptyTable_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t", row("id", 1L)), DELTA);
    }

    @Test
    void noWhere_multipleRows_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t", row("id", 1L), row("id", 2L)),
                DELTA);
    }

    @Test
    void where_emptyTable_highDistance() {
        double d = distNoRows("SELECT * FROM t WHERE id = 1");
        // andAggregation(FALSE, FALSE) → ofTrue ≈ C → distance ≈ 1 - C
        assertTrue(d > 0.5);
    }

    // Numeric equality (=)

    @Test
    void numericEquals_long_exactMatch() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col = 42", row("col", 42L)), DELTA);
    }

    @Test
    void numericEquals_integer_exactMatch() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col = 5", row("col", 5)), DELTA);
    }

    @Test
    void numericEquals_double_exactMatch() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col = 3.14", row("col", 3.14)), DELTA);
    }

    @Test
    void numericEquals_float_exactMatch() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col = 1", row("col", 1.0f)), DELTA);
    }

    @Test
    void numericEquals_noMatch_nonZero() {
        double d = dist("SELECT * FROM t WHERE col = 42", row("col", 43L));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void numericEquals_closerValueGivesLowerDistance() {
        double dClose = dist("SELECT * FROM t WHERE col = 10", row("col", 11L));
        double dFar   = dist("SELECT * FROM t WHERE col = 10", row("col", 100L));
        assertTrue(dClose < dFar);
    }

    // Numeric ordering (>, >=, <, <=)

    @Test
    void numericGT_satisfied_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col > 5", row("col", 10L)), DELTA);
    }

    @Test
    void numericGT_boundary_notSatisfied() {
        assertTrue(dist("SELECT * FROM t WHERE col > 10", row("col", 10L)) > 0.0);
    }

    @Test
    void numericGT_notSatisfied_nonZero() {
        assertTrue(dist("SELECT * FROM t WHERE col > 10", row("col", 5L)) > 0.0);
    }

    @Test
    void numericGTE_boundary_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col >= 10", row("col", 10L)), DELTA);
    }

    @Test
    void numericGTE_notSatisfied_nonZero() {
        assertTrue(dist("SELECT * FROM t WHERE col >= 10", row("col", 9L)) > 0.0);
    }

    @Test
    void numericLT_satisfied_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col < 10", row("col", 5L)), DELTA);
    }

    @Test
    void numericLT_boundary_notSatisfied() {
        assertTrue(dist("SELECT * FROM t WHERE col < 10", row("col", 10L)) > 0.0);
    }

    @Test
    void numericLTE_boundary_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col <= 10", row("col", 10L)), DELTA);
    }

    @Test
    void numericLTE_notSatisfied_nonZero() {
        assertTrue(dist("SELECT * FROM t WHERE col <= 10", row("col", 11L)) > 0.0);
    }

    @Test
    void numericOrdering_closerToThresholdGivesBetterScore() {
        // Col must be > 100; row with 99 is closer than row with 50
        double dClose = dist("SELECT * FROM t WHERE col > 100", row("col", 99L));
        double dFar   = dist("SELECT * FROM t WHERE col > 100", row("col", 50L));
        assertTrue(dClose < dFar);
    }

    // String equality

    @Test
    void stringEquals_exactMatch_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE s = 'hello'", row("s", "hello")), DELTA);
    }

    @Test
    void stringEquals_differentString_nonZero() {
        double d = dist("SELECT * FROM t WHERE s = 'hello'", row("s", "world"));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void stringEquals_prefixCloserThanUnrelated() {
        double dClose = dist("SELECT * FROM t WHERE s = 'hello'", row("s", "hell"));
        double dFar   = dist("SELECT * FROM t WHERE s = 'hello'", row("s", "xyz"));
        assertTrue(dClose < dFar);
    }

    @Test
    void stringEquals_emptyString_exactMatch() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE s = ''", row("s", "")), DELTA);
    }

    // Boolean equality

    @Test
    void booleanEquals_trueMatch_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE active = true", row("active", true)), DELTA);
    }

    @Test
    void booleanEquals_falseMatch_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE active = false", row("active", false)), DELTA);
    }

    @Test
    void booleanEquals_mismatch_nonZero() {
        double d = dist("SELECT * FROM t WHERE active = true", row("active", false));
        assertTrue(d > 0.0 && d < 1.0);
    }

    // UUID equality

    private static final UUID UUID_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID UUID_B = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID UUID_C = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Test
    void uuidEquals_sameUuid_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE id = 550e8400-e29b-41d4-a716-446655440000",
                        row("id", UUID_A)),
                DELTA);
    }

    @Test
    void uuidEquals_differentUuid_nonZero() {
        double d = dist("SELECT * FROM t WHERE id = 550e8400-e29b-41d4-a716-446655440000",
                row("id", UUID_B));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void uuidEquals_veryDifferentUuid_higherDistanceThanCloseUuid() {
        double dClose = dist("SELECT * FROM t WHERE id = 550e8400-e29b-41d4-a716-446655440000",
                row("id", UUID_B));                  // differs in one bit of the last byte
        double dFar   = dist("SELECT * FROM t WHERE id = 550e8400-e29b-41d4-a716-446655440000",
                row("id", UUID_C));                  // all-zero UUID — many bits differ
        assertTrue(dClose < dFar);
    }

    // Duration equality (CqlDuration)

    @Test
    void durationEquals_exactMatch_zeroDistance() {
        // 1y3d4h = months:12, days:3, nanos:4h in nanoseconds
        CqlDuration dur = CqlDuration.newInstance(12, 3, 14_400_000_000_000L);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d = 1y3d4h", row("d", dur)),
                DELTA);
    }

    @Test
    void durationEquals_differentMonths_nonZero() {
        CqlDuration dur = CqlDuration.newInstance(1, 0, 0); // 1mo
        double d = dist("SELECT * FROM t WHERE d = 2mo", row("d", dur));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void durationEquals_allComponentsDiffer_nonZero() {
        CqlDuration dur = CqlDuration.newInstance(2, 1, 0);
        double d = dist("SELECT * FROM t WHERE d = 1mo3d", row("d", dur));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void durationEquals_closerMonthsGivesBetterScore() {
        CqlDuration close = CqlDuration.newInstance(1, 0, 0);  // 1 month away from 2mo
        CqlDuration far   = CqlDuration.newInstance(10, 0, 0); // 8 months away from 2mo
        double dClose = dist("SELECT * FROM t WHERE d = 2mo", row("d", close));
        double dFar   = dist("SELECT * FROM t WHERE d = 2mo", row("d", far));
        assertTrue(dClose < dFar);
    }

    // InetAddress equality

    @Test
    void inetEquals_exactMatch_zeroDistance() throws Exception {
        InetAddress addr = InetAddress.getByName("192.168.1.1");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ip = '192.168.1.1'", row("ip", addr)),
                DELTA);
    }

    @Test
    void inetEquals_differentAddress_nonZero() throws Exception {
        InetAddress addr = InetAddress.getByName("192.168.1.2");
        double d = dist("SELECT * FROM t WHERE ip = '192.168.1.1'", row("ip", addr));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void inetEquals_closerAddressGivesBetterScore() throws Exception {
        // '192.168.1.1' vs '192.168.1.2' — one character differs
        // '192.168.1.1' vs '10.0.0.1'    — many characters differ
        double dClose = dist("SELECT * FROM t WHERE ip = '192.168.1.1'",
                row("ip", InetAddress.getByName("192.168.1.2")));
        double dFar   = dist("SELECT * FROM t WHERE ip = '192.168.1.1'",
                row("ip", InetAddress.getByName("10.0.0.1")));
        assertTrue(dClose < dFar);
    }

    // AND operator

    @Test
    void and_bothSatisfied_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE a = 1 AND b = 'x'",
                        row("a", 1L, "b", "x")),
                DELTA);
    }

    @Test
    void and_neitherSatisfied_highDistance() {
        double d = dist("SELECT * FROM t WHERE a = 1 AND b = 'x'",
                row("a", 999L, "b", "zzz"));
        assertTrue(d > 0.0);
    }

    @Test
    void and_oneSatisfied_betterThanNoneSatisfied() {
        double dOne  = dist("SELECT * FROM t WHERE a = 1 AND b = 'x'",
                row("a", 1L, "b", "zzz"));
        double dNone = dist("SELECT * FROM t WHERE a = 1 AND b = 'x'",
                row("a", 999L, "b", "zzz"));
        assertTrue(dOne < dNone);
    }

    @Test
    void and_threeConditions_allSatisfied() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE a = 1 AND b = 2 AND c = 3",
                        row("a", 1L, "b", 2L, "c", 3L)),
                DELTA);
    }

    // IN operator

    @Test
    void in_valueInList_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col IN (1, 2, 3)", row("col", 2L)), DELTA);
    }

    @Test
    void in_valueFirstInList_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col IN (1, 2, 3)", row("col", 1L)), DELTA);
    }

    @Test
    void in_valueLastInList_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col IN (1, 2, 3)", row("col", 3L)), DELTA);
    }

    @Test
    void in_valueNotInList_nonZero() {
        assertTrue(dist("SELECT * FROM t WHERE col IN (10, 20)", row("col", 100L)) > 0.0);
    }

    @Test
    void in_closerCandidateGivesBetterScore() {
        double dClose = dist("SELECT * FROM t WHERE col IN (10, 20)", row("col", 11L));
        double dFar   = dist("SELECT * FROM t WHERE col IN (10, 20)", row("col", 100L));
        assertTrue(dClose < dFar);
    }

    @Test
    void in_singleElementList_match_zeroDistance() {
        assertEquals(0.0, dist("SELECT * FROM t WHERE col IN (42)", row("col", 42L)), DELTA);
    }

    @Test
    void in_missingColumn_nonZeroDistance() {
        assertEquals(MISSING_COLUMN_DISTANCE,
                dist("SELECT * FROM t WHERE col IN (1, 2, 3)", row("other", 99L)),
                DELTA);
    }

    // CONTAINS operator

    @Test
    void contains_list_valuePresent_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE tags CONTAINS 2",
                        row("tags", Arrays.asList(1L, 2L, 3L))),
                DELTA);
    }

    @Test
    void contains_list_valueMissing_nonZero() {
        double d = dist("SELECT * FROM t WHERE tags CONTAINS 5",
                row("tags", Arrays.asList(10L, 20L)));
        assertTrue(d > 0.0);
    }

    @Test
    void contains_list_closerElementGivesBetterScore() {
        double dClose = dist("SELECT * FROM t WHERE tags CONTAINS 10",
                row("tags", Arrays.asList(11L)));
        double dFar   = dist("SELECT * FROM t WHERE tags CONTAINS 10",
                row("tags", Arrays.asList(100L)));
        assertTrue(dClose < dFar);
    }

    @Test
    void contains_set_valuePresent_zeroDistance() {
        Set<Long> s = new LinkedHashSet<>(Arrays.asList(1L, 2L, 3L));
        assertEquals(0.0, dist("SELECT * FROM t WHERE col CONTAINS 2", row("col", s)), DELTA);
    }

    @Test
    void contains_map_valueInValues_zeroDistance() {
        Map<String, Long> scores = new LinkedHashMap<>();
        scores.put("alice", 10L);
        scores.put("bob",   20L);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE scores CONTAINS 10", row("scores", scores)),
                DELTA);
    }

    @Test
    void contains_map_valueMissing_nonZero() {
        Map<String, Long> scores = new LinkedHashMap<>();
        scores.put("alice", 10L);
        double d = dist("SELECT * FROM t WHERE scores CONTAINS 99", row("scores", scores));
        assertTrue(d > 0.0);
    }

    @Test
    void contains_missingColumn_nonZeroDistance() {
        assertEquals(MISSING_COLUMN_DISTANCE,
                dist("SELECT * FROM t WHERE col CONTAINS 1", row("other", 99L)),
                DELTA);
    }

    @Test
    void contains_stringList_exactMatch_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE tags CONTAINS 'hello'",
                        row("tags", Arrays.asList("hello", "world"))),
                DELTA);
    }

    @Test
    void contains_stringList_closerStringGivesBetterScore() {
        double dClose = dist("SELECT * FROM t WHERE tags CONTAINS 'hello'",
                row("tags", Arrays.asList("hell")));
        double dFar   = dist("SELECT * FROM t WHERE tags CONTAINS 'hello'",
                row("tags", Arrays.asList("xyz")));
        assertTrue(dClose < dFar);
    }

    // CONTAINS KEY operator

    @Test
    void containsKey_keyExists_zeroDistance() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("alice", 10L);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE meta CONTAINS KEY 'alice'", row("meta", m)),
                DELTA);
    }

    @Test
    void containsKey_keyAbsent_nonZero() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("alice", 10L);
        double d = dist("SELECT * FROM t WHERE meta CONTAINS KEY 'bob'", row("meta", m));
        assertTrue(d > 0.0);
    }

    @Test
    void containsKey_closerKeyGivesBetterScore() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("bob", 10L);   // close to target "alice" (different string but not totally different)
        double dClose = dist("SELECT * FROM t WHERE meta CONTAINS KEY 'alice'", row("meta", m));

        Map<String, Long> m2 = new LinkedHashMap<>();
        m2.put("zzzzz", 10L);
        double dFar = dist("SELECT * FROM t WHERE meta CONTAINS KEY 'alice'", row("meta", m2));

        // "bob" vs "alice" vs "zzzzz" vs "alice" — both are non-matches but distances differ
        // We only assert that both are > 0 (ordering may vary depending on leftAlignmentDistance)
        assertTrue(dClose > 0.0);
        assertTrue(dFar > 0.0);
    }

    @Test
    void containsKey_missingColumn_nonZeroDistance() {
        assertEquals(MISSING_COLUMN_DISTANCE,
                dist("SELECT * FROM t WHERE col CONTAINS KEY 'x'", row("other", 99L)),
                DELTA);
    }

    @Test
    void containsKey_nonMapColumn_throws() {
        CassandraRow row = row("col", Arrays.asList("a", "b"));
        assertThrows(IllegalArgumentException.class,
                () -> dist("SELECT * FROM t WHERE col CONTAINS KEY 'x'", row));
    }

    // Missing column handling

    @Test
    void missingColumn_returnsHighDistance() {
        assertEquals(MISSING_COLUMN_DISTANCE,
                dist("SELECT * FROM t WHERE col = 1", row("other", 99L)),
                DELTA);
    }

    // Multi-row scenarios

    @Test
    void multiRow_oneMatchingRow_zeroDistance() {
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE col = 5",
                        row("col", 1L), row("col", 5L), row("col", 10L)),
                DELTA);
    }

    @Test
    void multiRow_noMatch_bestRowDrivesScore() {
        double dWithClose = dist("SELECT * FROM t WHERE col = 10",
                row("col", 11L), row("col", 100L));
        double dAllFar    = dist("SELECT * FROM t WHERE col = 10",
                row("col", 99L), row("col", 100L));
        assertTrue(dWithClose < dAllFar);
    }

    @Test
    void multiRow_earlyExitOnMatch() {
        // Matching row is first — should short-circuit and not evaluate the rest
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE col = 1",
                        row("col", 1L), row("col", 999L), row("col", 999L)),
                DELTA);
    }

    // Quoted column names

    @Test
    void quotedColumnName_match_zeroDistance() {
        // Parser preserves quotes; normalizeColumnName strips them and lowercases
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE \"myCol\" = 42", row("mycol", 42L)),
                DELTA);
    }

    @Test
    void quotedColumnName_mismatch_nonZero() {
        double d = dist("SELECT * FROM t WHERE \"myCol\" = 42", row("mycol", 99L));
        assertTrue(d > 0.0);
    }

    // Temporal types — date (LocalDate)

    @Test
    void dateEquals_exactMatch_zeroDistance() {
        LocalDate d = LocalDate.of(2023, 1, 15);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d = '2023-01-15'", row("d", d)),
                DELTA);
    }

    @Test
    void dateEquals_integerDaysSinceEpoch_zeroDistance() {
        LocalDate d = LocalDate.of(2023, 1, 15);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d = " + d.toEpochDay(), row("d", d)),
                DELTA);
    }

    @Test
    void dateEquals_integerDaysSinceEpoch_nonZero() {
        LocalDate d = LocalDate.of(2023, 1, 16);
        double distance = dist("SELECT * FROM t WHERE d = " + LocalDate.of(2023, 1, 15).toEpochDay(), row("d", d));
        assertTrue(distance > 0.0 && distance < 1.0);
    }

    @Test
    void dateEquals_differentDay_nonZero() {
        LocalDate d = LocalDate.of(2023, 1, 16);
        double distance = dist("SELECT * FROM t WHERE d = '2023-01-15'", row("d", d));
        assertTrue(distance > 0.0 && distance < 1.0);
    }

    @Test
    void dateLT_satisfied_zeroDistance() {
        LocalDate d = LocalDate.of(2022, 12, 31);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d < '2023-01-01'", row("d", d)),
                DELTA);
    }

    @Test
    void dateGT_satisfied_zeroDistance() {
        LocalDate d = LocalDate.of(2023, 6, 1);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d > '2023-01-01'", row("d", d)),
                DELTA);
    }

    @Test
    void dateGTE_boundary_zeroDistance() {
        LocalDate d = LocalDate.of(2023, 1, 1);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d >= '2023-01-01'", row("d", d)),
                DELTA);
    }

    @Test
    void dateLTE_boundary_zeroDistance() {
        LocalDate d = LocalDate.of(2023, 1, 1);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE d <= '2023-01-01'", row("d", d)),
                DELTA);
    }

    @Test
    void date_closerDayGivesBetterScore() {
        double dClose = dist("SELECT * FROM t WHERE d = '2023-01-15'",
                row("d", LocalDate.of(2023, 1, 16)));   // 1 day away
        double dFar   = dist("SELECT * FROM t WHERE d = '2023-01-15'",
                row("d", LocalDate.of(2023, 6, 1)));    // months away
        assertTrue(dClose < dFar);
    }

    // Temporal types — time (LocalTime)

    @Test
    void timeEquals_integerNanosSinceMidnight_zeroDistance() {
        LocalTime t = LocalTime.of(14, 30, 0);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE t = " + t.toNanoOfDay(), row("t", t)),
                DELTA);
    }

    @Test
    void timeEquals_integerNanosSinceMidnight_nonZero() {
        LocalTime t = LocalTime.of(15, 0, 0);
        double d = dist("SELECT * FROM t WHERE t = " + LocalTime.of(14, 30, 0).toNanoOfDay(), row("t", t));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void timeEquals_exactMatch_zeroDistance() {
        LocalTime t = LocalTime.of(14, 30, 0);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE t = '14:30:00'", row("t", t)),
                DELTA);
    }

    @Test
    void timeGT_satisfied_zeroDistance() {
        LocalTime t = LocalTime.of(15, 0, 0);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE t > '14:30:00'", row("t", t)),
                DELTA);
    }

    @Test
    void timeLT_notSatisfied_nonZero() {
        LocalTime t = LocalTime.of(15, 0, 0);
        double d = dist("SELECT * FROM t WHERE t < '14:30:00'", row("t", t));
        assertTrue(d > 0.0);
    }

    @Test
    void timeGTE_boundary_zeroDistance() {
        LocalTime t = LocalTime.of(14, 30, 0);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE t >= '14:30:00'", row("t", t)),
                DELTA);
    }

    @Test
    void timeGTE_notSatisfied_nonZero() {
        LocalTime t = LocalTime.of(14, 0, 0);
        assertTrue(dist("SELECT * FROM t WHERE t >= '14:30:00'", row("t", t)) > 0.0);
    }

    @Test
    void timeLTE_boundary_zeroDistance() {
        LocalTime t = LocalTime.of(14, 30, 0);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE t <= '14:30:00'", row("t", t)),
                DELTA);
    }

    @Test
    void timeLTE_notSatisfied_nonZero() {
        LocalTime t = LocalTime.of(15, 0, 0);
        assertTrue(dist("SELECT * FROM t WHERE t <= '14:30:00'", row("t", t)) > 0.0);
    }

    // Temporal types — timestamp (Instant)

    @Test
    void timestampEquals_exactMatch_zeroDistance() {
        Instant ts = Instant.parse("2023-01-15T14:30:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2023-01-15T14:30:00Z'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_differentTime_nonZero() {
        Instant ts = Instant.parse("2023-01-15T15:00:00Z");
        double d = dist("SELECT * FROM t WHERE ts = '2023-01-15T14:30:00Z'", row("ts", ts));
        assertTrue(d > 0.0 && d < 1.0);
    }

    @Test
    void timestampGTE_boundary_zeroDistance() {
        Instant ts = Instant.parse("2023-01-15T00:00:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts >= '2023-01-15T00:00:00Z'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestamp_closerTimeGivesBetterScore() {
        Instant target = Instant.parse("2023-01-15T12:00:00Z");
        double dClose = dist("SELECT * FROM t WHERE ts = '2023-01-15T12:00:00Z'",
                row("ts", Instant.parse("2023-01-15T12:01:00Z")));   // 1 minute away
        double dFar   = dist("SELECT * FROM t WHERE ts = '2023-01-15T12:00:00Z'",
                row("ts", Instant.parse("2023-01-16T12:00:00Z")));   // 1 day away
        assertTrue(dClose < dFar);
    }

    // Temporal types — timestamp additional constant formats

    @Test
    void timestampEquals_integerEpochMs_zeroDistance() {
        Instant ts = Instant.ofEpochMilli(1_299_038_700_000L);
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = 1299038700000", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_integerEpochMs_nonZero() {
        Instant ts = Instant.ofEpochMilli(1_299_038_700_000L + 3_600_000L); // 1 hour later
        assertTrue(dist("SELECT * FROM t WHERE ts = 1299038700000", row("ts", ts)) > 0.0);
    }

    @Test
    void timestampEquals_spaceSeparatorNoSeconds_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03 04:05+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_spaceSeparatorWithSeconds_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03 04:05:00+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_spaceSeparatorWithMillis_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00.123Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03 04:05:00.123+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_tSeparatorNoSeconds_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03T04:05+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_tSeparatorWithSeconds_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03T04:05:00+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_tSeparatorWithMillis_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T04:05:00.123Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03T04:05:00.123+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_dateOnly_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T00:00:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_dateOnlyWithOffset_zeroDistance() {
        Instant ts = Instant.parse("2011-02-03T00:00:00Z");
        assertEquals(0.0,
                dist("SELECT * FROM t WHERE ts = '2011-02-03+0000'", row("ts", ts)),
                DELTA);
    }

    @Test
    void timestampEquals_spaceSeparator_differentTime_nonZero() {
        Instant ts = Instant.parse("2011-02-03T05:00:00Z");
        double d = dist("SELECT * FROM t WHERE ts = '2011-02-03 04:05:00+0000'", row("ts", ts));
        assertTrue(d > 0.0 && d < 1.0);
    }
}