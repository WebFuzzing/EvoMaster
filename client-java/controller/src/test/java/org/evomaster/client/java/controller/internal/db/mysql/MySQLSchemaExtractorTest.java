package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SchemaExtractor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLSchemaExtractorTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {

    @Test
    public void testCreateWithBitColumn() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE TB(b BIT(8))");

        DbSchemaDto schema = SchemaExtractor.extract(getConnection());
        assertNotNull(schema);

        TableDto table = schema.tables.get(0);
        assertEquals(1, table.columns.size());
        assertEquals("BIT",table.columns.get(0).type);
        assertEquals("b",table.columns.get(0).name);
        assertEquals(8,table.columns.get(0).size);

    }

    @Test
    public void testNumericUnsignedColumn() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE TB(a INT(5) ZEROFILL, b BIGINT(10) UNSIGNED, c MEDIUMINT, d SERIAL);");

        DbSchemaDto schema = SchemaExtractor.extract(getConnection());
        assertNotNull(schema);

        TableDto table = schema.tables.get(0);
        assertEquals(4, table.columns.size());

        assertEquals("INT",table.columns.get(0).type);
        assertTrue(table.columns.get(0).isUnsigned);
        assertEquals("a",table.columns.get(0).name);
        assertEquals(10,table.columns.get(0).size);

        assertEquals("BIGINT",table.columns.get(1).type);
        assertTrue(table.columns.get(1).isUnsigned);
        assertEquals("b",table.columns.get(1).name);
        assertEquals(20,table.columns.get(1).size);

        assertEquals("MEDIUMINT",table.columns.get(2).type);
        assertFalse(table.columns.get(2).isUnsigned);
        assertEquals("c",table.columns.get(2).name);
        assertEquals(7,table.columns.get(2).size);

        /*
            see https://dev.mysql.com/doc/refman/8.0/en/numeric-type-syntax.html
            SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE.
         */
        assertEquals("BIGINT",table.columns.get(3).type);
        assertTrue(table.columns.get(3).isUnsigned);
        assertEquals("d",table.columns.get(3).name);
        assertEquals(20,table.columns.get(3).size);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
