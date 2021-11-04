package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import org.apache.commons.io.FileUtils
import org.evomaster.core.remote.SutProblemException
import java.net.ConnectException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Created by arcuri82 on 22-Jan-20.
 */
object OpenApiAccess {

    fun getOpenAPI(openApiUrl: String): OpenAPI {

        //could be either JSON or YAML
       val data = if(openApiUrl.startsWith("http", true)){
           readFromRemoteServer(openApiUrl)
       } else {
           readFromDisk(openApiUrl)
       }

        val schema = try {
            OpenAPIParser().readContents(data, null, null).openAPI
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse OpenApi schema: $e")
        }

        return schema
    }

    private fun readFromRemoteServer(openApiUrl: String) : String{
        val response = connectToServer(openApiUrl, 10)

        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw SutProblemException("Cannot retrieve OpenAPI schema from $openApiUrl , status=${response.status}")
        }

        return response.readEntity(String::class.java)
    }

    private fun readFromDisk(openApiUrl: String) : String {
        val fileScheme = "file:"
        val path = if (openApiUrl.startsWith(fileScheme, true)) {
            Paths.get(URI.create(openApiUrl))
        } else {
            Paths.get(openApiUrl)
        }
        if (!Files.exists(path)) {
            throw SutProblemException("Cannot find OpenAPI schema at file location: $openApiUrl")
        }

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
                    throw IllegalStateException("Failed to connect to $openApiUrl: ${e.message}")
                }
            }
        }

        throw IllegalStateException("Failed to connect to $openApiUrl")
    }
}