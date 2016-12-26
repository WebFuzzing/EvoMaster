package org.evomaster.core.problem.rest

import org.evomaster.clientJava.controllerApi.ControllerConstants
import org.evomaster.clientJava.controllerApi.SutInfoDto
import org.evomaster.clientJava.controllerApi.SutRunDto
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


    private fun getTarget(): WebTarget {

        return client.target("http://$host:$port" + ControllerConstants.BASE_PATH)
    }

    fun close(){
        client.close()
    }

    fun getInfo(): SutInfoDto? {

        val response = getTarget()
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


    private fun changeState(run: Boolean, reset: Boolean): Boolean {

        val response = getTarget()
                .path(ControllerConstants.RUN_SUT_PATH)
                .request()
                .put(Entity.json(SutRunDto(run, reset)))

        val success = response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)

        if (!success) {
            log.warn("Failed to change running state of the SUT. HTTP status: {}", response.status)
        }

        return success
    }


    fun startSUT() = changeState(true, false)

    fun stopSUT() = changeState(false, false)

    fun resetSUT() = changeState(true, true)

}