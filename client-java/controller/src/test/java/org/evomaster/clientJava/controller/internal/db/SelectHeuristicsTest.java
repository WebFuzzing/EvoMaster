package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.controller.db.QueryResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SelectHeuristicsTest {



    @Test
    public void testReadFromJoinedTables(){

        String select = "SELECT Orders.OrderID, Customers.CustomerName, Orders.OrderDate" +
                " FROM Orders " +
                " INNER JOIN Customers ON Orders.CustomerID=Customers.CustomerID;";

        Map<String, Set<String>> data = SelectHeuristics.getReadDataFields(select);

        assertEquals(2, data.size());

        Set<String> columns = data.get("Orders");
        //FIXME: once supporting actual fields instead of *
        assertEquals(1, columns.size());
        assertTrue(columns.contains("*"));

        columns = data.get("Customers");
        //FIXME: once supporting actual fields instead of *
        assertEquals(1, columns.size());
        assertTrue(columns.contains("*"));
    }


    @Test
    public void testReadAllFromSingleTable(){

        String select = "select *  from Foo";

        Map<String, Set<String>> data = SelectHeuristics.getReadDataFields(select);

        assertEquals(1, data.size());

        Set<String> columns = data.get("Foo");

        assertEquals(1, columns.size());
        assertTrue(columns.contains("*"));
    }

    @Test
    public void testCount(){

        String select = "select count(*) as n from Foo f where f.id=1";

        String withFields =  SelectHeuristics.addFieldsToSelect(select);
        String withoutConstraints = SelectHeuristics.removeConstraints(withFields);
        String withoutOperations = SelectHeuristics.removeOperations(withoutConstraints);

        assertFalse(withoutOperations.contains("where"));
        assertTrue(withoutOperations.contains("id"));
        assertFalse(withoutOperations.contains("count"));
    }


    @Test
    public void testAddFields() {
        String select = "select f.x from Foo f where f.y=5";

        String enh = SelectHeuristics.addFieldsToSelect(select);
        String res = SelectHeuristics.removeConstraints(enh);

        assertTrue(res.toLowerCase().contains("f.x"));
        assertTrue(res.toLowerCase().contains("f.y"));
        assertFalse(res.toLowerCase().contains("where"));
    }


    @Test
    public void testRemoveWhenUnion() {

        int x = 15;
        int y = 72;

        String select = "select x from Foo where x=" + x +
                " UNION ALL " +
                "select z from Bar where y=" + y;

        String res = SelectHeuristics.removeConstraints(select).toLowerCase();

        assertTrue(res.contains("foo"));
        assertTrue(res.contains("bar"));
        assertTrue(res.contains("union"));
        assertTrue(res.contains("all"));

        assertFalse(res.contains("" + x));
        assertFalse(res.contains("" + y));
    }

    @Test
    public void testRemoveNested() {

        String select = "select t.x, t.y from (select z as x, 1 as y from Foo where z<10) t where x>3";

        String res = SelectHeuristics.removeConstraints(select.toLowerCase());

        assertTrue(res.contains("foo"));
        assertTrue(res.contains("x"));
        assertTrue(res.contains("y"));
        assertTrue(res.contains("z"));

        assertFalse(res.contains("3"));

        /*
            TODO: This is a bit tricky. Likely we would need
            to consider each nested SELECT as independent,
            with their own heuristics calculations
         */
        assertTrue(res.contains("10"));
    }


    @Test
    public void testRemoveInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                SelectHeuristics.removeConstraints("select * from"));
    }

    private void assertEquivalent(String a, String b) {
        if (a == null && b == null) {
            return;
        }
        assertEquals(a.trim().toLowerCase(), b.trim().toLowerCase());
    }

    @Test
    public void testRemoveSame() {

        String sql = "select * from Foo";

        String res = SelectHeuristics.removeConstraints(sql);

        assertEquivalent(sql, res);
    }

    @Test
    public void testRemoveWhere() {

        String base = "select a from Foo ";
        String sql = base + " where a=5";

        String res = SelectHeuristics.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testRemoveWithLimit() {

        String base = "select a from Foo ";
        String sql = base + " where a=5 limit 1";

        String res = SelectHeuristics.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testRemoveWhere_aliases() {

        String base = "select t.a as x, t.b as y from Foo t";
        String sql = base + " where x=5 and y=8";

        String res = SelectHeuristics.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testEmpty() {

        String sql = "select x from Foo";

        QueryResult data = new QueryResult(Arrays.asList("x"));

        double dist = SelectHeuristics.computeDistance(sql, data);
        assertTrue(dist > 0);

        DataRow row = new DataRow("x", "9");
        data.addRow(row);

        dist = SelectHeuristics.computeDistance(sql, data);
        assertEquals(0d, dist);
    }

    private void checkIncreasingTillCovered(String name,
                                            List<Object> values,
                                            Object solution,
                                            String sql) {

        QueryResult data = new QueryResult(Arrays.asList(name));

        double prev = -1;

        for (Object val : values) {
            data.addRow(new DataRow(name, val));
            double dist = SelectHeuristics.computeDistance(sql, data);
            assertTrue(dist > 0);
            if (prev >= 0) {
                assertTrue(dist < prev, "dist=" + dist + " , previous=" + prev);
            }
            prev = dist;
        }

        data.addRow(new DataRow(name, solution));
        double target = SelectHeuristics.computeDistance(sql, data);
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


}