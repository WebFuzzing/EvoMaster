package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLSchemaExtractorTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {

    @Test
    public void testCreateWithBitColumn() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE TB(b BIT(8))");

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
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

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
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

    @Test
    public void testCaseSensitivityOfExtractor() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE TableNot(intColumn INT)");

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
        assertNotNull(schema);

        TableDto table = schema.tables.get(0);
        assertEquals("intColumn",table.columns.get(0).name);
        assertEquals("TableNot", table.name);

    }

    @Test
    public void testExtractSpatialTypes() throws Exception {

        String sql = "CREATE TABLE spatialdatatypes(intcolumn INT NOT NULL, \n" +
                "  pointcolumn POINT NOT NULL,\n" +
                "  linestringcolumn LINESTRING NOT NULL,\n" +
                "  polygoncolumn POLYGON NOT NULL,\n" +
                "  geometrycolumn GEOMETRY NOT NULL,\n" +
                "  multipointcolumn MULTIPOINT NOT NULL,\n" +
                "  multilinestringcolumn MULTILINESTRING NOT NULL,\n" +
                "  multipolygoncolumn MULTIPOLYGON NOT NULL,\n" +
                "  geometrycollectioncolumn GEOMETRYCOLLECTION NOT NULL)";

        SqlScriptRunner.execCommand(getConnection(), sql);

        DbInfoDto schema = DbInfoExtractor.extract(getConnection());
        assertNotNull(schema);

        TableDto table = schema.tables.get(0);
        assertEquals("intcolumn",table.columns.get(0).name);
        assertEquals("INT",table.columns.get(0).type);

        assertEquals("pointcolumn",table.columns.get(1).name);
        assertEquals("POINT",table.columns.get(1).type);

        assertEquals("linestringcolumn",table.columns.get(2).name);
        assertEquals("LINESTRING",table.columns.get(2).type);

        assertEquals("polygoncolumn",table.columns.get(3).name);
        assertEquals("POLYGON",table.columns.get(3).type);

        assertEquals("geometrycolumn",table.columns.get(4).name);
        assertEquals("GEOMETRY",table.columns.get(4).type);

        assertEquals("multipointcolumn",table.columns.get(5).name);
        assertEquals("MULTIPOINT",table.columns.get(5).type);

        assertEquals("multilinestringcolumn",table.columns.get(6).name);
        assertEquals("MULTILINESTRING",table.columns.get(6).type);

        assertEquals("multipolygoncolumn",table.columns.get(7).name);
        assertEquals("MULTIPOLYGON",table.columns.get(7).type);

        assertEquals("geometrycollectioncolumn",table.columns.get(8).name);
        assertEquals("GEOMCOLLECTION",table.columns.get(8).type);
    }

    @Test
    public void testTwoSchemas() throws Exception {
        final String url = getUrl();

        final String anotherTestUserName = "testuser1";
        final String anotherTestUserPassword = "testuser1password";

        createNewUserInDatabase(url, anotherTestUserName, anotherTestUserPassword);

        final Connection testUser1Connection = DriverManager.getConnection(url, anotherTestUserName, anotherTestUserPassword);
        SqlScriptRunner.execCommand(testUser1Connection, "CREATE SCHEMA new_schema");
        SqlScriptRunner.execCommand(testUser1Connection, "USE new_schema");

        final Connection testUser0Connection = getConnection();

        SqlScriptRunner.execCommand(testUser0Connection, "CREATE TABLE my_table (\n" +
                "    id INT\n" +
                ")");

        SqlScriptRunner.execCommand(testUser1Connection, "CREATE TABLE my_table (\n" +
                "    id INT\n" +
                ")");

        DbInfoDto schemaTest0 = DbInfoExtractor.extract(testUser0Connection);
        assertEquals("test", schemaTest0.name);
        assertEquals(1, schemaTest0.tables.size());
        assertEquals("my_table", schemaTest0.tables.get(0).name);

        DbInfoDto newSchema = DbInfoExtractor.extract(testUser1Connection);
//        assertEquals("new_schema", newSchema.name);
        assertEquals("test", newSchema.name); //the connection is still on same URL, with same catalog
        //assertEquals(1, newSchema.tables.size());
        //we are now fetching data for all schemas (/catalogs in MySQL)
        assertEquals(2, newSchema.tables.size());
        assertEquals("my_table", newSchema.tables.get(0).name);

        SqlScriptRunner.execCommand(testUser1Connection, "DROP SCHEMA IF EXISTS new_schema");
        deleteUserInDatabase(url, anotherTestUserName);

    }

    private static void createNewUserInDatabase(String url, String anotherTestUserName, String anotherTestUserPassword) throws SQLException {
        final Connection rootConnection = DriverManager.getConnection(url, MYSQL_ROOT_USER_NAME, MYSQL_ROOT_USER_PASSWORD);
        SqlScriptRunner.execCommand(rootConnection, String.format("CREATE USER '%s'@'%%' IDENTIFIED BY '%s'", anotherTestUserName, anotherTestUserPassword));
        SqlScriptRunner.execCommand(rootConnection, String.format("GRANT CREATE ON *.* TO '%s'@'%%'", anotherTestUserName));
        SqlScriptRunner.execCommand(rootConnection, String.format("GRANT DROP ON *.* TO '%s'@'%%'", anotherTestUserName));
        SqlScriptRunner.execCommand(rootConnection, "FLUSH PRIVILEGES");
    }

    private static void deleteUserInDatabase(String url, String anotherTestUserName) throws SQLException {
        final Connection rootConnection = DriverManager.getConnection(url, MYSQL_ROOT_USER_NAME, MYSQL_ROOT_USER_PASSWORD);
        SqlScriptRunner.execCommand(rootConnection, String.format("DROP USER '%s'", anotherTestUserName));
    }

}
