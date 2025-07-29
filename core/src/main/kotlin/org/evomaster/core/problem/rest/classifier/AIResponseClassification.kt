package org.evomaster.core.problem.rest.classifier

class AIResponseClassification(
    /**
     * Map from HTTP Status Code to the scores of getting such code as response.
     * Score is a positive value reflecting the likelihood or probability of occurrence based on the applied model
     * If code is not defined, then expect the score as 0.
     */
    val scores : Map<Int, Double> = mapOf()
) {

    fun scoreOf400() : Double{

        if(scores[400] == null){
            return 0.0
        }
        return scores[400]!!
    }
}