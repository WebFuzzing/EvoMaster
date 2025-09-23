package org.evomaster.core.problem.rest.classifier

data class ModelEvaluation(
    val accuracy: Double,
    val precision400: Double,
    val recall400: Double,
    val f1Score400: Double,
    val mcc: Double
)

