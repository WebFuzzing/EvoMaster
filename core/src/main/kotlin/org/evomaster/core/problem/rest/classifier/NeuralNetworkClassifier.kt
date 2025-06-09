package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

//TODO work-in-progress
class NeuralNetworkClassifier(private val dimension: Int=0) : AIModel {

    private val statusToClass = mapOf(200 to 0, 400 to 1)
    private val classToStatus = statusToClass.entries.associate { (k, v) -> v to k }

    private val model: MultiLayerNetwork = NeuralNetConfiguration.Builder()
        .updater(Adam(0.01))
        .list()
        .layer(
            OutputLayer.Builder()
                .nIn(dimension)
                .nOut(2) // binary classification: 200 vs 400
                .activation(Activation.SOFTMAX)
                .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .build()
        )
        .build()
        .let { MultiLayerNetwork(it).apply { init() } }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = InputEncoderUtils.encode(input)

        val label = statusToClass[output.getStatusCode()] ?: return

        val inputArray = Nd4j.create(arrayOf(inputVector.map { it.toFloat() }.toFloatArray()))
        val labelArray = Nd4j.create(1, 2)
        labelArray.putScalar(intArrayOf(0, label), 1.0)

        val data = DataSet(inputArray, labelArray)
        model.fit(data)
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