package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.client.java.controller.api.dto.ControllerInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inGreen
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.AnsiColor.Companion.inYellow
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.TestSuiteCode
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.enterprise.service.WFCReportWriter
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.service.GraphQLBlackBoxModule
import org.evomaster.core.problem.graphql.service.GraphQLModule
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.rest.service.module.BlackBoxRestModule
import org.evomaster.core.problem.rest.service.module.ResourceRestModule
import org.evomaster.core.problem.rest.service.module.RestModule
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.service.RPCModule
import org.evomaster.core.problem.security.service.HttpCallbackVerifier
import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.problem.webfrontend.service.WebModule
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.*
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import java.lang.reflect.InvocationTargetException
import java.util.Locale
import kotlin.system.exitProcess


/**
 * This will be the entry point of the tool when run from command line
 */
class Main {
    companion object {

        /**
         * Main entry point of the EvoMaster application
         */
        @JvmStatic
        fun main(args: Array<String>) {

            try {
                Locale.setDefault(Locale.ENGLISH)

                printLogo()
                printVersion()
                if (!JdkIssue.checkAddOpens()) {
                    return
                }

                /*
                    Before running anything, check if the input
                    configurations are valid.
                    Note: some setting might be evaluated later, eg, if they require
                    to analyze the API schema.
                 */
                val parser = try {
                    EMConfig.validateOptions(args)
                } catch (e: ConfigProblemException) {
                    logError(
                        "Invalid parameter settings: " + e.message +
                                "\nUse --help to see the available options"
                    )
                    return
                }

                val options = parser.parse(*args)

                if (options.has("help")) {
                    parser.printHelpOn(System.out)
                    return
                }

                val config = EMConfig().apply { updateProperties(options) }

                if (config.runningInDocker) {
                    if (config.blackBox) {
                        LoggingUtil.getInfoLogger().info(
                            inGreen(
                                "You are running EvoMaster inside Docker." +
                                        " To access the generated test suite under '/generated_tests', you will need to mount a folder" +
                                        " or volume." +
                                        " Also references to host machine on 'localhost' would need to be replaced with" +
                                        " 'host.docker.internal'." +
                                        " If this is the first time you run EvoMaster in Docker, you are strongly recommended to first" +
                                        " check the documentation at:"
                            ) +
                                    " ${inBlue("https://github.com/WebFuzzing/EvoMaster/blob/master/docs/docker.md")}"
                        )
                    } else {
                        LoggingUtil.getInfoLogger().warn(
                            inYellow(
                                "White-box testing (default in EvoMaster) is currently not supported / not recommended in Docker." +
                                        " To run EvoMaster in black-box mode, you can use '--blackBox true'." +
                                        " If you need to run in white-box mode, it is recommended to download an OS installer or" +
                                        " the uber JAR file from the release-page on GitHub."
                            )
                        )
                    }
                }

                initAndRun(args)

                LoggingUtil.getInfoLogger().apply {
                    info("EvoMaster process has completed successfully")
                    info(
                        "Use ${inGreen("--help")} and visit ${inBlue("https://www.evomaster.org")} to" +
                                " learn more about available options"
                    )
                }

            } catch (e: Exception) {

                var cause: Throwable = e
                while (cause.cause != null) {
                    cause = cause.cause!!
                }

                when (cause) {
                    is NoRemoteConnectionException ->
                        logError(
                            "ERROR: ${cause.message}" +
                                    "\n  For WHITE-BOX testing (e.g., for JVM applications, requiring to write a driver" +
                                    " class) make sure the EvoMaster Driver for the system under test is running correctly." +
                                    "\n  On the other hand, if you are doing BLACK-BOX testing (for any kind of programming language)" +
                                    " without code analyses, remember to specify '--blackBox true' on the command-line."
                        )

                    is SutProblemException ->
                        logError(
                            "ERROR related to the system under test: ${cause.message}" +
                                    "\n  For white-box testing, look at the logs of the EvoMaster Driver to help debugging this problem."
                        )

                    is ConfigProblemException ->
                        logError(
                            "Invalid parameter settings: ${cause.message}" +
                                    "\nUse --help to see the available options"
                        )

                    else ->
                        LoggingUtil.getInfoLogger().error(
                            inRed("[ERROR] ") +
                                    inYellow(
                                        "EvoMaster process terminated abruptly." +
                                                " This is likely a bug in EvoMaster." +
                                                " Please copy&paste the following stacktrace, and create a new issue on" +
                                                " " + inBlue("https://github.com/WebFuzzing/EvoMaster/issues")
                                    ), e
                        )
                }

                /*
                    Need to signal error status.
                    But this code can become problematic if reached by any test.
                    Also in case of exceptions, must shutdown explicitly, otherwise running threads in
                    the background might keep the JVM alive.
                    See for example HarvestActualHttpWsResponseHandler
                 */
                exitProcess(1)
            }
        }

        private fun logError(msg: String) {
            LoggingUtil.getInfoLogger().error(inRed("[ERROR] ") + inYellow(msg))
        }

        private fun logWarn(msg: String) {
            LoggingUtil.getInfoLogger().warn(inYellow("[WARNING] ") + inYellow(msg))
        }

        private fun printLogo() {

            val logo = """
 _____          ___  ___          _
|  ___|         |  \/  |         | |
| |____   _____ | .  . | __ _ ___| |_ ___ _ __
|  __\ \ / / _ \| |\/| |/ _` / __| __/ _ \ '__|
| |___\ V / (_) | |  | | (_| \__ \ ||  __/ |
\____/ \_/ \___/\_|  |_/\__,_|___/\__\___|_|

                    """

            LoggingUtil.getInfoLogger().info(inBlue(logo))
        }

        private fun printVersion() {

            val version = this::class.java.`package`?.implementationVersion ?: "SNAPSHOT"

            LoggingUtil.getInfoLogger().info("EvoMaster version: $version")
        }

        @JvmStatic
        fun initAndRun(args: Array<String>): Solution<*> {

            val injector = init(args)
            val solution = runAndPostProcess(injector)
            return solution
        }

        @JvmStatic
        fun initAndDebug(args: Array<String>): Pair<Injector,Solution<*>> {

            val injector = init(args)
            val solution = runAndPostProcess(injector)

            return Pair(injector,solution)
        }

        private fun runAndPostProcess(injector: Injector): Solution<*> {

            checkExperimentalSettings(injector)

            val controllerInfo = checkState(injector)

            val config = injector.getInstance(EMConfig::class.java)
            val idMapper = injector.getInstance(IdMapper::class.java)
            val epc = injector.getInstance(ExecutionPhaseController::class.java)

            var solution = run(injector, controllerInfo)

            //save data regarding the search phase
            writeOverallProcessData(injector)
            writeDependencies(injector)
            writeImpacts(injector, solution)
            writeExecuteInfo(injector)

            logTimeSearchInfo(injector, config)

            //apply new phases
            solution = phaseHttpOracle(injector, config, solution)
            solution = phaseSecurity(injector, config, epc, solution)

            val suites = writeTests(injector, solution, controllerInfo)
            writeWFCReport(injector, solution, suites)

            writeCoveredTargets(injector, solution)
            writeStatistics(injector, solution)
            //FIXME if other phases after search, might get skewed data on 100% snapshots...

            resetExternalServiceHandler(injector)
            // Stop the WM before test execution
            stopHTTPCallbackVerifier(injector)

            val statistics = injector.getInstance(Statistics::class.java)
            val data = statistics.getData(solution)

            val timeouts = data.find { p -> p.header == Statistics.TEST_TIMEOUTS }!!.element.toInt()
            if (timeouts > 0) {
                LoggingUtil.getInfoLogger().info("TCP timeouts: $timeouts")
            }
            val faults = solution.overall.potentialFoundFaults(idMapper)
            LoggingUtil.getInfoLogger().info("Potential faults: ${faults.size}")

            logCodeCoverage(injector, config, solution, idMapper)
            logActionCoverage(injector, config, data)
            logDefaultTimeBudgetWarning(config)

            solution.statistics = data.toMutableList()

            epc.finishSearch()

            return solution
        }

        private fun logDefaultTimeBudgetWarning(config: EMConfig) {
            if (config.stoppingCriterion == EMConfig.StoppingCriterion.TIME &&
                config.maxTime == config.defaultMaxTime
            ) {
                LoggingUtil.getInfoLogger().warn(
                    inGreen(
                        "You are using the default time budget '${config.defaultMaxTime}'." +
                                " This is only for demo purposes. " +
                                " You should increase such test budget." +
                                " To obtain better results, use the '--maxTime' option" +
                                " to run the search for longer, like for example something between '1h' and '24h' hours."
                    )
                )
            }
        }

        private fun logActionCoverage(
            injector: Injector,
            config: EMConfig,
            data: List<Statistics.Pair>
        ) {
            val sampler: Sampler<*> = injector.getInstance(Key.get(object : TypeLiteral<Sampler<*>>() {}))

            val n = data.find { it.header == Statistics.DISTINCT_ACTIONS }!!.element.toInt()

            when (config.problemType) {
                EMConfig.ProblemType.REST -> {
                    val k = data.find { it.header == Statistics.COVERED_2XX }!!.element.toInt()
                    val t = if (sampler.getPreDefinedIndividuals().isNotEmpty()) {
                        /*
                            FIXME this is a temporary hack...
                            right now we might have 1 call to Schema that messes up this statistics
                         */
                        n + 1
                    } else {
                        n
                    }
                    assert(k <= t)
                    val p = String.format("%.0f", (k.toDouble() / t) * 100)
                    LoggingUtil.getInfoLogger()
                        .info("Successfully executed (HTTP code 2xx) $k endpoints out of $t ($p%)")
                }

                EMConfig.ProblemType.GRAPHQL -> {
                    val k = data.find { it.header == Statistics.GQL_NO_ERRORS }!!.element.toInt()
                    val p = String.format("%.0f", (k.toDouble() / n) * 100)
                    LoggingUtil.getInfoLogger().info("Successfully executed (no 'errors') $k endpoints out of $n ($p%)")
                }

                else -> {}
                //TODO others, eg RPC
            }
        }

        private fun logCodeCoverage(
            injector: Injector,
            config: EMConfig,
            solution: Solution<*>,
            idMapper: IdMapper
        ) {
            if (!config.blackBox || config.bbExperiments) {
                val rc = injector.getInstance(RemoteController::class.java)
                val unitsInfo = rc.getSutInfo()?.unitsInfoDto
                val bootTimeInfo = rc.getSutInfo()?.bootTimeInfoDto

                val targetsInfo = solution.overall.unionWithBootTimeCoveredTargets(null, idMapper, bootTimeInfo)
                val linesInfo =
                    solution.overall.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, bootTimeInfo)

                if (unitsInfo != null) {
                    val units = unitsInfo.unitNames.size
                    val totalLines = unitsInfo.numberOfLines
                    val percentage = String.format("%.0f", (linesInfo.total / totalLines.toDouble()) * 100)

                    /*
                        This is a quite tricky case...
                        the number of covered lines X should be less or equal than the total T, ie X<=T.
                        However, we end up with cases like X > T where T=0.
                        Should never happen in practice, but it does for E2E tests.
                        This is because we could have different test suites working on same SUTs.
                        Once one is finished, it would reset all data.
                        Such data would not then be recomputed in the next test suite execution, as
                        the classes are already loaded...
                        Not sure if there is any clean solution for this...
                        executing these tests in own process might be done with Failsafe/Surefire.

                        Having check for totalLines == 0 was not a good solution. If the assertion fails,
                        and test is re-executed on same JVM with classes already loaded, then we would get
                        totalLines == 0 after the reset... and so the test cases will always pass :(
                     */
                    //assert(totalLines == 0 || linesInfo.total <= totalLines){ "${linesInfo.total} > $totalLines"}
                    /*
                        Having this assertion is way too problematic... not only issue when more than 2 E2E use
                        the same SUT, but also when flacky tests are re-run (both in our scaffolding, and in Maven)
                     */
                    //assert(linesInfo.total <= totalLines){ "WRONG COVERAGE: ${linesInfo.total} > $totalLines"}

                    LoggingUtil.getInfoLogger()
                        .info("Covered targets (lines, branches, faults, etc.): ${targetsInfo.total}")

                    if (totalLines == 0 || units == 0) {
                        logError(
                            "Detected $totalLines lines to cover, for a total of $units units/classes." +
                                    " Are you sure you did setup getPackagePrefixesToCover() correctly?"
                        )
                    } else {
                        LoggingUtil.getInfoLogger()
                            .info("Bytecode line coverage: $percentage% (${linesInfo.total} out of $totalLines in $units units/classes)")
                    }
                } else {
                    LoggingUtil.getInfoLogger().warn("Failed to retrieve SUT info")
                }
            }
        }

        private fun phaseSecurity(
            injector: Injector,
            config: EMConfig,
            epc: ExecutionPhaseController,
            solution: Solution<*>
        ): Solution<*> {
            if (!config.security) {
                return solution
            }
            //apply security testing phase
            LoggingUtil.getInfoLogger().info("Starting to apply security testing")
            epc.startSecurity()

            //TODO might need to reset stc, and print some updated info again

            return when (config.problemType) {
                EMConfig.ProblemType.REST -> {
                    val securityRest = injector.getInstance(SecurityRest::class.java)
                    val solution = securityRest.applySecurityPhase()

                    if (config.ssrf && config.isEnabledFaultCategory(DefinedFaultCategory.SSRF)) {
                        LoggingUtil.getInfoLogger().info("Starting to apply SSRF detection.")

                        val ssrfAnalyser = injector.getInstance(SSRFAnalyser::class.java)
                        ssrfAnalyser.apply()
                    } else {
                        if(!config.isEnabledFaultCategory(DefinedFaultCategory.SSRF)) {
                            LoggingUtil.uniqueUserInfo("Skipping security test for SSRF detection as disabled in configuration")
                        }

                        return solution
                    }
                }

                else -> {
                    LoggingUtil.getInfoLogger()
                        .warn("Security phase currently not handled for problem type: ${config.problemType}")
                    solution
                }
            }
        }

        private fun phaseHttpOracle(
            injector: Injector,
            config: EMConfig,
            solution: Solution<*>
        ): Solution<*> {

            return if (config.httpOracles && config.problemType == EMConfig.ProblemType.REST) {
                LoggingUtil.getInfoLogger().info("Starting to apply HTTP")

                val httpSemanticsService = injector.getInstance(HttpSemanticsService::class.java)
                httpSemanticsService.applyHttpSemanticsPhase()
            } else {
                solution
            }
        }

        private fun logTimeSearchInfo(injector: Injector, config: EMConfig) {
            val stc = injector.getInstance(SearchTimeController::class.java)

            LoggingUtil.getInfoLogger().apply {
                info("Evaluated tests: ${stc.evaluatedIndividuals}")
                info("Evaluated actions: ${stc.evaluatedActions}")
                info("Needed budget: ${stc.neededBudget()}")

                if (!config.avoidNonDeterministicLogs) {
                    info("Passed time (seconds): ${stc.getElapsedSeconds()}")
                    info("Execution time per test (ms): ${stc.averageTestTimeMs}")
                    info("Execution time per action (ms): ${stc.averageActionTimeMs}")
                    info("Computation overhead between tests (ms): ${stc.averageOverheadMsBetweenTests}")
                    if (!config.blackBox) {
                        info("Computation overhead of resetting the SUT (ms): ${stc.averageResetSUTTimeMs}")
                        //This one might be confusing, as based only on minimization phase...
                        //info("Data transfer overhead of test results, per test, all targets (bytes): ${stc.averageByteOverheadTestResultsAll}")
                        debug("Data transfer overhead of fetching test results, per test, subset of targets (bytes): ${stc.averageByteOverheadTestResultsSubset}")
                        info("Computation overhead of fetching test results, per test, subset of targets (ms): ${stc.averageOverheadMsTestResultsSubset}")
                    }
                }
            }
        }

        @JvmStatic
        fun init(args: Array<String>): Injector {

            LoggingUtil.getInfoLogger().info("Initializing...")

            val base = BaseModule(args)
            val config = base.getEMConfig()

            if (config.problemType == EMConfig.ProblemType.DEFAULT) {
                /*
                    Note that, in case ob BB-testing, this would had been already modified
                 */
                assert(!config.blackBox || config.bbExperiments)

                val rc = RemoteControllerImplementation(base.getEMConfig())

                rc.checkConnection()

                /*
                    Note: we need to start the SUT, because the sutInfo might depend on its dynamic
                    state, eg the ephemeral port of the server
                 */
                val started = rc.startSUT()
                if (!started) {
                    throw SutProblemException("Failed to start the SUT")
                }

                val info = rc.getSutInfo()
                    ?: throw SutProblemException(
                        "No 'problemType' was defined, but failed to retried the needed" +
                                " info from the EM Driver."
                    )

                if (info.restProblem != null) {
                    config.problemType = EMConfig.ProblemType.REST
                } else if (info.graphQLProblem != null) {
                    config.problemType = EMConfig.ProblemType.GRAPHQL
                } else if (info.rpcProblem != null) {
                    config.problemType = EMConfig.ProblemType.RPC
                } else if (info.webProblem != null) {
                    config.problemType = EMConfig.ProblemType.WEBFRONTEND
                } else {
                    throw IllegalStateException("Can connect to the EM Driver, but cannot infer the 'problemType'")
                }
                //TODO in future might support Driver with multi-problem definitions

                //as we modified problemType, we need to re-check these constraints
                config.checkMultiFieldConstraints()
            }

            val problemModule = when (config.problemType) {
                EMConfig.ProblemType.REST -> {
                    if (config.blackBox) {
                        BlackBoxRestModule(config.bbExperiments)
                    } else if (config.isEnabledResourceStrategy()) {
                        /*
                            default for white-box testing using MIO
                         */
                        ResourceRestModule()
                    } else {
                        /*
                            old, pre-resource handling, version for white-box testing.
                            not deprecated, as algorithms different from MIO would still use this
                         */
                        RestModule()
                    }
                }

                EMConfig.ProblemType.GRAPHQL -> {
                    if (config.blackBox) {
                        GraphQLBlackBoxModule(config.bbExperiments)
                    } else {
                        GraphQLModule()
                    }
                }

                EMConfig.ProblemType.RPC -> {
                    if (config.blackBox) {
                        throw IllegalStateException("NOT SUPPORT black-box for RPC yet")
                    } else {
                        RPCModule()
                    }
                }

                EMConfig.ProblemType.WEBFRONTEND -> {
                    //TODO black-box mode
                    WebModule()
                }

                //this should never happen, unless we add new type and forget to add it here
                else -> throw IllegalStateException("Unrecognized problem type: ${config.problemType}")
            }

            val injector = try {
                LifecycleInjector.builder()
                    .withModules(base, problemModule)
                    .build()
                    .createInjector()

            } catch (e: Error) {
                /*
                    Workaround to Governator bug:
                    https://github.com/Netflix/governator/issues/371
                 */
                if (e.cause != null &&
                    InvocationTargetException::class.java.isAssignableFrom(e.cause!!.javaClass)
                ) {
                    throw e.cause!!
                }

                throw e
            }

            val cfg = injector.getInstance(EMConfig::class.java)
            cfg.problemType = config.problemType

            return injector
        }


        //Unfortunately Guice does not like this solution... :( so, we end up with copy&paste
//        private  fun <T : Individual> getAlgorithmKey(config: EMConfig) : Key<out SearchAlgorithm<T>>{
//
//            return  when {
//                config.blackBox || config.algorithm == EMConfig.Algorithm.RANDOM ->
//                    Key.get(object : TypeLiteral<RandomAlgorithm<T>>() {})
//
//                config.algorithm == EMConfig.Algorithm.MIO ->
//                    Key.get(object : TypeLiteral<MioAlgorithm<T>>() {})
//
//                config.algorithm == EMConfig.Algorithm.WTS ->
//                    Key.get(object : TypeLiteral<WtsAlgorithm<T>>() {})
//
//                config.algorithm == EMConfig.Algorithm.MOSA ->
//                    Key.get(object : TypeLiteral<MosaAlgorithm<T>>() {})
//
//                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
//            }
//        }

        private fun getAlgorithmKeyGraphQL(config: EMConfig): Key<out SearchAlgorithm<GraphQLIndividual>> {

            return when (config.algorithm) {
                EMConfig.Algorithm.SMARTS ->
                    Key.get(object : TypeLiteral<SmartsAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.RW ->
                    Key.get(object : TypeLiteral<RandomWalkAlgorithm<GraphQLIndividual>>() {})
                EMConfig.Algorithm.StandardGA ->
                    Key.get(object : TypeLiteral<StandardGeneticAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.LIPS ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.LIPSAlgorithm<GraphQLIndividual>>() {})
                EMConfig.Algorithm.MuPlusLambdaEA ->
                    Key.get(object : TypeLiteral<MuPlusLambdaEvolutionaryAlgorithm<GraphQLIndividual>>() {})
                    
                EMConfig.Algorithm.MuLambdaEA ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.MuLambdaEvolutionaryAlgorithm<GraphQLIndividual>>(){})
                EMConfig.Algorithm.BreederGA ->
                    Key.get(object : TypeLiteral<BreederGeneticAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.CellularGA ->
                    Key.get(object : TypeLiteral<CellularGeneticAlgorithm<GraphQLIndividual>>() {})

                EMConfig.Algorithm.OnePlusLambdaLambdaGA ->
                    Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<GraphQLIndividual>>() {})


                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyRPC(config: EMConfig): Key<out SearchAlgorithm<RPCIndividual>> {

            return when (config.algorithm) {
                EMConfig.Algorithm.SMARTS ->
                    Key.get(object : TypeLiteral<SmartsAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.RW ->
                    Key.get(object : TypeLiteral<RandomWalkAlgorithm<RPCIndividual>>() {})
                EMConfig.Algorithm.LIPS ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.LIPSAlgorithm<RPCIndividual>>() {})
        
                EMConfig.Algorithm.MuPlusLambdaEA ->
                    Key.get(object : TypeLiteral<MuPlusLambdaEvolutionaryAlgorithm<RPCIndividual>>() {})
                EMConfig.Algorithm.MuLambdaEA ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.MuLambdaEvolutionaryAlgorithm<RPCIndividual>>(){})

                EMConfig.Algorithm.BreederGA ->
                    Key.get(object : TypeLiteral<BreederGeneticAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.CellularGA ->
                    Key.get(object : TypeLiteral<CellularGeneticAlgorithm<RPCIndividual>>() {})

                EMConfig.Algorithm.OnePlusLambdaLambdaGA ->
                    Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<RPCIndividual>>() {})
                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyWeb(config: EMConfig): Key<out SearchAlgorithm<WebIndividual>> {

            return when (config.algorithm) {
                EMConfig.Algorithm.SMARTS ->
                    Key.get(object : TypeLiteral<SmartsAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.RW ->
                    Key.get(object : TypeLiteral<RandomWalkAlgorithm<WebIndividual>>() {})
                EMConfig.Algorithm.LIPS ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.LIPSAlgorithm<WebIndividual>>() {})
                    
                EMConfig.Algorithm.MuPlusLambdaEA ->
                    Key.get(object : TypeLiteral<MuPlusLambdaEvolutionaryAlgorithm<WebIndividual>>() {})
                EMConfig.Algorithm.MuLambdaEA ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.MuLambdaEvolutionaryAlgorithm<WebIndividual>>(){})

                EMConfig.Algorithm.BreederGA ->
                    Key.get(object : TypeLiteral<BreederGeneticAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.CellularGA ->
                    Key.get(object : TypeLiteral<CellularGeneticAlgorithm<WebIndividual>>() {})

                EMConfig.Algorithm.OnePlusLambdaLambdaGA ->
                    Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<WebIndividual>>() {})
                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyRest(config: EMConfig): Key<out SearchAlgorithm<RestIndividual>> {

            return when (config.algorithm) {
                EMConfig.Algorithm.SMARTS ->
                    Key.get(object : TypeLiteral<SmartsAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.StandardGA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.MonotonicGA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.SteadyStateGA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.RW ->
                    Key.get(object : TypeLiteral<RandomWalkAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.LIPS ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.LIPSAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.MuPlusLambdaEA ->
                    Key.get(object : TypeLiteral<MuPlusLambdaEvolutionaryAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.MuLambdaEA ->
                    Key.get(object : TypeLiteral<org.evomaster.core.search.algorithms.MuLambdaEvolutionaryAlgorithm<RestIndividual>>(){})

                EMConfig.Algorithm.BreederGA ->
                    Key.get(object : TypeLiteral<BreederGeneticAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.CellularGA ->
                    Key.get(object : TypeLiteral<CellularGeneticAlgorithm<RestIndividual>>() {})

                EMConfig.Algorithm.OnePlusLambdaLambdaGA ->
                    Key.get(object : TypeLiteral<OnePlusLambdaLambdaGeneticAlgorithm<RestIndividual>>() {})

                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        fun run(injector: Injector, controllerInfo: ControllerInfoDto?): Solution<*> {

            val config = injector.getInstance(EMConfig::class.java)
            val epc = injector.getInstance(ExecutionPhaseController::class.java)
            epc.startSearch()

            if (!config.blackBox || config.bbExperiments) {
                val rc = injector.getInstance(RemoteController::class.java)
                rc.startANewSearch()
            }

            val key = when (config.problemType) {
                EMConfig.ProblemType.REST -> getAlgorithmKeyRest(config)
                EMConfig.ProblemType.GRAPHQL -> getAlgorithmKeyGraphQL(config)
                EMConfig.ProblemType.RPC -> getAlgorithmKeyRPC(config)
                EMConfig.ProblemType.WEBFRONTEND -> getAlgorithmKeyWeb(config)
                else -> throw IllegalStateException("Unrecognized problem type ${config.problemType}")
            }

            val imp = injector.getInstance(key)

            LoggingUtil.getInfoLogger().info("Starting to generate test cases")

            return imp.search { solution: Solution<*>,
                                snapshotTimestamp: String ->
                writeTestsAsSnapshots(injector, solution, controllerInfo, snapshotTimestamp)
            }.also {
                if (config.isEnabledHarvestingActualResponse()) {
                    val hp = injector.getInstance(HarvestActualHttpWsResponseHandler::class.java)
                    hp.shutdown()
                }
            }
        }


        /**
         * Log a warning if any experimental setting is used
         */
        private fun checkExperimentalSettings(injector: Injector) {

            val config = injector.getInstance(EMConfig::class.java)

            val experimental = config.experimentalFeatures()

            if (experimental.isEmpty()) {
                return
            }

            val options = "[" + experimental.joinToString(", ") + "]"

            logWarn(
                "Using experimental settings." +
                        " Those might not work as expected, or simply straight out crash." +
                        " Furthermore, they might simply be incomplete features still under development." +
                        " Used experimental settings: $options"
            )
        }

        fun checkState(injector: Injector): ControllerInfoDto? {

            val config = injector.getInstance(EMConfig::class.java)

            if (config.blackBox && !config.bbExperiments) {
                return null
            }

            val rc = injector.getInstance(RemoteController::class.java)

            val dto = rc.getControllerInfo() ?: throw IllegalStateException(
                "Cannot retrieve Remote Controller info from ${rc.address()}"
            )

            if (dto.isInstrumentationOn != true) {
                LoggingUtil.getInfoLogger().warn("The system under test is running without instrumentation")
            }

            if (dto.fullName.isNullOrBlank()) {
                throw IllegalStateException("Failed to retrieve the name of the EvoMaster Driver")
            }

            //TODO check if the type of controller does match the output format

            return dto
        }

        fun writeExecuteInfo(injector: Injector) {

            val config = injector.getInstance(EMConfig::class.java)

            if (config.outputExecutedSQL != EMConfig.OutputExecutedSQL.ALL_AT_END && !config.recordExecutedMainActionInfo) {
                return
            }
            val reporter = injector.getInstance(ExecutionInfoReporter::class.java)
            reporter.saveAll()
        }

        private fun writeTestsAsSnapshots(
            injector: Injector,
            solution: Solution<*>,
            controllerInfoDto: ControllerInfoDto?,
            snapshotTimestamp: String
        ) {

            val config = injector.getInstance(EMConfig::class.java)
            if (!config.createTests) {
                return
            }
            LoggingUtil.getInfoLogger().info("Saving snapshot of tests so far")

            writeTests(injector,solution,controllerInfoDto,snapshotTimestamp)
        }

        fun writeTests(
            injector: Injector,
            solution: Solution<*>,
            controllerInfoDto: ControllerInfoDto?,
            snapshotTimestamp: String = ""
        ): List<TestSuiteCode> {

            val config = injector.getInstance(EMConfig::class.java)
            if (!config.createTests) {
                return listOf()
            }

            val n = solution.individuals.size
            val tests = if (n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save $tests to ${config.outputFolder}")

            val writer = injector.getInstance(TestSuiteWriter::class.java)

            if (config.dtoSupportedForPayload()) {
                writer.writeDtos(solution)
            }

            val splitResult = TestSuiteSplitter.split(solution, config)

            val suites = splitResult.splitOutcome.map {
                writer.convertToCompilableTestCode(
                    it,
                    it.getFileName(),
                    snapshotTimestamp,
                    controllerInfoDto?.fullName,
                    controllerInfoDto?.executableFullPath
                )
            }
            suites.forEach { suite -> writer.writeTests(suite) }

            if (config.problemType == EMConfig.ProblemType.RPC) {
                //TODO what is this? need to clarify
                // Man: only enable for RPC as it lacks of unit tests
                writer.writeTestsDuringSeeding(
                    solution,
                    controllerInfoDto?.fullName,
                    controllerInfoDto?.executableFullPath
                )
            }

            return suites
        }


        private fun writeWFCReport(injector: Injector, solution: Solution<*>, suites: List<TestSuiteCode>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.writeWFCReport || suites.isEmpty()) {
                return
            }

            val wfcr = injector.getInstance(WFCReportWriter::class.java)

            wfcr.writeReport(solution,suites)

            if(!config.writeWFCReportExcludeWebApp){
                wfcr.writeWebApp()
            }
        }

        private fun writeStatistics(injector: Injector, solution: Solution<*>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.writeStatistics) {
                return
            }

            val statistics = injector.getInstance(Statistics::class.java)

            statistics.writeStatistics(solution)

            if (config.snapshotInterval > 0) {
                statistics.writeSnapshot()
            }
        }

        private fun writeOverallProcessData(injector: Injector) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.enableProcessMonitor) {
                return
            }

            val process = injector.getInstance(SearchProcessMonitor::class.java)
            process.saveOverall()
        }

        /**
         * save possible dependencies among resources (e.g., a resource might be related to other resource) derived during search
         * info is designed for experiment analysis
         */
        private fun writeDependencies(injector: Injector) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.exportDependencies) {
                return
            }

            val dm = injector.getInstance(ResourceDepManageService::class.java)
            dm.exportDependencies()
        }

        /**
         * save derived impacts of genes of actions.
         * info is designed for experiment analysis
         */
        private fun writeImpacts(injector: Injector, solution: Solution<*>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.exportImpacts) {
                return
            }

            val am = injector.getInstance(ArchiveImpactSelector::class.java)
            am.exportImpacts(solution)
        }

        /**
         * save covered target info
         * info is designed for experiment analysis
         */
        private fun writeCoveredTargets(injector: Injector, solution: Solution<*>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.exportCoveredTarget) {
                return
            }

            val statistics = injector.getInstance(Statistics::class.java)
            statistics.writeCoveredTargets(solution, config.coveredTargetSortedBy)
        }

        private fun writeExecSummary(
            injector: Injector,
            controllerInfoDto: ControllerInfoDto?,
            splitResult: SplitResult,
            snapshotTimestamp: String = ""
        ) {

            val executiveSummary = splitResult.executiveSummary
                ?: return

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val writer = injector.getInstance(TestSuiteWriter::class.java)
            assert(controllerInfoDto == null || controllerInfoDto.fullName != null)
            writer.writeTests(
                executiveSummary,
                controllerInfoDto?.fullName,
                controllerInfoDto?.executableFullPath,
                snapshotTimestamp
            )
        }

        /**
         * To reset external service handler to clear the existing
         * WireMock instances to free up the IP addresses.
         */
        private fun resetExternalServiceHandler(injector: Injector) {
            val externalServiceHandler = injector.getInstance(HttpWsExternalServiceHandler::class.java)
            externalServiceHandler.reset()
        }

        private fun stopHTTPCallbackVerifier(injector: Injector) {
            val httpCallbackVerifier = injector.getInstance(HttpCallbackVerifier::class.java)
            httpCallbackVerifier.stop()
        }
    }
}


