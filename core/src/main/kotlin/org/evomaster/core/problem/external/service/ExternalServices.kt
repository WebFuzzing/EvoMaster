package org.evomaster.core.problem.external.service

import com.alibaba.dcm.DnsCacheManipulator
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.external.service.ExternalServiceUtils.generateRandomIPAddress
import org.evomaster.core.problem.external.service.ExternalServiceUtils.isAddressAvailable
import org.evomaster.core.problem.external.service.ExternalServiceUtils.nextIPAddress

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExternalServices {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     *
     * TODO: This is not the final implementation need to refactor but
     * the concept is working.
     */

    companion object {
        private const val WIREMOCK_PORT : Int = 8080
    }

    /**
     * Contains the information about each external calls made
     */
    private val externalServiceInfos: MutableList<ExternalServiceInfo> = mutableListOf()

    private var lastIPAddress : String = ""

    @Inject
    private lateinit var config : EMConfig

    private val log: Logger = LoggerFactory.getLogger(ExternalServiceUtils::class.java)

    /**
     * This will allow adding ExternalServiceInfo to the Collection
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        // TODO: The below code intentionally commented out
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            val ip = getIP()
            lastIPAddress = ip
            val wm : WireMockServer = initWireMockServer(ip)
            updateDNSCache(externalServiceInfo.remoteHostname, ip)
            externalServiceInfo.assignWireMockServer(wm)
        }
        externalServiceInfos.add(externalServiceInfo)
    }

    /**
     * Will return the next available IP address from the last know IP address
     * used for external service.
     */
    private fun getNextAvailableAddress() : String {
        val nextAddress: String = nextIPAddress(lastIPAddress)

        if (isAddressAvailable(nextAddress, WIREMOCK_PORT)) {
            return nextAddress
        } else {
            throw IllegalStateException(nextAddress.plus(" is not available for use"))
        }
    }

    /**
     * Returns the default IP address for external service initialisation.
     * If the respective address and port is not available it'll throw an
     * exception.
     */
    private fun getDefaultAddress() : String {
        if (isAddressAvailable("127.0.0.2", WIREMOCK_PORT)) {
            return "127.0.0.2"
        } else {
            throw Exception("Default loopback address is unavailable for binding")
        }
    }

    /**
     * Will generate random IP address within the loopback range
     * while checking the availability. If not available will
     * generate a new one.
     */
    private fun generateRandomAvailableAddress() : String {
        val ip = generateRandomIPAddress()
        if (isAddressAvailable(ip, WIREMOCK_PORT)) {
            return ip
        }
        return generateRandomAvailableAddress()
    }

    fun getExternalServices() : List<ExternalServiceInfo> {
        return externalServiceInfos
    }

    /**
     * Default IP address will be a randomly generated IP
     */
    private fun getIP() : String {
        val ip: String
        when (config.externalServiceIPSelectionStrategy) {
            // Although the default address will be a random, this
            // option allows selecting explicitly
            EMConfig.ExternalServiceIPSelectionStrategy.RANDOM -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress()
                } else {
                    generateRandomAvailableAddress()
                }
            }
            EMConfig.ExternalServiceIPSelectionStrategy.USER -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress()
                } else {
                    config.externalServiceIP
                }
            }
            else -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress()
                } else {
                    generateRandomAvailableAddress()
                }
            }
        }
        return ip
    }
    /**
     * Will initialise WireMock instance on a given IP address for a given port.
     */
    private fun initWireMockServer(address: String): WireMockServer {
        val wm = WireMockServer(
            WireMockConfiguration()
                .bindAddress(address)
                .port(WIREMOCK_PORT)
                .extensions(ResponseTemplateTransformer(false)))
        wm.start()

        // to prevent from the 404 when no matching stub below stub is added
        wm.stubFor(get(urlMatching("/.*"))
            .atPriority(2)
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("Not found!!")))

        return wm
    }

    /**
     * Update the DNS cache with a different IP address for a given host to enable spoofing.
     * If there is an entry already it'll skip from adding to the cache.
     */
    private fun updateDNSCache(host : String, address : String) {
        val entry = DnsCacheManipulator.getDnsCache(host)
        if (entry == null) {
            DnsCacheManipulator.setDnsCache(host, address)
        } else {
            log.warn("$host already has a DNS cache entry.")
        }
    }

    /**
     * Clean up the DNS cache
     */
    fun cleanDNSCache() {
        DnsCacheManipulator.clearDnsCache()
    }
}