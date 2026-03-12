package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlTableIdTest {

    @Test
    void invalidCatalogName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SqlTableId("my.catalog", "my_schema", "my_table"));
    }

    @Test
    void invalidSchemaName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SqlTableId("my_catalog", "my.schema", "my_table"));
    }

    @Test
    void invalidTableName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SqlTableId("my_catalog", "my_schema", "my.table"));
    }

    @Test
    void testBuildQualifiedTableName_Postgres() {
        DatabaseType db = DatabaseType.POSTGRES;

        assertEquals("table", new SqlTableId(null, null, "table").buildQualifiedTableName(db));
        assertEquals("schema.table", new SqlTableId(null, "schema", "table").buildQualifiedTableName(db));
        assertEquals("catalog.schema.table", new SqlTableId("catalog", "schema", "table").buildQualifiedTableName(db));
        assertEquals("catalog.table", new SqlTableId("catalog", null, "table").buildQualifiedTableName(db));
    }

    @Test
    void testBuildQualifiedTableName_MySQL() {
        DatabaseType db = DatabaseType.MYSQL;

        assertEquals("table", new SqlTableId(null, null, "table").buildQualifiedTableName(db));
        assertEquals("schema.table", new SqlTableId(null, "schema", "table").buildQualifiedTableName(db));
        assertEquals("catalog.table", new SqlTableId("catalog", "schema", "table").buildQualifiedTableName(db));
        assertEquals("catalog.table", new SqlTableId("catalog", null, "table").buildQualifiedTableName(db));
    }

}
