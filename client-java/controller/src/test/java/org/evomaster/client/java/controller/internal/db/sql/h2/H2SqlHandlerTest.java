package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlCommandWithDistance;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2SqlHandlerTest extends DatabaseH2TestInit {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleNoRows(boolean useCompleteSqlHeuristics) throws Exception {

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        assertEquals(1, schema.tables.size());
        assertEquals("Person".toUpperCase(), schema.tables.get(0).name.toUpperCase());

        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person Where Age=15";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleOneOrMoreRows(boolean useCompleteSqlHeuristics) throws Exception {

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        assertEquals(1, schema.tables.size());
        assertEquals("Person".toUpperCase(), schema.tables.get(0).name.toUpperCase());

        SqlScriptRunner.execCommand(connection, "INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (1, 'John', 'Doe', 30, 'john.doe@example.com');");

        SqlScriptRunner.execCommand(connection, "INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (2, 'Jane', 'Smith', 28, 'jane.smith@example.com');");

        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person Where Age=15";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(2, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
            assertEquals(Math.abs(28 - 15), sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleNoWhereClause(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleWhereNoColumns(boolean useCompleteSqlHeuristics) throws Exception {
        // Setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        String sqlCommand = "Select * From Person Where 1=1";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // Exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleWhereNoColumnsWithOneRow(boolean useCompleteSqlHeuristics) throws Exception {
        // Setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        SqlScriptRunner.execCommand(connection, "INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (1, 'John', 'Doe', 30, 'john.doe@example.com');");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        String sqlCommand = "Select * From Person Where 1=1";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // Exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(1, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
            assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleNoWhereClauseWithOneRow(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");
        SqlScriptRunner.execCommand(connection, "INSERT INTO Person (person_id, first_name, last_name, age, email)\n" +
                "VALUES (1, 'John', 'Doe', 30, 'john.doe@example.com');");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "Select * From Person";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleDeleteNoWhereClause(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "DELETE FROM Person";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleUpdateNoWhereClause(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "UPDATE Person SET age = 15";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        if (!useCompleteSqlHeuristics) {
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
            assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleDeleteWhereClauseWithNoColumns(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "DELETE FROM Person WHERE 1 = 1";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testHandleUpdateWhereClauseWithNoColumns(boolean useCompleteSqlHeuristics) throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Person (\n" +
                "    person_id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    age INT,\n" +
                "    email VARCHAR(100)\n" +
                ");");

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(useCompleteSqlHeuristics);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        String sqlCommand = "UPDATE Person SET age = 15 WHERE 1 = 1";
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

        // exercise
        List<SqlCommandWithDistance> sqlCommandWithDistances = sqlHandler.getSqlDistances(null, true);

        // check
        assertEquals(1, sqlCommandWithDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlCommandWithDistances.get(0);
        assertEquals(false, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
        if (!useCompleteSqlHeuristics) {
            assertEquals(0, sqlCommandWithDistance.sqlDistanceWithMetrics.numberOfEvaluatedRows);
            assertEquals(Double.MAX_VALUE, sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance);
        }
    }


    @Test
    public void testCharacterLargeObjectInsertion() throws Exception {
        // setup
        SqlScriptRunner.execCommand(connection, "CREATE TABLE documents (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    content CLOB\n" +
                ");");

        String sqlCommand = "INSERT INTO documents (id,content) VALUES (1,'LOB')";
        SqlScriptRunner.execCommand(connection, sqlCommand);

        DbInfoExtractor dbInfoExtractor = new DbInfoExtractor();
        DbInfoDto schema = dbInfoExtractor.extract(connection);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setConnection(connection);
        sqlHandler.setSchema(schema);
        sqlHandler.setCompleteSqlHeuristics(true);

        assertTrue(sqlHandler.getSqlDistances(null, true).isEmpty());
        SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        sqlHandler.handle(sqlExecutionLogDto);

    }
}
