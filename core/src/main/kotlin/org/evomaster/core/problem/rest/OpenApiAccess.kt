package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.remote.SutProblemException
import java.net.ConnectException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Created by arcuri82 on 22-Jan-20.
 */
object OpenApiAccess {

    fun getOpenAPI(openApiUrl: String): OpenAPI {

        val response = connectToServer(openApiUrl, 10)

        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw SutProblemException("Cannot retrieve Swagger JSON data from $openApiUrl , status=${response.status}")
        }

        val json = response.readEntity(String::class.java)

        val schema = try {
            OpenAPIParser().readContents(json, null, null).openAPI
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse OpenApi JSON data: $e")
        }

        return schema
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