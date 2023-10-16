package org.evomaster.core.problem.rest

import org.evomaster.core.remote.SutProblemException

import io.swagger.v3.oas.models.OpenAPI

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.util.*

/*
Testing the local URL issue with OpenAPI, 4 test cases:
- a local file which exists and a valid URL
- a local file which does not exist and a valid URL
- a local file which exists and an invalid URL
- a local file which does not exist and an invalid URL
- a local file which is not a valid swagger but a valid JSON
- a local file which is not a valid JSON
- an invalid url
 */
class OpenAPILocalURLIssueTest {

    companion object {

        private var executionPath :String = System.getProperty("user.dir")
        private lateinit var swagger: OpenAPI
        private lateinit var swaggerTestDirectory: String
        private lateinit var hostOs: String


        @JvmStatic
        @BeforeAll
        fun setSwaggerDirectoryBasedOnOS() {

            hostOs = System.getProperty("os.name").lowercase(Locale.getDefault())

            if (hostOs.contains("win")) {
                executionPath = executionPath.replace('\\', '/')
            }

            swaggerTestDirectory = "$executionPath/src/test/resources/swagger/urlissue"
        }
    }

    /*
    Check that the swagger is created with a valid URL and an existing file
    */
    @Test
    fun testExistingFileValidURL() {

        // get the current directory, in Mac or Linux, it starts with //
        // but in Windows, it has to have just one /
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/openapi_pet.json"
        }
        else {
            "file://$swaggerTestDirectory/openapi_pet.json"
        }

        // create swagger from URI
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)

        // a valid swagger is created with 13 endpoints
        Assertions.assertTrue(swagger.paths.size == 13)
    }

    /*
    Check that an exception is thrown with a non-existing file and a valid url
    */
    @Test
    fun testNonExistingFileValidURL() {

        // get the current directory
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/openapi_pet_non_existing.json"
        }
        else {
            "file://$swaggerTestDirectory/openapi_pet_non_existing.json"
        }

        // since the file does not exist, a valid swagger cannot be created but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // the message in the SutException should be "Cannot find OpenAPI schema at file location: $urlToTest
        // check that the message is correct"
        Assertions.assertTrue(exception.message!!.contains("The provided swagger file does " +
                "not exist: $urlToTest"))
    }

    /*
    Check that an exception is thrown when the file exists but the URL is not valid, missing one slash (/)
    */
    @Test
    fun testExistingFileInvalidURL() {

        // get the current directory
        //val urlToTest = "file:/$swaggerTestDirectory/openapi_pet.json"

        val urlToTest = if (hostOs.contains("win")) {
            "file://$swaggerTestDirectory/openapi_pet_non_existing.json"
        }
        else {
            "file:/$swaggerTestDirectory/openapi_pet.json"
        }

        // since the file URL is invalid, a valid swagger cannot be created but an  SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // The message in the SutException should contain "The file path provided for the OpenAPI Schema
        // $urlToTest is not a valid path
        Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI Schema " +
                "$urlToTest," + " ended up with the following error: "))
    }

    /*
    Check that an exception is thrown when the file does not exist and the URL is not valid, missing one slash (/)
    */
    @Test
    fun testNonExistingFileInvalidURL() {

        // get the current directory
        val urlToTest = "file:/$swaggerTestDirectory/openapi_pet_non_existent.json"

        // since the file does not exist and URL is invalid, a valid swagger cannot be created
        // but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // The message in the SutException should be "The file path provided for the OpenAPI Schema
        // $urlToTest , is not a valid path"
        Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI Schema " +
                "$urlToTest," + " ended up with the following error: "))
    }

    @Test
    fun testRelativeFilePathExistingFile() {

        // swagger test directory
        val urlToTest = "file://./src/test/resources/swagger/openapi_pet.json"

        // since the file does not exist and URL is invalid, a valid swagger cannot be created
        // but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // The message in the SutException should be "The file path provided for the OpenAPI Schema
        // $urlToTest , is not a valid path"
        Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI Schema " +
                "$urlToTest," + " ended up with the following error: "))
    }

    @Test
    fun testFileNameOnlyNonExistingFile() {

        // get the current directory
        val urlToTest = "file://openapi_pet.json"

        // since the file does not exist and URL is invalid, a valid swagger cannot be created
        // but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // The message in the SutException should be "The file path provided for the OpenAPI Schema
        // $urlToTest , is not a valid path"
        if (hostOs.contains("win")) {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI " +
                    "Schema $urlToTest is empty"))
        }
        else {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI Schema " +
                    "$urlToTest," + " ended up with the following error: URI has an authority component"
                )
            )
        }
    }

    //Check that when the swagger is invalid, an empty swagger object is created
    @Test
    fun testInvalidSwagger() {

        // get the current directory
        val urlToTest = "file://$swaggerTestDirectory/invalid_swagger.json"

        // create swagger
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)

        //An empty swagger should be created
        Assertions.assertTrue(swagger.paths.size == 0)
    }

    @Test
    fun testInvalidJSON() {

        // get the current directory
        val urlToTest = "file://$swaggerTestDirectory/invalid_json.json"

        // exception to throw
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // Failed to parse OpenApi schema
        Assertions.assertTrue( exception.message!!.contains("Failed to parse OpenApi schema"))
    }
}