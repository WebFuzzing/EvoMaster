package org.evomaster.core

import joptsimple.OptionParser
import joptsimple.OptionSet
import org.evomaster.clientJava.controllerApi.ControllerConstants
import org.evomaster.core.output.OutputFormat
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType


/**
 * Class used to hold all the main configuration properties
 * of EvoMaster.
 */
class EMConfig {

    companion object {

        fun validateOptions(args: Array<String>): OptionParser{

            val config = EMConfig()

            val parser = EMConfig.getOptionParser()
            val options = parser.parse(*args)

            if(!options.has("help")) {
                config.updateProperties(options)
            }

            return parser
        }


        /**
         * Get all available "console options" for the annotated properties
         */
        fun getOptionParser(): OptionParser {

            val defaultInstance = EMConfig()

            var parser = OptionParser()

            parser.accepts("help").forHelp()

            getConfigurationProperties().forEach { m ->
                        /*
                            Note: here we could use typing in the options,
                            instead of converting everything to string.
                            But it looks bit cumbersome to do it in Kotlin,
                            at least for them moment

                            TODO: do documentation
                            TODO: groups and ordering
                         */
                        parser.accepts(m.name)
                                .withRequiredArg()
                                .defaultsTo("" + m.call(defaultInstance))
                    }

            return parser
        }

        private fun getConfigurationProperties(): List<KMutableProperty<*>> {
            return EMConfig::class.members
                    .filter { m -> m is KMutableProperty }
                    .map{ m -> m as KMutableProperty }
                    .filter {
                        m ->
                        m.annotations.any {
                            a ->
                            a.annotationClass.equals(Cfg::class)
                        }
                    }
        }
    }

    /**
     * Update the values of the properties based on the options
     * chosen on the command line
     *
     * @return whether the update was successful. An update might
     *         fail if constraints are violated
     *
     * @throws IllegalArgumentException if there are constraint violations
     */
    fun updateProperties(options: OptionSet): Boolean {

        getConfigurationProperties().forEach { m ->

            val opt = options.valueOf(m.name)?.toString() ?:
                    throw IllegalArgumentException("Value not found for property ${m.name}")

            val returnType = m.returnType.javaType as Class<*>

            //TODO: ugly checks. But not sure yet if can be made better in Kotlin

            try {
                if (Integer.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, Integer.parseInt(opt))

                } else if (java.lang.Long.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Long.parseLong(opt))

                } else if (java.lang.Double.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Double.parseDouble(opt))

                } else if (java.lang.Boolean.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Boolean.parseBoolean(opt))

                } else if (java.lang.String::class.java.isAssignableFrom(returnType)) {
                    m.setter.call(this, opt)

                } else if (returnType.isEnum) {
                    var valueOfMethod = returnType.getDeclaredMethod("valueOf",
                            java.lang.String::class.java)
                    m.setter.call(this, valueOfMethod.invoke(null, opt))

                } else {
                    throw IllegalStateException("BUG: cannot handle type " + returnType)
                }
            } catch (e: Exception){
                throw IllegalArgumentException("Failed to handle property ${m.name}", e)
            }

            //TODO constraint checks, eg Min/Max
        }

        return true
    }

    //------------------------------------------------------------------------
    //--- custom annotations

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Cfg(val description: String)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Min(val min: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Max(val max: Double)


    //------------------------------------------------------------------------
    //--- properties

    enum class Algorithm {
        MIO, RANDOM, WTS, MOSA
    }

    @Cfg("The algorithm used to generate test cases")
    var algorithm = Algorithm.MIO


    enum class ProblemType {
        REST
    }

    @Cfg("The type of SUT we want to generate tests for, e.g., a RESTful API")
    var problemType = ProblemType.REST


    @Cfg("Specify in which format the tests should be outputted")
    var outputFormat = OutputFormat.JAVA_JUNIT_4


    @Cfg("Specify if test classes should be created as output of the tool. " +
            "Usually, you would put it to 'false' only when debugging EvoMaster itself")
    var createTests = true

    @Cfg("The path directory of where the generated test classes should be saved to")
    //TODO check if can be created
    var outputFolder = "src/em"


    @Cfg("The name of generated file with the test cases, without file type extension. " +
            "In JVM languages, if the name contains '.', folders will be created to represent " +
            "the given package structure")
    //TODO constrain of no spaces or weird characters, eg use regular expression
    var testSuiteFileName = "EvoMasterTest"


    @Cfg("The seed for the random generator used during the search. " +
            "A negative value means the CPU clock time will be rather used as seed")
    var seed: Long = -1

    @Cfg("TCP port of where the SUT REST controller is listening on")
    @Min(0.0) @Max(65535.0)
    var sutControllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT

    @Cfg("Host name or IP address of where the SUT REST controller is listening o")
    var sutControllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST

    @Cfg("Limit of number of individuals per target to keep in the archive")
    @Min(1.0)
    var archiveTargetLimit = 10

    @Cfg("Probability of sampling a new individual at random")
    @Min(0.0) @Max(1.0)
    var probOfRandomSampling = 0.5

    @Cfg("The percentage of passed search before starting a more focused, less exploratory one")
    @Min(0.0) @Max(1.0)
    var focusedSearchActivationTime = 0.5

    @Cfg("Number of applied mutations on sampled individuals, at the start of the search")
    @Min(0.0)
    var startNumberOfMutations = 1

    @Cfg("Number of applied mutations on sampled individuals, by the end of the search")
    @Min(0.0)
    var endNumberOfMutations = 20


    enum class StoppingCriterion() {
        TIME,
        FITNESS_EVALUATIONS
    }

    @Cfg("Stopping criterion for the search")
    var stoppingCriterion = StoppingCriterion.FITNESS_EVALUATIONS


    @Cfg("Maximum number of fitness evaluations for the search. " +
            "Only applicable depending on the stopping criterion.")
    @Min(1.0)
    var maxFitnessEvaluations = 1000;

    @Cfg("Whether or not writing statistics of the search process. " +
            "This is only needed when running experiments with different parameter settings")
    var writeStatistics = false

    @Cfg("Where the statistics file (if any) is going to be written (in CSV format)")
    var statisticsFile = "statistics.csv"

    @Cfg("Whether should add to an existing statistics file, instead of replacing it")
    var appendToStatisticsFile = false

    @Cfg("An id that will be part as a column of the statistics file (if any is generated)")
    var statisticsColumnId = "-"

    @Cfg("Define the population size in the search algorithms that use populations (eg, Genetic Algorithms)")
    @Min(1.0)
    var populationSize = 30

    @Cfg("Define the maximum number of tests in a suite in the search algorithms that evolve whole suites, e.g. WTS")
    @Min(1.0)
    var maxSearchSuiteSize = 50

    @Cfg("Probability of applying crossover operation (if any is used in the search algorithm)")
    @Min(0.0) @Max(1.0)
    var xoverProbability = 0.7

    @Cfg("Number of elements to consider in a Tournament Selection (if any is used in the search algorithm)")
    @Min(1.0)
    var tournamentSize = 10

    @Cfg("When sampling new test cases to evaluate, probability of using some smart strategy instead of plain random")
    @Min(0.0) @Max(1.0)
    var probOfSmartSampling = 0.0 // 0.7 TODO change once working

    @Cfg("Max number of 'actions' (e.g., RESTful calls or SQL commands) that can be done in a single test")
    @Min(1.0)
    var maxTestSize = 1 // 20 TODO change once working
}