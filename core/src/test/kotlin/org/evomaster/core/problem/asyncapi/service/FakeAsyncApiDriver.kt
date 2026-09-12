package org.evomaster.core.problem.asyncapi.service

import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.ControllerInfoDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.problem.external.service.DummyController
import org.evomaster.core.remote.service.RemoteController

/**
 * A driver that publishes nothing: it records what it was asked to publish, and answers each
 * message with whatever [answer] decides. No instrumentation, so it reports no code targets.
 */
class FakeAsyncApiDriver(
    private val info: SutInfoDto,
    private val answer: (AsyncApiActionDto) -> AsyncApiReplyDto?
) : RemoteController by DummyController() {

    companion object {

        fun replied(payload: String, correlationMatched: Boolean = true) = AsyncApiReplyDto().apply {
            published = true
            replyExpected = true
            replyReceived = true
            replyPayload = payload
            this.correlationMatched = correlationMatched
            waitedMs = 12
        }

        fun silence(waited: Long = 5000) = AsyncApiReplyDto().apply {
            published = true
            replyExpected = true
            replyReceived = false
            waitedMs = waited
        }

        fun fireAndForget() = AsyncApiReplyDto().apply {
            published = true
            replyExpected = false
        }
    }

    /**
     * Everything this driver was asked to publish, in order.
     */
    val published: MutableList<AsyncApiActionDto> = mutableListOf()

    override fun checkConnection() {}

    override fun startSUT() = true

    override fun resetSUT() = true

    override fun startANewSearch() = true

    override fun getSutInfo() = info

    override fun getControllerInfo() = ControllerInfoDto()

    override fun registerNewAction(actionDto: ActionDto) = true

    override fun executeNewAsyncApiActionAndGetReply(actionDto: ActionDto): AsyncApiReplyDto? {
        val call = actionDto.asyncApiCall
        published.add(call)
        return answer(call)?.apply { index = actionDto.index }
    }

    override fun getTestResults(
        ids: Set<Int>,
        ignoreKillSwitch: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean
    ) = TestResultsDto()
}
