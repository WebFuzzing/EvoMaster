package org.evomaster.core.problem.asyncapi.builder

import com.webfuzzing.asyncapi.models.AsyncApiDocument
import com.webfuzzing.asyncapi.models.AsyncApiOperation
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.util.ActionBuilderUtil
import org.evomaster.core.search.action.Action

/**
 * Turns a parsed AsyncAPI document into the actions a search samples from, one per message that
 * can actually be published.
 *
 * This is the AsyncAPI counterpart of
 * [org.evomaster.core.problem.rest.builder.RestActionBuilderV3.addActionsFromSwagger] and
 * [org.evomaster.core.problem.graphql.builder.GraphQLActionBuilder.addActionsFromSchema], and
 * keeps their contract: fill a cluster keyed by action name, and return what had to be skipped
 * rather than raising.
 */
object AsyncApiActionBuilder {

    /**
     * Build one action per publishable message and add them to [actionCluster].
     *
     * @return anything that could not be built, to be reported to the user
     */
    fun addActionsFromSchema(
        schema: AsyncApiDocument,
        actionCluster: MutableMap<String, Action>,
        options: RestActionBuilderV3.Options
    ): List<String> {

        actionCluster.clear()

        val messages = mutableListOf<String>()
        var skipped = 0

        schema.operations.values.forEach { operation ->

            /*
                Only what the service consumes can be published to. An operation the service
                sends is something to subscribe to, not to drive, so it is counted rather than
                turned into an action.
             */
            if (operation.action != AsyncApiOperation.Action.RECEIVE) {
                skipped++
                return@forEach
            }

            val built = buildActionsFor(operation, schema, options, messages)

            if (built.isEmpty()) {
                skipped++
            }

            built.forEach { actionCluster[it.getName()] = it }
        }

        ActionBuilderUtil.printActionNumberInfo("AsyncAPI", actionCluster.size, skipped, 0)

        return messages
    }

    private fun buildActionsFor(
        operation: AsyncApiOperation,
        schema: AsyncApiDocument,
        options: RestActionBuilderV3.Options,
        messages: MutableList<String>
    ): List<AsyncApiAction> {

        val carried = schema.messagesOf(operation)

        val actions = carried.mapNotNull { message ->

            val payload = try {
                AsyncApiGeneBuilder.buildPayloadGene(schema, message, options)
            } catch (e: Exception) {
                /*
                    One message that cannot be built must not cost the whole document, which is
                    the same rule the parser follows.
                 */
                messages.add(
                    "Failed to build the payload of message '${message.id}' for operation" +
                            " '${operation.name}': ${e.message}"
                )
                return@mapNotNull null
            }

            val headers = try {
                AsyncApiGeneBuilder.buildHeadersGene(schema, message, options)
            } catch (e: Exception) {
                messages.add(
                    "Failed to build the headers of message '${message.id}' for operation" +
                            " '${operation.name}': ${e.message}"
                )
                null
            }

            if (payload == null && headers == null) {
                //nothing to vary and nothing to send: there is no action to make of it
                messages.add(
                    "Message '${message.id}' of operation '${operation.name}' declares neither a" +
                            " payload nor headers, so there is nothing to publish"
                )
                return@mapNotNull null
            }

            val parameters = mutableListOf<Param>()
            payload?.let { parameters.add(AsyncApiParam(AsyncApiParam.PAYLOAD, it)) }
            headers?.let { parameters.add(AsyncApiParam(AsyncApiParam.HEADERS, it)) }

            AsyncApiAction(
                operationId = operation.name,
                channelName = operation.channelName,
                messageId = message.id,
                inputParameters = parameters,
                replyTemplate = operation.reply
            )
        }

        if (carried.isEmpty()) {
            messages.add("Operation '${operation.name}' carries no message that can be published")
        }

        //only when an operation carries more than one does the message need naming in the action
        actions.forEach { it.singleMessage = actions.size == 1 }

        return actions
    }
}
