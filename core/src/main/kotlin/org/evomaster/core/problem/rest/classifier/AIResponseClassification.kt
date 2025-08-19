package org.evomaster.core.problem.rest.classifier

class AIResponseClassification(
    /**
     * Map from HTTP Status Code to the probability of getting such code as response.
     * If code is not defined, then expect the probability to be 0.
     */
    val probabilities : Map<Int, Double> = mapOf()
) {

    init{
        probabilities.forEach {
            if(it.value !in 0.0..1.0){
                throw IllegalArgumentException("Probability value must be between 0 and 1.")
            }
        }
    }

    fun probabilityOf400() : Double{

        if(probabilities[400] == null){
            return 0.0
        }
        return probabilities[400]!!
    }
}