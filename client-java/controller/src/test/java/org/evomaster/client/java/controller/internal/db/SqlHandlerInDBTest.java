package org.evomaster.client.java.controller.internal.db;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.TEST_RESULTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public interface SqlHandlerInDBTest extends DatabaseTestTemplate {

    default ExecutionDto getSqlExecutionDto(int index, String url) {

        TestResultsDto dto = given().accept(ContentType.JSON)
                .get(url + TEST_RESULTS)
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

        try {
            ExecutionDto dto = executeCommand(starter, "Delete FROM Foo");

            assertNotNull(dto);
            assertNotNull(dto.deletedData);
            assertEquals(1, dto.deletedData.size());
            assertTrue(dto.deletedData.contains("Foo"));

        } finally {
            starter.stop();
        }
    }

    @Test
    public default void testInsertTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            ExecutionDto dto = executeCommand(starter, "insert into Foo (x) values (42)");

            assertNotNull(dto);
            assertNotNull(dto.insertedData);
            assertEquals(1, dto.insertedData.size());
            assertTrue(dto.insertedData.containsKey("Foo"));

        } finally {
            starter.stop();
        }
    }


    @Test
    public default void testUpdateTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            ExecutionDto dto = executeCommand(starter, "update Foo set x=42");

            assertNotNull(dto);
            assertNotNull(dto.updatedData);
            assertEquals(1, dto.updatedData.size());
            assertTrue(dto.updatedData.containsKey("Foo"));

        } finally {
            starter.stop();
        }
    }


    default ExecutionDto executeCommand(InstrumentedSutStarter starter, String sqlCommand) throws SQLException {
        String url = startInstrumentedSutStarterAndNewTest(starter);
        ExecutionDto dto = getSqlExecutionDto(0, url);

        assertDataIsEmpty(dto);

        startNewActionInSameTest(url, 1);

        SqlScriptRunner.execCommand(getConnection(), sqlCommand);

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
        url += BASE_PATH;
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
