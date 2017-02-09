package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.clientJava.controllerApi.dto.ControllerInfoDto
import org.evomaster.core.output.TestSuiteWriter
import org.evomaster.core.problem.rest.service.RemoteController
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.service.RestModule
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.Statistics


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
                LoggingUtil.getInfoLogger()
                        .error("ERROR: EvoMaster process terminated abruptly. Message: " + e.message, e)
            }
        }

        private fun printLogo(){

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

        @JvmStatic
        fun initAndRun(args: Array<String>): Solution<*> {

            val injector = init(args)

            //FIXME: check already done in @PostConstruct of some beans,
            // need to use lifecycle phase "start"
            val controllerInfo = checkConnection(injector)

            val solution = run(injector)

            writeTests(injector, solution, controllerInfo)

            writeStatistics(injector, solution)

            return solution
        }


        fun init(args: Array<String>): Injector {

            //TODO check problem type

            val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(
                            BaseModule(args),
                            RestModule()
                    ))
                    .build()
                    .createInjector()

            return injector
        }


        fun run(injector: Injector): Solution<*> {

            //TODO check problem type
            val rc = injector.getInstance(RemoteController::class.java)
            rc.startANewSearch()

            val config = injector.getInstance(EMConfig::class.java)

            val key = when(config.algorithm) {
                EMConfig.Algorithm.MIO -> Key.get(
                        object : TypeLiteral<MioAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.RANDOM -> Key.get(
                        object : TypeLiteral<RandomAlgorithm<RestIndividual>>() {})
                EMConfig.Algorithm.WTS -> Key.get(
                        object : TypeLiteral<WtsAlgorithm<RestIndividual>>() {})
                else -> throw IllegalStateException("Unrecognized algorithm ${config.algorithm}")
            }

            val imp = injector.getInstance(key)

            LoggingUtil.getInfoLogger().info("Starting to generate test cases")
            val solution = imp.search()

            return solution
        }


        fun checkConnection(injector: Injector): ControllerInfoDto {

            val rc = injector.getInstance(RemoteController::class.java)

            val dto = rc.getControllerInfo() ?:
                    throw IllegalStateException(
                            "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")

            if(! (dto.isInstrumentationOn ?: false)){
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
            val tests = if(n == 1) "1 test" else "$n tests"

            LoggingUtil.getInfoLogger().info("Going to save $tests to ${config.outputFolder}")

            TestSuiteWriter.writeTests(
                    solution,
                    config.outputFormat,
                    config.outputFolder,
                    config.testSuiteFileName,
                    controllerInfoDto.fullName
            )
        }

        fun writeStatistics(injector: Injector, solution: Solution<*>){

            val config = injector.getInstance(EMConfig::class.java)

            if (!config.writeStatistics) {
                return
            }

            val statistics = injector.getInstance(Statistics::class.java)

            statistics.writeStatistics(solution)
        }
    }
}



