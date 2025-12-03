package org.evomaster.client.java.sql.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlTableIdParserTest {

    @Test
    void parseSingleName_returnsTableOnly() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mytable");
        assertNull(id.getCatalogName());
        assertNull(id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseSchemaAndTable_returnsSchemaAndTable() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("myschema.mytable");
        assertNull(id.getCatalogName());
        assertEquals("myschema", id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseCatalogSchemaTable_returnsAllParts() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("mycatalog.myschema.mytable");
        assertEquals("mycatalog", id.getCatalogName());
        assertEquals("myschema", id.getSchemaName());
        assertEquals("mytable", id.getTableName());
    }

    @Test
    void parseUppercaseParts_areLowerCased() {
        SqlTableId id = SqlTableIdParser.parseFullyQualifiedTableName("CATALOG.SCHEMA.TABLE");
        assertEquals("catalog", id.getCatalogName());
        assertEquals("schema", id.getSchemaName());
        assertEquals("table", id.getTableName());
    }

    @Test
    void parseNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName(null));
    }

    @Test
    void parseEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName(""));
    }

    @Test
    void parseTooManyParts_throws() {
        assertThrows(IllegalArgumentException.class, () -> SqlTableIdParser.parseFullyQualifiedTableName("a.b.c.d"));
    }

}

