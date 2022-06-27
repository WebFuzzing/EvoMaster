package org.evomaster.core.remote.service

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.inject.Inject
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.search.ActionResult
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
interface RemoteController : DatabaseExecutor {

    fun getSutInfo(): SutInfoDto?

    fun getControllerInfo(): ControllerInfoDto?

    fun startSUT() : Boolean

    fun stopSUT()  : Boolean

    fun resetSUT()  : Boolean

    fun checkConnection()
    fun startANewSearch(): Boolean

    fun getTestResults(ids: Set<Int> = setOf(), ignoreKillSwitch: Boolean = false): TestResultsDto?

    fun executeNewRPCActionAndGetResponse(actionDto: ActionDto) : ActionResponseDto?

    fun registerNewAction(actionDto: ActionDto) : Boolean

    fun address() : String

    fun close()
}