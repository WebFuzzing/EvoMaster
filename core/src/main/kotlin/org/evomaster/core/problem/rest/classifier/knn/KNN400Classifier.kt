package org.evomaster.core.problem.rest.classifier.knn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.knn.KNN400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.Int


class KNN400Classifier(
    private val encoderType: EMConfig.EncoderType = EMConfig.EncoderType.RAW,
    private val warmup: Int = 10,
    private val k: Int = 3
) : AIModel {

    /**
     * Maps each API endpoint to its corresponding AIModel.
     * - If all genes of the endpoint are supported, the value is an AIModel instance.
     * - If the endpoint contains unsupported genes, the value is null, meaning no classifier is used.
     */
    val models: MutableMap<Endpoint, KNN400EndpointModel?> = mutableMapOf()

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        val m = models.getOrPut(input.endpoint) { //get if the key exists otherwise create one

            val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)

            val hasUnsupportedGene = !encoder.areAllGenesSupported()

            if (hasUnsupportedGene) {
                return@getOrPut null
            }

            val listGenes = encoder.endPointToGeneList().map { gene -> gene.getLeafGene() }
            val dimension = listGenes.size

            // create a classifier if the key doesn't exist otherwise null
            KNN400EndpointModel(
                input.endpoint,
                k = k,
                warmup,
                dimension,
                encoderType)
        }

        m?.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val m = models[input.endpoint] ?: return AIResponseClassification(probabilities = mapOf(400 to 0.5))
        return m.classify(input)
    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        val m = models[endpoint] ?: return 0.5
        return m.estimateAccuracy(endpoint)
    }

    override fun estimateOverallAccuracy(): Double {
        if (models.isEmpty()) return 0.5

        //Average over all internal models
        val n = models.size.toDouble()
        val sum = models.values.sumOf { it?.estimateOverallAccuracy() ?: 0.5 } //assuming 0.5 accuracy for null models

        return sum / n
    }

}
