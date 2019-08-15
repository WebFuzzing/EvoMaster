package org.evomaster.core.problem.graphql.service

import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.SchemaParser
import graphql.introspection.IntrospectionResultToSchema
import graphql.introspection.Introspection
import graphql.parser.Parser
import graphql.language.Document
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Map
import graphql.introspection.IntrospectionQuery
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

        actionCluster.clear()
        modelCluster.clear()
        // Here we should create Actions and models from schema

    }

    override fun sampleAtRandom(): GraphqlIndividual {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getSchema(infoDto: SutInfoDto): TypeDefinitionRegistry
    {

        val graphqlEndpointUrl = infoDto?.graphqlProblem?.graphqlEndpointUrl ?: throw java.lang.IllegalStateException("Missing information about the graphql endpoint")

        val query = IntrospectionQuery.INTROSPECTION_QUERY
        val encodedQuery = URLEncoder.encode(query, "utf-8")
        val response = connectToEndpoint(graphqlEndpointUrl, 30, encodedQuery)

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw SutProblemException("Cannot get graphql schema info from $graphqlEndpointUrl , status=${response.status}")
        }


        val json = response.readEntity(String::class.java)

        var map = mapOf<String,Object>()
        val gson = Gson()
        map = gson.fromJson(json, object : TypeToken<Map<String, Object>>() {}.type)
        var mapJson = gson.toJson(map.getValue("data"))

        var schemaMap = mapOf<String,Object>()
        schemaMap = gson.fromJson(mapJson, object : TypeToken<Map<String, Object>>() {}.type)
        val document = IntrospectionResultToSchema().createSchemaDefinition(schemaMap)

        val schema = try{
            SchemaParser().buildRegistry(document)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse graphql schema: $e")
        }

        return schema

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
}