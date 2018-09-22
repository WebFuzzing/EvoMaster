package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.clientJava.controllerApi.dto.ControllerInfoDto
import org.evomaster.core.AnsiColor.Companion.inBlue
import org.evomaster.core.AnsiColor.Companion.inGreen
import org.evomaster.core.AnsiColor.Companion.inRed
import org.evomaster.core.AnsiColor.Companion.inYellow
import org.evomaster.core.output.TestSuiteWriter
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestModule
import org.evomaster.core.problem.web.service.WebModule
import org.evomaster.core.remote.NoRemoteConnectionException
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.Statistics
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
                    info("Use ${inGreen("--help")} and visit ${inBlue("www.evomaster.org")} to" +
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
                        logError("ERROR: EvoMaster process terminated abruptly. Message: " + e.message)
                }
            }
        }

        private fun logError(msg: String){
            LoggingUtil.getInfoLogger().error(inRed("[ERROR] ") + inYellow(msg))
        }

        private fun logWarn(msg: String){
            LoggingUtil.getInfoLogger().warn(inYellow("[WARNING] ") + inYellow(msg))
        }

        private fun printLogo() {

            val logo =  """
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

            val solution = run(injector)

            writeTests(injector, solution, controllerInfo)

            writeStatistics(injector, solution)

            val config = injector.getInstance(EMConfig::class.java)

            LoggingUtil.getInfoLogger().apply {
                val stc = injector.getInstance(SearchTimeController::class.java)
                info("Evaluated tests: ${stc.evaluatedIndividuals}")
                info("Evaluated actions: ${stc.evaluatedActions}")
                info("Last action improvement: ${stc.lastActionImprovement}")
                info("Passed time (seconds): ${stc.getElapsedSeconds()}")
                info("Covered targets: ${solution.overall.coveredTargets()}")

                if(config.stoppingCriterion == EMConfig.StoppingCriterion.TIME &&
                        config.maxTimeInSeconds == config.defaultMaxTimeInSeconds){
                    info(inGreen("To obtain better results, use the '--maxTimeInSeconds' option" +
                            " to run the search for longer"))
                }
            }

            return solution
        }

        @JvmStatic
        fun init(args: Array<String>): Injector {

            val base = BaseModule(args)

            val problemType = base.getEMConfig().problemType

            val problemModule = when (problemType) {
                EMConfig.ProblemType.REST -> RestModule()
                EMConfig.ProblemType.WEB -> WebModule()
                //this should never happen, unless we add new type and forget to add it here
                else -> throw IllegalStateException("Unrecognized problem type: $problemType")
            }

            try {
                return LifecycleInjector.builder()
                        .withModules(base, problemModule)
                        .build()
                        .createInjector()

            } catch (e: Error){
                /*
                    Workaround to Governator bug:
                    https://github.com/Netflix/governator/issues/371
                 */
                if(e.cause != null &&
                        InvocationTargetException::class.java.isAssignableFrom(e.cause!!.javaClass)){
                    throw e.cause!!
                }

                throw e
            }
        }


        fun run(injector: Injector): Solution<*> {

            //TODO check problem type
            val rc = injector.getInstance(RemoteController::class.java)
            rc.startANewSearch()

            val config = injector.getInstance(EMConfig::class.java)

            val key = when (config.algorithm) {
                EMConfig.Algorithm.MIO -> Key.get(
                        object : TypeLiteral<MioAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.RANDOM -> Key.get(
                        object : TypeLiteral<RandomAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.WTS -> Key.get(
                        object : TypeLiteral<WtsAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.MOSA -> Key.get(
                        object : TypeLiteral<MosaAlgorithm<RestIndividual>>() {})
                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }

            val imp = injector.getInstance(key)

            LoggingUtil.getInfoLogger().info("Starting to generate test cases")
            val solution = imp.search()

            return solution
        }

        private fun checkExperimentalSettings(injector: Injector){

            val config = injector.getInstance(EMConfig::class.java)

            val experimental = config.experimentalFeatures()

            if(experimental.isEmpty()){
                return
            }

            val options = "[" + experimental.joinToString(", ") + "]"

            logWarn("Using experimental settings." +
                    " Those might not work as expected, or simply straight out crash." +
                    " Furthermore, they might simply be incomplete features still under development." +
                    " Used experimental settings: $options")
        }

        private fun checkState(injector: Injector): ControllerInfoDto {

            val rc = injector.getInstance(RemoteController::class.java)

            val dto = rc.getControllerInfo() ?:
                    throw IllegalStateException(
                            "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")

            if (dto.isInstrumentationOn != true) {
                LoggingUtil.getInfoLogger().warn("The system under test is running without instrumentation")
            }

            //TODO check if the type of controller does match the output format

            return dto
        }


        private fun writeTests(injector: Injector, solution: Solution<*>, controllerInfoDto: ControllerInfoDto) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.createTests) {
                return
            }

            val n = solution.individuals.size
            val tests = if (n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save $tests to ${config.outputFolder}")

            TestSuiteWriter.writeTests(
                    solution,
                    controllerInfoDto.fullName,
                    config
            )
        }

        private fun writeStatistics(injector: Injector, solution: Solution<*>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.writeStatistics) {
                return
            }

            val statistics = injector.getInstance(Statistics::class.java)

            statistics.writeStatistics(solution)

            if(config.snapshotInterval > 0){
                statistics.writeSnapshot()
            }
        }
    }
}



