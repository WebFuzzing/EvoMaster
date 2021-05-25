package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.SchemaObj
import org.evomaster.core.remote.SutProblemException
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


class IntrospectiveQuery {

    companion object{
        private val log = LoggerFactory.getLogger(IntrospectiveQuery::class.java)
    }

    private val clientConfiguration = ClientConfig()
            .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
            .property(ClientProperties.READ_TIMEOUT, 10_000)
            //workaround bug in Jersey client
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.FOLLOW_REDIRECTS, false)


    /**
     * Client library to make HTTP calls. To get the schema from a GraphQL API, we need to make
     * an HTTP call that has, as body payload, and introspective query
     */
    private var client: Client = ClientBuilder.newClient(clientConfiguration)


    fun fetchSchema(
            /**
             * The endpoint URL of where we can query the GraphQL SUT
             */
            graphQlEndpoint: String) : String{

        /*
            Body payload for the introspective query
         */
        val query = Entity.entity("""
                    {
                        "query": "fragment FullType on __Type {  kind  name  fields(includeDeprecated: true) {    name    args {      ...InputValue    }   type {      ...TypeRef    }    isDeprecated    deprecationReason  }  inputFields {    ...InputValue  }  interfaces {    ...TypeRef  }  enumValues(includeDeprecated: true) {    name    isDeprecated    deprecationReason  }  possibleTypes {    ...TypeRef  }} fragment InputValue on __InputValue {  name type {   ...TypeRef  }  defaultValue}fragment TypeRef on __Type {  kind  name  ofType {    kind    name   ofType {      kind      name      ofType {        kind       name       ofType {          kind          name         ofType {            kind            name            ofType {             kind             name              ofType {                kind                name              }            }         }        }      }    }  }}query IntrospectionQuery {  __schema {    queryType {     name   }    mutationType {      name    }    types {      ...FullType   }    directives {      name      locations      args {        ...InputValue     }    } }}","variables":null,"operationName":"IntrospectionQuery"
                    }
                """.trimIndent(), MediaType.APPLICATION_JSON_TYPE)

        //TODO check if TCP problems
        val response = try {
            client.target(graphQlEndpoint)
                    .request("application/json")
                    .buildPost(query)
                    .invoke()
        } catch (e: Exception){
            log.error("Failed query to '$graphQlEndpoint' :  $query")
            throw e
        }

        //TODO check status code, and any other problem inside GraphQL response

        if(response.status != 200){
            throw SutProblemException("Failed to retrieve GraphQL schema. Status code: ${response.status}")
        }

        /*
            Extract the body from response as a string
         */
        val body = response.readEntity(String::class.java)

        return body
    }
}