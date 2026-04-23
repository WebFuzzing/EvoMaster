package org.evomaster.core.problem.rest.aiclassification

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.quantifier.ModelMetrics
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.kde.KDE400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.nn.NN400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType

object ExtraTools {

    fun executeRestCallAction(action: RestCallAction, baseUrlOfSut: String): RestCallResult {
        val fullUrl = "$baseUrlOfSut${action.resolvedPath()}"
        val url = URL(fullUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = action.verb.name
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val result = RestCallResult(action.getLocalId())

        try {
            val status = connection.responseCode
            result.setStatusCode(status)

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            result.setBody(body)
            result.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        } catch (e: Exception) {
            result.setTimedout(true)
            result.setBody("ERROR: ${e.message}")
        }

        return result
    }

    fun printModelMetrics(modelName: String, metrics: ModelMetrics) {

        println("=== $modelName ===")
        println(
            """
        +-------------------------------------------+
        +----- Confusion Matrix 400 vs. not400 -----+
        +-------------------------------------------+
        |            | Pred 400     | Pred ¬400     |
        +-------------------------------------------+
        | Actual 400 | TP=${metrics.truePositive400.toString().padEnd(10)}| FN=${metrics.falseNegative400.toString().padEnd(11)}|
        | Actual¬400 | FP=${metrics.falsePositive400.toString().padEnd(10)}| TN=${metrics.trueNegative400.toString().padEnd(11)}|
        +-------------------------------------------+
        Window Total      : ${metrics.windowTotal}
        Accuracy            : ${"%.4f".format(metrics.estimateMetrics().accuracy)}
        Precision400        : ${"%.4f".format(metrics.estimateMetrics().precision400)}
        Recall400           : ${"%.4f".format(metrics.estimateMetrics().sensitivity400)}
        F1Score400          : ${"%.4f".format(metrics.estimateMetrics().f1Score400)}
        MCC400              : ${"%.4f".format(metrics.estimateMetrics().mcc)}
        """.trimIndent()
        )
    }

    fun saveAllMetricsToTxt(
        models: Map<Endpoint, AbstractProbabilistic400EndpointModel?>,
        filePath: String,
        runIterations: Int,
        encoderType: EMConfig.EncoderType,
        modelName: String
    ) {
        val sb = StringBuilder()
        sb.appendLine("=============================================")
        sb.appendLine("EvoMaster Classifier Report")
        sb.appendLine("=============================================")
        sb.appendLine("Model Type     : $modelName")
        sb.appendLine("Encoder Type   : $encoderType")
        sb.appendLine("Number of Iterations : $runIterations")
        sb.appendLine("=============================================")
        sb.appendLine()

        for ((endpoint, model) in models) {
            val metrics: ModelMetrics? = when (model) {
                is Gaussian400EndpointModel -> model.modelMetrics
                is GLM400EndpointModel      -> model.modelMetrics
                is KDE400EndpointModel      -> model.modelMetrics
                is NN400EndpointModel       -> model.modelMetrics
                is KNN400EndpointModel      -> model.modelMetrics
                else -> null
            }

            metrics?.let {
                sb.appendLine("Endpoint: $endpoint")
                sb.appendLine()
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("+----- Confusion Matrix 400 vs. not400 -----+")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("|            | Pred 400     | Pred ¬400     |")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("| Actual 400 | TP=${it.truePositive400.toString().padEnd(10)}| FN=${it.falseNegative400.toString().padEnd(11)}|")
                sb.appendLine("| Actual¬400 | FP=${it.falsePositive400.toString().padEnd(10)}| TN=${it.trueNegative400.toString().padEnd(11)}|")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine()
                sb.appendLine("Window Total        : ${it.windowTotal}")
                sb.appendLine("Accuracy            : ${"%.4f".format(it.estimateMetrics().accuracy)}")
                sb.appendLine("Precision400        : ${"%.4f".format(it.estimateMetrics().precision400)}")
                sb.appendLine("Recall400           : ${"%.4f".format(it.estimateMetrics().sensitivity400)}")
                sb.appendLine("F1Score400          : ${"%.4f".format(it.estimateMetrics().f1Score400)}")
                sb.appendLine("MCC400              : ${"%.4f".format(it.estimateMetrics().mcc)}")
                sb.appendLine()
                sb.appendLine("=============================================")
                sb.appendLine()
            }
        }

        // Save the report
        File(filePath).writeText(sb.toString())
    }


}