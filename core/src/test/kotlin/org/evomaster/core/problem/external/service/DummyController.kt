package org.evomaster.core.problem.external.service

import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.client.java.controller.api.dto.database.operations.*
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsDto
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationsResult
import org.evomaster.core.remote.service.RemoteController

class DummyController: RemoteController {
    override fun getSutInfo(): SutInfoDto? {
        return SutInfoDto()
    }

    override fun getControllerInfo(): ControllerInfoDto? {
        TODO("Not yet implemented")
    }

    override fun startSUT(): Boolean {
        TODO("Not yet implemented")
    }

    override fun stopSUT(): Boolean {
        TODO("Not yet implemented")
    }

    override fun resetSUT(): Boolean {
        TODO("Not yet implemented")
    }

    override fun checkConnection() {
        TODO("Not yet implemented")
    }

    override fun startANewSearch(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTestResults(ids: Set<Int>, ignoreKillSwitch: Boolean,
                                fullyCovered: Boolean,
                                descriptiveIds: Boolean,): TestResultsDto? {
        TODO("Not yet implemented")
    }

    override fun executeNewRPCActionAndGetResponse(actionDto: ActionDto): ActionResponseDto? {
        TODO("Not yet implemented")
    }

    override fun postSearchAction(postSearchActionDto: PostSearchActionDto): Boolean {
        TODO("Not yet implemented")
    }

    override fun registerNewAction(actionDto: ActionDto): Boolean {
        TODO("Not yet implemented")
    }

    override fun address(): String {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
        TODO("Not yet implemented")
    }

    override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
        TODO("Not yet implemented")
    }

    override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): InsertionResultsDto? {
        TODO("Not yet implemented")
    }

    override fun executeMongoDatabaseInsertions(dto: MongoDatabaseCommandDto): MongoInsertionResultsDto? {
        TODO("Not yet implemented")
    }

    override fun invokeScheduleTasksAndGetResults(dtos: ScheduleTaskInvocationsDto): ScheduleTaskInvocationsResult? {
        TODO("Not yet implemented")
    }

}