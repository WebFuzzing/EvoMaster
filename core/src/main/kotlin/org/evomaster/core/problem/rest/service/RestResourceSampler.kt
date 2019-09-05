package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.RestActionBuilder
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Action
import java.net.ConnectException
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class RestResourceSampler : ResourceSampler(){

    @Inject
    private lateinit var rc: RemoteController

    @PostConstruct
    private fun initialize() {

        assert(config.resourceSampleStrategy != EMConfig.ResourceSamplingStrategy.NONE)

        log.debug("Initializing {}", RestResourceSampler::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val swagger = getSwagger(infoDto)
        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        val actionCluster = mutableMapOf<String, Action>()
        actionCluster.clear()
        RestActionBuilder.addActionsFromSwagger(swagger, actionCluster, infoDto.restProblem?.endpointsToSkip ?: listOf(), doParseDescription = config.doesApplyNameMatching)

        setupAuthentication(infoDto)

        val sqlBuilder = if (infoDto.sqlSchemaDto != null && (config.shouldGenerateSqlData() || config.extractSqlExecutionInfo || (config.probOfApplySQLActionToCreateResources > 0.0))) {
            SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
        }else null

        assert(config.resourceSampleStrategy != EMConfig.ResourceSamplingStrategy.NONE)

        initialize(setupAuthentication(infoDto), actionCluster, sqlBuilder)

        if(config.outputFormat == OutputFormat.DEFAULT){
            try {
                val format = OutputFormat.valueOf(infoDto.defaultOutputFormat?.toString()!!)
                config.outputFormat = format
            } catch (e : Exception){
                throw SutProblemException("Failed to use test output format: " + infoDto.defaultOutputFormat)
            }
        }

        log.debug("Done initializing {}", RestResourceSampler::class.simpleName)
    }

    private fun setupAuthentication(infoDto: SutInfoDto) : MutableList<AuthenticationInfo>{

        val authenticationsInfo = mutableListOf<AuthenticationInfo>()

        val info = infoDto.infoForAuthentication ?: return authenticationsInfo

        info.forEach { i ->
            if (i.name == null || i.name.isBlank()) {
                ResourceSampler.log.warn("Missing name in authentication info")
                return@forEach
            }

            val headers: MutableList<AuthenticationHeader> = mutableListOf()

            i.headers.forEach loop@{ h ->
                val name = h.name?.trim()
                val value = h.value?.trim()
                if (name == null || value == null) {
                    ResourceSampler.log.warn("Invalid header in ${i.name}")
                    return@loop
                }

                headers.add(AuthenticationHeader(name, value))
            }

            val auth = AuthenticationInfo(i.name.trim(), headers)

            authenticationsInfo.add(auth)
        }
        return authenticationsInfo
    }

    private fun getSwagger(infoDto: SutInfoDto): Swagger {

        val swaggerURL = infoDto.restProblem?.swaggerJsonUrl ?: throw IllegalStateException("Missing information about the Swagger URL")

        val response = connectToSwagger(swaggerURL, 30)

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw SutProblemException("Cannot retrieve Swagger JSON data from $swaggerURL , status=${response.status}")
        }

        val json = response.readEntity(String::class.java)

        val swagger = try {
            SwaggerParser().parse(json)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse Swagger JSON data: $e")
        }

        return swagger
    }

    private fun connectToSwagger(swaggerURL: String, attempts: Int): Response {

        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                        .target(swaggerURL)
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
                    throw IllegalStateException("Failed to connect to $swaggerURL: ${e.message}")
                }
            }
        }

        throw IllegalStateException("Failed to connect to $swaggerURL")
    }

}