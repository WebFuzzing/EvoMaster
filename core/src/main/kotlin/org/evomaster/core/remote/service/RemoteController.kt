package org.evomaster.core.remote.service

import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.core.scheduletask.ScheduleTaskExecutor
import org.evomaster.core.sql.DatabaseExecutor


/**
 * Class used to communicate with the remote RestController that does
 * handle the SUT.
 */
interface RemoteController : DatabaseExecutor, ScheduleTaskExecutor {

    /**
     * Return all information regarding the System Under Test (SUT),
     * like the problem definition and other preferred configuration settings.
     *
     * Note: some of this info might be dynamic, and so this call might give different results when
     * executed multiple-times
     */
    fun getSutInfo(): SutInfoDto?

    /**
     * Return the latest cached SUT info, or make new call if this is the first.
     * This is useful for optimization reasons when we are not interested in any dynamic information,
     * and so a cached value would be fine
     */
    fun getCachedSutInfo(): SutInfoDto? {
        return getSutInfo()
    }

    fun getControllerInfo(): ControllerInfoDto?

    fun startSUT() : Boolean

    fun stopSUT()  : Boolean

    fun resetSUT()  : Boolean

    fun checkConnection()

    fun startANewSearch(): Boolean

    fun getTestResults(ids: Set<Int> = setOf(),
                       ignoreKillSwitch: Boolean = false,
                       fullyCovered: Boolean = false,
                       descriptiveIds: Boolean = false,
    ): TestResultsDto?

    fun executeNewRPCActionAndGetResponse(actionDto: ActionDto) : ActionResponseDto?

    fun postSearchAction(postSearchActionDto: PostSearchActionDto) : Boolean

    fun registerNewAction(actionDto: ActionDto) : Boolean

    fun address() : String

    fun close()
}