package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
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
import kotlin.reflect.full.isSuperclassOf


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
                var parser = try {
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

                LoggingUtil.getInfoLogger().info("EvoMaster process has completed successfully")

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

            LoggingUtil.getInfoLogger().info(
                    """
 _____          ___  ___          _
|  ___|         |  \/  |         | |
| |____   _____ | .  . | __ _ ___| |_ ___ _ __
|  __\ \ / / _ \| |\/| |/ _` / __| __/ _ \ '__|
| |___\ V / (_) | |  | | (_| \__ \ ||  __/ |
\____/ \_/ \___/\_|  |_/\__,_|___/\__\___|_|

                    """
            )
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

            LoggingUtil.getInfoLogger().apply {
                val stc = injector.getInstance(SearchTimeController::class.java)
                info("Evaluated tests: ${stc.evaluatedIndividuals}")
                info("Evaluated actions: ${stc.evaluatedActions}")
                info("Last action improvement: ${stc.lastActionImprovement}")
                info("Passed time (seconds): ${stc.getElapsedSeconds()}")
            }

            return solution
        }


        fun init(args: Array<String>): Injector {

            //TODO check problem type

            try {
                val injector = LifecycleInjector.builder()
                        .withModules(* arrayOf<Module>(
                                BaseModule(args),
                                RestModule()
                        ))
                        .build()
                        .createInjector()

                return injector
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


        fun checkState(injector: Injector): ControllerInfoDto {

            val rc = injector.getInstance(RemoteController::class.java)

            val dto = rc.getControllerInfo() ?:
                    throw IllegalStateException(
                            "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")

            if (!(dto.isInstrumentationOn ?: false)) {
                LoggingUtil.getInfoLogger().warn("The system under test is running without instrumentation")
            }

            //TODO check if the type of controller does match the output format

            return dto
        }


        fun writeTests(injector: Injector, solution: Solution<*>, controllerInfoDto: ControllerInfoDto) {

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

        fun writeStatistics(injector: Injector, solution: Solution<*>) {

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.writeStatistics) {
                return
            }

            val statistics = injector.getInstance(Statistics::class.java)

            statistics.writeStatistics(solution)
        }
    }
}



