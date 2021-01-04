package org.evomaster.core.problem.httpws.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.HeuristicEntryDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.rest.BlackBoxUtils
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.AbstractRestFitness
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Action
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.SearchTimeController
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.annotation.PostConstruct
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response

abstract class HttpWsFitness<T>: FitnessFunction<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestFitness::class.java)
    }

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var extraHeuristicsLogger: ExtraHeuristicsLogger

    @Inject
    protected lateinit var searchTimeController: SearchTimeController

    @Inject
    protected lateinit var writer: TestSuiteWriter


    protected val clientConfiguration = ClientConfig()
            .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
            .property(ClientProperties.READ_TIMEOUT, 10_000)
            //workaround bug in Jersey client
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.FOLLOW_REDIRECTS, false)


    protected var client: Client = ClientBuilder.newClient(clientConfiguration)


    protected lateinit var infoDto: SutInfoDto


    @PostConstruct
    protected fun initialize() {

       log.debug("Initializing {}", HttpWsFitness::class.simpleName)

        if (!config.blackBox || config.bbExperiments) {
            rc.checkConnection()

            val started = rc.startSUT()
            if (!started) {
                throw SutProblemException("Failed to start the system under test")
            }

            infoDto = rc.getSutInfo()
                    ?: throw SutProblemException("Failed to retrieve the info about the system under test")
        }

        log.debug("Done initializing {}", HttpWsFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            if (!config.blackBox) {
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
        } else if (configuration.extractSqlExecutionInfo) {

            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }
        }
    }

    protected fun getBaseUrl(): String {
        var baseUrl = if (!config.blackBox || config.bbExperiments) {
            infoDto.baseUrlOfSUT
        } else {
            BlackBoxUtils.restUrl(config)
        }

        try{
            /*
                Note: this in theory should already been checked: either in EMConfig for
                Black-box testing, or already in the driver for White-Box testing
             */
            URL(baseUrl)
        } catch (e: MalformedURLException){
            val base = "Invalid 'baseUrl'."
            val wb = "In the EvoMaster driver, in the startSut() method, you must make sure to return a valid URL."
            val err = " ERROR: $e"

            val msg = if(config.blackBox) "$base $err" else "$base $wb $err"
            throw SutProblemException(msg)
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        return baseUrl
    }

    /**
     * In general, we should avoid having SUT send close requests on the TCP socket.
     * However, Tomcat (default in SpringBoot) by default will do that any 100 requests... :(
     */
    protected fun handlePossibleConnectionClose(response: Response) {
        if(response.getHeaderString("Connection")?.contains("close", true) == true){
            searchTimeController.reportConnectionCloseRequest(response.status)
        }
    }

    /**
     * If any action needs auth based on cookies, do a "login" before
     * running the actions, and collect the cookies from the server.
     *
     * @return a map from username to auth cookie for those users
     */
    protected fun getCookies(ind: T): Map<String, List<NewCookie>> {

        val cookieLogins = CookieWriter.getCookieLoginAuth(ind)

        val map: MutableMap<String, List<NewCookie>> = HashMap()

        val baseUrl = getBaseUrl()

        for (cl in cookieLogins) {

            val mediaType = when (cl.contentType) {
                ContentType.X_WWW_FORM_URLENCODED -> MediaType.APPLICATION_FORM_URLENCODED_TYPE
                ContentType.JSON -> MediaType.APPLICATION_JSON_TYPE
            }

            val response = try {
                client.target(baseUrl + cl.loginEndpointUrl)
                        .request()
                        //TODO could consider other cases besides POST
                        .buildPost(Entity.entity(cl.payload(), mediaType))
                        .invoke()
            } catch (e: Exception) {
                log.warn("Failed to login for ${cl.username}/${cl.password}: $e")
                continue
            }

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {

                /*
                    if it is a 3xx, we need to look at Location header to determine
                    if a success or failure.
                    TODO: could explicitly ask for this info in the auth DTO.
                    However, as 3xx makes little sense in a REST API, maybe not so
                    important right now, although had this issue with some APIs using
                    default settings in Spring Security
                */
                if (response.statusInfo.family == Response.Status.Family.REDIRECTION) {
                    val location = response.getHeaderString("location")
                    if (location != null && (location.contains("error", true) || location.contains("login", true))) {
                        log.warn("Login request failed with ${response.status} redirection toward $location")
                        continue
                    }
                } else {
                    log.warn("Login request failed with status ${response.status}")
                    continue
                }
            }

            if (response.cookies.isEmpty()) {
                log.warn("Cookie-based login request did not give back any new cookie")
                continue
            }

            map[cl.username] = response.cookies.values.toList()
        }

        return map
    }

    override fun targetsToEvaluate(targets: Set<Int>, individual: T): Set<Int> {
        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list
        val ts = targets.filter { !IdMapper.isLocal(it) }.toMutableSet()
        val nc = archive.notCoveredTargets().filter { !IdMapper.isLocal(it) && !ts.contains(it)}
        recordExceededTarget(nc)
        return when {
            ts.size > 100 -> randomness.choose(ts, 100)
            nc.isEmpty() -> ts
            else -> ts.plus(randomness.choose(nc, 100 - ts.size))
        }
    }

    private fun recordExceededTarget(targets: Collection<Int>){
        if(!config.recordExceededTargets) return
        if (targets.size <= 100) return

        val path = Paths.get(config.exceedTargetsFile)
        if (Files.notExists(path.parent)) Files.createDirectories(path.parent)
        if (Files.notExists(path)) Files.createFile(path)
        Files.write(path, listOf(time.evaluatedIndividuals.toString()).plus(targets.map { idMapper.getDescriptiveId(it) }), StandardOpenOption.APPEND)
    }

    protected fun registerNewAction(action: Action, index: Int){

        rc.registerNewAction(ActionDto().apply {
            this.index = index
            //for now, we only include specialized regex
            this.inputVariables = action.seeGenes()
                    .flatMap { it.flatView() }
                    .filterIsInstance<StringGene>()
                    .filter { it.getSpecializationGene() != null && it.getSpecializationGene() is RegexGene }
                    .map { it.getSpecializationGene()!!.getValueAsRawString()}
        })
    }

    protected fun updateFitnessAfterEvaluation(targets: Set<Int>, individual: T, fv: FitnessValue) : TestResultsDto?{
        val ids = targetsToEvaluate(targets, individual)

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {

                val noMethodReplacement = !config.useMethodReplacement && t.descriptiveId.startsWith(ObjectiveNaming.METHOD_REPLACEMENT)
                val noNonIntegerReplacement = !config.useNonIntegerReplacement && t.descriptiveId.startsWith(ObjectiveNaming.NUMERIC_COMPARISON)

                if (noMethodReplacement || noNonIntegerReplacement) {
                    return@forEach
                }

                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        return dto
    }

}