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
     * @return the status code (key) with the maximum probability if it is higher than the threshold
     * otherwise returns a negative value
     *
     * The decisionThreshold of 0.5 is used to ensure that the chosen class
     * has at least 50% posterior probability as in the binary case
     * (e.g., {400, not-400}); this follows from the fact that:
     * posteriorProb400 + posteriorProbNot400 = 1
     * which means if posteriorProb400 > posteriorProbNot400 so posteriorProb400 >= 0.5
     */
    fun prediction(decisionThreshold: Double = 0.5): Int {
        val best = probabilities.maxByOrNull { it.value }
        return if (best != null && best.value >= decisionThreshold) {
            best.key
        } else {
            -1
        }
    }
}