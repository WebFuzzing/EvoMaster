package org.evomaster.core.problem.rest

import org.evomaster.core.remote.SutProblemException

import io.swagger.v3.oas.models.OpenAPI

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.util.*

/*
Testing the local URL issue with OpenAPI, 4 test cases:
1. A local file which exists and the provided URL is valid.
2. A local file does not exist but the provided URL is valid.
3. A local file exists but the provided URL is not valid.
4. A local file does not exist and the provided URL is not valid.
5. A local file which exists but the relative file path is provided.
6. Two different exceptions in Windows and others (e.g., "URI Path Component is empty" is tested here).
7. The swagger is an existing valid json file, but it is not a valid swagger.
8: The swagger is an invalid json file.
 */
class OpenAPILocalURLIssueTest {

    // companion object to set up tests.
    companion object {

        // execution path, it can be different from one machine to another
        private var executionPath :String = System.getProperty("user.dir")

        // swagger object
        private lateinit var swagger: OpenAPI

        // swagger test directory to find test files
        private lateinit var swaggerTestDirectory: String

        // host operating system
        private lateinit var hostOs: String

        @JvmStatic
        @BeforeAll
        // This is to deal with differences in Windows and Linux paths
        fun setSwaggerDirectoryBasedOnOS() {

            // get the name of the current operating system
            hostOs = System.getProperty("os.name").lowercase(Locale.getDefault())

            // if the operating system is Windows, then replace \ with /
            if (hostOs.contains("win")) {
                executionPath = executionPath.replace('\\', '/')
            }

            // swagger files for testing
            swaggerTestDirectory = "$executionPath/src/test/resources/swagger/urlissue"
        }
    }

    /*
    Test Case 1: A local file which exists and the provided URL is valid
    Check that the swagger is created with a valid URL and an existing file
    */
    @Test
    fun testExistingFileValidURL() {

        // get the current directory, in Mac or Linux, it starts with file://
        // but in Windows, it has to have just one file:/
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/openapi_pet.json"
        }
        else {
            "file://$swaggerTestDirectory/openapi_pet.json"
        }

        // create swagger from URL
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)

        // a valid swagger is created with 13 endpoints
        Assertions.assertTrue(swagger.paths.size == 13)
    }

    /*
    Test Case 2: A local file schema does not exist but the provided URL is valid
    Check that an exception is thrown which states that the provided swagger file does not exist
    */
    @Test
    fun testNonExistingFileValidURL() {

        // The Windows file URL starts with file:/
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/openapi_pet_non_existing.json"
        }
        // file URL in other operating systems
        else {
            "file://$swaggerTestDirectory/openapi_pet_non_existing.json"
        }

        // since the file does not exist, a valid swagger cannot be created
        // but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            // create swagger from URL
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // the message in the SutException should be "The provided swagger file does not exist: $urlToTest
        // check that the message is correct", it is the same for both Windows and other operating systems
        Assertions.assertTrue(exception.message!!.contains("The provided OpenAPI file does " +
                "not exist: $urlToTest"))
    }

    /*
    Test Case 3: A local file which exists but the provided URL is invalid
    Check that an exception is thrown when the file exists but the URL is not valid, for Windows file:// is
    not valid, for others file:/ is not valid
    */
    @Test
    fun testExistingFileInvalidURL() {

        // The Windows file URL starts with file://, which is not valid
        val urlToTest = if (hostOs.contains("win")) {
            "file://$swaggerTestDirectory/openapi_pet.json"
        }
        // URL starts with file:/ for other operating systems.
        else {
            "file:/$swaggerTestDirectory/openapi_pet.json"
        }

        // since the file URL is invalid, a valid swagger cannot be created,
        // so an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // In windows, the file cannot be located, in others the error is URL related
        if (hostOs.contains("win")) {
            Assertions.assertTrue(exception.message!!.contains("The provided OpenAPI file " +
                    "does not exist: $urlToTest"))
        }
        else {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the " +
                    "OpenAPI Schema $urlToTest ended up with the following error: "))
        }

    }

    /* Test Case 4: A local file does not exist and the provided URL is not valid
    Check that an exception is thrown for non-existent file in Windows, and an SUTException
    is thrown with the error message for others.
    */
    @Test
    fun testNonExistingFileInvalidURL() {

        // File path in Windows and others
        val urlToTest = if (hostOs.contains("win")) {
            "file://$swaggerTestDirectory/openapi_pet_non_existent.json"
        }
        else {
            "file:/$swaggerTestDirectory/openapi_pet_non_existent.json"
        }

        // since the file does not exist and URL is invalid, a valid swagger cannot be created
        // but an SutException should be thrown
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // In windows, the file cannot be located, in others the error is URL related
        if (hostOs.contains("win")) {
            Assertions.assertTrue(exception.message!!.contains("The provided OpenAPI file " +
                    "does not exist: $urlToTest"))
        }
        else {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the OpenAPI " +
                    "Schema $urlToTest ended up with the following error: "))
        }
    }


    @Test
    /* Test Case 5: A local file which exists but the relative file path is provided.
    In that case, an exception stating the file does not exist is thrown in Windows
    SutException is thrown in other operating systems.
     */
    fun testRelativeFilePathExistingFile() {

        // file path in Windows and others
        val urlToTest = if (hostOs.contains("win")) {
            "file:/./src/test/resources/swagger/openapi_pet.json"
        }
        else {
            "file://./src/test/resources/swagger/openapi_pet.json"
        }

        // create swagger
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // Assert the thrown exception
        if (hostOs.contains("win")) {
            Assertions.assertTrue(exception.message!!.contains("The provided OpenAPI file " +
                    "does not exist: $urlToTest"))
        }
        else {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the " +
                    "OpenAPI Schema $urlToTest ended up with the following error: "))
        }
    }

    /*
    Test case 6: Test for two different exceptions in Windows and others
    For the URL file://openapi_pet.json, Windows throws URI path component is empty,
    others throw URI has an authority component
     */
    @Test
    fun testFileNameOnlyNonExistingFile() {

        // same URL for both Windows and others, but different exceptions are expected
        val urlToTest = "file://openapi_pet.json"

        // create swagger
        val exception = Assertions.assertThrows(
            SutProblemException::class.java
        ) {
            swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)
        }

        // Check the thrown exception for windows and others
        if (hostOs.contains("win")) {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the " +
                    "OpenAPI Schema $urlToTest ended up with the following error: " +
                    "URI path component is empty"))
        }
        else {
            Assertions.assertTrue(exception.message!!.contains("The file path provided for the " +
                    "OpenAPI Schema $urlToTest ended up with the following error: " +
                    "URI has an authority component"
                )
            )
        }
    }

    /*
    Test case 7: If the swagger is an existing valid json file, but it is not
    a valid swagger, a swagger object is created with 0 endpoints
     */
    @Test
    fun testInvalidSwagger() {

        // file path in Windows and others
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/invalid_swagger.json"
        }
        else {
            "file://$swaggerTestDirectory/invalid_swagger.json"
        }

        // create swagger
        swagger = OpenApiAccess.getOpenAPIFromURL(urlToTest)

        //An empty swagger should be created
        Assertions.assertTrue(swagger.paths.size == 0)
    }

    /*
    Test case 8: If the swagger is an invalid json file, an exception stating that the swagger
    could not be parsed should be thrown
     */
    @Test
    fun testInvalidJSON() {

        // file path in Windows and others
        val urlToTest = if (hostOs.contains("win")) {
            "file:/$swaggerTestDirectory/invalid_json.json"
        }
        else {
            "file://$swaggerTestDirectory/invalid_json.json"
        }

        // create swagger
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