package org.evomaster.core.problem.asyncapi.builder

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import org.evomaster.core.EMConfig
import org.evomaster.core.database.mongo.MongoDbAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiActionBuilderTest {

    private val options = AsyncApiGeneBuilder.options(EMConfig())

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun build(resourcePath: String): Pair<Map<String, Action>, List<String>> {
        val schema = AsyncApiAccess.getAsyncApiFromResource(resourcePath)
        return build(schema)
    }

    private fun build(schema: AsyncApiDocument): Pair<Map<String, Action>, List<String>> {
        val cluster = mutableMapOf<String, Action>()
        val messages = AsyncApiActionBuilder.addActionsFromSchema(schema, cluster, options)
        return cluster to messages
    }

    private fun actionsOf(resourcePath: String) =
        build(resourcePath).first.values.map { it as AsyncApiAction }

    // ------------------------------------------------------------------ what becomes an action

    @Test
    fun testOneActionPerConsumedOperation() {

        val (cluster, messages) = build("/asyncapi/sut/ncs-kafka.yaml")

        assertTrue(messages.isEmpty(), "unexpected problems: $messages")

        //six operations, each consuming a single message, so six actions named after them
        assertEquals(
            setOf("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder"),
            cluster.keys
        )

        val bessj = cluster.getValue("bessj") as AsyncApiAction
        assertEquals("bessj", bessj.operationId)
        assertEquals("bessjRequest", bessj.channelName)
        assertEquals("bessjRequest", bessj.messageId)
    }

    @Test
    fun testOperationsTheServiceEmitsAreNotDrivable() {

        /*
            A 'send' operation is what the service emits. There is nothing to publish to it, so
            it is not turned into an action; only what the service consumes can be driven.
         */
        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: One of each direction
              version: 1.0.0
            channels:
              c:
                address: a
                messages:
                  m:
                    payload:
                      type: object
            operations:
              consumed:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/c'
              emitted:
                action: send
                channel:
                  ${'$'}ref: '#/channels/c'
            """.trimIndent()
        )

        assertEquals(setOf("consumed"), build(schema).first.keys)
    }

    @Test
    fun testAnOperationCarryingSeveralMessagesGetsOnePerMessage() {

        val cluster = build("/asyncapi/artificial/websocket-reply.yaml").first

        /*
            Which message to send is a choice made by picking an action, not by mutating a gene,
            so an operation carrying several becomes several actions. Here each carries one, so
            each keeps the operation's own name.
         */
        assertTrue(cluster.containsKey("recv_list_legs"))
        assertTrue(cluster.containsKey("recv_get_leg"))

        val listLegs = cluster.getValue("recv_list_legs") as AsyncApiAction
        assertEquals("listLegs", listLegs.messageId)
        assertEquals("vsi", listLegs.channelName)
    }

    @Test
    fun testTheMessageIsNamedOnlyWhenThereIsMoreThanOne() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: An operation that can send either of two messages
              version: 1.0.0
            channels:
              c:
                address: a
                messages:
                  first:
                    ${'$'}ref: '#/components/messages/first'
                  second:
                    ${'$'}ref: '#/components/messages/second'
            operations:
              o:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/c'
            components:
              messages:
                first:
                  payload:
                    type: object
                second:
                  payload:
                    type: object
            """.trimIndent()
        )

        assertEquals(setOf("o:first", "o:second"), build(schema).first.keys)
    }

    @Test
    fun testAnOperationInheritsEveryMessageOfItsChannelWhenItNarrowsToNone() {

        val (cluster, _) = build("/asyncapi/sut/scalar.yaml")

        /*
            This is how an operation ends up carrying several messages in practice. No document
            in the corpus narrows to more than one with a `messages:` array; what happens is
            the opposite -- an operation declares no array at all, and so inherits everything
            its channel carries. Here one channel carries five genuinely different payloads and
            the operation does not say which, so each becomes an action of its own: they are
            different things to send, and picking between them is a structural choice rather
            than a mutation.
         */
        val userEvents = cluster.keys.filter { it.startsWith("subscribeToUserEvents") }.sorted()

        assertEquals(
            listOf(
                "subscribeToUserEvents:LoginAttempt",
                "subscribeToUserEvents:UserAuthenticated",
                "subscribeToUserEvents:UserDeleted",
                "subscribeToUserEvents:UserProfileUpdated",
                "subscribeToUserEvents:UserSignedUp"
            ),
            userEvents
        )

        //they share the operation they came from, which is what the document names
        val actions = userEvents.map { cluster.getValue(it) as AsyncApiAction }
        assertTrue(actions.all { it.operationId == "subscribeToUserEvents" })
        assertTrue(actions.all { it.channelName == "userEvents" })
        //but each carries its own message, and so its own payload genes
        assertEquals(userEvents.size, actions.map { it.messageId }.distinct().size)
    }

    // ------------------------------------------------------------------ what an action holds

    @Test
    fun testThePayloadIsTheGenes() {

        val bessj = actionsOf("/asyncapi/sut/ncs-kafka.yaml").first { it.operationId == "bessj" }

        val payload = bessj.parameters.first { it.name == AsyncApiParam.PAYLOAD }
        assertTrue(payload.gene is ObjectGene)

        //the genes the search may vary are exactly the input, and nothing else
        assertEquals(payload.seeGenes().size, bessj.seeTopGenes().size)
    }

    @Test
    fun testTheReplyIsDescribedButNotSearchedOver() {

        val bessj = actionsOf("/asyncapi/sut/ncs-kafka.yaml").first { it.operationId == "bessj" }

        assertTrue(bessj.expectsReply())
        //two declared outcomes, which is what gives a black-box search something to tell apart
        assertEquals(listOf("doubleResult", "error"), bessj.replyTemplate!!.messageIds)
        assertEquals("bessjReply", bessj.replyTemplate!!.channelName)

        //but none of that is a gene: it is what to expect, not what to vary
        assertTrue(bessj.seeTopGenes().none { it.name.contains("result", ignoreCase = true) })
    }

    @Test
    fun testAFireAndForgetOperationHasNoReplyTemplate() {

        val action = actionsOf("/asyncapi/sut/microcks.yaml").first()

        assertFalse(action.expectsReply())
        assertNull(action.replyTemplate)
    }

    @Test
    fun testHeadersAreASeparateParameterFromThePayload() {

        val action = build(AsyncApiAccess.parseFromText(headerDocument(true))).first
            .values.map { it as AsyncApiAction }.first()

        assertEquals(
            listOf(AsyncApiParam.PAYLOAD, AsyncApiParam.HEADERS),
            action.parameters.map { it.name }
        )
    }

    @Test
    fun testTheStampedCorrelationHeaderIsNotAGene() {

        val action = build(AsyncApiAccess.parseFromText(headerDocument(true))).first
            .values.map { it as AsyncApiAction }.first()

        /*
            The document declares where the correlation id travels, and the headers schema
            declares a property of that name. The value is stamped fresh at each execution so a
            reply can be paired with its request, so a gene holding it would only be overwritten
            -- the search would spend mutations on something that never travels.
         */
        val headers = action.parameters.first { it.name == AsyncApiParam.HEADERS }.gene as ObjectGene
        assertEquals(listOf("tenant"), headers.fields.map { it.name })
    }

    @Test
    fun testNoHeadersParameterWhenTheStampedIdIsTheOnlyHeader() {

        val action = build(AsyncApiAccess.parseFromText(headerDocument(false))).first
            .values.map { it as AsyncApiAction }.first()

        //an empty headers schema would invite the search to invent headers never declared
        assertEquals(listOf(AsyncApiParam.PAYLOAD), action.parameters.map { it.name })
    }

    /**
     * A document whose message declares a correlation id in its headers, with or without
     * another header of its own beside it.
     */
    private fun headerDocument(withOtherHeader: Boolean) =
        if (withOtherHeader) {
            """
            asyncapi: 3.0.0
            info:
              title: A stamped id and a header of its own
              version: 1.0.0
            channels:
              c:
                address: a
                messages:
                  m:
                    ${'$'}ref: '#/components/messages/m'
            operations:
              o:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/c'
            components:
              messages:
                m:
                  correlationId:
                    location: '${'$'}message.header#/correlationId'
                  payload:
                    type: object
                    properties:
                      value:
                        type: string
                  headers:
                    type: object
                    properties:
                      correlationId:
                        type: string
                      tenant:
                        type: string
            """.trimIndent()
        } else {
            """
            asyncapi: 3.0.0
            info:
              title: A stamped id and nothing else
              version: 1.0.0
            channels:
              c:
                address: a
                messages:
                  m:
                    ${'$'}ref: '#/components/messages/m'
            operations:
              o:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/c'
            components:
              messages:
                m:
                  correlationId:
                    location: '${'$'}message.header#/correlationId'
                  payload:
                    type: object
                    properties:
                      value:
                        type: string
                  headers:
                    type: object
                    properties:
                      correlationId:
                        type: string
            """.trimIndent()
        }

    // ------------------------------------------------------------------ degrading gracefully

    @Test
    fun testAMessageThatCannotBePublishedIsReported() {

        val schema = AsyncApiAccess.parseFromText(
            """
            asyncapi: 3.0.0
            info:
              title: A message with nothing to send
              version: 1.0.0
            channels:
              c:
                address: a
                messages:
                  empty:
                    name: Empty
            operations:
              o:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/c'
            """.trimIndent()
        )

        val (cluster, messages) = build(schema)

        assertTrue(cluster.isEmpty())
        assertTrue(
            messages.any { it.contains("neither a payload nor headers") },
            messages.toString()
        )
    }

    @Test
    fun testBuildingIsRepeatable() {

        //the gene builder keeps a static cache, so building twice must give the same shape
        val first = build("/asyncapi/sut/ncs-kafka.yaml").first
        val second = build("/asyncapi/sut/ncs-kafka.yaml").first

        assertEquals(first.keys, second.keys)
        assertEquals(
            (first.getValue("bessj") as AsyncApiAction).seeTopGenes().size,
            (second.getValue("bessj") as AsyncApiAction).seeTopGenes().size
        )
    }

    // ------------------------------------------------------------------ the individual

    @Test
    fun testAnIndividualHoldsTheMessagesToPublish() {

        val actions = actionsOf("/asyncapi/sut/ncs-kafka.yaml").take(2).map { it.copy() as AsyncApiAction }

        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        assertEquals(2, individual.seeMainExecutableActions().size)
        assertTrue(individual.canMutateStructure())
    }

    @Test
    fun testMessagesCanBeAddedAndRemoved() {

        val actions = actionsOf("/asyncapi/sut/ncs-kafka.yaml").map { it.copy() as AsyncApiAction }

        val individual = AsyncApiIndividual(SampleType.RANDOM, mutableListOf(actions[0]))
        assertEquals(1, individual.seeMainExecutableActions().size)

        individual.addAction(action = actions[1])
        assertEquals(2, individual.seeMainExecutableActions().size)

        individual.removeAction(0)
        assertEquals(1, individual.seeMainExecutableActions().size)
        assertEquals(actions[1].getName(), individual.seeMainExecutableActions().first().getName())
    }

    @Test
    fun testCopyingAnIndividualKeepsItsMessages() {

        val actions = actionsOf("/asyncapi/sut/ncs-kafka.yaml").take(3).map { it.copy() as AsyncApiAction }
        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        val copy = individual.copy() as AsyncApiIndividual

        assertEquals(
            individual.seeMainExecutableActions().map { it.getName() },
            copy.seeMainExecutableActions().map { it.getName() }
        )
        //a copy must be independent, or mutating one would change the other
        assertNotSame(individual.seeMainExecutableActions()[0], copy.seeMainExecutableActions()[0])
    }

    @Test
    fun testCopyingAnIndividualThatWasSetUpWithMoreThanSql() {

        /*
            Only SQL is put in front of the messages today, but the individual inherits every
            other kind of setup an enterprise individual can hold. The children are copied
            wholesale, so a group whose size was not measured would not match what is handed
            over, and the copy would fail outright rather than come back wrong.
         */
        val actions = actionsOf("/asyncapi/sut/ncs-kafka.yaml").take(1).map { it.copy() as AsyncApiAction }
        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList())

        individual.addInitializingMongoDbActions(
            actions = listOf(MongoDbAction("db", "collection", "collection", listOf()))
        )

        val copy = individual.copy() as AsyncApiIndividual

        assertEquals(1, copy.seeMainExecutableActions().size)
        assertEquals(
            individual.seeInitializingActions().size,
            copy.seeInitializingActions().size
        )
    }
}
