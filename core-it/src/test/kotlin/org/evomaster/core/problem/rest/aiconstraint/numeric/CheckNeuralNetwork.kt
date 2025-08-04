package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICMultiTypeController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.seeding.service.rest.PirToRest
import kotlin.random.Random

class CheckNeuralNetwork : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(AICMultiTypeController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = CheckNeuralNetwork()
            init()
            test.initializeTest()
            test.runClassifierEvaluation()
        }
    }

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", "NN"))
    }

    fun runClassifierEvaluation() {
        val classifier = injector.getInstance(AIResponseClassifier::class.java)
        val pirTest = getPirToRest()

        val baseAction = pirTest.fromVerbPath("get", "/api/petShop",
            mapOf(
                "category" to "REPTILE",
                "gender" to "FEMALE",
                "birthYear" to "2007",
                "vaccinationYear" to "2020",
                "isAlive" to "true",
                "weight" to "10.5"
            ))!!

        val dimension = baseAction.seeTopGenes().count {
            it is IntegerGene || it is DoubleGene || it is BooleanGene || it is EnumGene<*>
        }
        require(dimension == 6)
        classifier.initModel() //TODO

        val trainingSet = mutableListOf<Pair<RestCallAction, RestCallResult>>()
        val testSet = mutableListOf<Pair<RestCallAction, Int>>()

        collectTrainingSamples(pirTest, 10_000, trainingSet)
        collectTestSamples(pirTest, 10_000, testSet)

        for ((i, pair) in trainingSet.withIndex()) {
            val (action, result) = pair

            classifier.updateModel(normalizeForModel(action), result)

            if ((i + 1) % 10 == 0) {
                val acc = evaluateAccuracy(classifier, testSet)
                println("Test accuracy after ${i + 1} updates: $acc")
            }
        }

        evaluateDetailedMetrics(classifier, testSet)
    }

    private fun collectTrainingSamples(
        pirTest: PirToRest,
        totalSamples: Int,
        target: MutableList<Pair<RestCallAction, RestCallResult>>
    ) {
        val desiredPerClass = totalSamples / 2
        var count200 = 0
        var count400 = 0
        var attempts = 0
        val maxAttempts = 20_000

        while ((count200 < desiredPerClass || count400 < desiredPerClass) && attempts < maxAttempts) {
            attempts++

            val label = if (Random.nextBoolean()) 200 else 400

            val inputParams = generateInputForLabel(label)
            val action = pirTest.fromVerbPath("get", "/api/petShop", inputParams) ?: continue

            val individual = createIndividual(listOf(action), SampleType.RANDOM)
            val result = individual.evaluatedMainActions()[0].result as RestCallResult
            val status = result.getStatusCode()

            if (status == 200 && count200 < desiredPerClass) {
                target.add(action to result)
                count200++
            } else if (status == 400 && count400 < desiredPerClass) {
                target.add(action to result)
                count400++
            }
        }

        println("Training set: Collected $count200 x 200 and $count400 x 400 (after $attempts attempts)")
    }


    private fun collectTestSamples(
        pirTest: PirToRest,
        totalSamples: Int,
        target: MutableList<Pair<RestCallAction, Int>>
    ) {
        var count200 = 0
        var count400 = 0
        var attempts = 0
        val maxAttempts = 10000

        while ((count200 < totalSamples / 2 || count400 < totalSamples / 2) && attempts < maxAttempts) {
            attempts++

            val label = if (Random.nextBoolean()) 200 else 400
            val inputParams = generateInputForLabel(label)
            val action = pirTest.fromVerbPath("get", "/api/petShop", inputParams) ?: continue

            val individual = createIndividual(listOf(action), SampleType.RANDOM)
            val result = individual.evaluatedMainActions()[0].result as RestCallResult
            val status = result.getStatusCode()

            if (status == label) {
                target.add(action to status)
                if (status == 200) count200++
                else count400++
            }

            if (count200 + count400 >= totalSamples) break
        }

        println("Test set: Collected $count200 x 200 and $count400 x 400 samples (after $attempts attempts)")
    }


    private fun evaluateAccuracy(classifier: AIResponseClassifier, testSet: List<Pair<RestCallAction, Int>>): Double {
        var correct = 0
        for ((action, actualStatus) in testSet) {
            val prediction = classifier.classify(normalizeForModel(action))
            val predictedStatus = prediction.probabilities.maxByOrNull { it.value }?.key
            if (predictedStatus == actualStatus) correct++
        }
        return correct.toDouble() / testSet.size
    }

    private fun evaluateDetailedMetrics(classifier: AIResponseClassifier, testSet: List<Pair<RestCallAction, Int>>) {
        var tp = 0
        var tn = 0
        var fp = 0
        var fn = 0

        for ((action, actualStatus) in testSet) {
            val prediction = classifier.classify(normalizeForModel(action))
            val predictedStatus = prediction.probabilities.maxByOrNull { it.value }?.key

            when {
                predictedStatus == 200 && actualStatus == 200 -> tp++
                predictedStatus == 400 && actualStatus == 400 -> tn++
                predictedStatus == 200 && actualStatus == 400 -> fp++
                predictedStatus == 400 && actualStatus == 200 -> fn++
            }
        }

        val precision = tp.toDouble() / (tp + fp).coerceAtLeast(1)
        val recall = tp.toDouble() / (tp + fn).coerceAtLeast(1)
        val f1 = 2 * precision * recall / (precision + recall).coerceAtLeast(1e-9)

        println("Confusion Matrix: TP=$tp, FP=$fp, FN=$fn, TN=$tn")
        println("Precision: %.3f".format(precision))
        println("Recall: %.3f".format(recall))
        println("F1 Score: %.3f".format(f1))
    }

    private fun randomCategory(): String = listOf("REPTILE", "DOG", "CAT", "BIRD").random()
    private fun randomGender(): String = listOf("MALE", "FEMALE").random()
    private fun randomBool(): Boolean = Random.nextBoolean()
    private fun randomYear(start: Int, end: Int): Int = Random.nextInt(start, end + 1)
    private fun randomWeight(): Double = (5..20).random() + Random.nextDouble()

    private fun normalizeYear(year: Int, min: Int = 1600, max: Int = 2200): Double {
        return (year - min).toDouble() / (max - min)
    }

    private fun normalizeWeight(weight: Double, min: Double = 5.0, max: Double = 21.0): Double {
        return (weight - min) / (max - min)
    }

    private fun normalizeBoolean(value: Boolean): Double {
        return if (value) 1.0 else 0.0
    }

    private fun normalizeEnum(value: String, choices: List<String>): Double {
        return choices.indexOf(value).toDouble() / (choices.size - 1)
    }

    private fun normalizeForModel(action: RestCallAction): RestCallAction {

        val copy = deepCopyUsingReflection(action)

        val yearMin = 1600
        val yearMax = 2200
        val weightMin = 5.0
        val weightMax = 21.0

        copy.parameters.forEach { param ->
            val name = param.name
            val gene = param.primaryGene()

            when (name) {
                "birthYear", "vaccinationYear" -> {
                    val value = gene.getValueAsRawString().toIntOrNull()
                    if (value != null && gene is DoubleGene) {
                        val normalizedValue = (value - yearMin).toDouble() / (yearMax - yearMin)
                        gene.value = normalizedValue
                    }
                }
                "weight" -> {
                    val value = gene.getValueAsRawString().toDoubleOrNull()
                    if (value != null && gene is DoubleGene) {
                        val normalizedValue = (value - weightMin) / (weightMax - weightMin)
                        gene.value = normalizedValue
                    }
                }
                "isAlive" -> {
                    val value = gene.getValueAsRawString().toBooleanStrictOrNull()
                    if (value != null && gene is DoubleGene) {
                        gene.value = if (value) 1.0 else 0.0
                    }
                }
                "category" -> {
                    val value = gene.getValueAsRawString()
                    val normalized = normalizeEnum(value, listOf("REPTILE", "DOG", "CAT", "BIRD"))
                    if (gene is DoubleGene) {
                        gene.value = normalized
                    }
                }
                "gender" -> {
                    val value = gene.getValueAsRawString()
                    val normalized = normalizeEnum(value, listOf("MALE", "FEMALE"))
                    if (gene is DoubleGene) {
                        gene.value = normalized
                    }
                }

            }
        }

        return copy
    }

    private fun generateInputForLabel(label: Int): Map<String, String> {
        val category = randomCategory()
        val gender = randomGender()
        val isAlive = randomBool()
        val weight = randomWeight()

        return when (label) {
            200 -> {
                // Use large positive years for both fields, ensure birthYear <= vaccinationYear
                val birthYear = Random.nextInt(1, Int.MAX_VALUE / 2)
                val vaccinationYear = Random.nextInt(birthYear, birthYear + 1000)

                mapOf(
                    "category" to category,
                    "gender" to gender,
                    "birthYear" to birthYear.toString(),
                    "vaccinationYear" to vaccinationYear.toString(),
                    "isAlive" to isAlive.toString(),
                    "weight" to weight.toString()
                )
            }
            400 -> {
                // Either use negative values or make vaccinationYear < birthYear
                val useNegative = Random.nextBoolean()

                val birthYear: Int
                val vaccinationYear: Int

                if (useNegative) {
                    birthYear = Random.nextInt(Int.MIN_VALUE, 0)
                    vaccinationYear = Random.nextInt(Int.MIN_VALUE, 0)
                } else {
                    birthYear = Random.nextInt(1, Int.MAX_VALUE / 2)
                    vaccinationYear = Random.nextInt(1, birthYear) // vaccinationYear < birthYear
                }

                mapOf(
                    "category" to category,
                    "gender" to gender,
                    "birthYear" to birthYear.toString(),
                    "vaccinationYear" to vaccinationYear.toString(),
                    "isAlive" to isAlive.toString(),
                    "weight" to weight.toString()
                )
            }
            else -> error("Unsupported label: $label")
        }
    }


    private fun deepCopyUsingReflection(action: RestCallAction): RestCallAction {
        val method = RestCallAction::class.java.getDeclaredMethod("copyContent")
        method.isAccessible = true
        return method.invoke(action) as RestCallAction
    }

}
