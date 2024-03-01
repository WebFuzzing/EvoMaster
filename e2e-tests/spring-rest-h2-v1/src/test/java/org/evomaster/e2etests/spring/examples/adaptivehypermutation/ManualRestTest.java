package org.evomaster.e2etests.spring.examples.adaptivehypermutation;


import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.hamcrest.Matchers.equalTo;


/**
 * created by manzh on 2020/10/23
 */
public class ManualRestTest extends AHypermuationTestBase{

    @Test
    public void test_example(){
        List<InsertionDto> insertions = sql().insertInto("BAR", 0L)
                .d("A", "0")
                .d("B", "\"bar\"")
                .d("C", "369")
                .and().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 42L)
                .d("X", "42")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/22?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4B5"));

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/bars/0")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("'b'", equalTo("bar"))
                .body("'c'",equalTo(369));
    }



    @Test
    public void test_foo_B0(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B0"));
    }

    @Test
    public void test_foo_B1(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 100.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B1"));
    }

    @Test
    public void test_foo_B2(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 200.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B2"));
    }

    @Test
    public void test_foo_B3(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3"));
    }

    @Test
    public void test_foo_B4(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4"));
    }

    @Test
    public void test_foo_B5(){

        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 42L)
                .d("X", "42")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/4?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4B5"));
    }

    @Test
    public void test_less_foo(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/2?y=foo")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void test_foo_bad_x(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"evomaster_48_input\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/-1?y=foo")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void test_foo_bad_y(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2010-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=bar")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void test_foo_bad_zt(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"evomaster_48_input\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @AfterEach
    public void reset(){
        controller.resetDatabase(null);
    }
}
