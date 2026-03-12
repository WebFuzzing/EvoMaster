package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlTableIdParserTest {

    @Test
    void parseSingleName_returnsTableOnly() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mytable", DatabaseType.POSTGRES);
        assertNull(id.getCatalogName());
        assertNull(id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseSchemaAndTable_returnsSchemaAndTable() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("myschema.mytable", DatabaseType.POSTGRES);
        assertNull(id.getCatalogName());
        assertEquals("myschema", id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseCatalogAndTableForMySQL_returnsCatalogAndTable() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mycatalog.mytable", DatabaseType.MYSQL);
        assertEquals("mycatalog", id.getCatalogName());
        assertNull(id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseCatalogAndTableForMariaDB_returnsCatalogAndTable() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mycatalog.mytable", DatabaseType.MARIADB);
        assertEquals("mycatalog", id.getCatalogName());
        assertNull(id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseCatalogSchemaTable_returnsAllParts() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mycatalog.myschema.mytable", DatabaseType.POSTGRES);
        assertEquals("mycatalog", id.getCatalogName());
        assertEquals("myschema", id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseUppercaseParts_areLowerCased() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("CATALOG.SCHEMA.TABLE", DatabaseType.POSTGRES);
        assertEquals("catalog", id.getCatalogName());
        assertEquals("schema", id.getSchemaName());
        assertEquals("table", id.getTableName());
    }

    @Test
    void parseNull_throws() {
        assertThrows(NullPointerException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName(null, DatabaseType.POSTGRES));
    }

    @Test
    void parseEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName("", DatabaseType.POSTGRES));
    }

    @Test
    void parseTooManyParts_throws() {
        assertThrows(IllegalArgumentException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName("a.b.c.d", DatabaseType.POSTGRES));
    }

}

