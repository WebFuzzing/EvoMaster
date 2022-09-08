package org.evomaster.core.problem.external.service.httpws

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.external.service.httpws.ExternalServiceUtils.generateRandomIPAddress
import org.evomaster.core.problem.external.service.httpws.ExternalServiceUtils.isAddressAvailable
import org.evomaster.core.problem.external.service.httpws.ExternalServiceUtils.nextIPAddress

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.evomaster.core.problem.external.service.httpws.ExternalServiceUtils.isReservedIP
import org.evomaster.core.search.service.Randomness

class ExternalServiceHandler {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     *
     * TODO: This is not the final implementation need to refactor but
     *  the concept is working.
     */

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    /**
     * Contains the information about external services as map.
     *
     * Mapped against to hostname with ExternalService.
     */
    private val externalServices: MutableMap<String, ExternalService> = mutableMapOf()

    /**
     * Contains last used loopback address for reference when creating
     * a new address
     */
    private var lastIPAddress: String = ""

    private var counter: Long = 0

    /**
     * Collection of captured external service requests under SUT
     */
    private val externalServiceRequests: MutableList<HttpExternalServiceRequest> = mutableListOf()

    /**
     * This will allow adding ExternalServiceInfo to the Collection.
     *
     * If there is a WireMock instance is available for the hostname,
     * it will be skipped from creating a new one.
     */
    fun addExternalService(externalServiceInfo: HttpExternalServiceInfo) {
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            if (!externalServices.containsKey(externalServiceInfo.remoteHostname)) {
                val ip = getIP(externalServiceInfo.remotePort)
                lastIPAddress = ip
                val wm: WireMockServer = initWireMockServer(ip, externalServiceInfo.remotePort)

                externalServices[externalServiceInfo.remoteHostname] = ExternalService(externalServiceInfo, wm)
            }
        }
    }

    fun getExternalServiceMappings(): Map<String, String> {
        return externalServices.mapValues { it.value.getWireMockAddress() }
    }

    /**
     * Will return the next available IP address from the last know IP address
     * used for external service.
     */
    private fun getNextAvailableAddress(port: Int): String {
        val nextAddress: String = nextIPAddress(lastIPAddress)

        if (isAddressAvailable(nextAddress, port)) {
            return nextAddress
        } else {
            throw IllegalStateException(nextAddress.plus(" is not available for use"))
        }
    }

    /**
     * Will generate random IP address within the loopback range
     * while checking the availability. If not available will
     * generate a new one.
     */
    private fun generateRandomAvailableAddress(port: Int): String {
        val ip = generateRandomIPAddress(randomness)
        if (isAddressAvailable(ip, port)) {
            return ip
        }
        return generateRandomAvailableAddress(port)
    }

    fun getExternalServices(): Map<String, ExternalService> {
        return externalServices
    }

    fun reset() {
        externalServices.forEach {
            it.value.stopWireMockServer()
        }
    }

    /**
     * Reset all the served requests
     */
    fun resetServedRequests() {
        externalServices.forEach { it.value.reset() }
    }

    /**
     * This takes all the served requests from WireMock server and creates them
     * as ExternalServiceAction. It ignores if the same absolute URL is added
     * already.
     *
     * This is not perfect yet, have to explore more scenarios and perfect the
     * implementation.
     */
    fun getExternalServiceActions(): MutableList<HttpExternalServiceAction> {
        val actions = mutableListOf<HttpExternalServiceAction>()
        externalServices.forEach { (_, u) ->
            u.getAllServedRequests().forEach {
                // TODO: This needs to be revised to make it nicer
                if (externalServiceRequests.none { r -> r.absoluteURL == it.absoluteURL }) {
                    externalServiceRequests.add(it)
                }
                if (actions.none { a -> a.request.url == it.url }) {
                    counter++
                    val localId = "ExternalServiceAction_$counter"
                    actions.add(
                        HttpExternalServiceAction(
                            it,
                            "",
                            u,
                            counter
                        )
                    )
                }

            }
        }
        return actions
    }

    /**
     * Default IP address will be a randomly generated IP
     *
     * If user provided IP address isn't available on the port
     * IllegalStateException will be thrown.
     */
    private fun getIP(port: Int): String {
        val ip: String
        when (config.externalServiceIPSelectionStrategy) {
            // Although the default address will be a random, this
            // option allows selecting explicitly
            EMConfig.ExternalServiceIPSelectionStrategy.RANDOM -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress(port)
                } else {
                    generateRandomAvailableAddress(port)
                }
            }
            EMConfig.ExternalServiceIPSelectionStrategy.USER -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress(port)
                } else {
                    if (!isReservedIP(config.externalServiceIP)) {
                        if (isAddressAvailable(config.externalServiceIP, port)) {
                            config.externalServiceIP
                        } else {
                            throw IllegalStateException("User provided IP address is not available")
                        }
                    } else {
                        throw IllegalStateException("Can not use a reserved IP address")
                    }
                }
            }
            else -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress(port)
                } else {
                    generateRandomAvailableAddress(port)
                }
            }
        }
        return ip
    }

    /**
     * Will initialise WireMock instance on a given IP address for a given port.
     */
    private fun initWireMockServer(address: String, port: Int): WireMockServer {
        // TODO: Port need to be changed to the remote service port
        //  In CI also using remote ports as 80 and 443 fails
        val wm = WireMockServer(
            WireMockConfiguration()
                .bindAddress(address)
                .port(port)
                .extensions(ResponseTemplateTransformer(false))
        )
        wm.start()

        // to prevent from the 404 when no matching stub below stub is added
        // TODO: Need to decide what should be the default behaviour
        wm.stubFor(
            any(anyUrl())
                .atPriority(10)
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        return wm
    }

    fun getExternalServiceRequests(): MutableList<HttpExternalServiceRequest> {
        return externalServiceRequests
    }

}