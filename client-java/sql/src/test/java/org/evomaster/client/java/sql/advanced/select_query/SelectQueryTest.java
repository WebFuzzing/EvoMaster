package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.expression.Expression;
import org.junit.Test;

import java.util.List;

import static org.evomaster.client.java.sql.advanced.select_query.SelectQuery.createSelectQuery;
import static org.junit.Assert.*;

public class SelectQueryTest {

    @Test
    public void testGetTables() { //Single table
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20 AND true AND EXISTS (SELECT * FROM orders WHERE customer_id = customers.id)");
        List<QueryTable> tables = query.getFromTables(true);
        assertEquals(1, tables.size());
        assertEquals("customers", tables.get(0).getName());
    }

    @Test
    public void testGetTables2() { //Join
        SelectQuery query = createSelectQuery("SELECT * FROM customers c JOIN orders");
        List<QueryTable> tables = query.getFromTables(true);
        assertEquals(2, tables.size());
        assertEquals("customers", tables.get(0).getName());
        assertEquals("c", tables.get(0).getAlias());
        assertEquals("orders", tables.get(1).getName());
    }

    @Test
    public void testGetTables3() { //Implicit join
        SelectQuery query = createSelectQuery("SELECT * FROM customers, orders");
        List<QueryTable> tables = query.getFromTables(true);
        assertEquals(2, tables.size());
        assertEquals("customers", tables.get(0).getName());
        assertEquals("orders", tables.get(1).getName());
    }

    @Test
    public void testGetTables4() { //Subquery
        SelectQuery query = createSelectQuery("SELECT * FROM (SELECT * FROM customers) c");
        List<QueryTable> tables = query.getFromTables(true);
        assertEquals(1, tables.size());
        assertEquals("customers", tables.get(0).getName());
        assertEquals("c", tables.get(0).getAlias());
    }

    @Test
    public void testIsPlain() { //Is plain select
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        assertTrue(query.isPlainSelect());
    }

    @Test
    public void testIsPlain2() { //Is not plain select
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20 " +
            "UNION SELECT * FROM customers where age < 30");
        assertFalse(query.isPlainSelect());
    }

    @Test
    public void testIsSetOperationList() { //Is set operation list
        SelectQuery query = createSelectQuery("SELECT * FROM a " +
            "UNION SELECT * FROM b INTERSECT SELECT * FROM c");
        assertTrue(query.isSetOperationList());
    }

    @Test
    public void testIsSetOperationList2() { //Is not set operation list
        SelectQuery query = createSelectQuery("SELECT * FROM a");
        assertFalse(query.isSetOperationList());
    }

    @Test
    public void testGetSetOperationList() { //Is set operation list
        SelectQuery query = createSelectQuery("SELECT * FROM a " +
            "UNION SELECT * FROM b INTERSECT SELECT * FROM c");
        assertEquals(3, query.getSetOperationSelects().size());
        assertEquals("SELECT * FROM a", query.getSetOperationSelects().get(0).toString());
        assertEquals("SELECT * FROM b", query.getSetOperationSelects().get(1).toString());
        assertEquals("SELECT * FROM c", query.getSetOperationSelects().get(2).toString());
    }

    @Test
    public void testIsUnion() { //Is UNION
        SelectQuery query = createSelectQuery("SELECT * FROM a UNION SELECT * FROM b");
        assertTrue(query.isUnion());
    }

    @Test
    public void testIsUnion2() { //Is UNION ALL
        SelectQuery query = createSelectQuery("SELECT * FROM a UNION ALL SELECT * FROM b");
        assertTrue(query.isUnion());
    }

    @Test
    public void testIsUnion3() { //Is not UNION
        SelectQuery query = createSelectQuery("SELECT * FROM a INTERSECT SELECT * FROM b");
        assertFalse(query.isUnion());
    }

    @Test
    public void testIsInnerJoin() { //Is INNER JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a INNER JOIN b ON a1 = b1");
        assertTrue(query.isInnerJoin());
    }

    @Test
    public void testIsInnerJoin2() { //Is INNER JOIN (implicit)
        SelectQuery query = createSelectQuery("SELECT * FROM a JOIN b ON a1 = b1");
        assertTrue(query.isInnerJoin());
    }

    @Test
    public void testIsInnerJoin3() { //Is not INNER JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a LEFT JOIN b ON a1 = b1");
        assertFalse(query.isInnerJoin());
    }

    @Test
    public void testIsFullJoin() { //Is FULL JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a FULL JOIN b ON a1 = b1");
        assertTrue(query.isFullJoin());
    }

    @Test
    public void testIsFullJoin2() { //Is not FULL JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a LEFT JOIN b ON a1 = b1");
        assertFalse(query.isFullJoin());
    }

    @Test
    public void testIsLeftJoin() { //Is LEFT JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a LEFT JOIN b ON a1 = b1");
        assertTrue(query.isLeftJoin());
    }

    @Test
    public void testIsLeftJoin2() { //Is not LEFT JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a RIGHT JOIN b ON a1 = b1");
        assertFalse(query.isLeftJoin());
    }

    @Test
    public void testIsRightJoin() { //Is RIGHT JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a RIGHT JOIN b ON a1 = b1");
        assertTrue(query.isRightJoin());
    }

    @Test
    public void testIsRightJoin2() { //Is not RIGHT JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a LEFT JOIN b ON a1 = b1");
        assertFalse(query.isRightJoin());
    }

    @Test
    public void testIsCrossJoin() { //Is CROSS JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a CROSS JOIN b ON a1 = b1");
        assertTrue(query.isCrossJoin());
    }

    @Test
    public void testIsCrossJoin2() { //Is CROSS JOIN (implicit)
        SelectQuery query = createSelectQuery("SELECT * FROM a JOIN b");
        assertTrue(query.isCrossJoin());
    }

    @Test
    public void testIsCrossJoin3() { //Is not CROSS JOIN
        SelectQuery query = createSelectQuery("SELECT * FROM a LEFT JOIN b ON a1 = b1");
        assertFalse(query.isCrossJoin());
    }

    @Test
    public void testGetJoinSelects() {
        SelectQuery query = createSelectQuery("SELECT a.a2 FROM a JOIN B JOIN (SELECT c1 FROM c WHERE c1 = 'c1 value') c " +
            "WHERE upper(a.a1) = 'upper a.a1 value'");
        List<SelectQuery> selects = query.getJoinSelects();
        assertEquals(3, selects.size());
        assertEquals("SELECT * FROM a", selects.get(0).toString());
        assertEquals("SELECT * FROM B", selects.get(1).toString());
        assertEquals("SELECT c1 FROM c WHERE c1 = 'c1 value'", selects.get(2).toString());
    }

    @Test
    public void testConvertToCrossJoin() {
        SelectQuery query = createSelectQuery("SELECT * FROM a INNER JOIN b ON a1 = b1 AND upper(a2) = 'upper a2 value'");
        SelectQuery select = query.convertToCrossJoin();
        assertEquals("SELECT *, upper(a2) FROM a CROSS JOIN b WHERE a1 = b1 AND upper(a2) = 'upper a2 value'", select.toString());
    }

    @Test
    public void testHasWhere() { //Where clause is present
        SelectQuery query = createSelectQuery("SELECT * FROM customers");
        assertFalse(query.hasWhere());
    }
    @Test
    public void testHasWhere2() { //Where clause is not present
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        assertTrue(query.hasWhere());
    }

    @Test
    public void testGetWhere() {
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        Expression where = query.getWhere();
        assertNotNull(where);
        assertEquals("age > 20", where.toString());
    }

    @Test
    public void testRemoveWhere() { //Simplest case (removes where)
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        SelectQuery queryWithoutWhere = query.removeWhere();
        assertEquals("SELECT * FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testRemoveWhere2() { //Maintains join
        SelectQuery query = createSelectQuery("SELECT * FROM customers JOIN orders ON orders.customer_id = customers.id WHERE name = 'john' AND product = 'guitar'");
        SelectQuery queryWithoutWhere = query.removeWhere();
        assertEquals("SELECT * FROM customers JOIN orders ON orders.customer_id = customers.id", queryWithoutWhere.toString());
    }

    @Test
    public void testRemoveWhere3() { //Erases columns and adds * as field
        SelectQuery query = createSelectQuery("SELECT age FROM customers WHERE age > 20");
        SelectQuery queryWithoutWhere = query.removeWhere();
        assertEquals("SELECT * FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testRemoveWhere4() { //Erases aggregation field and group by clause and adds * as field
        SelectQuery query = createSelectQuery("SELECT COUNT(*) FROM customers WHERE name = 'john' GROUP BY age");
        SelectQuery queryWithoutWhere = query.removeWhere();
        assertEquals("SELECT * FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testRemoveWhere5() { //Adds function as field along with *
        SelectQuery query = createSelectQuery("SELECT age FROM customers WHERE UPPER(name) = 'JOHN'");
        SelectQuery queryWithoutWhere = query.removeWhere();
        assertEquals("SELECT *, UPPER(name) FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testDelete() {
        SelectQuery query = createSelectQuery("DELETE FROM customers JOIN orders WHERE age = 25");
        assertEquals("SELECT * FROM customers JOIN orders WHERE age = 25", query.toString());
    }

    @Test
    public void testUpdate() {
        SelectQuery query = createSelectQuery("UPDATE customers SET age = 26 WHERE age = 25");
        assertEquals("SELECT * FROM customers WHERE age = 25", query.toString());
    }
}
