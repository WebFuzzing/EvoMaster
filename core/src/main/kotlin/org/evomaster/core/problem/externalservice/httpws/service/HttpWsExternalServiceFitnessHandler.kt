package org.evomaster.core.problem.externalservice.httpws.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.externalservice.HostnameResolutionInfo
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceInfo
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import java.util.Objects

/**
 * What a fitness function does with the HTTP services the SUT calls while a test is evaluated:
 * set up the WireMock instances before each main action, record which requests they served
 * after it, tell the driver where they are, and act on what the instrumentation saw the SUT
 * resolve and connect to.
 *
 * The state (which WireMock instances run where, which hostname maps to which local address)
 * is [HttpWsExternalServiceHandler]'s. These steps only need an [EnterpriseIndividual] whose main
 * actions sit in [EnterpriseActionGroup]s, so any problem type can use them.
 *
 * TODO REST has its own copies of these steps: AbstractRestFitness.handleExternalServiceInfo and
 *  getActionDto, and ResourceRestFitness.handleExternalActionsBeforeMainActionExecution and
 *  handleVisitedExternalAction. Moving them onto this class would be behaviour-preserving, and is
 *  what the TODO in AbstractRestFitness about pushing that code up the hierarchy points to.
 */
class HttpWsExternalServiceFitnessHandler {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var externalServiceHandler: HttpWsExternalServiceHandler

    @Inject
    private lateinit var harvestResponseHandler: HarvestActualHttpWsResponseHandler

    /**
     * Whether the HTTP services the SUT calls are mocked in this run.
     *
     * Only in white-box mode: which hostnames the SUT resolves, and where it connects, is only
     * known from the instrumentation, and a black-box run has none to read it from.
     */
    fun isEnabled(): Boolean = !config.blackBox && config.isEnabledExternalServiceMocking()

    /**
     * Put the WireMock instances in the state [action] expects, right before it is executed.
     *
     * Every instance goes back to its default response first, whether or not [action] has
     * external service actions of its own: a call that the SUT starts making because of a
     * mutation must not be answered with what an earlier action of the same test set up.
     *
     * @return the external service actions of [action]'s group, all of them, active or not, to be
     *         handed to [registerServedRequests] once [action] has been executed
     * @throws IllegalArgumentException if [action] is not inside an [EnterpriseActionGroup]
     */
    fun prepareForMainAction(action: Action): List<HttpExternalServiceAction> {
        Objects.requireNonNull(action)

        val group = action.parent as? EnterpriseActionGroup<*>
            ?: throw IllegalArgumentException(
                "Action ${action.getName()} is not inside an EnterpriseActionGroup, but in" +
                        " ${action.parent?.javaClass?.simpleName}"
            )

        externalServiceHandler.resetWireMockServersToDefaultState()

        val externalServiceActions = group.getExternalServiceActions()
            .filterIsInstance<HttpExternalServiceAction>()

        externalServiceActions.forEach { it.resetActive() }

        externalServiceActions
            .filter { it.active }
            .forEach { it.buildResponse() }

        return externalServiceActions
    }

    /**
     * Record which requests the WireMock instances served while the main action at [actionIndex]
     * was executed, and which of [externalServiceActions] were used to answer them.
     *
     * What is registered on [fv] is what the structure mutator later turns into new external
     * service actions, whose responses the search can then mutate.
     *
     * @param externalServiceActions what [prepareForMainAction] returned for the same action
     * @return the requests served, to be passed to [harvestActualResponses] once the whole test
     *         has been executed
     */
    fun registerServedRequests(
        fv: FitnessValue,
        actionIndex: Int,
        externalServiceActions: List<HttpExternalServiceAction>
    ): List<HttpExternalServiceRequest> {
        Objects.requireNonNull(fv)
        Objects.requireNonNull(externalServiceActions)

        val served = externalServiceHandler.getAllServedExternalServiceRequests()

        if (served.isNotEmpty()) {
            fv.registerExternalServiceRequest(actionIndex, served)
        }

        externalServiceActions
            .groupBy { it.request.absoluteURL }
            .forEach { (url, actions) ->
                //how many times this URL was called while the action was executed
                val count = served.count { it.absoluteURL == url }
                //the first ones are assumed to be those that answered
                actions.forEachIndexed { index, action ->
                    if (index < count) {
                        action.confirmUsed()
                    } else {
                        action.confirmNotUsed()
                    }
                }
            }

        return served
    }

    /**
     * Hand the requests the SUT made over to the harvester, which may fetch what the real
     * services answer to them. It does nothing unless harvesting is enabled.
     */
    fun harvestActualResponses(requests: List<HttpExternalServiceRequest>) {
        Objects.requireNonNull(requests)
        harvestResponseHandler.addHttpRequests(requests)
    }

    /**
     * Act on the hostnames the SUT resolved, and the services it connected to, while
     * [individual] was executed: [infoDto] has one entry per main action, as the driver collected
     * them.
     *
     * Every hostname gets a local address, and every service a WireMock instance on it. A
     * hostname that actually resolved also gets a [HostnameResolutionAction] to the reserved
     * local address added to [individual], which is what the instrumentation redirected it to:
     * a generated test must not reach the real service either.
     *
     * @return whether a WireMock instance was started as a result. If so, the results of this
     *         evaluation were obtained with that service unreachable, and should not be kept.
     */
    fun handleExternalServiceInfo(individual: EnterpriseIndividual, infoDto: List<AdditionalInfoDto>): Boolean {
        Objects.requireNonNull(individual)
        Objects.requireNonNull(infoDto)

        var started = false

        infoDto.forEach { info ->

            info.hostnameResolutionInfoDtos.forEach { hn ->

                val dns = HostnameResolutionInfo(hn.remoteHostname, hn.resolvedAddress)
                externalServiceHandler.addHostname(dns)

                if (dns.isResolved() && needsReservedAddress(individual, hn.remoteHostname)) {
                    individual.addChildToGroup(
                        HostnameResolutionAction(hn.remoteHostname, ExternalServiceSharedUtils.RESERVED_RESOLVED_LOCAL_IP),
                        GroupsOfChildren.INITIALIZATION_DNS
                    )
                }
            }

            info.externalServices.forEach { es ->
                val startedNow = externalServiceHandler.addExternalService(
                    HttpExternalServiceInfo(es.protocol, es.remoteHostname, es.remotePort)
                )
                started = started || startedNow
            }
        }

        return started
    }

    /**
     * Whether a hostname that really resolved has nothing in [individual] saying where it goes,
     * and is not the address of one of the WireMock instances, which a SUT resolving an
     * address with InetAddress would also report.
     */
    private fun needsReservedAddress(individual: EnterpriseIndividual, hostname: String): Boolean {

        val actions = individual.seeActions(ActionFilter.ONLY_DNS).filterIsInstance<HostnameResolutionAction>()

        return actions.none { it.hostname == hostname } && !externalServiceHandler.isWireMockAddress(hostname)
    }

    /**
     * Tell the driver, in [actionDto], where the hostnames of [individual] resolve to, where the
     * WireMock instances are, and which services it should leave alone.
     *
     * The driver reads these only from the first action of a test, and they hold for the whole of
     * it, so they are only added when [actionIndex] is 0.
     */
    fun addMappings(actionDto: ActionDto, actionIndex: Int, individual: EnterpriseIndividual) {
        Objects.requireNonNull(actionDto)
        Objects.requireNonNull(individual)

        if (actionIndex != 0) {
            return
        }

        actionDto.localAddressMapping = individual
            .seeActions(ActionFilter.ONLY_DNS)
            .filterIsInstance<HostnameResolutionAction>()
            .associate { it.hostname to it.localIPAddress }
        actionDto.externalServiceMapping = externalServiceHandler.getExternalServiceMappings()
        actionDto.skippedExternalServices = externalServiceHandler.getSkippedExternalServices()
    }
}
