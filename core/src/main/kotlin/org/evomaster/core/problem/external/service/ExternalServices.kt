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
//        val ip = getIP()
//        val wm : WireMockServer = initWireMockServer(ip)
//        updateDNSCache(externalServiceInfo.remoteHostname, ip)
//        externalServiceInfo.assignWireMockServer(wm)
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
            lastIPAddress = nextAddress
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
        if (config.externalServiceIPSelectionStrategy == EMConfig.ExternalServiceIPSelectionStrategy.USER && config.externalServiceIP == null) {
            throw Exception("externalServiceIP is not provided")
        }

        return when (config.externalServiceIPSelectionStrategy) {
            EMConfig.ExternalServiceIPSelectionStrategy.DEFAULT -> getNextAvailableAddress("127.0.0.2")
            EMConfig.ExternalServiceIPSelectionStrategy.USER -> config.externalServiceIP.toString()
            else -> {
                generateRandomAvailableAddress()
            }
        }
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
        return wm
    }

    /**
     * Update the DNS cache with a different IP address for a given host to enable spoofing.
     */
    fun updateDNSCache(host : String, address : String) {
        DnsCacheManipulator.setDnsCache(host, address)
    }

    /**
     * Clean up the DNS cache
     */
    fun cleanDNSCache() {
        DnsCacheManipulator.clearDnsCache()
    }

    companion object {
        private const val WIREMOCK_PORT : Int = 8000
    }
}