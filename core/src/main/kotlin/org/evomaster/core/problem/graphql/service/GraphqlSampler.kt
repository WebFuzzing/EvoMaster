package org.evomaster.core.problem.graphql.service

//import graphql.schema.idl.TypeDefinitionRegistry
import java.net.URLEncoder
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GraphqlAction
import org.evomaster.core.problem.graphql.GraphqlIndividual
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class GraphqlSampler : Sampler<GraphqlIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestSampler::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var configuration: EMConfig

    private val adHocInitialIndividuals: MutableList<GraphqlAction> = mutableListOf()

    private val modelCluster: MutableMap<String, ObjectGene> = mutableMapOf()

    @PostConstruct
    private fun initialize() {

        GraphqlSampler.log.debug("Initializing {}", GraphqlSampler::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val schema = getSchema(infoDto)
//        if (swagger.paths == null) {
//            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
//        }

    }

    override fun sampleAtRandom(): GraphqlIndividual {
        print("YEEEESSS")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getSchema(infoDto: SutInfoDto): String {

        val graphqlEndpointUrl = infoDto?.graphqlProblem?.graphqlEndpointUrl ?: throw java.lang.IllegalStateException("Missing information about the graphql endpoint")

        val query = createSchemaFetchingRequest()
        val response = connectToEndpoint(graphqlEndpointUrl, 30, query)

        return "hi"

    }

    private fun connectToEndpoint(endPointUrl: String, attempts: Int, query: String): Response {

        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                        .target(endPointUrl + "?query=" + query)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get()
            } catch (e: Exception) {

                if (e.cause is ConnectException) {
                    Thread.sleep(1_000)
                } else {
                    throw IllegalStateException("Failed to connect to $endPointUrl: ${e.message}")
                }
            }
        }

        throw IllegalStateException("Failed to connect to $endPointUrl")
    }

    private fun createSchemaFetchingRequest(): String {

        // Todo: the query value is a bit ulgy ! should be refactored !
        val query = "query IntrospectionQuery {      __schema {        queryType { name }        " +
                "mutationType { name }        subscriptionType { name }        types {          ...FullType        }        " +
                "directives {          name          description          locations          args {            ...InputValue          }   " +
                "     }      }    }    fragment FullType on __Type {      kind      name      description      " +
                "fields(includeDeprecated: true) {        name        description        args {          ...InputValue        }        " +
                "type {          ...TypeRef        }        isDeprecated        deprecationReason      }      " +
                "inputFields {        ...InputValue      }      interfaces {        ...TypeRef      }      " +
                "enumValues(includeDeprecated: true) {        name        description        isDeprecated        deprecationReason      } " +
                "     possibleTypes {        ...TypeRef      }    }    fragment InputValue on __InputValue" +
                " {      name      description      type { ...TypeRef }      defaultValue    }    fragment TypeRef on __Type " +
                "{      kind      name      ofType {        kind        name        ofType " +
                "{          kind          name          ofType {            kind            name            ofType" +
                " {              kind              name              ofType {                kind                name                ofType " +
                "{                  kind                  name                  ofType {                    kind                    name                  }" +
                "                }              }            }          }        }      }    }"

        return URLEncoder.encode(query, "utf-8")

    }
}