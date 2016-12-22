package org.evomaster.core

import joptsimple.OptionParser
import joptsimple.OptionSet
import org.evomaster.clientJava.controllerApi.ControllerConstants


/**
 * Class used to hold all the main configuration properties
 * of EvoMaster.
 */
class EMConfig {

    companion object {
        /**
         * Get all available "console options" for the annotated properties
         */
        fun getOptionParser(): OptionParser {

            var parser = OptionParser()

            EMConfig::class.members
                    .filter {
                        m -> m.annotations.any {
                            a -> a.annotationClass.equals(Cfg::class)
                        }
                    }
                    .forEach { m ->
                        //TODO default values
                        parser.accepts(m.name).withRequiredArg()
                    }

            return parser
        }
    }

    /**
     * Update the values of the properties based on the options
     * chosen on the command line
     *
     * @return whether the update was successful. An update might
     *         fail if constraints are violated
     */
    fun updateProperties(options: OptionSet) : Boolean{

        //TODO
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

    enum class Algorithm{
        MIO, RANDOM
    }

    @Cfg("The algorithm used to generate test cases")
    var algorithm = Algorithm.MIO


    enum class ProblemType{
        REST
    }

    @Cfg("The type of SUT we want to generate tests for, e.g., a RESTful API")
    var problemType = ProblemType.REST


    @Cfg("The seed for the random generator used during the search. " +
            "A negative value means the CPU clock time will be rather used")
    var seed: Long = -1

    @Cfg("TCP port of where the SUT REST controller is listening on")
    @Min(0.0) @Max(65535.0)
    var sutControllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT


    @Cfg("Limit of number of individuals per target to keep in the archive")
    @Min(1.0)
    var archiveTargetLimit = 10;

    @Cfg("Probability of sampling a new individual at random")
    @Min(0.0) @Max(1.0)
    var probOfRandomSampling = 0.5

    @Cfg("The percentage of passed search before the probability of random sampling is reduced to 0")
    @Min(0.0) @Max(1.0)
    var decreasingRandomSampling = 0.5

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


}