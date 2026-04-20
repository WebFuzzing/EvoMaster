package org.evomaster.core.problem.rest.schema

import org.evomaster.core.remote.SutProblemException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

/*
Testing the local URL issue with Arazzo:
1. A local file which exists and the provided URL is valid.
 */
class ArazzoLocalURLIssueTest {

    // companion object to set up tests.
    companion object {

        // execution path, it can be different from one machine to another
        private var executionPath: String = System.getProperty("user.dir")

        // swagger object
        private lateinit var swagger: SchemaOpenAPI

        // arazzo object
        private lateinit var arazzo: SchemaArazzo

        // swagger test directory to find test files
        private lateinit var swaggerTestDirectory: String

        // arazzo test directory to find test files
        private lateinit var arazzoTestDirectory: String

        // host operating system
        private lateinit var hostOs: String

        @JvmStatic
        @BeforeAll
        // This is to deal with differences in Windows and Linux paths
        fun setSwaggerDirectoryBasedOnOS() {

            // get the name of the current operating system
            hostOs = System.getProperty("os.name").lowercase()

            // if the operating system is Windows, then replace \ with /
            if (hostOs.contains("win")) {
                executionPath = executionPath.replace('\\', '/')
            }

            // swagger files for testing
            swaggerTestDirectory = "$executionPath/src/test/resources/swagger/urlissue"

            // arazzo files for testing
            arazzoTestDirectory = "$executionPath/src/test/resources/arazzo"
        }
    }

    /*
    Test Case 1: A local file which exists and the provided URL is valid
    Check that the arazzo is created with a valid URL and an existing file
    */
    @Test
    fun testExistingFileValidURL() {
        // get the current directory, in Mac or Linux, it starts with file://
        // but in Windows, it has to have just one file:/
        val urlToTest = if (hostOs.contains("win")) {
            "file:/${swaggerTestDirectory}/openapi_pet.json"
        } else {
            "file://${swaggerTestDirectory}/openapi_pet.json"
        }

        // create swagger from URL
        swagger = OpenApiAccess.getOpenAPIFromLocation(urlToTest)

        // get the current directory, in Mac or Linux, it starts with file://
        // but in Windows, it has to have just one file:/
        val urlArazzoToTest = if (hostOs.contains("win")) {
            "file:/$arazzoTestDirectory/arazzo_pet.yaml"
        } else {
            "file://$arazzoTestDirectory/arazzo_pet.yaml"
        }

        // create arazzo from URL
        val arazzo = ArazzoAccess.getArazzoFromLocation(urlArazzoToTest, swagger)

        // a valid arazzo is created with 3 workflows
        Assertions.assertTrue(arazzo.schemaParsed.workflows.size == 3)
        Assertions.assertEquals("Petstore - Apply Coupons", arazzo.schemaParsed.info.title)
    }

}