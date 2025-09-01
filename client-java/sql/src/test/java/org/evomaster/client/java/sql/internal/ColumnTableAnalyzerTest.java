package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;
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

        Map.Entry<SqlTableId, Set<SqlColumnId>> data = ColumnTableAnalyzer.getInsertedDataFields(sql);

        assertEquals(new SqlTableId("Bar.Foo"), data.getKey());
    }


    @Test
    public void testInsertInSimpleTable() {

        String sql = "insert into Foo (x) values (42)";

        Map.Entry<SqlTableId, Set<SqlColumnId>> data = ColumnTableAnalyzer.getInsertedDataFields(sql);

        assertEquals(new SqlTableId("Foo"), data.getKey());

    }

    @Test
    public void testUpdateInSimpleTable() {

        String sql = "update Foo set x=42";

        Map.Entry<SqlTableId, Set<SqlColumnId>> data = ColumnTableAnalyzer.getUpdatedDataFields(sql);

        assertEquals(new SqlTableId("Foo"), data.getKey());
        //TODO check on actual fields when implemented
    }


    @Test
    public void testDeleteSimpleTable() {

        String sql = "delete from Foo";

        DbInfoDto schema = new DbInfoDto();
        TableDto fooTableDto = new TableDto();
        fooTableDto.id = new TableIdDto();
        fooTableDto.id.name = "Foo";
        schema.tables.add(fooTableDto);
        SqlTableId deletedTableId = ColumnTableAnalyzer.getDeletedTable(sql);

        assertNotNull(deletedTableId);
        assertEquals(new SqlTableId("Foo"), deletedTableId);
    }

    @Test
    public void testDeleteWithQualifier() {

        String sql = "delete from v1.Foo";

        DbInfoDto schema = new DbInfoDto();
        TableDto tableDto = new TableDto();
        tableDto.id = new TableIdDto();
        tableDto.id.name = "v1.Foo";
        schema.tables.add(tableDto);

        SqlTableId deletedTableId = ColumnTableAnalyzer.getDeletedTable(sql);

        assertNotNull(deletedTableId);
        assertEquals(new SqlTableId("v1.Foo"), deletedTableId);
    }


    @Test
    public void testSelectReadAllFromSingleTable() {

        String select = "SELECT * FROM Foo";
        Map<SqlTableId, Set<SqlColumnId>> data = ColumnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(1, data.size());
        Set<SqlColumnId> columns = data.get(new SqlTableId("Foo"));

        assertEquals(1, columns.size());
        assertTrue(columns.contains(new SqlColumnId("*")));
    }


    @Test
    public void testSelectReadFromJoinedTables() {

        String select = "SELECT Orders.OrderID, Customers.CustomerName, Orders.OrderDate" +
                " FROM Orders " +
                " INNER JOIN Customers ON Orders.CustomerID=Customers.CustomerID;";

        Map<SqlTableId, Set<SqlColumnId>> data = ColumnTableAnalyzer.getSelectReadDataFields(select);

        assertEquals(2, data.size());

        final Set<SqlColumnId> ordersColumns = data.get(new SqlTableId("Orders"));

        //FIXME: once supporting actual fields instead of *
        assertEquals(1, ordersColumns.size());
        assertTrue(ordersColumns.contains(new SqlColumnId("*")));

        final Set<SqlColumnId> customersColumns = data.get(new SqlTableId("Customers"));

        //FIXME: once supporting actual fields instead of *
        assertEquals(1, customersColumns.size());
        assertTrue(customersColumns.contains(new SqlColumnId(("*"))));
    }



}
