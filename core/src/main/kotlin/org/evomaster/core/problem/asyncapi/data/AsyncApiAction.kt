package org.evomaster.core.problem.asyncapi.data

import com.webfuzzing.asyncapi.models.AsyncApiReply
import org.evomaster.core.problem.api.ApiWsAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.asyncapi.auth.AsyncApiNoAuth
import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.search.gene.Gene

/**
 * Publishing one message on one channel, modelled on [org.evomaster.core.problem.rpc.RPCCallAction].
 *
 * The genes are the payload and the headers. The channel is fixed by the contract, the
 * correlation id is stamped at execution time, and the reply is observed, so none of them is one.
 */
class AsyncApiAction(

    /**
     * Key of the operation in the document.
     *
     * This is the unit coverage is counted against, so it is taken from the document verbatim
     * and never synthesised: `(reply variant x operation)` is the AsyncAPI analogue of REST's
     * `(status x endpoint)`, and it only means anything if the operation is stable.
     */
    val operationId: String,

    /**
     * Key of the channel the message is published on. The address it resolves to depends on the
     * transport, so it is left to be resolved by whatever holds the connection.
     */
    val channelName: String,

    /**
     * Id of the message being published. An operation may carry several, in which case there is
     * one action per message: which message to send is a choice the search makes by picking an
     * action, not by mutating a gene.
     */
    val messageId: String,

    /**
     * The payload and, when the message declares them, the headers. These are the genes.
     */
    inputParameters: MutableList<Param>,

    /**
     * What the contract says a reply may be, when the operation declares one. Immutable, and
     * not part of the children of this action: it is a description of what to expect, not
     * something to vary. Null for a fire-and-forget operation.
     */
    val replyTemplate: AsyncApiReply? = null,

    override var auth: AuthenticationInfo = AsyncApiNoAuth()

) : ApiWsAction(auth, false, inputParameters) {

    /**
     * Whether this action is the only one built for its operation. Set by whoever builds the
     * cluster, since it depends on the other actions rather than on this one.
     */
    var singleMessage: Boolean = true

    companion object {
        /**
         * The name an action is known by, which must be unique within a search.
         *
         * An operation carrying a single message is named after the operation alone, since
         * that reads better in a generated test; only when there are several does the message
         * need naming too.
         */
        fun nameFor(operationId: String, messageId: String, alone: Boolean) =
            if (alone) operationId else "$operationId:$messageId"
    }

    override fun getName(): String = nameFor(operationId, messageId, singleMessage)

    override fun seeTopGenes(): List<Gene> = parameters.flatMap { it.seeGenes() }

    override fun copyContent(): AsyncApiAction =
        AsyncApiAction(
            operationId,
            channelName,
            messageId,
            parameters.asSequence().map(Param::copy).toMutableList(),
            replyTemplate,
            auth
        ).also { it.singleMessage = singleMessage }

    /**
     * Whether the contract promises something observable comes back. Only such an operation can
     * be judged from outside without instrumentation.
     */
    fun expectsReply(): Boolean = replyTemplate != null

    override fun toString(): String = "${getName()} on $channelName"
}
