package org.evomaster.core.remote.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.remote.NoRemoteConnectionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
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

    @Inject
    private lateinit var config: EMConfig

    private val client: Client = ClientBuilder.newClient()

    constructor(host: String, port: Int) : this() {
        this.host = host
        this.port = port
    }

    @PostConstruct
    private fun initialize() {
        host = config.sutControllerHost
        port = config.sutControllerPort
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
            log.warn("Failed to parse SUT info dto", e)
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
            log.warn("Failed to parse controller info dto", e)
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
                    .put(Entity.json(SutRunDto(run, reset)))
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


    fun startSUT() = changeState(true, false)

    fun stopSUT() = changeState(false, false)

    fun resetSUT() = changeState(true, true)

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
            log.warn("Failed to parse target coverage dto", e)
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
            log.warn("Failed to execute database command. HTTP status: {}.", response.status)

            val responseDto = try {
                response.readEntity(object : GenericType<WrappedResponseDto<*>>() {})
            } catch (e: Exception) {
                log.warn("Failed to parse dto", e)
                return false
            }

            if(responseDto?.error != null) {
                log.warn("Error message: " + responseDto.error)
            }
            /*
                TODO refactor all methods in this class to print error message, if any
             */

            return false
        }

        return true
    }

    override fun executeDatabaseCommandAndGetResults(dto: DatabaseCommandDto): QueryResultDto? {

        val response = getWebTarget()
                .path(ControllerConstants.DATABASE_COMMAND)
                .request()
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))

        val responseDto = try {
            response.readEntity(object : GenericType<WrappedResponseDto<QueryResultDto>>() {})
        } catch (e: Exception) {
            log.warn("Failed to parse dto", e)
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

}