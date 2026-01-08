package org.evomaster.core.problem.rest.aiclassification

import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.e2etests.utils.RestTestBase

class AIModelsCheckWFDEM : RestTestBase() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheckWFDEM()
            test.runTest()
        }
    }

    val modelName = "KDE" // Choose "GAUSSIAN", "GLM", "KDE", "KNN", "NN", etc.
    val encoderType = "RAW" // Choose "RAW" or "NORMAL"
    val decisionMaking = "PROBABILITY" // Choose "PROBABILITY" or "THRESHOLD"
    val warmUpRep = 10
    val maxAttemptRepair = 100 // i.e., the classifier has 10 times the chances to pick an action with non-400 response

    val baseUrlOfSut = "http://localhost:8080"
//    val swaggerUrl = "http://localhost:8080/v2/api-docs"
    val swaggerUrl = "http://localhost:8080/api/v3/openapi.json"
//    val swaggerUrl ="../WFD_Dataset/openapi-swagger/youtube-mock.yaml"


    fun runTest() {
        runTestHandlingFlakyAndCompilation(
            "WFDTest",
            1000
        ) { args: MutableList<String> ->

            // Add black-box Swagger parameters
            args.add("--blackBox")
            args.add("true")

            args.add("--ratePerMinute")
            args.add("50000")

            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)

            args.add("--bbSwaggerUrl")
            args.add(swaggerUrl)

            args.add("--aiModelForResponseClassification")
            args.add(modelName)

            args.add("--aiClassifierRepairActivation")
            args.add(decisionMaking)

            args.add("--aiEncoderType")
            args.add(encoderType)

            args.add("--aiResponseClassifierWarmup")
            args.add(warmUpRep.toString())

            args.add("--maxRepairAttemptsInResponseClassification")
            args.add(maxAttemptRepair.toString())

            println("Running EvoMaster with arguments:")
            println(args.joinToString(" "))

            val (injector, solution) = initAndDebug(args)

            println("Run finished. Found ${solution.individuals.size} individuals")

            val model = injector.getInstance(AIResponseClassifier::class.java)

            val overAllMetrics = model.estimateOverallMetrics()
            println("Overall Accuracy: ${overAllMetrics.accuracy}")
            println("Overall Precision400: ${overAllMetrics.precision400}")
            println("Overall Recall400: ${overAllMetrics.recall400}")
            println("Overall F1Score400: ${overAllMetrics.f1Score400}")
            println("Overall MCC: ${overAllMetrics.mcc}")

        }
    }
}
