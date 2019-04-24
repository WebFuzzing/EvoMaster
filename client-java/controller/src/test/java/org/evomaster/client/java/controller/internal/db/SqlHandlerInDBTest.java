package org.evomaster.client.java.controller.internal.db;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.TEST_RESULTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class SqlHandlerInDBTest extends DatabaseTestTemplate {

    private ExecutionDto getSqlExecutionDto(int index, String url) {

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
    public void testDeleteTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            startNewTest(url);

            ExecutionDto dto = getSqlExecutionDto(0,url);

            assertTrue(dto == null || dto.deletedData == null || dto.deletedData.isEmpty());

            startNewActionInSameTest(url, 1);

            SqlScriptRunner.execCommand(getConnection(), "Delete FROM Foo");

            dto = getSqlExecutionDto(1,url);

            assertNotNull(dto);
            assertNotNull(dto.deletedData);
            assertEquals(1, dto.deletedData.size());
            assertTrue(dto.deletedData.contains("Foo"));

        } finally {
            starter.stop();
        }
    }


    @Test
    public void testInsertTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            startNewTest(url);

            ExecutionDto dto = getSqlExecutionDto(0,url);

            assertTrue(dto == null || dto.insertedData == null || dto.insertedData.isEmpty());

            startNewActionInSameTest(url, 1);

            SqlScriptRunner.execCommand(getConnection(), "insert into Foo (x) values (42)");

            dto = getSqlExecutionDto(1,url);

            assertNotNull(dto);
            assertNotNull(dto.insertedData);
            assertEquals(1, dto.insertedData.size());
            assertTrue(dto.insertedData.containsKey("Foo"));

        } finally {
            starter.stop();
        }
    }

    @Test
    public void testUpdateTable() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            startNewTest(url);

            ExecutionDto dto = getSqlExecutionDto(0,url);

            assertTrue(dto == null || dto.updatedData == null || dto.updatedData.isEmpty());

            startNewActionInSameTest(url, 1);

            SqlScriptRunner.execCommand(getConnection(), "update Foo set x=42");

            dto = getSqlExecutionDto(1,url);

            assertNotNull(dto);
            assertNotNull(dto.updatedData);
            assertEquals(1, dto.updatedData.size());
            assertTrue(dto.updatedData.containsKey("Foo"));

        } finally {
            starter.stop();
        }
    }
}
