package org.evomaster.core.problem.rest

import org.evomaster.clientJava.controllerApi.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


/**
 * Class used to communicate with the remote RestController that does
 * handle the SUT
 */
class RemoteController(val host: String, val port: Int) {

    companion object {
        val log : Logger = LoggerFactory.getLogger(RemoteController::class.java)
    }

    private val client: Client = ClientBuilder.newClient()


    private fun getWebTarget(): WebTarget {

        return client.target("http://$host:$port" + ControllerConstants.BASE_PATH)
    }

    fun close(){
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

    fun getControllerInfo() : ControllerInfoDto? {

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

        val response = getWebTarget()
                .path(ControllerConstants.RUN_SUT_PATH)
                .request()
                .put(Entity.json(SutRunDto(run, reset)))

        val success = wasSuccess(response)

        if (!success) {
            log.warn("Failed to change running state of the SUT. HTTP status: {}", response.status)
        }

        return success
    }


    fun startSUT() = changeState(true, false)

    fun stopSUT() = changeState(false, false)

    fun resetSUT() = changeState(true, true)


    fun getTargetCoverage(ids: Set<Int>) : TargetsResponseDto?{

        val queryParam = ids.joinToString(",")

        val response = getWebTarget()
                .path(ControllerConstants.TARGETS_PATH)
                .queryParam("ids", queryParam)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        val success = wasSuccess(response)

        if (!success) {
            log.warn("Failed to change running state of the SUT. HTTP status: {}", response.status)
            return null
        }

        return response.readEntity(TargetsResponseDto::class.java)
    }

    fun wasSuccess(response: Response?) : Boolean{
        return response?.statusInfo?.family?.equals(Response.Status.Family.SUCCESSFUL) ?: false
    }

}