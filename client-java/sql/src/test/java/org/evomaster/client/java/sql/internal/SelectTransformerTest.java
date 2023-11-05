package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.sql.internal.SelectTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectTransformerTest {

    @Test
    public void testCount(){

        String select = "select count(*) as n from Foo f where f.id=1";

        String withFields =  SelectTransformer.addFieldsToSelect(select);
        String withoutConstraints = SelectTransformer.removeConstraints(withFields);
        String withoutOperations = SelectTransformer.removeOperations(withoutConstraints);

        assertFalse(withoutOperations.contains("where"));
        assertTrue(withoutOperations.contains("id"));
        assertFalse(withoutOperations.contains("count"));
    }

    @Test
    public void testGroupBy(){

        String select = "select count(x), y from Foo f group by y";

        String withFields =  SelectTransformer.addFieldsToSelect(select);
        String withoutConstraints = SelectTransformer.removeConstraints(withFields);
        String withoutOperations = SelectTransformer.removeOperations(withoutConstraints);

        assertFalse(withoutOperations.contains("where"));
        assertTrue(withoutOperations.contains("y"));
        assertFalse(withoutOperations.contains("count"));
        assertFalse(withoutOperations.contains("group"));
    }


    @Test
    public void testAddFields() {
        String select = "select f.x from Foo f where f.y=5";

        String enh = SelectTransformer.addFieldsToSelect(select);
        String res = SelectTransformer.removeConstraints(enh);

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

        String res = SelectTransformer.removeConstraints(select).toLowerCase();

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

        String res = SelectTransformer.removeConstraints(select.toLowerCase());

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
                SelectTransformer.removeConstraints("select * from"));
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

        String res = SelectTransformer.removeConstraints(sql);

        assertEquivalent(sql, res);
    }

    @Test
    public void testRemoveWhere() {

        String base = "select a from Foo ";
        String sql = base + " where a=5";

        String res = SelectTransformer.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testRemoveWithLimit() {

        String base = "select a from Foo ";
        String sql = base + " where a=5 limit 1";

        String res = SelectTransformer.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testRemoveWhere_aliases() {

        String base = "select t.a as x, t.b as y from Foo t";
        String sql = base + " where x=5 and y=8";

        String res = SelectTransformer.removeConstraints(sql);

        assertEquivalent(base, res);
    }

    @Test
    public void testPatioApiIssue(){

        String select = "SELECT v.* FROM voting v, groups g WHERE v.expired = false AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' AND v.group_id = g.id";

        String res = SelectTransformer.addFieldsToSelect(select);

        //FIXME: should make sure that the fields of groups should be added
//        assertFalse(false);
    }
}