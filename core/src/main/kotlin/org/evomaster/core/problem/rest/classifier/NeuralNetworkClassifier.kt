package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

//TODO work-in-progress
class NeuralNetworkClassifier : AIModel {
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        // NN update logic
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        // NN classification
        return AIResponseClassification()
    }

}