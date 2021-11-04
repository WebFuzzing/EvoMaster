package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.ControllerInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inGreen
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.AnsiColor.Companion.inYellow
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.output.clustering.SplitResult
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.service.GraphQLBlackBoxModule
import org.evomaster.core.problem.graphql.service.GraphQLModule
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.web.service.WebModule
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.SearchAlgorithm
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneSelector
import java.lang.reflect.InvocationTargetException


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

                /*
                    Before running anything, check if the input
                    configurations are valid
                 */
                val parser = try {
                    EMConfig.validateOptions(args)
                } catch (e: Exception) {
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
                        logError("ERROR in the Remote EvoMaster Driver: ${cause.message}" +
                                "\n  Look at the logs of the EvoMaster Driver to help debugging this problem.")

                    else ->
                        LoggingUtil.getInfoLogger().error(inRed("[ERROR] ") +
                                inYellow("EvoMaster process terminated abruptly." +
                                        " This is likely a bug in EvoMaster." +
                                        " Please copy&paste the following stacktrace, and create a new issue on" +
                                        " " + inBlue("https://github.com/EMResearch/EvoMaster/issues")), e)
                }
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

            val solution = run(injector)
            val faults = solution.overall.potentialFoundFaults(idMapper)

            writeOverallProcessData(injector)

            writeDependencies(injector)

            writeImpacts(injector, solution)

            //writeStatistics(injector, solution)

            writeCoveredTargets(injector, solution)

            writeTests(injector, solution, controllerInfo)

            writeStatistics(injector, solution)

            val stc = injector.getInstance(SearchTimeController::class.java)
            val statistics = injector.getInstance(Statistics::class.java)
            val data = statistics.getData(solution)

            LoggingUtil.getInfoLogger().apply {
                info("Evaluated tests: ${stc.evaluatedIndividuals}")
                info("Evaluated actions: ${stc.evaluatedActions}")
                info("Needed budget: ${stc.neededBudget()}")

                if (!config.avoidNonDeterministicLogs) {
                    info("Passed time (seconds): ${stc.getElapsedSeconds()}")
                    info("Execution time per test (ms): ${stc.averageTestTimeMs}")
                    info("Computation overhead between tests (ms): ${stc.averageOverheadMsBetweenTests}")
                    val timeouts = data.find { p -> p.header == Statistics.TEST_TIMEOUTS }!!.element.toInt()
                    if (timeouts > 0) {
                        info("TCP timeouts: $timeouts")
                    }
                }

                if (!config.blackBox || config.bbExperiments) {
                    val rc = injector.getInstance(RemoteController::class.java)
                    val unitsInfo = rc.getSutInfo()?.unitsInfoDto

                    if (unitsInfo != null) {
                        val units = unitsInfo.unitNames.size
                        val totalLines = unitsInfo.numberOfLines
                        val coveredLines = solution.overall.coveredTargets(ObjectiveNaming.LINE, idMapper)
                        val percentage = String.format("%.0f", (coveredLines / totalLines.toDouble()) * 100)

                        info("Covered targets (lines, branches, faults, etc.): ${solution.overall.coveredTargets()}")
                        info("Potential faults: ${faults.size}")
                        info("Bytecode line coverage: $percentage% ($coveredLines out of $totalLines in $units units/classes)")
                    } else {
                        warn("Failed to retrieve SUT info")
                    }
                }

                if (config.stoppingCriterion == EMConfig.StoppingCriterion.TIME &&
                        config.maxTime == config.defaultMaxTime) {
                    info(inGreen("To obtain better results, use the '--maxTime' option" +
                            " to run the search for longer"))
                }
            }
            solution.statistics = data.toMutableList()
            return solution
        }

        @JvmStatic
        fun init(args: Array<String>): Injector {

            val base = BaseModule(args)
            val config = base.getEMConfig()

            if(config.problemType == EMConfig.ProblemType.DEFAULT){
                /*
                    Note that, in case ob BB-testing, this would had been already modified
                 */
                assert(!config.blackBox || config.bbExperiments)

                val rc = RemoteController(base.getEMConfig())

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
                    } else if (config.resourceSampleStrategy == EMConfig.ResourceSamplingStrategy.NONE) {
                        RestModule()
                    } else {
                        ResourceRestModule()
                    }
                }

                EMConfig.ProblemType.GRAPHQL -> {
                    if(config.blackBox){
                        GraphQLBlackBoxModule(config.bbExperiments)
                    } else {
                        GraphQLModule()
                    }
                }

                EMConfig.ProblemType.WEB -> WebModule()

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

        fun run(injector: Injector): Solution<*> {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.blackBox || config.bbExperiments) {
                val rc = injector.getInstance(RemoteController::class.java)
                rc.startANewSearch()
            }

            val key = when (config.problemType) {
                EMConfig.ProblemType.REST -> getAlgorithmKeyRest(config)
                EMConfig.ProblemType.GRAPHQL -> getAlgorithmKeyGraphQL(config)
                else -> throw IllegalStateException("Unrecognized problem type ${config.problemType}")
            }

            val imp = injector.getInstance(key)

            LoggingUtil.getInfoLogger().info("Starting to generate test cases")

            return imp.search()
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
                    "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")

            if (dto.isInstrumentationOn != true) {
                LoggingUtil.getInfoLogger().warn("The system under test is running without instrumentation")
            }

            if (dto.fullName.isNullOrBlank()) {
                throw IllegalStateException("Failed to retrieve the name of the EvoMaster Driver")
            }

            //TODO check if the type of controller does match the output format

            return dto
        }


         fun writeTests(injector: Injector, solution: Solution<*>, controllerInfoDto: ControllerInfoDto?) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val n = solution.individuals.size
            val tests = if (n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save $tests to ${config.outputFolder}")

            val writer = injector.getInstance(TestSuiteWriter::class.java)

            if (config.problemType == EMConfig.ProblemType.REST) {

                val splitResult = TestSuiteSplitter.split(solution, config, writer.getPartialOracles())

                solution.clusteringTime = splitResult.clusteringTime.toInt()
                splitResult.splitOutcome.filter { !it.individuals.isNullOrEmpty() }
                        .forEach { writer.writeTests(it, controllerInfoDto?.fullName) }

                if (config.executiveSummary) {
                    writeExecSummary(injector, controllerInfoDto, splitResult)
                    //writeExecutiveSummary(injector, solution, controllerInfoDto, partialOracles)
                }
            } else {
                /*
                    TODO refactor all the PartialOracle stuff that is meant for only REST
                 */

                writer.writeTests(solution, controllerInfoDto?.fullName)
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

            val am = injector.getInstance(ArchiveGeneSelector::class.java)
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
                                     splitResult: SplitResult) {
            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val writer = injector.getInstance(TestSuiteWriter::class.java)
            assert(controllerInfoDto == null || controllerInfoDto.fullName != null)
            writer.writeTests(splitResult.executiveSummary, controllerInfoDto?.fullName)
        }
    }
}



