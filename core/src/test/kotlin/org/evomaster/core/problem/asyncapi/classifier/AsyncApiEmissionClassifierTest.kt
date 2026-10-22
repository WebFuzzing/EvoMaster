package org.evomaster.core.problem.asyncapi.classifier

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsyncApiEmissionClassifierTest {

    companion object {

        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        /**
         * Send channels of every shape a declared address can take: literal, with placeholders,
         * one that a placeholder would also match, and one carried by the Kafka binding only.
         * Plus a channel only consumed from, and two operations sending different messages on
         * one channel.
         */
        private val SENDS = """
            asyncapi: 3.0.0
            info:
              title: Sends
              version: 1.0.0
            servers:
              kafka:
                host: localhost:9092
                protocol: kafka
            channels:
              placed:
                address: orders.placed
                messages:
                  placed:
                    payload:
                      type: object
              confirmed:
                address: orders.confirmed
                messages:
                  confirmed:
                    payload:
                      type: object
                  rejected:
                    payload:
                      type: object
              perOrder:
                address: orders.{orderId}
                parameters:
                  orderId: {}
                messages:
                  perOrder:
                    payload:
                      type: object
              perTenantAndUser:
                address: '{tenant}.users.{userId}'
                parameters:
                  tenant: {}
                  userId: {}
                messages:
                  user:
                    payload:
                      type: object
              bound:
                bindings:
                  kafka:
                    topic: audit.events
                messages:
                  audit:
                    payload:
                      type: object
            operations:
              consume:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/placed'
              confirm:
                action: send
                channel:
                  ${'$'}ref: '#/channels/confirmed'
                messages:
                  - ${'$'}ref: '#/channels/confirmed/messages/confirmed'
              reject:
                action: send
                channel:
                  ${'$'}ref: '#/channels/confirmed'
                messages:
                  - ${'$'}ref: '#/channels/confirmed/messages/rejected'
              perOrder:
                action: send
                channel:
                  ${'$'}ref: '#/channels/perOrder'
              user:
                action: send
                channel:
                  ${'$'}ref: '#/channels/perTenantAndUser'
              audit:
                action: send
                channel:
                  ${'$'}ref: '#/channels/bound'
        """.trimIndent()

        /**
         * A literal send channel, and one whose address is decided at run time.
         */
        private val RUNTIME = """
            asyncapi: 3.0.0
            info:
              title: Runtime
              version: 1.0.0
            channels:
              confirmed:
                address: orders.confirmed
                messages:
                  confirmed:
                    payload:
                      type: object
              runtime:
                address: null
                messages:
                  runtime:
                    payload:
                      type: object
            operations:
              confirm:
                action: send
                channel:
                  ${'$'}ref: '#/channels/confirmed'
              runtime:
                action: send
                channel:
                  ${'$'}ref: '#/channels/runtime'
        """.trimIndent()
    }

    private val sends = AsyncApiEmissionClassifier(AsyncApiAccess.parseFromText(SENDS))

    @Test
    fun testALiteralAddressIsItsSendChannel() {

        val channel = sends.sendChannelOf("orders.confirmed")

        assertNotNull(channel)
        assertEquals("confirmed", channel!!.channelName)
        assertEquals("orders.confirmed", channel.address)
    }

    @Test
    fun testTheMessagesOfEveryOperationSendingOnAChannelAreTogether() {

        val channel = sends.sendChannelOf("orders.confirmed")!!

        //messages written inline are known by the channel they are in, then their own key
        assertEquals(listOf("confirmed.confirmed", "confirmed.rejected"), channel.messages.map { it.id })
    }

    @Test
    fun testAPlaceholderStandsForAnyNonEmptyText() {

        assertEquals("orders.{orderId}", sends.sendChannelOf("orders.42")?.address)
        assertEquals("orders.{orderId}", sends.sendChannelOf("orders.a.b")?.address)
        assertNull(sends.sendChannelOf("orders."))
        assertEquals("{tenant}.users.{userId}", sends.sendChannelOf("acme.users.7")?.address)
        assertNull(sends.sendChannelOf("acme.users."))
    }

    @Test
    fun testTheChannelThatNamesAnAddressExactlyWinsOverOneWithAPlaceholder() {

        //orders.{orderId} matches it too, and comes later in the document
        assertEquals("confirmed", sends.sendChannelOf("orders.confirmed")?.channelName)
    }

    @Test
    fun testTheTextAroundAPlaceholderIsTakenLiterally() {

        //the dot is not a wildcard
        assertNull(sends.sendChannelOf("ordersX42"))
        assertNull(sends.sendChannelOf("acmeXusersX7"))
    }

    @Test
    fun testTheKafkaBindingGivesTheAddressOfAChannelThatDeclaresNone() {

        assertEquals("bound", sends.sendChannelOf("audit.events")?.channelName)
    }

    @Test
    fun testAChannelOnlyConsumedFromIsDeclaredButNoSendChannel() {

        //orders.{orderId} would match it, but orders.placed names it exactly
        assertNull(sends.sendChannelOf("orders.placed"))
        assertTrue(sends.isDeclared("orders.placed"))
    }

    @Test
    fun testAnAddressNoChannelHasIsNotDeclared() {

        assertNull(sends.sendChannelOf("somewhere.else"))
        assertFalse(sends.isDeclared("somewhere.else"))
    }

    @Test
    fun testAChannelWithItsAddressDecidedAtRunTimeLeavesNoAddressUndeclared() {

        val runtime = AsyncApiEmissionClassifier(AsyncApiAccess.parseFromText(RUNTIME))

        //any address may be the one the channel gets at run time
        assertTrue(runtime.isDeclared("somewhere.else"))
        //but nothing tells which, so it is no send channel to cover a target for
        assertNull(runtime.sendChannelOf("somewhere.else"))
        assertEquals("confirmed", runtime.sendChannelOf("orders.confirmed")?.channelName)
    }

    @Test
    fun testReplyChannelsAreDeclaredButNoSendChannels() {

        val ncs = AsyncApiEmissionClassifier(AsyncApiAccess.getAsyncApiFromResource(NCS))

        assertTrue(ncs.isDeclared("ncs.bessj.reply"))
        assertTrue(ncs.isDeclared("ncs.bessj.request"))
        assertNull(ncs.sendChannelOf("ncs.bessj.reply"))
        assertFalse(ncs.isDeclared("ncs.bessj"))
    }
}
