package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.sql.SchemaExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlCommandWithDistance;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2SqlHandlerTest extends DatabaseH2TestInit {

    @Test
    public void testHandleNoRows() throws Exception {

        SqlScriptRunner.execCommand(connection,"CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        SchemaExtractor schemaExtractor = new SchemaExtractor();
        DbSchemaDto schema = schemaExtractor.extract(connection);
        assertEquals(1, schema.tables.size());
        assertEquals("Person".toUpperCase(), schema.tables.get(0).name.toUpperCase());

        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person Where Age=15";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand,false,10L);
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);

    }

    @Test
    public void testHandleOneOrMoreRows() throws Exception {

        SqlScriptRunner.execCommand(connection,"CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        SchemaExtractor schemaExtractor = new SchemaExtractor();
        DbSchemaDto schema = schemaExtractor.extract(connection);
        assertEquals(1, schema.tables.size());
        assertEquals("Person".toUpperCase(), schema.tables.get(0).name.toUpperCase());

        SqlScriptRunner.execCommand(connection, "INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (1, 'John', 'Doe', 30, 'john.doe@example.com');");

        SqlScriptRunner.execCommand(connection,"INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (2, 'Jane', 'Smith', 28, 'jane.smith@example.com');");

        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person Where Age=15";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand,false,10L);
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(2, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(Math.abs(28-15), sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);

    }

}
