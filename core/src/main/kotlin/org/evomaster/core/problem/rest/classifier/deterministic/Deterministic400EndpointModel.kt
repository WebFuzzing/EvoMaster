package org.evomaster.core.problem.rest.classifier.deterministic

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.InputField
import org.evomaster.core.problem.rest.classifier.ModelAccuracy
import org.evomaster.core.problem.rest.classifier.ModelAccuracyWithTimeWindow
import org.evomaster.core.problem.rest.classifier.deterministic.constraints.ConstraintFor400
import org.evomaster.core.problem.rest.classifier.deterministic.constraints.RequiredConstraint
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

class Deterministic400EndpointModel(
    val endpoint: Endpoint
) : AIModel {

    private var initialized = false

    private val constraints: MutableList<ConstraintFor400> = mutableListOf()

    private val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    override fun updateModel(
        input: RestCallAction,
        output: RestCallResult
    ) {
        verifyEndpoint(input.endpoint)

        if(initialized){
            /*
                We need to verify the accuracy of the model.
                Before using these data points for the new learning, would the current
                model be able to correctly classify them?
             */
            TODO
        }

        if(!StatusGroup.G_2xx.isInGroup(output.getStatusCode())){
            /*
                Nothing to do.
                We can only "learn" from successful 2xx calls
             */
            return
        }

        if(!initialized){
            constraints.add(RequiredConstraint(input))
            initialized = true
            //TODO other constraints
        }

        constraints.forEach { it.update2xx(input)}
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        verifyEndpoint(input.endpoint)

        if(!initialized){
            //haven't seen a single success call yet... so nothing learned so far
            return AIResponseClassification()
        }

        val failedConstraintFields = mutableSetOf<InputField>()
        constraints.forEach {
            failedConstraintFields.addAll(it.checkUnsatisfiedConstraints(input))
        }

        val classification = if(failedConstraintFields.isEmpty()){
            0.0
        } else {
            1.0
        }

        val probabilities: MutableMap<Int, Double> = mutableMapOf()
        probabilities[400] = classification

        return AIResponseClassification(probabilities, failedConstraintFields)
    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        verifyEndpoint(endpoint)

        if(!initialized){
            //hasn't learned anything yet
            return 0.0
        }

        return modelAccuracy.estimateAccuracy()
    }

    private fun verifyEndpoint(inputEndpoint: Endpoint){
        if(inputEndpoint != endpoint){
            throw IllegalArgumentException("inout endpoint $inputEndpoint is not the same as the model endpoint $endpoint")
        }
    }
}