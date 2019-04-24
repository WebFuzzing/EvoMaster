package org.evomaster.client.java.controller.internal.db;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class ColumnTableAnalyzerTest {

    @Test
    public void testReadAllFromSingleTable(){

        String select = "select *  from Foo";

        Map<String, Set<String>> data = ColumnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(1, data.size());

        Set<String> columns = data.get("Foo");

        assertEquals(1, columns.size());
        assertTrue(columns.contains("*"));
    }


    @Test
    public void testReadFromJoinedTables(){

        String select = "SELECT Orders.OrderID, Customers.CustomerName, Orders.OrderDate" +
                " FROM Orders " +
                " INNER JOIN Customers ON Orders.CustomerID=Customers.CustomerID;";

        Map<String, Set<String>> data = ColumnTableAnalyzer.getSelectReadDataFields(select);

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
}