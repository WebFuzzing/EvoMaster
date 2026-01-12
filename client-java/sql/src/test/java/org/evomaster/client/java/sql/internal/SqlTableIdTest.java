package org.evomaster.client.java.sql.internal;

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


}
