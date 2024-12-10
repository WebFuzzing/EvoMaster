package org.evomaster.core.problem.graphql


import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.remote.HttpClientFactory
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.service.SearchTimeController
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.LoggerFactory
import javax.net.ssl.SSLContext
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


class IntrospectiveQuery {

    companion object {
        private val log = LoggerFactory.getLogger(IntrospectiveQuery::class.java)
    }


    /**
     * Client library to make HTTP calls. To get the schema from a GraphQL API, we need to make
     * an HTTP call that has, as body payload, and introspective query
     */
    private var client: Client = HttpClientFactory.createTrustingJerseyClient(true, 60_000)


    fun fetchSchema(
            /**
             * The endpoint URL of where we can query the GraphQL SUT
             */
            graphQlEndpoint: String,
            headers: List<String>
    ): String? {

        val list = headers.map {
            val k = it.indexOf(":")
            val name = it.substring(0, k)
            val content = it.substring(k + 1)
            Pair(name, content)
        }

        /*
            Body payload for the introspective query
         */
        val query = Entity.entity("""
                    {
                        "query": "fragment FullType on __Type {  kind  name  fields(includeDeprecated: true) {    name    args {      ...InputValue    }   type {      ...TypeRef    }    isDeprecated    deprecationReason  }  inputFields {    ...InputValue  }  interfaces {    ...TypeRef  }  enumValues(includeDeprecated: true) {    name    isDeprecated    deprecationReason  }  possibleTypes {    ...TypeRef  }} fragment InputValue on __InputValue {  name type {   ...TypeRef  }  defaultValue}fragment TypeRef on __Type {  kind  name  ofType {    kind    name   ofType {      kind      name      ofType {        kind       name       ofType {          kind          name         ofType {            kind            name            ofType {             kind             name              ofType {                kind                name              }            }         }        }      }    }  }}query IntrospectionQuery {  __schema {    queryType {     name   }    mutationType {      name    }    types {      ...FullType   }    directives {      name      locations      args {        ...InputValue     }    } }}","variables":null,"operationName":"IntrospectionQuery"
                    }
                """.trimIndent(), MediaType.APPLICATION_JSON_TYPE)

        //TODO check if TCP problems
        val response = SearchTimeController.measureTimeMillis({ ms, res ->
                LoggingUtil.getInfoLogger().info("Fetched GraphQL schema in ${ms}ms")
        }, {
            try {
                var request = client.target(graphQlEndpoint)
                    .request("application/json")

                for (h in list) {
                    request = request.header(h.first, h.second)
                }
                request.buildPost(query)
                    .invoke()
            } catch (e: Exception) {
                log.error("Failed query to '$graphQlEndpoint' :  $query")
                throw SutProblemException("Failed introspection query to '$graphQlEndpoint'." +
                        " Please check connection and URL format. Error: ${e.message}")
            }
        })




        /*
           Extract the body from response as a string
        */
        val body = response.readEntity(String::class.java)

        if (response.status != 200) {
            throw SutProblemException("Failed to retrieve GraphQL schema." +
                    " Status code: ${response.status}." +
                    " Response: $body")
        }

        val jackson = ObjectMapper()

        val node: JsonNode = try {
            jackson.readTree(body)
        } catch (e: JsonProcessingException) {
            throw SutProblemException("Failed to parse GraphQL schema as a JSON object: ${e.message}")
        }

        val withErrors= node.findPath("errors")

       if (!withErrors.isEmpty){
            throw SutProblemException("Failed to retrieve GraphQL schema." +
                    " Are introspective queries enabled on the tested application?" +
                    " Response contains error: $body .")
       }

        return body
    }
}