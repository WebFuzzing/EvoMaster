package org.evomaster.core.problem.rest.classifier

class AIResponseClassification(
    /**
     * Map from HTTP Status Code to the probability of getting such code as response.
     * If code is not defined, then expect the probability to be 0.
     */
    private val probabilities : Map<Int, Double> = mapOf(),

    /**
     * If the classification thinks the call will lead to a user error, it might provide some info
     * on the offending inputs.
     * However, as only few classifiers might be able to provide such info, such info is optional.
     */
    val invalidFields : Set<InputField> = setOf(),
) {

    init{
        probabilities.forEach {
            if(it.value !in 0.0..1.0){
                throw IllegalArgumentException("Probability value must be between 0 and 1." +
                        " But status code ${it.key} has probability value ${it.value}")
            }
        }

    }

    fun probabilityOf400() : Double{
        return getProbability(400)
    }

    fun getProbability(statusCode: Int) : Double{
        if(probabilities[statusCode] == null){
            return 0.0
        }
        return probabilities[statusCode]!!
    }

    /**
     * Returns the status code with the highest probability.
     * @return the status code (key) with maximum probability
     */
    fun prediction(): Int {
        return probabilities.maxByOrNull { it.value }?.key ?: -1
    }
}