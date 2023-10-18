package org.evomaster.core.problem.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import org.evomaster.core.remote.SutProblemException
import java.net.ConnectException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Created by arcuri82 on 22-Jan-20.
 */
object OpenApiAccess {

    fun getOpenApi(schemaText: String): OpenAPI {

        var parseResults: SwaggerParseResult? = null

        for (extension in OpenAPIV3Parser.getExtensions()) {
            parseResults = try {
                extension.readContents(schemaText, null, null)
            } catch (e: Exception) {
                throw SutProblemException("Failed to parse OpenApi schema: $e")
            }
            if (parseResults != null && parseResults.openAPI != null) {
                break
            }
        }

        return parseResults!!.openAPI
                ?: throw SutProblemException("Failed to parse OpenApi schema: " + parseResults.messages.joinToString("\n"))
    }

    fun getOpenAPIFromURL(openApiUrl: String): OpenAPI {

        //could be either JSON or YAML
       val data = if(openApiUrl.startsWith("http", true)){
           readFromRemoteServer(openApiUrl)
       } else {
           readFromDisk(openApiUrl)
       }

        return getOpenApi(data)
    }

    private fun readFromRemoteServer(openApiUrl: String) : String{
        val response = connectToServer(openApiUrl, 10)

        val body = response.readEntity(String::class.java)

        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw SutProblemException("Cannot retrieve OpenAPI schema from $openApiUrl ," +
                    " status=${response.status} , body: $body")
        }

        return body
    }

    private fun readFromDisk(openApiUrl: String) : String {

        // file schema
        val fileScheme = "file:"

        // create paths
        val path = try {
            if (openApiUrl.startsWith(fileScheme, true)) {
                Paths.get(URI.create(openApiUrl))
            }
            else {
                Paths.get(openApiUrl)
            }
        }
        // Exception is thrown if the path is not valid
        catch (e: Exception) {
            // state the exception with the error message
            throw SutProblemException("The file path provided for the OpenAPI Schema $openApiUrl" +
                        " ended up with the following error: " + e.message)
        }

        // If the path is valid but the file does not exist, an exception is thrown
        if (!Files.exists(path)) {
            throw SutProblemException("The provided OpenAPI file does not exist: $openApiUrl")
        }

        // return the schema text
        return path.toFile().readText()
    }

    private fun connectToServer(openApiUrl: String, attempts: Int): Response {

        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                        .target(openApiUrl)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get()
            } catch (e: Exception) {

                if (e.cause is ConnectException) {
                    /*
                        Even if SUT is running, Swagger service might not be ready
                        yet. So let's just wait a bit, and then retry
                    */
                    Thread.sleep(1_000)
                } else {
                    throw SutProblemException("Failed to connect to $openApiUrl: ${e.message}")
                }
            }
        }

        throw SutProblemException("Check if schema's URL is correct. Failed to connect to $openApiUrl")
    }
}