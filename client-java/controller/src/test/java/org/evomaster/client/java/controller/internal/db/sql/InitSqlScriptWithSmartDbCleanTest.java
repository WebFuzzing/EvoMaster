package org.evomaster.client.java.controller.internal.db.sql;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.ControllerConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface InitSqlScriptWithSmartDbCleanTest extends DatabaseTestTemplate {

    default String getInitSqlScript() {
        return String.join("\n",
                Arrays.asList(
                        "INSERT INTO Bar (id, valueColumn) VALUES (0, 0);",
                        "INSERT INTO Bar (id, valueColumn) VALUES (1, 0);",
                        "INSERT INTO Bar (id, valueColumn) VALUES (2, 0);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (0, 0, NULL, 0);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (1, 0, 0, 1);",
                        "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (2, 0, 1, 2);",
                        "UPDATE Foo SET valueColumn = 2 WHERE id = 2;",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (0, 0, 0);",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (1, 0, 1);",
                        "INSERT INTO Abc (id, valueColumn, foo_id) VALUES (2, 0, 2);",
                        "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (0, 0, 0);",
                        "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (1, 0, 1);"
//                        "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (2, 0, 2);"

                )
        );
    }

    @Test
    default void testAccessedFkClean() throws Exception {
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, refer_foo INT DEFAULT NULL, bar_id INT, " +
                "CONSTRAINT fk_foo FOREIGN KEY (bar_id) REFERENCES Bar(id)," +
                "CONSTRAINT fk_self_foo FOREIGN KEY (refer_foo) REFERENCES Foo(id) )", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Abc(id INT Primary Key, valueColumn INT, foo_id INT, " +
                "CONSTRAINT fk_abc FOREIGN KEY (foo_id) REFERENCES Foo(id) )", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Xyz(id INT Primary Key, valueColumn INT, abc_id INT, " +
                "CONSTRAINT fk_xyz FOREIGN KEY (abc_id) REFERENCES Abc(id) )", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res;

            startNewTest(url);

            // db with init data
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo WHERE valueColumn = 2;", true);
            assertEquals(1, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo WHERE valueColumn = 0;", true);
            assertEquals(2, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Abc;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(2, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // db with init data
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo WHERE valueColumn = 2;", true);
            assertEquals(1, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo WHERE valueColumn = 0;", true);
            assertEquals(2, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Abc;", true);
            assertEquals(3, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(2, res.seeRows().size());


            // table is accessed with INSERT
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (4, 4);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(4, res.seeRows().size());

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (2, 0, 2);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(3, res.seeRows().size());


            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(3, res.seeRows().size());


            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // db only contains init data
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Abc;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(2, res.seeRows().size());



            //2nd-round test: table is accessed with INSERT
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, refer_foo, bar_id) VALUES (3, 0, 1, 0);", true);

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(4, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Abc;", true);
            assertEquals(3, res.seeRows().size());

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Xyz (id, valueColumn, abc_id) VALUES (2, 0, 2);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(3, res.seeRows().size());


            RestAssured.given().accept(ContentType.JSON)
                .get(url + ControllerConstants.TEST_RESULTS)
                .then()
                .statusCode(200);

            startNewTest(url);

            // db only contains init data
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Abc;", true);
            assertEquals(3, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Xyz;", true);
            assertEquals(2, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }

}
