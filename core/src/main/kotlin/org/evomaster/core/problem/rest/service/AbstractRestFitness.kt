package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.*
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.OptionalGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.SearchTimeController
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


abstract class AbstractRestFitness<T> : FitnessFunction<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestFitness::class.java)
    }

    @Inject(optional = true)
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var extraHeuristicsLogger: ExtraHeuristicsLogger

    @Inject
    private lateinit var searchTimeController: SearchTimeController


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

        log.debug("Initializing {}", AbstractRestFitness::class.simpleName)

        if(! config.blackBox || config.bbExperiments) {
            rc.checkConnection()

            val started = rc.startSUT()
            if (!started) {
                throw SutProblemException("Failed to start the system under test")
            }

            infoDto = rc.getSutInfo()
                    ?: throw SutProblemException("Failed to retrieve the info about the system under test")
        }

        log.debug("Done initializing {}", AbstractRestFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            if(! config.blackBox) {
                rc.stopSUT()
            }
            initialize()
        } catch (e: Exception) {
            log.warn("Failed to re-initialize the SUT: $e")
            return false
        }

        return true
    }

    protected fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {
        if (configuration.heuristicsForSQL) {

            for (i in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[i]

                //TODO handling of toMaximize as well
                //TODO refactoring when will have other heuristics besides for SQL

                extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

                val toMinimize = extra.heuristics
                        .filter {
                            it != null
                                    && it.objective == HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                        }.map { it.value }
                        .toList()

                if (!toMinimize.isEmpty()) {
                    fv.setExtraToMinimize(i, toMinimize)
                }

                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }

            fv.aggregateDatabaseData()

            if (!fv.getViewOfAggregatedFailedWhere().isEmpty()) {
                searchTimeController.newIndividualsWithSqlFailedWhere()
            }
        }else if(configuration.extractSqlExecutionInfo){

            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }
        }
    }

    /**
     * Based on what executed by the test, we might need to add new genes to the individual.
     * This for example can happen if we detected that the test is using headers or query
     * params that were not specified in the Swagger schema
     */
    open fun expandIndividual(
            individual: RestIndividual,
            additionalInfoList: List<AdditionalInfoDto>
    ) {

        if (individual.seeActions().size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
           log.warn("Length mismatch between ${individual.seeActions().size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in 0 until additionalInfoList.size) {

            val action = individual.seeActions()[i]
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
                        !action.parameters.any { it is HeaderParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        action.parameters.add(HeaderParam(it, OptionalGene(it, StringGene(it), false)))
                    }

            info.queryParameters
                    .filter { name ->
                        !action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach { name ->
                        action.parameters.add(QueryParam(name, OptionalGene(name, StringGene(name), false)))
                    }
        }
    }



    /**
     * Initializing Actions before evaluating its fitness if need
     */
    open fun doInitializingActions(ind: T) {
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    protected fun handleResponseTargets(
            fv: FitnessValue,
            actions: List<RestAction>,
            actionResults: List<ActionResult>,
            additionalInfoList: List<AdditionalInfoDto>) {

        (0 until actionResults.size)
                .filter { actions[it] is RestCallAction }
                .filter { actionResults[it] is RestCallResult }
                .forEach {
                    val status = (actionResults[it] as RestCallResult)
                            .getStatusCode() ?: -1
                    val name = actions[it].getName()

                    //objective for HTTP specific status code
                    val statusId = idMapper.handleLocalTarget("$status:$name")
                    fv.updateTarget(statusId, 1.0, it)

                    /*
                        Objectives for results on endpoints.
                        Problem: we might get a 4xx/5xx, but then no gradient to keep sampling for
                        that endpoint. If we get 2xx, and full coverage, then no gradient to try
                        to keep sampling that endpoint to get a 5xx
                     */
                    val okId = idMapper.handleLocalTarget("HTTP_SUCCESS:$name")
                    val faultId = idMapper.handleLocalTarget("HTTP_FAULT:$name")

                    //OK -> 5xx being better than 4xx, as code executed
                    //FAULT -> 4xx worse than 2xx (can't find bugs if input is invalid)
                    if (status in 200..299) {
                        fv.updateTarget(okId, 1.0, it)
                        fv.updateTarget(faultId, 0.5, it)
                    } else if (status in 400..499) {
                        fv.updateTarget(okId, 0.1, it)
                        fv.updateTarget(faultId, 0.1, it)
                    } else if (status in 500..599) {
                        fv.updateTarget(okId, 0.5, it)
                        fv.updateTarget(faultId, 1.0, it)
                    }

                    /*
                        500 codes "might" be bugs. To distinguish between different bugs
                        that crash the same endpoint, we need to know what was the last
                        executed statement in the SUT.
                        So, we create new targets for it.
                     */
                    if (status == 500) {
                        val statement = additionalInfoList[it].lastExecutedStatement
                        val postfix = statement ?: "framework_code"
                        val descriptiveId = idMapper.getFaultDescriptiveId("$postfix $name")
                        val bugId = idMapper.handleLocalTarget(descriptiveId)
                        fv.updateTarget(bugId, 1.0, it)
                    }
                }
    }


    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    protected fun handleRestCall(a: RestCallAction,
                               actionResults: MutableList<ActionResult>,
                               chainState: MutableMap<String, String>)
            : Boolean {

        var baseUrl = if(!config.blackBox || config.bbExperiments){
            infoDto.baseUrlOfSUT
        } else {
            config.bbTargetUrl!!
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        val path = a.resolvedPath()

        val locationHeader = if (a.locationId != null) {
            chainState[locationName(a.locationId!!)]
                    ?: throw IllegalStateException("Call expected a missing chained 'location'")
        } else {
            null
        }

        val fullUri = EMTestUtils.resolveLocation(locationHeader, baseUrl + path)!!
                .let {
                    /*
                        TODO this will be need to be done properly, and check if
                        it is or not a valid char.
                        Furthermore, likely needed to be done in resolveLocation,
                        or at least check how RestAssured would behave
                     */
                    it.replace("\"", "")
                }

        /*
            TODO: This only considers the first in the list of produced responses
            This is fine for endpoints that only produce one type of response.
            Could be a problem in future
        */
        val produces = a.produces.first()

        val builder = client.target(fullUri).request(produces)

        a.auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val prechosenAuthHeaders = a.auth.headers.map { it.name }

        /*
            TODO: optimization, avoid mutating header gene if anyway
            using pre-chosen one
         */

        a.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    builder.header(it.name, it.gene.getValueAsRawString())
                }


        /*
            TODO: need to handle "accept" of returned resource
         */


        val body = a.parameters.find { p -> p is BodyParam }
        val forms = a.getBodyFormData()

        if (body != null && forms != null) {
            throw IllegalStateException("Issue in Swagger configuration: both Body and FormData definitions in the same endpoint")
        }

        val bodyEntity = if (body != null && body is BodyParam) {
            val mode = when {
                body.isJson() -> "json"
                //body.isXml() -> "xml" // might have to handle here: <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                body.isTextPlain() -> "text"
                else -> throw IllegalStateException("Cannot handle body type: " + body.contentType())
            }
            Entity.entity(body.gene.getValueAsPrintableString(mode = mode, targetFormat = configuration.outputFormat), body.contentType())
        } else if (forms != null) {
            Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if (a.verb == HttpVerb.PUT || a.verb == HttpVerb.PATCH) {
            /*
                PUT and PATCH must have a payload. But it might happen that it is missing in the Swagger schema
                when objects like WebRequest are used. So we default to urlencoded
             */
            Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else {
            null
        }

        val invocation = when (a.verb) {
            HttpVerb.GET -> builder.buildGet()
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.DELETE -> builder.buildDelete()
            HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.HEAD -> builder.build("HEAD")
        }

        val rcr = RestCallResult()
        actionResults.add(rcr)

        val response = try {
            invocation.invoke()
        } catch (e: ProcessingException) {

            //this can happen for example if call ends up in an infinite redirection loop
            if ((e.cause?.message?.contains("redirected too many") == true) && e.cause is ProtocolException) {
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
                    log.warn("A very large response body was retrieved from the endpoint '${a.path}'." +
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

    private fun handleSaveLocation(a: RestCallAction, response: Response, rcr: RestCallResult, chainState: MutableMap<String, String>): Boolean {
        if (a.saveLocation) {

            if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
                /*
                    If this failed, and following actions require the "location" header
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


    abstract fun hasParameterChild(a: RestCallAction): Boolean

    private fun locationName(id: String): String {
        return "location_$id"
    }
}