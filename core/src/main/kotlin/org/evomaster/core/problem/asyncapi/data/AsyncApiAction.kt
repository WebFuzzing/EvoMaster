package org.evomaster.core.problem.asyncapi.data

import com.webfuzzing.asyncapi.models.AsyncApiReply
import org.evomaster.core.problem.api.ApiWsAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.asyncapi.auth.AsyncApiNoAuth
import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.search.gene.Gene

/**
 * Publishing one message on one channel: the thing an AsyncAPI search actually does.
 *
 * It follows the shape of [org.evomaster.core.problem.rpc.RPCCallAction], which is the closest
 * analogue in EvoMaster -- a call with no URL, made through a driver, whose mutable state is
 * the input and whose response is read afterwards rather than searched over.
 *
 * Note what is deliberately *not* a gene:
 *
 * - the **address**, which is fixed by the contract. There is nothing to search over in where
 *   a message goes; sending to a channel the service does not read would only waste executions.
 * - the **correlation id**, which is stamped fresh at each execution. Searching over it could
 *   achieve nothing, since the service only echoes it back, and pairing a reply with its
 *   request needs a value unique to each execution rather than one carried in the genome.
 * - the **reply**, which is an observation. It is read at fitness time to decide what was
 *   covered, exactly as RPC reads its response.
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

    /**
     * Whether this action is the only one built for its operation. Set by whoever builds the
     * cluster, since it depends on the other actions rather than on this one.
     */
    var singleMessage: Boolean = true

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
