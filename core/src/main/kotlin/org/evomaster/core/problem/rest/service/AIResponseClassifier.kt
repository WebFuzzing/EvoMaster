package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.service.classifier.AIModel
import org.evomaster.core.problem.rest.service.classifier.GaussianOnlineClassifier
import org.evomaster.core.problem.rest.service.classifier.NeuralNetworkClassifier


class AIResponseClassifier {

    // This should be set based on the config
    // For now we consider GAUSSIAN as a default
    private val aiModelType="GAUSSIAN"

    @Inject
    lateinit var gaussian: GaussianOnlineClassifier

    @Inject
    lateinit var neuralNet: NeuralNetworkClassifier

    private val delegate: AIModel by lazy {
        when (aiModelType) {
            "GAUSSIAN" -> gaussian
            "NEURAL" -> neuralNet
            else -> error("Unknown model type: ${aiModelType}")
        }
    }

    fun updateModel(input: RestCallAction, output: RestCallResult) {
        delegate.updateModel(input, output)
    }

    fun classify(input: RestCallAction): StatusGroup {
        return delegate.classify(input)
    }

    fun probValidity(input: RestCallAction): Double {
        return delegate.probValidity(input)
    }
}

