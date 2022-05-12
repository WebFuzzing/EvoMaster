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

class ExternalServices {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     */

    /**
     * Contains the information about each external calls made
     */
    private val externalServiceInfos: MutableList<ExternalServiceInfo> = mutableListOf()

    private var lastIPAddress : String = ""

    @Inject
    private lateinit var config : EMConfig

    /**
     * This will allow adding ExternalServiceInfo to the Collection
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        // TODO: The below code intentionally commented out
        val ip = getIP()
        lastIPAddress = ip
        val wm : WireMockServer = initWireMockServer(ip)
        updateDNSCache(externalServiceInfo.remoteHostname, ip)
        externalServiceInfo.assignWireMockServer(wm)
        externalServiceInfos.add(externalServiceInfo)
    }

    /**
     * Will return the next available IP address for the given ip address
     * or from the last know IP address used for external service.
     */
    private fun getNextAvailableAddress(address : String?) : String {
        val nextAddress: String = if (lastIPAddress != "") {
            nextIPAddress(lastIPAddress)
        } else {
            address.toString()
        }

        if (isAddressAvailable(nextAddress, WIREMOCK_PORT)) {
            return nextAddress
        }
        return getNextAvailableAddress(nextAddress)
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

    private fun getIP() : String {
        val ip: String
        when (config.externalServiceIPSelectionStrategy) {
            EMConfig.ExternalServiceIPSelectionStrategy.RANDOM -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress(lastIPAddress)
                } else {
                    generateRandomAvailableAddress()
                }
            }
            EMConfig.ExternalServiceIPSelectionStrategy.USER -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress(lastIPAddress)
                } else {
                    config.externalServiceIP
                }
            }
            else -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress(lastIPAddress)
                } else {
                    getNextAvailableAddress("127.0.0.1")
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
     */
    private fun updateDNSCache(host : String, address : String) {
        DnsCacheManipulator.setDnsCache(host, address)
    }

    /**
     * Clean up the DNS cache
     */
    fun cleanDNSCache() {
        DnsCacheManipulator.clearDnsCache()
    }

    companion object {
        private const val WIREMOCK_PORT : Int = 9000
    }
}