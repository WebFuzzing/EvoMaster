package org.evomaster.core.problem.rest

import org.evomaster.core.remote.SutProblemException

import io.swagger.v3.oas.models.OpenAPI

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/*
Testing the local URL issue with OpenAPI, 4 test cases:
- a local file which exists and a valid URL
- a local file which does not exist and a valid URL
- a local file which exists and an invalid URL
- a local file which does not exist and an invalid URL
- a local file which is not a valid swagger
- an invalid url
 */
class OpenAPILocalURLIssueTest {

    private val executionPath :String = System.getProperty("user.dir")
    private lateinit var swagger: OpenAPI


    /*
    Check that the swagger is created with a valid URL
    and an existing file
    */
    @Test
    fun testExistingFileValidURL() {

        // get the current directory
        val urlToTest = "file://$executionPath/src/test/resources/openapi_pet.json";

        // create swagger from URI
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);

        // a valid swagger is created
        Assertions.assertTrue(swagger != null);
    }

    /*
    Check that an exception is thrown with a non-existing file
    and a valid url
    */
    @Test
    fun testNonExistingFileValidURL() {

        // get the current directory
        val urlToTest = "file://$executionPath/src/test" +
                "/resources/openapi_pet_non_existing.json";

        // since the file does not exist,
        // a valid swagger cannot be created but an
        // SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);
        }

        // the message in the SutException should be
        // Cannot find OpenAPI schema at file location: $urlToTest

        // check that the message is correct
        Assertions.assertTrue(
            exception.message!!.contains("Cannot find OpenAPI " +
                "schema at file location: $urlToTest"))
    }

    /*
    Check that an exception is thrown when the file exists
    but the URL is not valid, missing one slash (/)
    */
    @Test
    fun testExistingFileInvalidURL() {

        // get the current directory
        val urlToTest = "file:/$executionPath/src/test" +
                "/resources/openapi_pet.json";

        // since the file does not exist,
        // a valid swagger cannot be created but an
        // SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);
        }

        /*
        the message in the SutException should be
        The file path provided for the OpenAPI Schema
        $urlToTest , is not a valid path
        */
        Assertions.assertTrue(
            exception.message!!.contains("The file path provided for the OpenAPI Schema $urlToTest ," +
                    " is not a valid path"))
    }

    /*
    Check that an exception is thrown when the file
    does not exist and the URL is not valid,
    missing one slash (/)
    */
    @Test
    fun testNonExistingFileInvalidURL() {

        // get the current directory
        val urlToTest = "file:/$executionPath/src/test" +
                "/resources/openapi_pet_non_existent.json";

        // since the file does not exist,
        // a valid swagger cannot be created but an
        // SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);
        }

        /*
        the message in the SutException should be
        The file path provided for the OpenAPI Schema
        $urlToTest , is not a valid path
        */
        Assertions.assertTrue(
            exception.message!!.contains("The file path provided for the OpenAPI Schema $urlToTest ," +
                    " is not a valid path"))
    }

    /*
    Check that an exception is thrown when the file
    does not exist and the URL is not valid,
    missing one slash (/)
    */
    @Test
    fun testExistingFileInvalidSwagger() {

        // get the current directory
        val urlToTest = "file:/$executionPath/src/test" +
                "/resources/openapi_pet_non_existent.json";

        // since the file does not exist,
        // a valid swagger cannot be created but an
        // SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);
        }

        /*
        the message in the SutException should be
        The file path provided for the OpenAPI Schema
        $urlToTest , is not a valid path
        */
        Assertions.assertTrue(
            exception.message!!.contains("The file path provided for the OpenAPI Schema $urlToTest ," +
                    " is not a valid path"))
    }

    /*
    Check that when the swagger is invalid,
    an exception is thrown with the message

    */
    @Test
    fun testInvalidSwagger() {

        // get the current directory
        val urlToTest = "file://$executionPath/src/test" +
                "/resources/invalid_swagger.json";

        // create swagger
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);

        /*
        An empty swagger should be created
        */
        Assertions.assertTrue(swagger.paths.size == 0)
    }

    @Test
    fun testInvalidURLI() {

        // get the current directory
        val urlToTest = "file://$executionPath/xxxxxxxx";

        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest);
        }

        /*
        the message in the SutException should be
        The file path provided for the OpenAPI Schema
        $urlToTest , is not a valid path
        */
        Assertions.assertTrue(
            exception.message!!.contains("Cannot find OpenAPI " +
                    "schema at file location: $urlToTest"))
    }



}