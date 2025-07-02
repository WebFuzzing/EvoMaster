package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

class NeuralNetworkClassifier(private val dimension: Int = 6) : AIModel {

    private val statusToClass = mapOf(200 to 0, 400 to 1)
    private val classToStatus = statusToClass.entries.associate { (k, v) -> v to k }

    private val inputBuffer = mutableListOf<FloatArray>()
    private val labelBuffer = mutableListOf<FloatArray>()
    private val batchSize = 16

    private val model: MultiLayerNetwork = NeuralNetConfiguration.Builder()
        .updater(Adam(0.01))
        .list()
        .layer(
            DenseLayer.Builder()
                .nIn(dimension)
                .nOut(32)  // increased neurons
                .activation(Activation.RELU)
                .dropOut(0.5) // 🔽 Dropout here
                .build()
        )
        .layer(
            DenseLayer.Builder()
                .nIn(32)
                .nOut(16)
                .activation(Activation.RELU)
                .dropOut(0.5) // 🔽 Optional second dropout
                .build()
        )
        .layer(
            OutputLayer.Builder()
                .nIn(16)
                .nOut(2)
                .activation(Activation.SOFTMAX)
                .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .build()
        )
        .build()
        .let { MultiLayerNetwork(it).apply { init() } }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = InputEncoderUtils.encode(input).map { it.toFloat() }.toFloatArray()
        val labelIndex = statusToClass[output.getStatusCode()] ?: return

        val labelVector = FloatArray(2) { 0f }.also { it[labelIndex] = 1f }

        inputBuffer.add(inputVector)
        labelBuffer.add(labelVector)

        if (inputBuffer.size >= batchSize) {
            val inputMatrix = Nd4j.create(inputBuffer.toTypedArray())
            val labelMatrix = Nd4j.create(labelBuffer.toTypedArray())
            model.fit(DataSet(inputMatrix, labelMatrix))
            inputBuffer.clear()
            labelBuffer.clear()
        }
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val inputVector = InputEncoderUtils.encode(input)
        val inputArray = Nd4j.create(arrayOf(inputVector.map { it.toFloat() }.toFloatArray()))
        val output: INDArray = model.output(inputArray)
        val probs = output.toDoubleVector()

        val probabilities = classToStatus.entries.associate { (classIndex, code) ->
            code to probs[classIndex]
        }

        return AIResponseClassification(probabilities)
    }
}
