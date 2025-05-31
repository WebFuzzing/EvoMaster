package org.evomaster.core.problem.rest.service.classifier

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

class NeuralNetworkClassifier : AIModel {
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        // NN update logic
    }

    override fun classify(input: RestCallAction): StatusGroup {
        // NN classification
        return StatusGroup.G_2xx
    }

    override fun probValidity(input: RestCallAction): Double {
        // Probability from NN
        return 0.8
    }
}