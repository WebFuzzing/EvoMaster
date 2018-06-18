package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.clientJava.controllerApi.dto.ControllerInfoDto
import org.evomaster.core.output.TestSuiteWriter
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestModule
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
                    LoggingUtil.getInfoLogger().error(
                            "Invalid parameter settings: " + e.message +
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
                    info("Use \u001B[32m--help\u001B[0m and visit \u001B[34mwww.evomaster.org\u001B[0m to" +
                            " learn more about available options")
                }

            } catch (e: Exception) {

                var cause: Throwable = e
                while (cause.cause != null) {
                    cause = cause.cause!!
                }

                val log = LoggingUtil.getInfoLogger()

                when (cause) {
                    is NoRemoteConnectionException ->
                        log.error("ERROR: ${cause.message}" +
                                "\n  Make sure the EvoMaster Driver for the system under test is running correctly.")

                    is SutProblemException ->
                        log.error("ERROR in the Remote EvoMaster Driver: ${cause.message}" +
                                "\n  Look at the logs of the EvoMaster Driver to help debugging this problem.")

                    else ->
                        log.error("ERROR: EvoMaster process terminated abruptly. Message: " + e.message, e)
                }
            }
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

            LoggingUtil.getInfoLogger().info("\u001B[34m$logo\u001B[0m")
        }

        private fun printVersion() {

            val version = this.javaClass.`package`?.implementationVersion ?: "unknown"

            LoggingUtil.getInfoLogger().info("EvoMaster version: $version")
        }

        @JvmStatic
        fun initAndRun(args: Array<String>): Solution<*> {

            val injector = init(args)

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
                    info("\u001B[32mTo obtain better results, use the '--maxTimeInSeconds' option " +
                            "to run the search for longer\u001B[0m")
                }
            }

            return solution
        }

        @JvmStatic
        fun init(args: Array<String>): Injector {

            //TODO check problem type

            try {
                return LifecycleInjector.builder()
                        .withModules(BaseModule(args), RestModule())
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



