package org.evomaster.client.java.sql.distance.advanced.select_query;

import net.sf.jsqlparser.expression.Expression;
import org.junit.Test;

import static org.evomaster.client.java.sql.distance.advanced.select_query.SelectQuery.createSelectQuery;
import static org.junit.Assert.*;

public class SelectQueryTest {

    @Test
    public void testIsRestricted() { //Where clause is present
        SelectQuery query = createSelectQuery("SELECT * FROM customers");
        assertFalse(query.isRestricted());
    }
    @Test
    public void testIsRestricted2() { //Where clause is not present
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        assertTrue(query.isRestricted());
    }

    @Test
    public void testUnrestrict() { //Simplest case (removes where)
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        SelectQuery queryWithoutWhere = query.unrestrict();
        assertEquals("SELECT * FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testUnrestrict2() { //Maintains join
        SelectQuery query = createSelectQuery("SELECT * FROM customers JOIN orders ON orders.client_id = customers.id WHERE name = 'john' AND product = 'guitar'");
        SelectQuery queryWithoutWhere = query.unrestrict();
        assertEquals("SELECT * FROM customers JOIN orders ON orders.client_id = customers.id", queryWithoutWhere.toString());
    }

    @Test
    public void testUnrestrict3() { //Erases columns and adds * as field
        SelectQuery query = createSelectQuery("SELECT age FROM customers WHERE age > 20");
        SelectQuery queryWithoutWhere = query.unrestrict();
        assertEquals("SELECT * FROM customers", queryWithoutWhere.toString());
    }

    @Test
    public void testGetWhere() {
        SelectQuery query = createSelectQuery("SELECT * FROM customers WHERE age > 20");
        Expression where = query.getWhere();
        assertNotNull(where);
        assertEquals("age > 20", where.toString());
    }
}
