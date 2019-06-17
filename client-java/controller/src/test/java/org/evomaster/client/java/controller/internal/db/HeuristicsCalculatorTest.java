package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.db.DataRow;
import org.evomaster.client.java.controller.db.QueryResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class HeuristicsCalculatorTest {




    @Test
    public void testEmpty() {

        String sql = "select x from Foo";

        QueryResult data = new QueryResult(Arrays.asList("x"), "Foo");

        double dist = HeuristicsCalculator.computeDistance(sql, data);
        assertTrue(dist > 0);

        DataRow row = new DataRow("x", "9", "Foo");
        data.addRow(row);

        dist = HeuristicsCalculator.computeDistance(sql, data);
        assertEquals(0d, dist);
    }

    private void checkIncreasingTillCovered(String name,
                                            List<Object> values,
                                            Object solution,
                                            String sql) {

        QueryResult data = new QueryResult(Arrays.asList(name), "Foo");

        double prev = -1;

        for (Object val : values) {
            data.addRow(new DataRow(name, val, "Foo"));
            double dist = HeuristicsCalculator.computeDistance(sql, data);
            assertTrue(dist > 0);
            if (prev >= 0) {
                assertTrue(dist < prev, "dist=" + dist + " , previous=" + prev);
            }
            prev = dist;
        }

        data.addRow(new DataRow(name, solution, "Foo"));
        double target = HeuristicsCalculator.computeDistance(sql, data);
        assertTrue(target < prev);
        assertEquals(0d, target);
    }


    @Test
    public void testTrue() {
        String sql = "select a from Foo where x = true";

        checkIncreasingTillCovered("x", Arrays.asList(false), true, sql);
    }

    @Test
    public void testFalse() {
        String sql = "select a from Foo where x = false";

        checkIncreasingTillCovered("x", Arrays.asList(true), false, sql);
    }

    @Test
    public void testNotTrue() {
        String sql = "select a from Foo where x != true";

        checkIncreasingTillCovered("x", Arrays.asList(true), false, sql);
    }

    @Test
    public void testNotFalse() {
        String sql = "select a from Foo where x != FALSE";

        checkIncreasingTillCovered("x", Arrays.asList(false), true, sql);
    }

    @Test
    public void testWithParentheses() {

        String sql = "select a from Foo where x = (5)";

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, 6), 5, sql);
    }

    @Test
    public void testNegativeWithParentheses() {

        String sql = "select a from Foo where x = (-5)";

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, -7), -5, sql);
    }

    @Test
    public void testEqualInt() {

        String sql = "select x from Foo where x=5";

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, 6), 5, sql);
    }

    @Test
    public void testEqualToNull() {
        String sql = "select x from Foo where x = NULL";

        checkIncreasingTillCovered("x", Arrays.asList("foo"), null, sql);
    }

    @Test
    public void testIsNull() {
        String sql = "select x from Foo where x IS NULL";

        checkIncreasingTillCovered("x", Arrays.asList("foo"), null, sql);
    }

    @Test
    public void testIsNotNull() {
        String sql = "select x from Foo where x IS NOT NULL";

        List<Object> list = new ArrayList<>();
        list.add(null);

        checkIncreasingTillCovered("x", list, "foo", sql);
    }

    @Test
    public void testDifferentFromNull() {
        String sql = "select x from Foo where x != NULL";

        List<Object> list = new ArrayList<>();
        list.add(null);

        checkIncreasingTillCovered("x", list, "foo", sql);
    }

    @Test
    public void testInNumeric(){

        String sql = "select x from Foo where x IN (10, 20)";

        checkIncreasingTillCovered("x", Arrays.asList(-4, 6, 23, 12, 19), 10, sql);
    }

    @Test
    public void testInNumericWithParenthesis(){

        String sql = "select x from Foo where (x IN (10, 20))";

        checkIncreasingTillCovered("x", Arrays.asList(-4, 6, 23, 12, 19), 10, sql);
    }

    @Test
    public void testInStrings(){

        String sql = "select x from Foo where x IN ('a1', 'e5')";

        checkIncreasingTillCovered("x", Arrays.asList("z9", "z7", "c7", "c2", "b2", "b1"), "a1", sql);
    }

    @Test
    public void testNotInNumeric(){

        String sql = "select x from Foo where x Not IN (10, 20)";

        checkIncreasingTillCovered("x", Arrays.asList(10), 11, sql);
    }

    @Disabled("Need to handle sub-selects. Not so simple, as they might have their own WHEREs")
    @Test
    public void testInSelect(){
        String sql = "select * from Foo where 10 IN (select x from Foo)";

        checkIncreasingTillCovered("x", Arrays.asList(20, 15, 8), 10, sql);
    }


    @Test
    public void testEqualString() {

        String sql = "select t.bar as X from Foo t where X='abc123'";

        checkIncreasingTillCovered("x",
                Arrays.asList("a", "ab", "xxx123x", "xxx123", "axx123", "abc234"), "abc123", sql);
    }

    @Test
    public void testNotEqualString() {

        String sql = "select t.bar as X from Foo t where X!='foo'";

        checkIncreasingTillCovered("x", Arrays.asList("foo"), "blabla", sql);
    }

    @Test
    public void testNotEqual() {

        String sql = "select x from Foo where x != 5";

        checkIncreasingTillCovered("x", Arrays.asList(5), 6, sql);
    }

    @Test
    public void testGreaterThanEquals() {

        String sql = "select x from Foo where x >= 5";

        checkIncreasingTillCovered("x", Arrays.asList(-4, 2, 3), 5, sql);
    }

    @Test
    public void testGreaterThan() {

        String sql = "select x from Foo where x > 5";

        checkIncreasingTillCovered("x", Arrays.asList(-4, 2, 3, 5), 6, sql);
    }

    @Test
    public void testMinorThan() {

        String sql = "select x from Foo where x < 5";

        checkIncreasingTillCovered("x", Arrays.asList(10, 7, 6, 5), -2, sql);
    }

    @Test
    public void testMinorThanEquals() {

        String sql = "select x from Foo where x <= 5";

        checkIncreasingTillCovered("x", Arrays.asList(10, 7, 6), 5, sql);
    }


    @Test
    public void testAnd() {

        String sql = "select x from Foo where x > 5 and x < 10";

        checkIncreasingTillCovered("x", Arrays.asList(20, -1, 4), 7, sql);
    }

    @Test
    public void testOr() {

        String sql = "select x from Foo where x < 0 or x > 100";

        checkIncreasingTillCovered("x", Arrays.asList(50, 60, 20, 90, 5), -3, sql);
    }

    @Test
    public void testTimestamp(){

        String sql = "select x from Foo where x > '28-Feb-17'";

        checkIncreasingTillCovered("x", Arrays.asList(
                Timestamp.valueOf("1870-01-01 00:00:00"),
                Timestamp.valueOf("1900-01-01 00:00:00"),
                Timestamp.valueOf("2010-03-12 13:21:42"),
                Timestamp.valueOf("2017-02-27 00:00:00")
        ),
                Timestamp.valueOf("2017-03-01 00:00:00"),
                sql);
    }

    @Test
    public void testTimestampBetween(){

        String sql = "select x from Foo where x BETWEEN '28-Feb-17' AND '25-Mar-19'";

        checkIncreasingTillCovered("x", Arrays.asList(
                Timestamp.valueOf("1870-01-01 00:00:00"),
                Timestamp.valueOf("1900-01-01 00:00:00"),
                Timestamp.valueOf("2021-03-12 13:21:42"),
                Timestamp.valueOf("2016-02-27 00:00:00")
                ),
                Timestamp.valueOf("2018-03-01 00:00:00"),
                sql);
    }


    @Test
    public void testDeleteBase(){

        String sql = "delete from Foo where x=0";

        checkIncreasingTillCovered("x", Arrays.asList(10, -5, 2), 0, sql);
    }

    @Test
    public void testUpdateBase(){

        String sql = "update Foo set x=42 where x=0";

        checkIncreasingTillCovered("x", Arrays.asList(10, -5, 2), 0, sql);
    }
}