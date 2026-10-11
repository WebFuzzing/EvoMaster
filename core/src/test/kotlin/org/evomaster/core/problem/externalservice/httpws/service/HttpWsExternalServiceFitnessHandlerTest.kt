package org.evomaster.core.problem.externalservice.httpws.service

import com.google.inject.Injector
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.ExternalServiceInfoDto
import org.evomaster.client.java.controller.api.dto.HostnameResolutionInfoDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.AsyncApiSampler
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL

class HttpWsExternalServiceFitnessHandlerTest {

    companion object {

        /**
         * A hostname that does not resolve, as the SUT would call it.
         */
        const val SUPPLIER = "supplier.test"

        /**
         * A hostname the SUT is reported to have resolved, to [RESOLVED_ADDRESS]. It is never
         * looked up, so it need not resolve where the tests run.
         */
        private const val RESOLVED = "localhost.example"

        private const val RESOLVED_ADDRESS = "93.184.215.14"

        /**
         * Where the WireMock instances are asked to bind, given to EvoMaster with
         * `--externalServiceIP`. Not one of the reserved 127.0.0.0 to 127.0.0.3.
         */
        const val MOCK_IP = "127.0.0.5"

        private const val HTTP = "http"

        private const val PATH = "/api/stock/A-100"

        private const val OK = 200

        val MOCKING = arrayOf("--externalServiceIPSelectionStrategy=USER", "--externalServiceIP=$MOCK_IP")

        /**
         * One fire-and-forget operation, so that an individual can be built to hang actions on.
         */
        val RESTOCK = """
            asyncapi: 3.0.0
            info:
              title: Restock
              version: 1.0.0
            channels:
              restock:
                address: shop.restock.requested
                messages:
                  restockRequested:
                    payload:
                      type: object
                      required: [sku]
                      properties:
                        sku:
                          type: string
                          enum: [A-100, B-200]
            operations:
              requestRestock:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/restock'
        """.trimIndent()

        /**
         * A port free on [MOCK_IP], or null when nothing can bind there.
         *
         * On Linux every address in 127.0.0.0/8 is a loopback one. On macOS only 127.0.0.1 is,
         * unless an alias is added (see docs/loopback.md), and a WireMock instance cannot be
         * started there. The tests that need one are skipped rather than failed in that case.
         */
        fun freePortOnMockIp(): Int? =
            try {
                ServerSocket(0, 0, InetAddress.getByName(MOCK_IP)).use { it.localPort }
            } catch (e: Exception) {
                null
            }

        /**
         * What the SUT does: an HTTP GET, answered with its status code.
         */
        fun get(url: String): Int {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            return try {
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
    }

    private lateinit var injector: Injector
    private lateinit var handler: HttpWsExternalServiceFitnessHandler
    private lateinit var externalServiceHandler: HttpWsExternalServiceHandler

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    @AfterEach
    fun stopWireMock() {
        if (this::externalServiceHandler.isInitialized) {
            externalServiceHandler.reset()
        }
    }

    private fun start(vararg options: String) {
        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(RESTOCK)) { FakeAsyncApiDriver.fireAndForget() }
        val mode = if (options.any { it.startsWith("--blackBox") }) arrayOf() else arrayOf("--blackBox=false")
        injector = AsyncApiTestInjector.create(driver, *mode, *options)
        handler = injector.getInstance(HttpWsExternalServiceFitnessHandler::class.java)
        externalServiceHandler = injector.getInstance(HttpWsExternalServiceHandler::class.java)
    }

    private fun individual(): AsyncApiIndividual =
        AsyncApiTestInjector.individualOf(injector, "requestRestock")

    private fun dnsActionsOf(individual: AsyncApiIndividual) =
        individual.seeActions(ActionFilter.ONLY_DNS).filterIsInstance<HostnameResolutionAction>()

    private fun hostname(name: String, resolvedTo: String? = null) = AdditionalInfoDto().apply {
        hostnameResolutionInfoDtos.add(HostnameResolutionInfoDto(name, resolvedTo))
    }

    private fun connection(name: String, port: Int) = AdditionalInfoDto().apply {
        hostnameResolutionInfoDtos.add(HostnameResolutionInfoDto(name, null))
        externalServices.add(ExternalServiceInfoDto(HTTP, name, port))
    }

    @Test
    fun testMockingIsOffByDefault() {
        start()
        assertFalse(handler.isEnabled())
    }

    @Test
    fun testMockingIsOnOnceAnAddressStrategyIsChosen() {
        start(*MOCKING)
        assertTrue(handler.isEnabled())
    }

    @Test
    fun testABlackBoxRunMocksNothing() {
        //nothing would tell it which hostnames the SUT resolves: that comes from the instrumentation
        start("--blackBox=true", *MOCKING)
        assertFalse(handler.isEnabled())
    }

    @Test
    fun testAHostnameThatDoesNotResolveGetsALocalAddress() {

        start(*MOCKING)
        val individual = individual()

        val started = handler.handleExternalServiceInfo(individual, listOf(hostname(SUPPLIER)))

        assertFalse(started, "nothing was connected to, so no WireMock instance is needed yet")
        assertEquals(MOCK_IP, externalServiceHandler.getLocalDomainNameMapping(SUPPLIER))
        assertTrue(dnsActionsOf(individual).isEmpty(), "the structure mutator adds those, not the fitness")
    }

    @Test
    fun testAHostnameThatResolvesIsSentToTheReservedAddressInTheTest() {

        start(*MOCKING)
        val individual = individual()

        handler.handleExternalServiceInfo(individual, listOf(hostname(RESOLVED, RESOLVED_ADDRESS)))

        val dns = dnsActionsOf(individual).single()
        assertEquals(RESOLVED, dns.hostname)
        assertEquals(ExternalServiceSharedUtils.RESERVED_RESOLVED_LOCAL_IP, dns.localIPAddress)

        //reported again by a later action, it is not added a second time
        handler.handleExternalServiceInfo(individual, listOf(hostname(RESOLVED, RESOLVED_ADDRESS)))
        assertEquals(1, dnsActionsOf(individual).size)
    }

    @Test
    fun testOnlyTheFirstActionCarriesTheMappings() {

        start(*MOCKING)
        val individual = individual()
        individual.addInitializingHostnameResolutionActions(actions = listOf(HostnameResolutionAction(SUPPLIER, MOCK_IP)))

        val first = ActionDto()
        handler.addMappings(first, 0, individual)
        assertEquals(mapOf(SUPPLIER to MOCK_IP), first.localAddressMapping)

        /*
            The driver reads them from the first action only, and keeps them for the whole test.
            The later ones are left as they were built.
         */
        val second = ActionDto()
        handler.addMappings(second, 1, individual)
        assertTrue(second.localAddressMapping.isEmpty())
    }

    @Test
    fun testAnActionOutsideAGroupIsRefused() {

        start(*MOCKING)
        val sampler = injector.getInstance(AsyncApiSampler::class.java)

        assertThrows(IllegalArgumentException::class.java) {
            handler.prepareForMainAction(sampler.seeAvailableActions().single().copy() as AsyncApiAction)
        }
    }

    @Test
    fun testWithNoWireMockRunningNothingIsServed() {

        start(*MOCKING)
        val action = individual().seeMainExecutableActions().single()

        val externalServiceActions = handler.prepareForMainAction(action)
        assertTrue(externalServiceActions.isEmpty())

        val fv = FitnessValue(1.0)
        val served = handler.registerServedRequests(fv, 0, externalServiceActions)

        assertTrue(served.isEmpty())
        assertTrue(fv.getViewAccessedExternalServiceRequests().isEmpty())
    }

    @Test
    fun testAServiceTheSutConnectedToIsMockedAndWhatItServedIsRecorded() {

        val port = freePortOnMockIp()
        assumeTrue(port != null, "nothing can bind to $MOCK_IP here, see docs/loopback.md")

        start(*MOCKING)
        val individual = individual()

        val started = handler.handleExternalServiceInfo(individual, listOf(connection(SUPPLIER, port!!)))
        assertTrue(started)
        val service = externalServiceHandler.getExternalServices().values.single()
        assertEquals(MOCK_IP, service.getIP())

        //the SUT, redirected to the instance, is answered with its default response
        val action = individual.seeMainExecutableActions().single()
        val none = handler.prepareForMainAction(action)
        assertEquals(service.getWMDefaultCode(), get("$HTTP://$MOCK_IP:$port$PATH"))

        val fv = FitnessValue(1.0)
        val served = handler.registerServedRequests(fv, 0, none)
        assertEquals(PATH, served.single().url)
        assertEquals(served, fv.getViewAccessedExternalServiceRequests()[0])

        //once there is an action for that request, it is what the SUT is answered with
        val mocked = HttpExternalServiceAction(
            served.single(), HttpWsResponseParam(), service, active = true, used = true, id = 0
        ).apply {
            doInitialize(injector.getInstance(Randomness::class.java))
            (response as HttpWsResponseParam).setStatus(OK)
        }
        (action.parent as EnterpriseActionGroup<*>).addChildrenToGroup(listOf(mocked), GroupsOfChildren.EXTERNAL_SERVICES)

        val prepared = handler.prepareForMainAction(action)
        assertEquals(listOf(mocked), prepared)
        assertEquals(OK, get("$HTTP://$MOCK_IP:$port$PATH"))

        handler.registerServedRequests(FitnessValue(1.0), 0, prepared)
        assertTrue(mocked.used, "the action answered the request, so it is kept in use")

        //an action that nothing calls is no longer used, and not set up the next time
        handler.prepareForMainAction(action)
        handler.registerServedRequests(FitnessValue(1.0), 0, prepared)
        assertFalse(mocked.used)
        handler.prepareForMainAction(action)
        assertFalse(mocked.active)
        assertEquals(service.getWMDefaultCode(), get("$HTTP://$MOCK_IP:$port$PATH"))
    }
}
