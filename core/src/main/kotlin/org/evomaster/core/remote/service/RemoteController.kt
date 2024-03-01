package org.evomaster.core.remote.service

import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.core.sql.DatabaseExecutor


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

    fun getTestResults(ids: Set<Int> = setOf(), ignoreKillSwitch: Boolean = false, allCovered: Boolean = false): TestResultsDto?

    fun executeNewRPCActionAndGetResponse(actionDto: ActionDto) : ActionResponseDto?

    fun postSearchAction(postSearchActionDto: PostSearchActionDto) : Boolean

    fun registerNewAction(actionDto: ActionDto) : Boolean

    fun address() : String

    fun close()
}