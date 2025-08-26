package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICMultiTypeController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.classifier.NeuralNetworkClassifier
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.ws.rs.core.MediaType
import kotlin.math.abs

class NeuralNetworkCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(AICMultiTypeController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = NeuralNetworkCheck()
            init()
            test.initializeTest()
            test.runClassifierExample()
        }
    }

    fun initializeTest() {
        recreateInjectorForWhite(
            listOf(
                "--aiModelForResponseClassification", "NN",
                "--aiResponseClassifierLearningRate", "0.01"
            )
        )
    }

    private fun executeRestCallAction(action: RestCallAction, baseUrlOfSut: String): RestCallResult {
        val fullUrl = "$baseUrlOfSut${action.resolvedPath()}"
        val url = URL(fullUrl)
        val conn = url.openConnection() as HttpURLConnection

        conn.requestMethod = action.verb.name
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("Accept", "*/*")

        val result = RestCallResult(action.getLocalId())
        try {
            val status = conn.responseCode
            result.setStatusCode(status)

            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            result.setBody(body)

            conn.contentType?.let { ct ->
                // e.g., "application/json; charset=utf-8" -> "application/json"
                val main = ct.substringBefore(";").trim()
                if (main.isNotBlank()) {
                    try {
                        result.setBodyType(MediaType.valueOf(main))
                    } catch (_: Exception) {
                        // ignore invalid content types
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            result.setTimedout(true)
            result.setBody("TIMEOUT: ${e.message}")
        } catch (e: Exception) {
            result.setBody("ERROR: ${e.message}")
        } finally {
            conn.disconnect()
        }
        return result
    }

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val config = EMConfig().apply {
            aiModelForResponseClassification = EMConfig.AIResponseClassifierModel.NN
            enableSchemaConstraintHandling = true
            allowInvalidData = false
            probRestDefault = 0.0
            probRestExamples = 0.0
        }

        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)

        // Filter endpoints if desired
        val actionList = actionCluster.values
            .filterIsInstance<RestCallAction>();
//            .filter { a ->
//                a.getName().contains("petShopApi/petInfo") &&
//                        a.verb.name.equals("GET", ignoreCase = true)
//            }

        if (actionList.isEmpty()) {
            println("No actions after filtering — nothing to do.")
            return
        }

        // Compute input dimension per endpoint & create classifier when supported
        val endpointToDimension = mutableMapOf<String, Int?>()
        actionList.forEach { action ->
            val name = action.getName()
            val hasUnsupported = action.parameters.any { p ->
                val g = p.gene
                g !is IntegerGene && g !is DoubleGene && g !is BooleanGene && g !is EnumGene<*>
            }
            val dim = if (hasUnsupported) null else action.parameters.count { p ->
                val g = p.gene
                g is IntegerGene || g is DoubleGene || g is BooleanGene || g is EnumGene<*>
            }
            println("\nEndpoint: $name, dimension: $dim")
            endpointToDimension[name] = dim
        }

        val endpointToClassifier = mutableMapOf<String, NeuralNetworkClassifier?>()
        endpointToDimension.forEach { (name, dim) ->
            endpointToClassifier[name] = if (dim == null) {
                null
            } else {
                NeuralNetworkClassifier().apply { setDimension(dim) }
            }
        }

        // Metrics per endpoint
        data class Confusion(var tp: Int = 0, var tn: Int = 0, var fp: Int = 0, var fn: Int = 0, var seen: Int = 0)
        val confusionByEndpoint = actionList.associate { it.getName() to Confusion() }.toMutableMap()

        fun safeDiv(n: Int, d: Int) = if (d == 0) 0.0 else n.toDouble() / d
        fun printMetrics(name: String, c: Confusion) {
            val total = c.tp + c.tn + c.fp + c.fn
            val accuracy = safeDiv(c.tp + c.tn, total)
            val precision = safeDiv(c.tp, c.tp + c.fp)
            val recall = safeDiv(c.tp, c.tp + c.fn)
            val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
            println("\n**** [$name] after ${c.seen} predictions")
            println("TP=${c.tp}, TN=${c.tn}, FP=${c.fp}, FN=${c.fn}")
            println(
                "Accuracy=${"%.3f".format(accuracy)}  " +
                        "Precision=${"%.3f".format(precision)}  " +
                        "Recall=${"%.3f".format(recall)}  " +
                        "F1=${"%.3f".format(f1)}"
            )
        }

        // Sampler and loop settings
        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)
        val perEndpointBudget = 1_000      // how many samples per endpoint
        val reportEvery = 100              // print every N predictions per endpoint
        val positiveStatus = 200           // define "positive" label

        // Train/evaluate per endpoint
        for (template in actionList) {
            val name = template.getName()
            val classifier = endpointToClassifier[name]
            val dimension = endpointToDimension[name]

            if (classifier == null) {
                println("[$name] skipped: unsupported genes (dimension=$dimension)")
                continue
            }

            println("\n==== Training endpoint: $name (dimension=$dimension, model=NN) ====")

            repeat(perEndpointBudget) {
                // copy and initialize the action (inputs)
                val sampledAction = (template.copy() as RestCallAction).apply { doInitialize(random) }

                // predict
                val predictedProbs = classifier.classify(sampledAction).probabilities
                require(predictedProbs.isNotEmpty()) { "Classifier returned empty probabilities" }
                require(predictedProbs.values.all { it in 0.0..1.0 } &&
                        abs(predictedProbs.values.sum() - 1.0) < 1e-6) {
                    "Probabilities must be in [0,1] and sum to 1"
                }
                val predictedStatus = predictedProbs.maxByOrNull { it.value }!!.key
                val predictedPositive = (predictedStatus == positiveStatus)

                // call the endpoint
                val individual = sampler.createIndividual(SampleType.RANDOM, mutableListOf(sampledAction))
                val execAction = individual.seeMainExecutableActions()[0]
                val result = executeRestCallAction(execAction, baseUrlOfSut)
                val actualStatus = result.getStatusCode()
                val actualPositive = (actualStatus == positiveStatus)

                // update confusion per endpoint
                val c = confusionByEndpoint.getValue(name)
                when {
                    predictedPositive && actualPositive -> c.tp++
                    predictedPositive && !actualPositive -> c.fp++
                    !predictedPositive && actualPositive -> c.fn++
                    else -> c.tn++
                }
                c.seen++

                // update the model
                classifier.updateModel(execAction, result)

                // periodic report
                if (c.seen % reportEvery == 0) {
                    printMetrics(name, c)
                }
            }

            // final summary per endpoint
            printMetrics(name, confusionByEndpoint.getValue(name))
        }

        // Final Summary (after training all endpoints)

        // Preserve endpoint order as they appear in actionList
        val endpointNamesInOrder = actionList.map { it.getName() }

        // Helper to format one line of metrics
        fun metricsLine(c: Confusion): String {
            val total = c.tp + c.tn + c.fp + c.fn
            val accuracy  = safeDiv(c.tp + c.tn, total)
            val precision = safeDiv(c.tp, c.tp + c.fp)
            val recall    = safeDiv(c.tp, c.tp + c.fn)
            val f1        = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
            return "accuracy=${"%.3f".format(accuracy)}, precision=${"%.3f".format(precision)}, recall=${"%.3f".format(recall)}, f1=${"%.3f".format(f1)}"
        }

        println("\n================== FINAL REPORT ==================")

        // Per-endpoint (numbered)
        endpointNamesInOrder.forEachIndexed { idx, name ->
            val c = confusionByEndpoint[name] ?: return@forEachIndexed
            println("Endpoint ${idx + 1} ${name}: TP=${c.tp}, TN=${c.tn}, FP=${c.fp}, FN=${c.fn}")
            println(metricsLine(c))
            println()
        }

        // Totals across all endpoints
        val total = Confusion()
        confusionByEndpoint.values.forEach { c ->
            total.tp += c.tp
            total.tn += c.tn
            total.fp += c.fp
            total.fn += c.fn
            total.seen += c.seen
        }
        println("* Total: TP=${total.tp}, TN=${total.tn}, FP=${total.fp}, FN=${total.fn}")
        println(metricsLine(total))
    }
}
