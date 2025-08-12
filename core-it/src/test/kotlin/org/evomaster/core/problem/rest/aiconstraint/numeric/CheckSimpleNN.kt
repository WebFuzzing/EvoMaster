package org.evomaster.core.problem.rest.aiconstraint.numeric

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.evomaster.core.problem.rest.classifier.SimpleNeuralNetworkClassifier
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.util.*


// === Main ===
fun main() {
    val model = SimpleNeuralNetworkClassifier()
    val rand = Random(0)

    fun targetFunction(r: SimpleNeuralNetworkClassifier.Request): Boolean = r.a > 0 && r.b > 0 && r.a < r.b

    fun uniformRandom(from: Double, to: Double): Double {
        return rand.nextDouble() * (to - from) + from
    }

    fun uniformRandomInt(from: Int, to: Int): Int {
        return rand.nextInt(to - from + 1) + from
    }

    var tp = 0
    var tn = 0
    var fp = 0
    var fn = 0

    var accuracy = 0.0
    var precision = 0.0
    var recall = 0.0
    var f1 = 0.0
    for (i in 1..10_000) {
        var a = uniformRandom(-10_000_000.0, 10_000_000.0)
        var b = uniformRandom(-10_000_000.0, 10_000_000.0)
        var c = uniformRandomInt(-10_000_000, 10_000_000)
        var d = rand.nextBoolean()

        if(i==1){
            a = 2020.0
            b= 2021.0
            c = 42
            d = true
        }

        val req = SimpleNeuralNetworkClassifier.Request(a, b, c, d)
        val expected = targetFunction(req)
        val predicted = model.classify(req)

        model.updateModel(req, expected)

        when {
            predicted && expected -> tp++
            !predicted && !expected -> tn++
            predicted && !expected -> fp++
            !predicted && expected -> fn++
        }

        if (i % 100 == 0) {
            accuracy = (tp + tn).toDouble() / (tp + tn + fp + fn)
            precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
            recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
            f1 = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0

            println("\n=== i=$i ===")
            println("TP=$tp, TN=$tn, FP=$fp, FN=$fn")
            println("Accuracy: %.4f".format(accuracy))
            println("Precision: %.4f".format(precision))
            println("Recall: %.4f".format(recall))
            println("F1 Score: %.4f".format(f1))
        }
    }
}
