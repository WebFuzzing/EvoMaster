package org.evomaster.core.problem.rest.aiclassification

import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.utils.RestTestBase

class AIModelsCheckWFD2 : RestTestBase() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheckWFD2()
            test.runTest()
        }
    }

    fun runTest() {
        runTestHandlingFlakyAndCompilation(
            "WFD-Test",
            60
        ) { args: MutableList<String> ->

            // Add black-box Swagger parameters
            args.add("--blackBox")
            args.add("true")

            args.add("--ratePerMinute")
            args.add("60")

            args.add("--bbSwaggerUrl")
            args.add("http://localhost:8080/v2/api-docs")

            args.add("--aiModelForResponseClassification")
            args.add("KNN")

            args.add("--aiClassifierRepairActivation")
            args.add("PROBABILITY")

            println("Running EvoMaster with arguments:")
            println(args.joinToString(" "))

            val (injector, solution) = initAndDebug(args)

            println("Run finished. Found ${solution.individuals.size} individuals")

            val ptr = injector.getInstance(PirToRest::class.java)

            val model = injector.getInstance(AIResponseClassifier::class.java)

            val overallMetrics = model.estimateOverallMetrics()
            val overallAccuracy = overallMetrics.accuracy
            val overallPrecision400 = overallMetrics.precision400
            val overallRecall400 = overallMetrics.recall400
            val overallF1Score = overallMetrics.f1Score400
            val overallMCC = overallMetrics.mcc

            println("Overall Metrics are: Accuracy: $overallAccuracy, Precision@400: $overallPrecision400, Recall@400: $overallRecall400, F1Score: $overallF1Score ,MCC: $overallMCC")

        }
    }
}
