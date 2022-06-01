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

class ExternalServiceHandler {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     *
     * TODO: This is not the final implementation need to refactor but
     * the concept is working.
     */


    /**
     * Contains the information about each external calls made
     */
    private val externalServiceInfos: MutableList<ExternalServiceInfo> = mutableListOf()

    private val externalServices: MutableList<ExternalService> = mutableListOf()

    private var lastIPAddress : String = ""

    /**
     * Contains hostname against to WireMock instance mapping
     */
    private val externalServiceMapping: MutableMap<String, String> = mutableMapOf()

    @Inject
    private lateinit var config : EMConfig

    private val log: Logger = LoggerFactory.getLogger(ExternalServiceUtils::class.java)

    /**
     * This will allow adding ExternalServiceInfo to the Collection.
     *
     * If there is a WireMock instance is available for the hostname,
     * it will be skipped from creating a new one.
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            if (!externalServiceMapping.containsKey(externalServiceInfo.remoteHostname)) {
                val ip = getIP(externalServiceInfo.remotePort)
                lastIPAddress = ip
                val wm : WireMockServer = initWireMockServer(ip, externalServiceInfo.remotePort)

                // TODO: Should be moved under JUnit tests
                bindDNSCache(externalServiceInfo.remoteHostname, ip)

                externalServices.add(ExternalService(externalServiceInfo, wm))
                externalServiceMapping[externalServiceInfo.remoteHostname] = ip
            }
        }
        externalServiceInfos.add(externalServiceInfo)
    }

    fun getExternalServiceMappings() : Map<String, String> {
        return externalServiceMapping
    }

    /**
     * Will return the next available IP address from the last know IP address
     * used for external service.
     */
    private fun getNextAvailableAddress(port: Int) : String {
        val nextAddress: String = nextIPAddress(lastIPAddress)

        if (isAddressAvailable(nextAddress, port)) {
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
    private fun getDefaultAddress(port: Int) : String {
        if (isAddressAvailable("127.0.0.2", port)) {
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
    private fun generateRandomAvailableAddress(port: Int) : String {
        val ip = generateRandomIPAddress()
        if (isAddressAvailable(ip, port)) {
            return ip
        }
        return generateRandomAvailableAddress(port)
    }

    fun getExternalServices() : List<ExternalServiceInfo> {
        return externalServiceInfos
    }

    /**
     * Default IP address will be a randomly generated IP
     */
    private fun getIP(port: Int) : String {
        val ip: String
        when (config.externalServiceIPSelectionStrategy) {
            // Although the default address will be a random, this
            // option allows selecting explicitly
            EMConfig.ExternalServiceIPSelectionStrategy.RANDOM -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress(port)
                } else {
                    generateRandomAvailableAddress(port)
                }
            }
            EMConfig.ExternalServiceIPSelectionStrategy.USER -> {
                ip = if (externalServiceInfos.size > 0) {
                    getNextAvailableAddress(port)
                } else {
                    config.externalServiceIP
                }
            }
            else -> {
                ip = if (externalServiceInfos.size > 0) {
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
        val wm = WireMockServer(
            WireMockConfiguration()
                .bindAddress(address)
                .port(8080)
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

    private fun stopWireMockServers() {
        externalServices.forEach {
            it.stopWireMockServer()
        }
    }

    /**
     * Update the DNS cache with a different IP address for a given host to enable spoofing.
     * If there is an entry already it'll skip from adding to the cache.
     *
     * TODO: Need to refactor
     */
    private fun bindDNSCache(host : String, address : String) {
        val entry = DnsCacheManipulator.getDnsCache(host)
        if (entry == null) {
            DnsCacheManipulator.setDnsCache(host, address)
        } else {
            log.warn("$host already has a DNS cache entry.")
        }
    }

}