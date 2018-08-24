package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.clientJava.controllerApi.EMTestUtils
import org.evomaster.clientJava.controllerApi.dto.ExtraHeuristicDto
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto
import org.evomaster.clientJava.controllerApi.dto.database.execution.ReadDbDataDto
import org.evomaster.clientJava.controllerApi.dto.database.operations.DatabaseCommandDto
import org.evomaster.clientJava.controllerApi.dto.database.operations.InsertionDto
import org.evomaster.clientJava.controllerApi.dto.database.operations.InsertionEntryDto
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.EmptySelects
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.FitnessFunction
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


class RestFitness : FitnessFunction<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitness::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var sampler: RestSampler


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

        log.debug("Initializing {}", RestFitness::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        log.debug("Done initializing {}", RestFitness::class.simpleName)
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

    override fun doCalculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()

        doInitializingActions(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        val dbData = mutableListOf<ReadDbDataDto>()

        //run the test, one action at a time
        for (i in 0 until individual.actions.size) {

            rc.registerNewAction(i)
            val a = individual.actions[i]

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState)
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }

            if (configuration.heuristicsForSQL) {
                val extra = rc.getExtraHeuristics()
                if (extra == null) {
                    log.warn("Cannot retrieve extra heuristics")
                    return null
                }

                if (!isEmpty(extra)) {
                    //TODO handling of toMaximize
                    fv.setExtraToMinimize(i, extra.toMinimize)
                }

                extra.readDbData?.let {
                    dbData.add(it)
                }
            }
        }

        if (!dbData.isEmpty()) {
            fv.emptySelects = EmptySelects.fromDtos(dbData)
        }


        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ids = randomness.choose(archive.notCoveredTargets(), 100)

        val dto = rc.getTargetCoverage(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleResponseTargets(fv, individual.actions, actionResults)

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults)
    }

    private fun doInitializingActions(ind: RestIndividual) {

        if (ind.dbInitialization.isEmpty()) {
            return
        }

        val list = mutableListOf<InsertionDto>()
        val previous = mutableListOf<Gene>()

        for (i in 0 until ind.dbInitialization.size) {

            val action = ind.dbInitialization[i]
            val insertion = InsertionDto().apply { targetTable = action.table.name }

            for (g in action.seeGenes()) {
                if (g is SqlPrimaryKeyGene) {
                    /*
                        If there is more than one primary key field, this
                        will be overridden.
                        But, as we need it only for automatically generated ones,
                        this shouldn't matter, as in that case there should be just 1.
                     */
                    insertion.id = g.uniqueId
                }

                if (!g.isPrintable()) {
                    continue
                }

                val entry = InsertionEntryDto()

                if (g is SqlForeignKeyGene) {
                    handleSqlForeignKey(g, previous, entry)
                } else if (g is SqlPrimaryKeyGene) {
                    val k = g.gene
                    if (k is SqlForeignKeyGene) {
                        handleSqlForeignKey(k, previous, entry)
                    } else {
                        entry.printableValue = g.getValueAsPrintableString()
                    }
                } else {
                    entry.printableValue = g.getValueAsPrintableString()
                }

                entry.variableName = g.getVariableName()

                insertion.data.add(entry)
            }

            list.add(insertion)
            previous.addAll(action.seeGenes())
        }

        val dto = DatabaseCommandDto().apply { insertions = list }

        val ok = rc.executeDatabaseCommand(dto)
        if (!ok) {
            log.warn("Failed in executing database command")
        }
    }

    private fun handleSqlForeignKey(
            g: SqlForeignKeyGene,
            previous: List<Gene>,
            entry: InsertionEntryDto
    ) {
        if (g.isReferenceToNonPrintable(previous)) {
            entry.foreignKeyToPreviouslyGeneratedRow = g.uniqueIdOfPrimaryKey
        } else {
            entry.printableValue = g.getValueAsPrintableString(previous)
        }
    }

    private fun isEmpty(dto: ExtraHeuristicDto): Boolean {

        val hasMin = dto.toMinimize != null && !dto.toMinimize.isEmpty()
        val hasMax = dto.toMaximize != null && !dto.toMaximize.isEmpty()

        return !hasMin && !hasMax
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    private fun handleResponseTargets(
            fv: FitnessValue,
            actions: MutableList<RestAction>,
            actionResults: MutableList<ActionResult>) {

        (0 until actionResults.size)
                .filter { actions[it] is RestCallAction }
                .filter { actionResults[it] is RestCallResult }
                .forEach {
                    val status = (actionResults[it] as RestCallResult)
                            .getStatusCode() ?: -1
                    val desc = "$status:${actions[it].getName()}"
                    val id = idMapper.handleLocalTarget(desc)
                    fv.updateTarget(id, 1.0, it)
                }
    }


    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    private fun handleRestCall(a: RestCallAction,
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
        val forms = a.getBodyFormData()

        if (body != null && !forms.isBlank()) {
            throw IllegalStateException("Issue in Swagger configuration: both Body and FormData definitions in the same endpoint")
        }

        val bodyEntity = when {
            body != null -> Entity.json(body.gene.getValueAsPrintableString())
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

    private fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
                .filterIsInstance<RestCallAction>()
                .map { it.path }
                .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }

    private fun locationName(id: String): String {
        return "location_$id"
    }
}