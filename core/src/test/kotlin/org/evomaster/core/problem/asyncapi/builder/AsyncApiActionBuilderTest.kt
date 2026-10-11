package org.evomaster.core.problem.asyncapi.builder

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.param.AsyncApiParam
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiActionBuilderTest {

    companion object {
        /**
         * One operation whose message declares no payload, one whose payload is declared with no
         * structure, one whose payload has fields, and one with no payload that is not JSON.
         */
        private val OPAQUE = """
            asyncapi: 3.0.0
            info:
              title: Opaque payloads
              version: 1.0.0
            channels:
              returns:
                address: shop.returns
                messages:
                  returnRequested:
                    name: ReturnRequested
              audits:
                address: shop.audits
                messages:
                  audit:
                    payload:
                      type: object
              orders:
                address: shop.orders
                messages:
                  order:
                    payload:
                      type: object
                      properties:
                        id:
                          type: string
              images:
                address: shop.images
                messages:
                  image:
                    contentType: image/png
            operations:
              requestReturn:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/returns'
              audit:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/audits'
              placeOrder:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/orders'
              uploadImage:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/images'
        """.trimIndent()

        /**
         * A message whose payload is declared, but in Avro, with no content type to say it is
         * not JSON.
         */
        private val AVRO = """
            asyncapi: 3.0.0
            info:
              title: Avro payload
              version: 1.0.0
            channels:
              customers:
                address: crm.customers
                messages:
                  customer:
                    payload:
                      schemaFormat: application/vnd.apache.avro;version=1.9.0
                      schema:
                        type: record
                        name: Customer
                        fields:
                          - name: id
                            type: string
            operations:
              registerCustomer:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/customers'
        """.trimIndent()
    }

    private val options = AsyncApiGeneBuilder.options(EMConfig())

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun build(resourcePath: String): Pair<Map<String, Action>, List<String>> {
        val schema = AsyncApiAccess.getAsyncApiFromResource(resourcePath)
        return build(schema)
    }

    private fun build(
        schema: AsyncApiDocument,
        expandOpaquePayloads: Boolean = false
    ): Pair<Map<String, Action>, List<String>> {
        val cluster = mutableMapOf<String, Action>()
        val messages = AsyncApiActionBuilder.addActionsFromSchema(schema, cluster, options, expandOpaquePayloads)
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
            No corpus document narrows an operation to several messages with a `messages:`
            array; what happens is that the operation declares none and inherits everything its
            channel carries. Each of the five becomes an action of its own.
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

    // ------------------------------------------------------------------ opaque payloads

    private fun payloadParamOf(cluster: Map<String, Action>, name: String): AsyncApiParam =
        (cluster.getValue(name) as AsyncApiAction).parameters
            .single { it.name == AsyncApiParam.PAYLOAD } as AsyncApiParam

    @Test
    fun testAMessageWithoutAPayloadBecomesAnActionWhenOpaquePayloadsAreExpanded() {

        val (cluster, messages) = build(AsyncApiAccess.parseFromText(OPAQUE), expandOpaquePayloads = true)

        //published as an empty object until the service is seen reading it into a class
        val payload = payloadParamOf(cluster, "requestReturn")
        assertTrue(payload.opaque)
        assertTrue(payload.gene is ObjectGene, payload.gene.javaClass.simpleName)
        assertTrue((payload.gene as ObjectGene).fields.isEmpty())
        assertFalse(messages.any { it.contains("requestReturn") }, messages.toString())
    }

    @Test
    fun testADeclaredOpaquePayloadKeepsTheGenesBuiltForIt() {

        val schema = AsyncApiAccess.parseFromText(OPAQUE)

        val expanded = payloadParamOf(build(schema, expandOpaquePayloads = true).first, "audit")
        RestActionBuilderV3.cleanCache()
        val plain = payloadParamOf(build(schema).first, "audit")

        //marked, but built as it always was: only what the service is seen reading replaces it
        assertTrue(expanded.opaque)
        assertFalse(plain.opaque)
        assertEquals(plain.gene.javaClass, expanded.gene.javaClass)
    }

    @Test
    fun testAPayloadWithFieldsIsNeverOpaque() {

        val (cluster, _) = build(AsyncApiAccess.parseFromText(OPAQUE), expandOpaquePayloads = true)

        assertFalse(payloadParamOf(cluster, "placeOrder").opaque)
    }

    @Test
    fun testAMessageWithoutAJsonPayloadGetsNoPlaceholder() {

        val (cluster, messages) = build(AsyncApiAccess.parseFromText(OPAQUE), expandOpaquePayloads = true)

        assertFalse(cluster.containsKey("uploadImage"), cluster.keys.toString())
        assertTrue(messages.any { it.contains("neither a payload nor headers") }, messages.toString())
    }

    @Test
    fun testAPayloadDeclaredInAFormatThatCannotBeReadIsNotTakenForAMissingOne() {

        val schema = AsyncApiAccess.parseFromText(AVRO)

        //the parser drops a message whose payload it cannot read, rather than keep it without one
        assertTrue(schema.messages.isEmpty(), schema.messages.keys.toString())

        val (cluster, _) = build(schema, expandOpaquePayloads = true)

        //so there is no empty object to publish in place of an Avro record
        assertFalse(cluster.containsKey("registerCustomer"), cluster.keys.toString())
    }

    @Test
    fun testWithoutExpandingNothingChanges() {

        val (cluster, messages) = build(AsyncApiAccess.parseFromText(OPAQUE))

        //the message without a payload is still left out, and no payload is marked
        assertEquals(setOf("audit", "placeOrder"), cluster.keys)
        assertTrue(messages.any { it.contains("'requestReturn'") && it.contains("neither a payload nor headers") },
            messages.toString())
        assertTrue(cluster.values.flatMap { (it as AsyncApiAction).parameters }
            .none { (it as AsyncApiParam).opaque })
    }
}
