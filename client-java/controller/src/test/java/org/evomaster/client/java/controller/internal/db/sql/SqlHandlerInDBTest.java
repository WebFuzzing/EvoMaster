package org.evomaster.client.java.controller.internal.db.sql;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import  io.restassured.RestAssured;
import  org.evomaster.client.java.controller.api.ControllerConstants;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public interface SqlHandlerInDBTest extends DatabaseTestTemplate {

    default ExecutionDto getSqlExecutionDto(int index, String url) {

        TestResultsDto dto = RestAssured.given().accept(ContentType.JSON)
                .get(url + ControllerConstants.TEST_RESULTS)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                //.extraHeuristics["+index+"].databaseExecutionDto
                .getObject("data", TestResultsDto.class);

        return dto.extraHeuristics.get(index).databaseExecutionDto;
    }

    @Test
    public default void testDeleteTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        String command = "DELETE FROM Foo";
        try {
            ExecutionDto dto = executeCommand(starter, command, true);

            assertNotNull(dto);
            assertNotNull(dto.deletedData);
            Assertions.assertEquals(1, dto.deletedData.size());
            Assertions.assertTrue(dto.deletedData.contains("Foo"));

            // check info of executed sql
            assertNotNull(dto.sqlExecutionLogDtoList);
            Assertions.assertEquals(1, dto.sqlExecutionLogDtoList.size());
            Assertions.assertEquals(command, dto.sqlExecutionLogDtoList.get(0).command);

        } finally {
            starter.stop();
        }
    }

    @Test
    public default void testInsertTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();
        String command = "INSERT INTO Foo (x) VALUES (42)";
        try {
            ExecutionDto dto = executeCommand(starter, command, true);

            assertNotNull(dto);
            assertNotNull(dto.insertedData);
            Assertions.assertEquals(1, dto.insertedData.size());
            Assertions.assertTrue(dto.insertedData.containsKey("Foo"));
            // check info of executed sql
            assertNotNull(dto.sqlExecutionLogDtoList);
            Assertions.assertEquals(1, dto.sqlExecutionLogDtoList.size());
            Assertions.assertEquals(command, dto.sqlExecutionLogDtoList.get(0).command);
        } finally {
            starter.stop();
        }
    }


    @Test
    public default void testUpdateTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();
        String command = "UPDATE Foo SET x = 42";
        try {
            ExecutionDto dto = executeCommand(starter, command, true);

            assertNotNull(dto);
            assertNotNull(dto.updatedData);
            Assertions.assertEquals(1, dto.updatedData.size());
            Assertions.assertTrue(dto.updatedData.containsKey("Foo"));
            // check info of executed sql
            assertNotNull(dto.sqlExecutionLogDtoList);
            Assertions.assertEquals(1, dto.sqlExecutionLogDtoList.size());
            Assertions.assertEquals(command, dto.sqlExecutionLogDtoList.get(0).command);
        } finally {
            starter.stop();
        }
    }


    default ExecutionDto executeCommand(InstrumentedSutStarter starter, String sqlCommand, boolean instrumented) throws SQLException {
        String url = startInstrumentedSutStarterAndNewTest(starter);
        ExecutionDto dto = getSqlExecutionDto(0, url);

        assertDataIsEmpty(dto);

        startNewActionInSameTest(url, 1);

        EMSqlScriptRunner.execCommand(getConnection(), sqlCommand, instrumented);

        return getSqlExecutionDto(1, url);
    }

    default void assertDataIsEmpty(ExecutionDto dto) {
        assertUpdatedDataIsEmpty(dto);
        assertDeletedDataIsEmpty(dto);
        assertInsertedDataIsEmpty(dto);
    }

    @NotNull
    default String startInstrumentedSutStarterAndNewTest(InstrumentedSutStarter starter) {
        String url = start(starter);
        url += ControllerConstants.BASE_PATH;
        startNewTest(url);
        return url;
    }

    default void assertUpdatedDataIsEmpty(ExecutionDto dto) {
        assertTrue(dto == null || dto.updatedData == null || dto.updatedData.isEmpty());
    }

    default void assertDeletedDataIsEmpty(ExecutionDto dto) {
        assertTrue(dto == null || dto.deletedData == null || dto.deletedData.isEmpty());
    }

    default void assertInsertedDataIsEmpty(ExecutionDto dto) {
        assertTrue(dto == null || dto.insertedData == null || dto.insertedData.isEmpty());
    }
}
