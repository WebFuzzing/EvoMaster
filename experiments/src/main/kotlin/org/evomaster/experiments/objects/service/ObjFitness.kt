package org.evomaster.experiments.objects.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.experiments.objects.ObjRestCallAction
import org.evomaster.experiments.objects.param.BodyParam
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ProtocolException
import java.net.SocketTimeoutException
import javax.annotation.PostConstruct
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.evomaster.experiments.objects.ObjIndividual


class ObjFitness : FitnessFunction<ObjIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ObjFitness::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var sampler: ObjRestSampler


    private val client: Client = {
        val configuration = ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
                .property(ClientProperties.READ_TIMEOUT, 10_000)
                //workaround bug in Jersey client
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
        ClientBuilder.newClient(configuration)
    }.invoke()

    private lateinit var infoDto: SutInfoDto


    @PostConstruct
    private fun initialize() {

        log.debug("Initializing {}", ObjFitness::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        log.debug("Done initializing {}", ObjFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            rc.stopSUT()
            initialize()
        } catch (e: Exception) {
            log.warn("Failed to re-initialize the SUT: $e")
            return false
        }

        return true
    }

    override fun doCalculateCoverage(individual: ObjIndividual): EvaluatedIndividual<ObjIndividual>? {

        rc.resetSUT()

        doInitializingActions(individual)

        individual.checkCoherence()

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        for (i in 0 until individual.actions.size) {

            //rc.registerNewAction(i)
            val a = individual.actions[i]

            var ok = false

            if (a is ObjRestCallAction) {
                ok = handleRestCall(a, actionResults, chainState)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(archive.notCoveredTargets(), 100)

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            ObjFitness.log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleExtra(dto, fv)

        handleResponseTargets(fv, individual.actions, actionResults)

        expandIndividual(individual, dto.additionalInfoList)

        return EvaluatedIndividual(fv, individual.copy() as ObjIndividual, actionResults)

        /*
            TODO when dealing with seeding, might want to extend EvaluatedIndividual
            to keep track of AdditionalInfo
         */
    }

    private fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {
        if (configuration.heuristicsForSQL) {

            val dbData = mutableListOf<ExecutionDto>()

            for (i in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[i]

                //TODO need update
//                if (!isEmpty(extra)) {
//                    //TODO handling of toMaximize
//                    fv.setExtraToMinimize(i, extra.toMinimize)
//                }

                extra.databaseExecutionDto?.let {
                    dbData.add(it)
                }
            }

            if (!dbData.isEmpty()) {
               // fv.emptySelects = DatabaseExecution.fromDtos(dbData)
            }
        }
    }

    /**
     * Based on what executed by the test, we might need to add new genes to the individual.
     * This for example can happen if we detected that the test is using headers or query
     * params that were not specified in the Swagger schema
     */
    private fun expandIndividual(
            individual: ObjIndividual,
            additionalInfoList: List<AdditionalInfoDto>
    ) {

        if (individual.actions.size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
            ObjFitness.log.warn("Length mismatch between ${individual.actions.size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in 0 until additionalInfoList.size) {

            val action = individual.actions[i]
            val info = additionalInfoList[i]

            if (action !is RestCallAction) {
                continue
            }

            /*
                Those are OptionalGenes, which MUST be off by default.
                We are changing the genotype, but MUST not change the phenotype.
                Otherwise, the fitness value we just computed would be wrong.
             */

            info.headers
                    .filter { name ->
                        ! action.parameters.any { it is HeaderParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        action.parameters.add(HeaderParam(it, OptionalGene(it, StringGene(it), false)))
                    }

            info.queryParameters
                    .filter { name ->
                        ! action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach { name ->
                        action.parameters.add(QueryParam(name, OptionalGene(name, StringGene(name), false)))
                    }
        }
    }


    private fun doInitializingActions(ind: ObjIndividual) {

        if (ind.dbInitialization.isEmpty()) {
            return
        }

        val dto = DbActionTransformer.transform(ind.dbInitialization)

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            log.warn("Failed in executing database command")
        }
    }



    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    private fun handleResponseTargets(
            fv: FitnessValue,
            callActions: MutableList<RestAction>,
            actionResults: MutableList<ActionResult>) {

        (0 until actionResults.size)
                .filter { callActions[it] is RestAction }
                .filter { actionResults[it] is RestCallResult }
                .forEach {
                    val status = (actionResults[it] as RestCallResult)
                            .getStatusCode() ?: -1
                    val desc = "$status:${callActions[it].getName()}"
                    val id = idMapper.handleLocalTarget(desc)
                    fv.updateTarget(id, 1.0, it)
                }
    }


    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    private fun handleRestCall(a: ObjRestCallAction,
                               actionResults: MutableList<ActionResult>,
                               chainState: MutableMap<String, String>)
            : Boolean {

        var baseUrl = infoDto.baseUrlOfSUT
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        val path = a.resolvedPath()

        val fullUri = if (a.locationId != null) {
            val locationHeader = chainState[locationName(a.locationId!!)]
                    ?: throw IllegalStateException("Call expected a missing chained 'location'")

            EMTestUtils.resolveLocation(locationHeader, baseUrl + path)!!

        } else {
            baseUrl + path
        }.let {
            /*
                TODO this will be need to be done properly, and check if
                it is or not a valid char
             */
            it.replace("\"", "")
        }

        val builder = client.target(fullUri).request()

        a.auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val prechosenAuthHeaders = a.auth.headers.map { it.name }

        /*
            TODO: optimization, avoid mutating header gene if anyway
            using pre-chosen one
         */

        a.parameters.filterIsInstance<org.evomaster.core.problem.rest.param.HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    builder.header(it.name, it.gene.getValueAsRawString())
                }


        /*
            TODO: need to handle "accept" of returned resource
         */


        /*
           TODO: need to handle also other formats in the body,
           not just JSON and forms
         */
        val body = a.parameters.find { p -> p is BodyParam }
        a.parameters.find{ p -> p is BodyParam }
        val forms = a.getBodyFormData()

        if (body != null && !forms.isBlank()) {
            throw IllegalStateException("Issue in Swagger configuration: both Body and FormData definitions in the same endpoint")
        }

        val bodyEntity = when {
            body != null -> Entity.json(body.gene.getValueAsPrintableString(targetFormat = null))
            !forms.isBlank() -> Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            else -> Entity.json("") //FIXME
        }


        val invocation = when (a.verb) {
            HttpVerb.GET -> builder.buildGet()
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.DELETE -> builder.buildDelete()
            HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.HEAD -> builder.build("HEAD")
            HttpVerb.TRACE -> builder.build("TRACE")
        }

        val rcr = RestCallResult()
        actionResults.add(rcr)

        val response = try {
            invocation.invoke()
        } catch (e: ProcessingException) {

            //this can happen for example if call ends up in an infinite redirection loop
            if ((e.cause?.message?.contains("redirected too many") ?: false) && e.cause is ProtocolException) {
                rcr.setInfiniteLoop(true)
                rcr.setErrorMessage(e.cause!!.message!!)
                return false
            } else if (e.cause is SocketTimeoutException) {
                /*
                    This is very tricky. In theory it shouldn't happen that a REST call
                    does timeout (eg 10 seconds). But it might happen due to glitch,
                    or if very slow hardware. If it is a glitch, we do not want to
                    kill whole EM process, as might not happen again. If it is a
                    constant, we want to avoid using such test if possible, as it
                    would kill performance.
                    In any case, a generated test should never check assertions on time,
                    eg expect that a is SocketTimeoutException thrown. Not only because
                    maybe it was just a glitch, but also because the test might be run
                    on different machines (remote CI vs local development PC) with
                    different performance (and so the test would become flaky)
                 */
                rcr.setTimedout(true)
                statistics.reportTimeout()
                return false
            } else {
                throw e
            }
        }

        rcr.setStatusCode(response.status)

        if (response.hasEntity()) {
            if (response.mediaType != null) {
                rcr.setBodyType(response.mediaType)
            }
            try {
                /*
                    FIXME should read as byte[]
                 */
                val body = response.readEntity(String::class.java)

                if (body.length < configuration.maxResponseByteSize) {
                    rcr.setBody(body)
                } else {
                    LoggingUtil.uniqueWarn(log,
                            "A very large response body was retrieved from the endpoint '${a.path}'." +
                            " If that was expected, increase the 'maxResponseByteSize' threshold" +
                            " in the configurations.")
                    rcr.setTooLargeBody(true)
                }

            } catch (e: Exception) {
                log.warn("Failed to parse HTTP response: ${e.message}")
            }

        }

        if (response.status == 401 && a.auth !is NoAuth) {
            //this would likely be a misconfiguration in the SUT controller
            log.warn("Got 401 although having auth for '${a.auth.name}'")
        }


        if (!handleSaveLocation(a, response, rcr, chainState)) return false

        return true
    }

    private fun handleSaveLocation(a: ObjRestCallAction, response: Response, rcr: RestCallResult, chainState: MutableMap<String, String>): Boolean {
        if (a.saveLocation) {

            if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
                /*
                    If this failed, and following callActions require the "location" header
                    of this call, there is no point whatsoever to continue evaluating
                    the remaining calls
                 */
                rcr.stopping = true
                return false
            }

            val name = locationName(a.path.lastElement())
            var location = response.getHeaderString("location")

            if (location == null) {
                /*
                    Excluding bugs, this might happen if API was
                    designed to return the created resource, from
                    which an "id" can be extracted.
                    This is usually not a good practice, but it can
                    happen. So, here we "heuristically" (cannot be 100% sure)
                    check if this is indeed the case
                 */
                val id = rcr.getResourceId()

                if (id != null && hasParameterChild(a)) {
                    location = a.resolvedPath() + "/" + id
                    rcr.setHeuristicsForChainedLocation(true)
                }
            }

            //save location for the following REST calls
            chainState[name] = location ?: ""
        }
        return true
    }

    private fun hasParameterChild(a: ObjRestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<ObjRestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }

    private fun locationName(id: String): String {
        return "location_$id"
    }
}