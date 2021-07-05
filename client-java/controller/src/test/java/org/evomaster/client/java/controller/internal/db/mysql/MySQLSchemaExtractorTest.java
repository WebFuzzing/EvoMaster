package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SchemaExtractor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
