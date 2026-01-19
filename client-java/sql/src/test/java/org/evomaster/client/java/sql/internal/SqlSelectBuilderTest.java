package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlSelectBuilderTest {

    @Test
    void testBuildSelectWithSingleColumn() {
        SqlTableId tableId = new SqlTableId(null, null, "users");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.H2, tableId, columnIds);

        assertEquals("SELECT id FROM users", result);
    }

    @Test
    void testBuildSelectWithMultipleColumns() {
        SqlTableId tableId = new SqlTableId(null, null, "employees");
        List<SqlColumnId> columnIds = Arrays.asList(
            new SqlColumnId("id"),
            new SqlColumnId("name"),
            new SqlColumnId("salary")
        );

        String result = SqlSelectBuilder.buildSelect(DatabaseType.POSTGRES, tableId, columnIds);

        assertEquals("SELECT id, name, salary FROM employees", result);
    }

    @Test
    void testBuildSelectWithNullColumnList() {
        SqlTableId tableId = new SqlTableId(null, null, "users");

        assertThrows(IllegalArgumentException.class, () -> {
            SqlSelectBuilder.buildSelect(DatabaseType.H2, tableId, null);
        });
    }

    @Test
    void testBuildSelectWithEmptyColumnList() {
        SqlTableId tableId = new SqlTableId(null, null, "users");
        List<SqlColumnId> columnIds = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> {
            SqlSelectBuilder.buildSelect(DatabaseType.H2, tableId, columnIds);
        });
    }

    @Test
    void testBuildSelectWithSchemaName() {
        SqlTableId tableId = new SqlTableId(null, "public", "customers");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("email"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.POSTGRES, tableId, columnIds);

        assertEquals("SELECT email FROM public.customers", result);
    }

    @Test
    void testBuildSelectWithCatalogAndSchema() {
        SqlTableId tableId = new SqlTableId("mydb", "public", "orders");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("order_id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.POSTGRES, tableId, columnIds);

        assertEquals("SELECT order_id FROM mydb.public.orders", result);
    }

    @Test
    void testBuildSelectMySQLWithCatalog() {
        SqlTableId tableId = new SqlTableId("mydb", "ignored_schema", "products");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("product_id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.MYSQL, tableId, columnIds);

        assertEquals("SELECT product_id FROM mydb.products", result);
    }

    @Test
    void testBuildSelectMySQLWithSchemaOnly() {
        SqlTableId tableId = new SqlTableId(null, "myschema", "categories");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("category_id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.MYSQL, tableId, columnIds);

        assertEquals("SELECT category_id FROM myschema.categories", result);
    }

    @Test
    void testBuildSelectMySQLWithNoCatalogOrSchema() {
        SqlTableId tableId = new SqlTableId(null, null, "inventory");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("item_id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.MYSQL, tableId, columnIds);

        assertEquals("SELECT item_id FROM inventory", result);
    }

    @Test
    void testBuildSelectWithCatalogOnly() {
        SqlTableId tableId = new SqlTableId("testdb", null, "logs");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("timestamp"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.H2, tableId, columnIds);

        assertEquals("SELECT timestamp FROM testdb.logs", result);
    }

    @Test
    void testBuildSelectMSSQLWithSchemaAndCatalog() {
        SqlTableId tableId = new SqlTableId("master", "dbo", "users");
        List<SqlColumnId> columnIds = Collections.singletonList(new SqlColumnId("user_id"));

        String result = SqlSelectBuilder.buildSelect(DatabaseType.MS_SQL_SERVER, tableId, columnIds);

        assertEquals("SELECT user_id FROM master.dbo.users", result);
    }

}
