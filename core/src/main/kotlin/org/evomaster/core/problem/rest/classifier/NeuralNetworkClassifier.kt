package org.evomaster.core.problem.rest.classifier

import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.impl.LossNegativeLogLikelihood

class NeuralNetworkClassifier(private val learningRate: Double = 0.01) : AIModel {
    data class Request(val a: Double, val b: Double, val c: Int, val d: Boolean)

    private var model: MultiLayerNetwork? = null
    private var dimension: Int? = null
    private val outputSize = 2  // [false, true]

    /** Must be called once to initialize model weights based on dimension */
    fun setDimension(d: Int) {
        require(d > 0) { "Dimension must be positive." }
        if (dimension != d) {
            dimension = d
            model = buildModel(d)
            trainingData.clear()
        }
    }

    private fun getDimension(): Int =
        requireNotNull(dimension) { "Classifier not initialized. Call setDimension(d) first." }


    private val trainingData = mutableListOf<Pair<List<Double>, Int>>()

    private fun buildModel(nIn: Int): MultiLayerNetwork {
        val config: MultiLayerConfiguration = NeuralNetConfiguration.Builder()
            .updater(Adam(learningRate))
            .list()
            .layer(
                DenseLayer.Builder()
                    .nIn(nIn)
                    .nOut(10)
                    .activation(Activation.RELU)
                    .build()
            )
            .layer(
                OutputLayer.Builder()
                    .nIn(10)
                    .nOut(outputSize)
                    .activation(Activation.SOFTMAX)
                    // Class weights: [non-200, 200]
                    .lossFunction(LossNegativeLogLikelihood(Nd4j.create(floatArrayOf(1.0f, 10.0f))))
                    .build()
            )
            .build()

        return MultiLayerNetwork(config).apply { init() }
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val dimension = getDimension()
        val encodedInputs = InputEncoderUtils.encode(input)
        require(encodedInputs.size == dimension) { "Encoded input size ${encodedInputs.size} != expected dimension $dimension" }

        val labelIndex = if (output.getStatusCode() == 200) 1 else 0

        trainingData.add(encodedInputs to labelIndex)

        val m = requireNotNull(model) { "Model not initialized. Call setDimension(d) first." }

        // Build dataset from stored samples
        val inputArray = Nd4j.create(trainingData.size.toLong(), dimension.toLong())
        val outputArray = Nd4j.create(trainingData.size.toLong(), outputSize.toLong())

        for ((i, pair) in trainingData.withIndex()) {
            val features = pair.first
            val lblIdx = pair.second
            inputArray.putRow(i.toLong(), Nd4j.create(features))
            val oneHot = DoubleArray(outputSize) { idx -> if (idx == lblIdx) 1.0 else 0.0 }
            outputArray.putRow(i.toLong(), Nd4j.create(oneHot))
        }

        m.fit(DataSet(inputArray, outputArray))
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val dimension = getDimension()
        val encodedInputs = InputEncoderUtils.encode(input).map { it.toDouble() }.toDoubleArray()
        require(encodedInputs.size == dimension) { "Encoded input size ${encodedInputs.size} != expected dimension $dimension" }

        val m = requireNotNull(model) { "Model not initialized. Call setDimension(d) first." }
        val output = m.output(Nd4j.create(encodedInputs).reshape(1L, dimension.toLong()))

        return AIResponseClassification(
            probabilities = mapOf(
                200 to output.getDouble(1),
                400 to output.getDouble(0)
            )
        )
    }
}