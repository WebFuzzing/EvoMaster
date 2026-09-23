package org.evomaster.core.problem.rest.classifier.probabilistic.dnn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import kotlin.math.*

/**
 * Online multilayer perceptron for HTTP 400 vs. other responses.
 * Uses He initialization, leaky ReLU hidden units, binary cross entropy, Adam,
 * global gradient clipping and bounded reservoir replay. Replay retains the observed
 * class distribution; replayed samples never contribute to evaluation metrics.
 *
 * Signed log compression bounds extreme encoded features without a changing scaler.
 * This is a numerical safeguard, not a replacement for a suitable input encoder.
 * Cost per update is O(batchSize * parameterCount); storage is bounded per endpoint.
 */
class DNN400EndpointModel(
    endpoint: Endpoint,
    warmup: Int,
    modelKeys: List<String>? = null,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType,
    metricType: EMConfig.AIClassificationMetrics,
    private val learningRate: Double = 0.001,
    randomness: Randomness,
    hiddenSizes: List<Int> = listOf(64, 32, 16),
    private val maxStoredSamples: Int = 1024,
    private val batchSize: Int = 16
) : AbstractProbabilistic400EndpointModel(
    endpoint, warmup, modelKeys, dimension, encoderType, metricType, randomness
) {
    private val hiddenSizes = hiddenSizes.toList()
    private class Layer(val inputs: Int, val outputs: Int, val weights: DoubleArray) {
        // Each output has inputs weights followed by one bias.
        val firstMoment = DoubleArray(weights.size)
        val secondMoment = DoubleArray(weights.size)
    }
    private data class Sample(val input: DoubleArray, val target: Double)
    private var layers = emptyList<Layer>()
    private val replay = mutableListOf<Sample>()
    private var observations = 0L
    private var steps = 0L

    init {
        require(warmup > 0)
        require(learningRate.isFinite() && learningRate > 0)
        require(this.hiddenSizes.size >= 2 && this.hiddenSizes.all { it > 0 })
        require(maxStoredSamples > 0)
        require(batchSize > 0)
    }

    override fun initializeIfNeeded(input: RestCallAction) {
        if (initialized) return
        super.initializeIfNeeded(input)
        val sizes = listOf(requireNotNull(dimension)) + hiddenSizes + 1
        layers = sizes.zipWithNext().map { (inputs, outputs) ->
            val limit = sqrt(6.0 / inputs)
            Layer(inputs, outputs, DoubleArray(outputs * (inputs + 1)) { index ->
                if (index % (inputs + 1) == inputs) 0.0 else randomness.nextDouble(-limit, limit)
            })
        }
    }

    private fun encode(input: RestCallAction): DoubleArray =
        encodeUsingModelKeys(input).map { value ->
            if (value.isFinite()) sign(value) * ln1p(abs(value)).coerceAtMost(20.0) else 0.0
        }.toDoubleArray()

    override fun classify(input: RestCallAction): AIResponseClassification {
        verifyEndpoint(input.endpoint)
        if (input.parameters.isEmpty()) return AIResponseClassification()
        initializeIfNeeded(input)
        val p = if (modelMetrics.totalSentRequests < warmup) 0.5 else forward(encode(input)).last()[0]
        return AIResponseClassification(probabilities = mapOf(400 to p, NOT_400 to 1.0 - p))
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        verifyEndpoint(input.endpoint)
        if (input.parameters.isEmpty()) return
        // Transport failures carry no HTTP label and must not teach the negative class.
        val status = output.getStatusCode() ?: return
        initializeIfNeeded(input)
        updateModelMetrics(input, output)
        val sample = Sample(encode(input), if (status == 400) 1.0 else 0.0)
        val batch = mutableListOf(sample)
        if (replay.isNotEmpty()) repeat(minOf(batchSize - 1, replay.size)) {
            batch.add(replay[randomness.nextInt(replay.size)])
        }
        train(batch)
        observations++
        if (replay.size < maxStoredSamples) replay.add(sample)
        else if (randomness.nextDouble() < maxStoredSamples.toDouble() / observations) {
            replay[randomness.nextInt(replay.size)] = sample
        }
    }

    private fun forward(input: DoubleArray): List<DoubleArray> {
        require(input.size == dimension)
        val activations = mutableListOf(input)
        layers.forEachIndexed { index, layer ->
            val previous = activations.last()
            activations.add(DoubleArray(layer.outputs) { j ->
                val offset = j * (layer.inputs + 1)
                var z = layer.weights[offset + layer.inputs]
                for (i in previous.indices) z += previous[i] * layer.weights[offset + i]
                if (index == layers.lastIndex) {
                    if (z >= 0) 1.0 / (1.0 + exp(-z)) else exp(z) / (1.0 + exp(z))
                } else if (z >= 0) z else 0.01 * z
            })
        }
        return activations
    }

    private fun train(batch: List<Sample>) {
        val gradients = layers.map { DoubleArray(it.weights.size) }
        for (sample in batch) {
            val activations = forward(sample.input)
            var delta = doubleArrayOf(activations.last()[0] - sample.target)
            for (l in layers.indices.reversed()) {
                val layer = layers[l]
                val previous = activations[l]
                val previousDelta = DoubleArray(layer.inputs)
                for (j in 0 until layer.outputs) {
                    val offset = j * (layer.inputs + 1)
                    for (i in previous.indices) {
                        gradients[l][offset + i] += delta[j] * previous[i] / batch.size
                        previousDelta[i] += delta[j] * layer.weights[offset + i]
                    }
                    gradients[l][offset + layer.inputs] += delta[j] / batch.size
                }
                for (i in previousDelta.indices) {
                    previousDelta[i] *= if (previous[i] >= 0) 1.0 else 0.01
                }
                delta = previousDelta
            }
        }
        var norm = 0.0
        for (gradient in gradients) for (g in gradient) norm = hypot(norm, g)
        if (!norm.isFinite()) return
        val scale = if (norm > 5.0) 5.0 / norm else 1.0
        steps++
        val correction1 = 1.0 - 0.9.pow(steps.toDouble())
        val correction2 = 1.0 - 0.999.pow(steps.toDouble())
        layers.forEachIndexed { l, layer ->
            for (i in layer.weights.indices) {
                val g = gradients[l][i] * scale
                layer.firstMoment[i] = 0.9 * layer.firstMoment[i] + 0.1 * g
                layer.secondMoment[i] = 0.999 * layer.secondMoment[i] + 0.001 * g * g
                layer.weights[i] -= learningRate * (layer.firstMoment[i] / correction1) /
                    (sqrt(layer.secondMoment[i] / correction2) + 1e-8)
            }
        }
    }
}
