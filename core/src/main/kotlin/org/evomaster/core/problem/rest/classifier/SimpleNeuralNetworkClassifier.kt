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
import org.nd4j.linalg.lossfunctions.LossFunctions

class SimpleNeuralNetworkClassifier {
    data class Request(val a: Double, val b: Double, val c: Int, val d: Boolean)

//    override fun updateModel(input: RestCallAction, output: RestCallResult) {
//        TODO("Not yet implemented")
//    }
//
//    override fun classify(input: RestCallAction): AIResponseClassification {
//        TODO("Not yet implemented")
//    }

    private val inputSize = 2
    private val outputSize = 2  // [false, true]

    private val model: MultiLayerNetwork
    private val trainingData = mutableListOf<Pair<Request, Boolean>>()

    init {
        val config: MultiLayerConfiguration = NeuralNetConfiguration.Builder()
            .updater(Adam(0.01))
            .list()
            .layer(DenseLayer.Builder().nIn(inputSize).nOut(10).activation(Activation.RELU).build())
            .layer(
                OutputLayer.Builder()
                .nIn(10)
                .nOut(outputSize)
                .activation(Activation.SOFTMAX)
                .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .build())
            .build()

        model = MultiLayerNetwork(config)
        model.init()
    }

    fun updateModel(request: Request, result: Boolean) {
        trainingData.add(Pair(request, result))

        // Prepare data
        val inputArray = Nd4j.create(trainingData.size, inputSize)
        val outputArray = Nd4j.create(trainingData.size, outputSize)

        for ((i, pair) in trainingData.withIndex()) {
            inputArray.putRow(i.toLong(), Nd4j.create(doubleArrayOf(pair.first.a, pair.first.b)))
            val labelIndex = if (pair.second) 1 else 0
            val labelArray = DoubleArray(outputSize) { if (it == labelIndex) 1.0 else 0.0 }
            outputArray.putRow(i.toLong(), Nd4j.create(labelArray))
        }

        val dataSet = DataSet(inputArray, outputArray)
        model.fit(dataSet)
    }

    fun classify(request: Request): Boolean {
        val input = Nd4j.create(doubleArrayOf(request.a, request.b)).reshape(1L, inputSize.toLong())
        val output = model.output(input)
        return output.getDouble(1) > output.getDouble(0)
    }
}