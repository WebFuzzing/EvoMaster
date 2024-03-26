package org.evomaster.core.problem.externalservice.httpws.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ExternalServiceMappingDto
import org.evomaster.client.java.controller.api.dto.problem.ExternalServiceDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils.isDefaultSignature
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.problem.externalservice.ExternalService
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.HostnameResolutionInfo
import org.evomaster.core.problem.externalservice.httpws.*
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalServiceUtils.generateRandomIPAddress
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalServiceUtils.isAddressAvailable
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalServiceUtils.isReservedIP
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalServiceUtils.nextIPAddress
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.mail.Address

/**
 * To manage the external service related activities
 *
 * might create a superclass for external service handler
 */
class HttpWsExternalServiceHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpWsExternalServiceHandler::class.java)
    }

    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     *
     */

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig


    /**
     * Contains the information about [HttpWsExternalService] as map.
     *
     * Mapped against to signature of the [HttpWsExternalService].
     */
    private val externalServices: MutableMap<String, HttpWsExternalService> = mutableMapOf()

    /**
     * Skipped external services information provided through the driver to skip from
     * handling.
     */
    private val skippedExternalServices: MutableList<ExternalService> = mutableListOf()

    /**
     * Map of remote hostname vs local DNS replacement
     */
    private val hostnameLocalAddressMapping: MutableMap<String, String> = mutableMapOf()

    private val hostnameResolutionInfos: MutableList<HostnameResolutionInfo> = mutableListOf()

    /**
     * Contains last used loopback address for reference when creating
     * a new address
     */
    private var lastIPAddress: String = ""

    private var counter: Long = 0

    /**
     * whether the fake WM is initialized that the SUT will connect for the first time
     */
    private var isDefaultInitialized = false


    @PostConstruct
    fun initialize() {
        log.debug("Initializing {}", HttpWsExternalServiceHandler::class.simpleName)
        initDefaultWM()
    }


    /**
     * init default WM
     */
    private fun initDefaultWM() {
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            if (!isDefaultInitialized) {
                addHostname(HostnameResolutionInfo(ExternalServiceSharedUtils.DEFAULT_WM_DUMMY_HOSTNAME, ""))
                registerHttpExternalServiceInfo(DefaultHttpExternalServiceInfo.createDefaultHttps())
                registerHttpExternalServiceInfo(DefaultHttpExternalServiceInfo.createDefaultHttp())
                isDefaultInitialized = true
            }
        }
    }

    /**
     * This will allow adding ExternalServiceInfo to the Collection.
     *
     * If there is a WireMock instance is available for the [HttpWsExternalService] signature,
     * it will be skipped from creating a new one.
     */
    fun addExternalService(externalServiceInfo: HttpExternalServiceInfo) {
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            registerHttpExternalServiceInfo(externalServiceInfo)
        }
    }

    fun addHostname(hostnameResolutionInfo: HostnameResolutionInfo) {
        if (config.externalServiceIPSelectionStrategy != EMConfig.ExternalServiceIPSelectionStrategy.NONE) {
            // Additional validation to prevent local IP as a DNS entry
            if (!hostnameLocalAddressMapping.containsKey(hostnameResolutionInfo.remoteHostName)
                && !hostnameLocalAddressMapping.containsValue(hostnameResolutionInfo.remoteHostName)
            ) {
                val ip =
                    if (hostnameResolutionInfo.remoteHostName == ExternalServiceSharedUtils.DEFAULT_WM_DUMMY_HOSTNAME) {
                        ExternalServiceSharedUtils.DEFAULT_WM_LOCAL_IP
                    } else {
                        getNewIP()
                    }
                lastIPAddress = ip
                hostnameLocalAddressMapping[hostnameResolutionInfo.remoteHostName] = ip
                hostnameResolutionInfos.add(hostnameResolutionInfo)
            }
        }
    }

    private fun registerHttpExternalServiceInfo(externalServiceInfo: HttpExternalServiceInfo) {
        if (skippedExternalServices.contains(externalServiceInfo.toExternalService())) {
            return
        }

        if (externalServices.containsKey(externalServiceInfo.signature())) {
            return
        }

        if (!hostnameLocalAddressMapping.containsKey(externalServiceInfo.remoteHostname)) {
            return
        }

        val ip: String = hostnameLocalAddressMapping[externalServiceInfo.remoteHostname]!!

        val registered = externalServices.filterValues {
            it.getRemoteHostName() == externalServiceInfo.remoteHostname &&
                    !it.isActive()
        }

        if (registered.isNotEmpty()) {
            registered.forEach { (k, e) ->
                if (!externalServiceInfo.isPartial()) {
                    e.updateRemotePort(externalServiceInfo.remotePort)
                    e.startWireMock()
                    /*
                        Signature should be updated after the port is updated
                        So the existing element will be removed from the map.
                        After port information is updated element will be added
                        to the map with the new key.
                     */
                    externalServices[e.getSignature()] = e
                    externalServices.remove(k)
                }
            }
        } else {
            if (!externalServices.containsKey(externalServiceInfo.signature())) {
                val es = HttpWsExternalService(externalServiceInfo, ip)

                if (!externalServiceInfo.isPartial()) {
                    Lazy.assert { isAddressAvailable(es.getIP(), externalServiceInfo.remotePort) }
                    es.startWireMock()
                }

                /*
                    External service information will be added if it is not there
                    in the map already.
                 */
                externalServices[es.getSignature()] = es
            }
        }
    }

    fun getExternalServiceMappings(): Map<String, ExternalServiceMappingDto> {
        return externalServices.mapValues { (_, v) ->
            ExternalServiceMappingDto(
                v.getRemoteHostName(),
                v.getIP(),
                v.getSignature(),
                v.isActive()
            )
        }
    }

    fun getLocalDomainNameMapping(): Map<String, String> {
        return hostnameLocalAddressMapping.toMap()
    }

    fun isWireMockAddress(address: String) : Boolean {
        return hostnameLocalAddressMapping.containsValue(address)
    }

    /**
     * Returns a list of [HostnameResolutionAction].
     * Excluding Default WireMock entries.
     */
    fun getHostnameResolutionActions(): List<HostnameResolutionAction> {
        val output: MutableList<HostnameResolutionAction> = mutableListOf()
        hostnameLocalAddressMapping
            .filter { it.key != ExternalServiceSharedUtils.DEFAULT_WM_DUMMY_HOSTNAME }
            .filter { it.value != ExternalServiceSharedUtils.DEFAULT_WM_LOCAL_IP }
            .forEach {
            val action = HostnameResolutionAction(it.key, it.value)
            output.add(action)

        }
        return output
    }

    fun hasActiveMockServer(hostname: String): Boolean {
        return externalServices
            .filter { it.value.getRemoteHostName() == hostname && it.value.isActive() }
            .isNotEmpty()
    }

    /**
     * Will return the next available IP address from the last know IP address
     * used for external service.
     */
    private fun getNextAvailableAddress(): String {
        return nextIPAddress(lastIPAddress)
    }

    /**
     * Will generate random IP address within the loopback range
     * while checking the availability. If not available will
     * generate a new one.
     */
    private fun generateRandomAvailableAddress(): String {
        return generateRandomIPAddress(randomness)
    }

    fun getExternalServices(): Map<String, HttpWsExternalService> {
        return externalServices.filter { it.value.isActive() }
    }

    fun reset() {
        externalServices.filter { it.value.isActive() }.forEach {
            it.value.stopWireMockServer()
        }
    }

    /**
     * Reset all the served requests.
     * The WireMock instances will still be up and running
     */
    fun resetServedRequests() {
        externalServices.filter { it.value.isActive() }.forEach { it.value.resetServedRequests() }
    }

    /**
     * Creates an [HttpExternalServiceAction] based on the given [HttpExternalServiceRequest]
     */
    fun createExternalServiceAction(
        request: HttpExternalServiceRequest,
        responseParam: HttpWsResponseParam?
    ): HttpExternalServiceAction {
        val externalService = getExternalService(request.wireMockSignature)

        val action = if (responseParam == null)
            HttpExternalServiceAction(request, "", externalService, counter++).apply {
                doInitialize(randomness)
            }
        else
            HttpExternalServiceAction(
                request = request,
                response = responseParam,
                externalService = externalService,
                id = counter++
            ).apply {
                seeTopGenes().forEach { g -> g.markAllAsInitialized() }
            }

        return action
    }

    /**
     * Returns the [HttpWsExternalService] if the signature exists
     */
    fun getExternalService(signature: String): HttpWsExternalService {
        return externalServices.getValue(signature)
    }

    /**
     * Returns a list of the served requests related to the specific WireMock
     * as [HttpExternalServiceRequest]
     */
    fun getAllServedExternalServiceRequests(): List<HttpExternalServiceRequest> {
        return externalServices.values.filter { it.isActive() }.filter { !isDefaultSignature(it.getSignature()) }
            .flatMap { it.getAllServedRequests() }
    }

    /**
     * @return a list of the served requests to the default WM
     */
    fun getAllServedRequestsToDefaultWM(): List<HttpExternalServiceRequest> {
        return externalServices.values.filter { isDefaultSignature(it.getSignature()) }
            .flatMap { it.getAllServedRequests() }
    }

    /**
     * Default IP address will be a randomly generated IP
     *
     * If user provided IP address isn't available on the port
     * IllegalStateException will be thrown.
     */
    private fun getNewIP(): String {
        val ip: String
        when (config.externalServiceIPSelectionStrategy) {
            // Although the default address will be a random, this
            // option allows selecting explicitly
            EMConfig.ExternalServiceIPSelectionStrategy.RANDOM -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress()
                } else {
                    generateRandomAvailableAddress()
                }
            }

            EMConfig.ExternalServiceIPSelectionStrategy.USER -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress()
                } else {
                    if (!isReservedIP(config.externalServiceIP)) {
                        config.externalServiceIP
                    } else {
                        throw IllegalStateException("Can not use a reserved IP address: ${config.externalServiceIP}")
                    }
                }
            }

            else -> {
                ip = if (externalServices.isNotEmpty()) {
                    getNextAvailableAddress()
                } else {
                    generateRandomAvailableAddress()
                }
            }
        }
        return ip
    }


    /**
     * To prevent from the 404 when no matching stub below stub is added
     * WireMock throws an exception when there is no stub for the request
     * to avoid the exception it handled manually
     */
    private fun wireMockSetDefaults(es: HttpWsExternalService) {
        es.getWireMockServer().stubFor(es.getDefaultWMMappingBuilder())
    }

    fun registerExternalServiceToSkip(service: ExternalService) {
        skippedExternalServices.add(service)
    }

    fun getSkippedExternalServices(): List<ExternalServiceDto> {
        val output: MutableList<ExternalServiceDto> = mutableListOf()
        skippedExternalServices.forEach {
            val dto = ExternalServiceDto()
            dto.hostname = it.getHostName()
            dto.port = it.getPort()
            output.add(dto)
        }
        return output
    }

}
