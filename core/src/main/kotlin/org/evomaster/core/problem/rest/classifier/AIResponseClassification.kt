package org.evomaster.core.problem.rest.classifier

class AIResponseClassification(
    /**
     * Map from HTTP Status Code to the probability of getting such code as response.
     * If code is not defined, then expect the probability to be 0.
     */
    val probabilities : Map<Int, Double> = mapOf()
) {

    fun scoreOf400() : Double{

        if(probabilities[400] == null){
            return 0.0
        }
        return probabilities[400]!!
    }
}