package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.ControllerInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inGreen
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.AnsiColor.Companion.inYellow
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.service.GraphQLBlackBoxModule
import org.evomaster.core.problem.graphql.service.GraphQLModule
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.service.RPCModule
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.problem.webfrontend.service.WebModule
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.genemutation.ArchiveImpactSelector
import java.lang.reflect.InvocationTargetException
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

                printLogo()
                printVersion()

                if(!JdkIssue.checkAddOpens()){
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
                    logError("Invalid parameter settings: " + e.message +
                            "\nUse --help to see the available options")
                    return
                }

                if (parser.parse(*args).has("help")) {
                    parser.printHelpOn(System.out)
                    return
                }

                initAndRun(args)

                LoggingUtil.getInfoLogger().apply {
                    info("EvoMaster process has completed successfully")
                    info("Use ${inGreen("--help")} and visit ${inBlue("http://www.evomaster.org")} to" +
                            " learn more about available options")
                }

            } catch (e: Exception) {

                var cause: Throwable = e
                while (cause.cause != null) {
                    cause = cause.cause!!
                }

                when (cause) {
                    is NoRemoteConnectionException ->
                        logError("ERROR: ${cause.message}" +
                                "\n  Make sure the EvoMaster Driver for the system under test is running correctly.")

                    is SutProblemException ->
                        logError("ERROR related to the system under test: ${cause.message}" +
                                "\n  For white-box testing, look at the logs of the EvoMaster Driver to help debugging this problem.")

                    is ConfigProblemException ->
                        logError("Invalid parameter settings: ${cause.message}" +
                                "\nUse --help to see the available options")

                    else ->
                        LoggingUtil.getInfoLogger().error(inRed("[ERROR] ") +
                                inYellow("EvoMaster process terminated abruptly." +
                                        " This is likely a bug in EvoMaster." +
                                        " Please copy&paste the following stacktrace, and create a new issue on" +
                                        " " + inBlue("https://github.com/EMResearch/EvoMaster/issues")), e)
                }

                /*
                    Need to signal error status.
                    But this code can become problematic if reached by any test.
                    Also in case of exceptions, must shutdown explicitely, otherwise running threads in
                    the background might keep the JVM alive.
                    See for example HarvestActualHttpWsResponseHandler
                 */
                exitProcess(1);
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

            val version = this.javaClass.`package`?.implementationVersion ?: "unknown"

            LoggingUtil.getInfoLogger().info("EvoMaster version: $version")
        }

        @JvmStatic
        fun initAndRun(args: Array<String>): Solution<*> {

            val injector = init(args)

            checkExperimentalSettings(injector)

            val controllerInfo = checkState(injector)

            val config = injector.getInstance(EMConfig::class.java)
            val idMapper = injector.getInstance(IdMapper::class.java)

            var solution = run(injector, controllerInfo)

            //save data regarding the search phase
            writeOverallProcessData(injector)
            writeDependencies(injector)
            writeImpacts(injector, solution)
            writeExecuteInfo(injector)


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
                    if(!config.blackBox){
                        info("Computation overhead of resetting the SUT (ms): ${stc.averageResetSUTTimeMs}")
                        //This one might be confusing, as based only on minimization phase...
                        //info("Data transfer overhead of test results, per test, all targets (bytes): ${stc.averageByteOverheadTestResultsAll}")
                        debug("Data transfer overhead of fetching test results, per test, subset of targets (bytes): ${stc.averageByteOverheadTestResultsSubset}")
                        info("Computation overhead of fetching test results, per test, subset of targets (ms): ${stc.averageOverheadMsTestResultsSubset}")
                    }
                }
            }


            if(config.security){
                //apply security testing phase
                LoggingUtil.getInfoLogger().info("Starting to apply security testing")

                //TODO might need to reset stc, and print some updated info again

                when(config.problemType){
                    EMConfig.ProblemType.REST -> {
                        val securityRest = injector.getInstance(SecurityRest::class.java)
                        solution = securityRest.applySecurityPhase()
                    }
                    else ->{
                        LoggingUtil.getInfoLogger().warn("Security phase currently not handled for problem type: ${config.problemType}")
                    }
                }
            }

            writeCoveredTargets(injector, solution)
            writeTests(injector, solution, controllerInfo)
            writeStatistics(injector, solution) //FIXME if other phases after search, might get skewed data on 100% snapshots...

            val statistics = injector.getInstance(Statistics::class.java)
            val data = statistics.getData(solution)
            val faults = solution.overall.potentialFoundFaults(idMapper)
            val sampler : Sampler<*> = injector.getInstance(Key.get(object : TypeLiteral<Sampler<*>>(){}))

            LoggingUtil.getInfoLogger().apply {

                val timeouts = data.find { p -> p.header == Statistics.TEST_TIMEOUTS }!!.element.toInt()
                if (timeouts > 0) {
                    info("TCP timeouts: $timeouts")
                }

                info("Potential faults: ${faults.size}")

                if (!config.blackBox || config.bbExperiments) {
                    val rc = injector.getInstance(RemoteController::class.java)
                    val unitsInfo = rc.getSutInfo()?.unitsInfoDto
                    val bootTimeInfo = rc.getSutInfo()?.bootTimeInfoDto

                    val targetsInfo = solution.overall.unionWithBootTimeCoveredTargets(null, idMapper, bootTimeInfo)
                    val linesInfo = solution.overall.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, bootTimeInfo)

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

                        info("Covered targets (lines, branches, faults, etc.): ${targetsInfo.total}")

                        if(totalLines == 0 || units == 0){
                            logError("Detected $totalLines lines to cover, for a total of $units units/classes." +
                                    " Are you sure you did setup getPackagePrefixesToCover() correctly?")
                        } else {
                            info("Bytecode line coverage: $percentage% (${linesInfo.total} out of $totalLines in $units units/classes)")
                        }
                    } else {
                        warn("Failed to retrieve SUT info")
                    }
                }

                val n = data.find { it.header == Statistics.DISTINCT_ACTIONS }!!.element.toInt()

                when(config.problemType){
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
                        val p = String.format("%.0f", (k.toDouble()/t) * 100 )
                        info("Successfully executed (HTTP code 2xx) $k endpoints out of $t ($p%)")
                    }
                    EMConfig.ProblemType.GRAPHQL ->{
                        val k = data.find { it.header == Statistics.GQL_NO_ERRORS }!!.element.toInt()
                        val p = String.format("%.0f", (k.toDouble()/n) * 100 )
                        info("Successfully executed (no 'errors') $k endpoints out of $n ($p%)")
                    }
                    else -> {}
                    //TODO others, eg RPC
                }

                if (config.stoppingCriterion == EMConfig.StoppingCriterion.TIME &&
                        config.maxTime == config.defaultMaxTime) {
                    warn(inGreen("You are using the default time budget '${config.defaultMaxTime}'." +
                            " This is only for demo purposes. " +
                            " You should increase such test budget." +
                            " To obtain better results, use the '--maxTime' option" +
                            " to run the search for longer, like for example something between '1h' and '24h' hours."))
                }
            }

            resetExternalServiceHandler(injector)

            solution.statistics = data.toMutableList()
            return solution
        }

        @JvmStatic
        fun init(args: Array<String>): Injector {

            LoggingUtil.getInfoLogger().info("Initializing...")

            val base = BaseModule(args)
            val config = base.getEMConfig()

            if(config.problemType == EMConfig.ProblemType.DEFAULT){
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
                if(! started){
                    throw SutProblemException("Failed to start the SUT")
                }

                val info = rc.getSutInfo()
                        ?: throw SutProblemException("No 'problemType' was defined, but failed to retried the needed" +
                                " info from the EM Driver.")

                if(info.restProblem != null){
                    config.problemType = EMConfig.ProblemType.REST
                } else if (info.graphQLProblem != null){
                    config.problemType = EMConfig.ProblemType.GRAPHQL
                } else if (info.rpcProblem != null){
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
                        ResourceRestModule()
                    } else {
                        RestModule()
                    }
                }

                EMConfig.ProblemType.GRAPHQL -> {
                    if(config.blackBox){
                        GraphQLBlackBoxModule(config.bbExperiments)
                    } else {
                        GraphQLModule()
                    }
                }

                EMConfig.ProblemType.RPC ->{
                    if (config.blackBox){
                        throw IllegalStateException("NOT SUPPORT black-box for RPC yet")
                    }else{
                        RPCModule()
                    }
                }

                EMConfig.ProblemType.WEBFRONTEND ->{
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
                        InvocationTargetException::class.java.isAssignableFrom(e.cause!!.javaClass)) {
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

            return when {
                config.blackBox || config.algorithm == EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<GraphQLIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<GraphQLIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<GraphQLIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<GraphQLIndividual>>() {})

                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyRPC(config: EMConfig): Key<out SearchAlgorithm<RPCIndividual>> {

            return when {
                config.blackBox || config.algorithm == EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<RPCIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<RPCIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<RPCIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RPCIndividual>>() {})

                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyWeb(config: EMConfig): Key<out SearchAlgorithm<WebIndividual>> {

            return when {
                config.blackBox || config.algorithm == EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<WebIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<WebIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<WebIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<WebIndividual>>() {})

                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        private fun getAlgorithmKeyRest(config: EMConfig): Key<out SearchAlgorithm<RestIndividual>> {

            return when {
                config.blackBox || config.algorithm == EMConfig.Algorithm.RANDOM ->
                    Key.get(object : TypeLiteral<RandomAlgorithm<RestIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MIO ->
                    Key.get(object : TypeLiteral<MioAlgorithm<RestIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.WTS ->
                    Key.get(object : TypeLiteral<WtsAlgorithm<RestIndividual>>() {})

                config.algorithm == EMConfig.Algorithm.MOSA ->
                    Key.get(object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})

                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }
        }

        fun run(injector: Injector, controllerInfo: ControllerInfoDto?): Solution<*> {

            val config = injector.getInstance(EMConfig::class.java)

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
                if (config.isEnabledHarvestingActualResponse()){
                    val hp = injector.getInstance(HarvestActualHttpWsResponseHandler::class.java)
                    hp.shutdown()
                }
            }
        }

        private fun checkExperimentalSettings(injector: Injector) {

            val config = injector.getInstance(EMConfig::class.java)

            val experimental = config.experimentalFeatures()

            if (experimental.isEmpty()) {
                return
            }

            val options = "[" + experimental.joinToString(", ") + "]"

            logWarn("Using experimental settings." +
                    " Those might not work as expected, or simply straight out crash." +
                    " Furthermore, they might simply be incomplete features still under development." +
                    " Used experimental settings: $options")
        }

         fun checkState(injector: Injector): ControllerInfoDto? {

            val config = injector.getInstance(EMConfig::class.java)

            if (config.blackBox && !config.bbExperiments) {
                return null
            }

            val rc = injector.getInstance(RemoteController::class.java)

            val dto = rc.getControllerInfo() ?: throw IllegalStateException(
                    "Cannot retrieve Remote Controller info from ${rc.address()}")

            if (dto.isInstrumentationOn != true) {
                LoggingUtil.getInfoLogger().warn("The system under test is running without instrumentation")
            }

            if (dto.fullName.isNullOrBlank()) {
                throw IllegalStateException("Failed to retrieve the name of the EvoMaster Driver")
            }

            //TODO check if the type of controller does match the output format

            return dto
        }

        fun writeExecuteInfo(injector: Injector){

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
            snapshotTimestamp: String = ""
        ) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val n = solution.individuals.size
            val tests = if (n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save snapshot $tests to ${config.outputFolder}")

            val writer = injector.getInstance(TestSuiteWriter::class.java)

            //TODO: enable splitting for csharp. Currently not enabled due to an error while running generated tests in multiple classes (error in starting the SUT)
            if (config.problemType == EMConfig.ProblemType.REST && config.outputFormat!=OutputFormat.CSHARP_XUNIT) {

                val splitResult = TestSuiteSplitter.split(solution, config, writer.getPartialOracles())

                solution.clusteringTime = splitResult.clusteringTime.toInt()
                splitResult.splitOutcome.filter { !it.individuals.isNullOrEmpty() }
                    .forEach { writer.writeTests(it, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath, snapshotTimestamp) }

                if (config.executiveSummary) {
                    writeExecSummary(injector, controllerInfoDto, splitResult, snapshotTimestamp)
                    //writeExecutiveSummary(injector, solution, controllerInfoDto, partialOracles)
                }
            } else {
                /*
                    TODO refactor all the PartialOracle stuff that is meant for only REST
                 */

                writer.writeTests(solution, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath, snapshotTimestamp)
            }
        }

        fun writeTests(injector: Injector, solution: Solution<*>, controllerInfoDto: ControllerInfoDto?,
                       snapshot: String = "") {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val n = solution.individuals.size
            val tests = if (n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save $tests to ${config.outputFolder}")

            val writer = injector.getInstance(TestSuiteWriter::class.java)
            //TODO: enable splitting for csharp. Currently not enabled due to an error while running generated tests in multiple classes (error in starting the SUT)
            if (config.problemType == EMConfig.ProblemType.REST && config.outputFormat!=OutputFormat.CSHARP_XUNIT) {

                val splitResult = TestSuiteSplitter.split(solution, config, writer.getPartialOracles())

                solution.clusteringTime = splitResult.clusteringTime.toInt()
                splitResult.splitOutcome
                    .filter { !it.individuals.isNullOrEmpty() }
                    .flatMap {
                        TestSuiteSplitter.splitSolutionByLimitSize(
                            it as Solution<ApiWsIndividual>,
                            config.maxTestsPerTestSuite
                        )
                    }
                    .forEach { writer.writeTests(it, controllerInfoDto?.fullName,controllerInfoDto?.executableFullPath, snapshot) }

                if (config.executiveSummary) {

                    // Onur - if there are fault cases, executive summary makes sense
                    if ( splitResult.splitOutcome.any{ it.individuals.isNotEmpty()
                                && it.termination != Termination.SUCCESSES}) {
                        writeExecSummary(injector, controllerInfoDto, splitResult)
                    }

                    //writeExecSummary(injector, controllerInfoDto, splitResult)
                    //writeExecutiveSummary(injector, solution, controllerInfoDto, partialOracles)
                }
            } else if (config.problemType == EMConfig.ProblemType.RPC){

                // Man: only enable for RPC as it lacks of unit tests
                writer.writeTestsDuringSeeding(solution, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath)

                when(config.testSuiteSplitType){
                    EMConfig.TestSuiteSplitType.NONE -> writer.writeTests(solution, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath)
                    EMConfig.TestSuiteSplitType.CODE -> throw IllegalStateException("RPC problem does not support splitting tests by code")
                    /*
                        for RPC, just simple split based on whether there exist any exception in a test
                        TODD need to check with Andrea whether we use cluster or other type
                     */
                    EMConfig.TestSuiteSplitType.CLUSTER -> {
                        val splitResult = TestSuiteSplitter.splitRPCByException(solution as Solution<RPCIndividual>)
                        splitResult.splitOutcome
                            .filter { !it.individuals.isNullOrEmpty() }
                            .flatMap {
                                TestSuiteSplitter.splitSolutionByLimitSize(
                                    it as Solution<ApiWsIndividual>,
                                    config.maxTestsPerTestSuite
                                )
                            }
                            .forEach { writer.writeTests(it, controllerInfoDto?.fullName,controllerInfoDto?.executableFullPath, snapshot) }

                        // disable executiveSummary
//                        if (config.executiveSummary) {
//                            writeExecSummary(injector, controllerInfoDto, splitResult)
//                        }
                    }
                }

            }else if (config.problemType == EMConfig.ProblemType.GRAPHQL) {
                when(config.testSuiteSplitType){
                    EMConfig.TestSuiteSplitType.NONE -> writer.writeTests(solution, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath,)
                    //EMConfig.TestSuiteSplitType.CLUSTER -> throw IllegalStateException("GraphQL problem does not support splitting tests by cluster at this time")
                    //EMConfig.TestSuiteSplitType.CODE ->
                    else -> {
                        //throw IllegalStateException("GraphQL problem does not support splitting tests by code at this time")
                        val splitResult = TestSuiteSplitter.split(solution, config)
                        splitResult.splitOutcome
                            .filter{ !it.individuals.isNullOrEmpty() }
                            .flatMap {
                                TestSuiteSplitter.splitSolutionByLimitSize(
                                    it as Solution<ApiWsIndividual>,
                                    config.maxTestsPerTestSuite
                                )
                            }
                            .forEach { writer.writeTests(it, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath, snapshot ) }
                    }
                    /*
                      GraphQL could be split by code (where code is available and trustworthy)
                     */
                }
            } else
            {
                /*
                    TODO refactor all the PartialOracle stuff that is meant for only REST
                 */

                writer.writeTests(solution, controllerInfoDto?.fullName, controllerInfoDto?.executableFullPath,)
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

        private fun writeExecSummary(injector: Injector,
                                     controllerInfoDto: ControllerInfoDto?,
                                     splitResult: SplitResult,
                                     snapshotTimestamp: String = "") {
            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val writer = injector.getInstance(TestSuiteWriter::class.java)
            assert(controllerInfoDto == null || controllerInfoDto.fullName != null)
            writer.writeTests(splitResult.executiveSummary, controllerInfoDto?.fullName,controllerInfoDto?.executableFullPath, snapshotTimestamp)
        }

        /**
         * To reset external service handler to clear the existing
         * WireMock instances to free up the IP addresses.
         */
        private fun resetExternalServiceHandler(injector: Injector) {
            val externalServiceHandler = injector.getInstance(HttpWsExternalServiceHandler::class.java)
            externalServiceHandler.reset()
        }
    }
}


