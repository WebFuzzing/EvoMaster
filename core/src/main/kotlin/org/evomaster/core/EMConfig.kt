package org.evomaster.core

/**
 * Class used to hold all the main configuration properties
 * of EvoMaster.
 */
class EMConfig {

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Cfg(val description: String)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Min(val min: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Max(val max: Double)


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


    enum class StoppingCriterion(){
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