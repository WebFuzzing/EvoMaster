package org.evomaster.core.remote.service

import com.google.inject.Inject
import org.evomaster.clientJava.controllerApi.ControllerConstants
import org.evomaster.clientJava.controllerApi.dto.*
import org.evomaster.clientJava.controllerApi.dto.database.operations.DatabaseCommandDto
import org.evomaster.core.EMConfig
import org.evomaster.core.remote.NoRemoteConnectionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Form
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * Class used to communicate with the remote RestController that does
 * handle the SUT.
 */
class RemoteController() {

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

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            log.warn("Failed to connect to remote RestController. HTTP status: {}", response.status)
            return null
        }

        val dto = try {
            response.readEntity(SutInfoDto::class.java)
        } catch (e: Exception) {
            log.warn("Failed to parse SUT info dto", e)
            null
        }

        return dto
    }

    fun getControllerInfo(): ControllerInfoDto? {

        val response = getWebTarget()
                .path(ControllerConstants.CONTROLLER_INFO)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            log.warn("Failed to connect to remote RestController. HTTP status: {}", response.status)
            return null
        }

        val dto = try {
            response.readEntity(ControllerInfoDto::class.java)
        } catch (e: Exception) {
            log.warn("Failed to parse the controller info dto", e)
            null
        }

        return dto
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

        try{
            getWebTarget()
                    .path(ControllerConstants.CONTROLLER_INFO)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        } catch (e : Exception){
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

    fun getTargetCoverage(ids: Set<Int> = setOf()): TargetsResponseDto? {

        val queryParam = ids.joinToString(",")

        val response = getWebTarget()
                .path(ControllerConstants.TARGETS_PATH)
                .queryParam("ids", queryParam)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        if (!wasSuccess(response)) {
            log.warn("Failed to change running state of the SUT. HTTP status: {}", response.status)
            return null
        }

        return response.readEntity(TargetsResponseDto::class.java)
    }

    fun getExtraHeuristics(): ExtraHeuristicDto? {

        val response = getWebTarget()
                .path(ControllerConstants.EXTRA_HEURISTICS)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        if (!wasSuccess(response)) {
            log.warn("Failed to retrieve extra heuristics. HTTP status: {}", response.status)
            return null
        }

        return response.readEntity(ExtraHeuristicDto::class.java)
    }

    fun registerNewAction(actionIndex: Int){

        val response = getWebTarget()
                .path(ControllerConstants.NEW_ACTION)
                .request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .put(Entity.form(Form("index", actionIndex.toString())))

        if (!wasSuccess(response)) {
            log.warn("Failed to register new action. HTTP status: {}", response.status)
        }
    }

    fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {

        val response = getWebTarget()
                .path(ControllerConstants.DATABASE_COMMAND)
                .request()
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON_TYPE))

        if (!wasSuccess(response)) {
            log.warn("Failed to execute database command. HTTP status: {}", response.status)
            return false
        }

        return true
    }


    fun resetExtraHeuristics() {

        val response = getWebTarget()
                .path(ControllerConstants.EXTRA_HEURISTICS)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete()

        if (!wasSuccess(response)) {
            log.warn("Failed to reset extra heuristics. HTTP status: {}", response.status)
        }
    }

    private fun wasSuccess(response: Response?): Boolean {
        return response?.statusInfo?.family?.equals(Response.Status.Family.SUCCESSFUL) ?: false
    }

}