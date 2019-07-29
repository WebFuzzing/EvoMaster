package org.evomaster.core.remote.service

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * Class used to communicate with the remote RestController that does
 * handle the SUT.
 */
class RemoteController() : DatabaseExecutor {

    companion object {
        val log: Logger = LoggerFactory.getLogger(RemoteController::class.java)
    }

    lateinit var host: String
    var port: Int = 0

    private var computeSqlHeuristics = true


    @Inject
    private lateinit var config: EMConfig

    private val client: Client = ClientBuilder.newClient()

    constructor(host: String, port: Int, computeSqlHeuristics: Boolean) : this() {
        this.host = host
        this.port = port
        this.computeSqlHeuristics = computeSqlHeuristics;
    }

    @PostConstruct
    private fun initialize() {
        host = config.sutControllerHost
        port = config.sutControllerPort
        computeSqlHeuristics = config.heuristicsForSQL
    }

    @PreDestroy
    private fun preDestroy() {
        close()
    }

    private fun getWebTarget(): WebTarget {
        return client.target("http://$host:$port" + ControllerConstants.BASE_PATH)
    }


    fun close() {
        client.close()
    }

    fun getSutInfo(): SutInfoDto? {

        val response = getWebTarget()
                .path(ControllerConstants.INFO_SUT_PATH)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        val dto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<SutInfoDto>>() {})
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            null
        }

        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL  || dto?.error != null) {
            log.warn("Failed request to EM controller. HTTP status {}. Message: '{}'", response.status, dto?.error)
            return null
        }

        if (dto?.data == null) {
            log.warn("Missing DTO")
            return null
        }

        return dto.data
    }

    fun getControllerInfo(): ControllerInfoDto? {

        val response = getWebTarget()
                .path(ControllerConstants.CONTROLLER_INFO)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        val dto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<ControllerInfoDto>>() {})
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            null
        }

        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL  || dto?.error != null) {
            log.warn("Failed request to EM controller. HTTP status {}. Message: '{}'", response.status, dto?.error)
            return null
        }

        if (dto?.data == null) {
            log.warn("Missing DTO")
            return null
        }

        return dto.data
    }

    private fun changeState(run: Boolean, reset: Boolean): Boolean {

        val response = try {
            getWebTarget()
                    .path(ControllerConstants.RUN_SUT_PATH)
                    .request()
                    .put(Entity.json(SutRunDto(run, reset, computeSqlHeuristics)))
        } catch (e: Exception) {
            log.warn("Failed to connect to SUT: ${e.message}")
            return false
        }

        val success = wasSuccess(response)

        if (!success) {
            log.warn("Failed to change running state of the SUT. HTTP status: {}", response.status)
        }

        return success
    }

    /*
        Starting implies a clean reset state.
        Reset needs SUT to be up and running.
        If SUT already running, no need to restart it, we can reset its state.
        So, start and reset have same functionality here.
    */

    fun startSUT() = changeState(true, true)

    fun stopSUT() = changeState(false, false)

    fun resetSUT() = startSUT()

    fun checkConnection() {

        try {
            getWebTarget()
                    .path(ControllerConstants.CONTROLLER_INFO)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        } catch (e: Exception) {
            throw NoRemoteConnectionException(port, host)
        }
    }

    fun startANewSearch(): Boolean {

        val response = getWebTarget()
                .path(ControllerConstants.NEW_SEARCH)
                .request()
                .post(Entity.entity("{\"newSearch\"=true}", MediaType.APPLICATION_JSON_TYPE))

        if (!wasSuccess(response)) {
            log.warn("Failed to inform SUT of new search. HTTP status: {}", response.status)
            return false
        }

        return true
    }

    fun getTestResults(ids: Set<Int> = setOf()): TestResultsDto? {

        val queryParam = ids.joinToString(",")

        val response = getWebTarget()
                .path(ControllerConstants.TEST_RESULTS)
                .queryParam("ids", queryParam)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        if (!wasSuccess(response)) {
            log.warn("Failed to retrieve target coverage. HTTP status: {}", response.status)
            return null
        }

        val dto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<TestResultsDto>>() {})
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            null
        }

        return dto?.data
    }

    fun registerNewAction(actionIndex: Int) {

        val response = getWebTarget()
                .path(ControllerConstants.NEW_ACTION)
                .request()
                .put(Entity.entity(actionIndex, MediaType.APPLICATION_JSON_TYPE))

        if (!wasSuccess(response)) {
            log.warn("Failed to register new action. HTTP status: {}", response.status)
        }
    }

    override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {

        val response = getWebTarget()
                .path(ControllerConstants.DATABASE_COMMAND)
                .request()
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))

        if (!wasSuccess(response)) {
            LoggingUtil.uniqueWarn(log, "Failed to execute database command. HTTP status: {}.", response.status)

            if(response.mediaType == MediaType.TEXT_PLAIN_TYPE){
                //something weird is going on... possibly a bug in the Driver?

                val res = response.readEntity(String::class.java)
                log.error("Database command failure, HTTP status ${response.status}: $res")
                return false
            }

            val responseDto = try {
                response.readEntity(object : GenericType<WrappedResponseDto<*>>() {})
            } catch (e: Exception) {
                handleFailedDtoParsing(e)
                return false
            }

            if(responseDto?.error != null) {
                //this can happen if we do not handle all constraints
                LoggingUtil.uniqueWarn(log, "Error message: " + responseDto.error)
            }
            /*
                TODO refactor all methods in this class to print error messages, if any
             */

            return false
        }

        return true
    }

    override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
        return executeDatabaseCommandAndGetResults(dto, object : GenericType<WrappedResponseDto<QueryResultDto>>() {})
    }

    override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): Map<Long, Long>? {
        return executeDatabaseCommandAndGetResults(dto, object : GenericType<WrappedResponseDto<Map<Long, Long>>>() {})
    }

    private fun <T> executeDatabaseCommandAndGetResults(dto: DatabaseCommandDto, type: GenericType<WrappedResponseDto<T>>): T? {

        val response = getWebTarget()
                .path(ControllerConstants.DATABASE_COMMAND)
                .request()
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))

        val responseDto = try {
            response.readEntity(type)
        } catch (e: Exception) {
            handleFailedDtoParsing(e)
            return null
        }

        if (!wasSuccess(response)) {
            log.warn("Failed to execute database command. HTTP status: {}.", response.status)

            if(responseDto?.error != null) {
                log.warn("Error message: " + responseDto.error)
            }

            return null
        }

        return responseDto.data
    }


    private fun wasSuccess(response: Response?): Boolean {
        return response?.statusInfo?.family?.equals(Response.Status.Family.SUCCESSFUL) ?: false
    }

    private fun handleFailedDtoParsing(exception: Exception){

        if(exception is ProcessingException && exception.cause is UnrecognizedPropertyException){

            val version = this.javaClass.`package`?.implementationVersion
                    ?: "(cannot determine, likely due to EvoMaster being run directly from IDE and not as a packaged uber jar)"

            throw SutProblemException("There is a mismatch between the DTO that EvoMaster Driver is " +
                    "sending and what the EvoMaster Core process (this process) is expecting to receive. " +
                    "Are you sure you are using the same matching versions? This EvoMaster Core " +
                    "process version is: $version")
        } else {
            log.warn("Failed to parse dto", exception)
        }
    }
}