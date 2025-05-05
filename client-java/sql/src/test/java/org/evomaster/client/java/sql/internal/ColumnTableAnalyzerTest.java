package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class ColumnTableAnalyzerTest {


    @Test
    public void testInsertWithQualifier() {

        String sql = "insert into Bar.Foo (x) values (42)";

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(new DbInfoDto(), Collections.emptySet());
        Map.Entry<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getInsertedDataFields(sql);

        assertEquals(new SqlTableId("Bar.Foo"), data.getKey());
        assertEquals(1, data.getValue().size());
        assertTrue(data.getValue().contains(new SqlColumnId("x")));
    }


    @Test
    public void testInsertInSimpleTable() {

        String sql = "insert into Foo (x) values (42)";

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(new DbInfoDto(), Collections.emptySet());
        Map.Entry<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getInsertedDataFields(sql);

        assertEquals(new SqlTableId("Foo"), data.getKey());
        assertEquals(1, data.getValue().size());
        assertTrue(data.getValue().contains(new SqlColumnId("x")));

    }

    @Test
    public void testUpdateInSimpleTable() {

        String sql = "update Foo set x=42";

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(new DbInfoDto(), Collections.emptySet());
        Map.Entry<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getUpdatedDataFields(sql);

        assertEquals(new SqlTableId("Foo"), data.getKey());
        assertEquals(1, data.getValue().size());
        assertTrue(data.getValue().contains(new SqlColumnId("x")));
    }


    @Test
    public void testDeleteSimpleTable() {

        String sql = "delete from Foo";

        DbInfoDto schema = new DbInfoDto();
        TableDto fooTableDto = new TableDto();
        fooTableDto.name = "Foo";
        schema.tables.add(fooTableDto);
        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        SqlTableId deletedTableId = columnTableAnalyzer.getDeletedTable(sql);

        assertNotNull(deletedTableId);
        assertEquals(new SqlTableId("Foo"), deletedTableId);
    }

    @Test
    public void testDeleteWithQualifier() {

        String sql = "delete from v1.Foo";

        DbInfoDto schema = new DbInfoDto();
        TableDto tableDto = new TableDto();
        tableDto.name = "v1.Foo";
        schema.tables.add(tableDto);

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        SqlTableId deletedTableId = columnTableAnalyzer.getDeletedTable(sql);

        assertNotNull(deletedTableId);
        assertEquals(new SqlTableId("v1.Foo"), deletedTableId);
    }


    @Test
    public void testSelectReadAllFromSingleTable() {

        String select = "SELECT * FROM Foo";

        ColumnDto idColumn = new ColumnDto();
        idColumn.name = "id";
        idColumn.table = "Foo";

        ColumnDto nameColumn = new ColumnDto();
        nameColumn.name = "name";
        nameColumn.table = "Foo";

        ColumnDto descriptionColumn = new ColumnDto();
        descriptionColumn.name = "description";
        descriptionColumn.table = "Foo";

        TableDto tableDto = new TableDto();
        tableDto.name = "Foo";
        tableDto.columns.add(idColumn);
        tableDto.columns.add(nameColumn);
        tableDto.columns.add(descriptionColumn);

        DbInfoDto schema = new DbInfoDto();
        schema.tables.add(tableDto);

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        Map<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(1, data.size());
        Set<SqlColumnId> columns = data.get(new SqlTableId("Foo"));

        assertEquals(3, columns.size());
        assertTrue(columns.contains(new SqlColumnId("id")));
        assertTrue(columns.contains(new SqlColumnId("name")));
        assertTrue(columns.contains(new SqlColumnId("description")));
    }


    @Test
    public void testSelectReadFromJoinedTables() {

        String select = "SELECT Orders.OrderID, Customers.CustomerName, Orders.OrderDate" +
                " FROM Orders " +
                " INNER JOIN Customers ON Orders.CustomerID=Customers.CustomerID;";

        final DbInfoDto schema = buildSchema();

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        Map<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(2, data.size());

        final Set<SqlColumnId> ordersColumns = data.get(new SqlTableId("Orders"));

        assertEquals(3, ordersColumns.size());
        assertTrue(ordersColumns.contains(new SqlColumnId("OrderID")));
        assertTrue(ordersColumns.contains(new SqlColumnId("OrderDate")));
        assertTrue(ordersColumns.contains(new SqlColumnId("CustomerID")));

        final Set<SqlColumnId> customersColumns = data.get(new SqlTableId("Customers"));

        assertEquals(2, customersColumns.size());
        assertTrue(customersColumns.contains(new SqlColumnId(("CustomerName"))));
        assertTrue(customersColumns.contains(new SqlColumnId(("CustomerID"))));
    }

    private static @NotNull DbInfoDto buildSchema() {
        ColumnDto orderIdColumn = new ColumnDto();
        orderIdColumn.name = "OrderID";
        orderIdColumn.table = "Orders";

        ColumnDto orderDateColumn = new ColumnDto();
        orderDateColumn.name = "OrderDate";
        orderDateColumn.table = "Orders";

        ColumnDto ordersCustomerIdColumn = new ColumnDto();
        ordersCustomerIdColumn.name = "CustomerID";
        ordersCustomerIdColumn.table = "Orders";

        TableDto ordersTable = new TableDto();
        ordersTable.name = "Orders";
        ordersTable.columns.add(orderIdColumn);
        ordersTable.columns.add(orderDateColumn);
        ordersTable.columns.add(ordersCustomerIdColumn);

        ColumnDto customerNameColumn = new ColumnDto();
        customerNameColumn.name = "CustomerName";
        customerNameColumn.table = "Customers";

        ColumnDto customerIdColumn = new ColumnDto();
        customerIdColumn.name = "CustomerID";
        customerIdColumn.table = "Customers";


        TableDto customersTable = new TableDto();
        customersTable.name = "Customers";
        customersTable.columns.add(customerNameColumn);
        customersTable.columns.add(customerIdColumn);

        DbInfoDto schema = new DbInfoDto();
        schema.tables.add(ordersTable);
        schema.tables.add(customersTable);
        return schema;
    }

    @Test
    public void testSelectSubsetOfColumns() {

        String select = "SELECT OrderID FROM Orders";

        final DbInfoDto schema = buildSchema();

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        Map<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(1, data.size());

        final Set<SqlColumnId> ordersColumns = data.get(new SqlTableId("Orders"));

        assertEquals(1, ordersColumns.size());
        assertTrue(ordersColumns.contains(new SqlColumnId("OrderID")));
    }

    @Test
    public void testSelectAllColumns() {

        String select = "SELECT * FROM Orders";

        final DbInfoDto schema = buildSchema();

        ColumnTableAnalyzer columnTableAnalyzer = new ColumnTableAnalyzer(schema, Collections.emptySet());
        Map<SqlTableId, Set<SqlColumnId>> data = columnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(1, data.size());

        final Set<SqlColumnId> ordersColumns = data.get(new SqlTableId("Orders"));

        assertEquals(3, ordersColumns.size());
        assertTrue(ordersColumns.contains(new SqlColumnId("OrderID")));
        assertTrue(ordersColumns.contains(new SqlColumnId("OrderDate")));
        assertTrue(ordersColumns.contains(new SqlColumnId("CustomerID")));
    }
}
