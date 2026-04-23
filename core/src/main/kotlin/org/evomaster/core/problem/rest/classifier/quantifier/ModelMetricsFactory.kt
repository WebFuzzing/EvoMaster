package org.evomaster.core.problem.rest.classifier.quantifier

import org.evomaster.core.EMConfig

/**
 * Factory for creating [ModelMetrics] based on configuration.
 *
 * Centralizes the logic for selecting the appropriate metrics tracker
 * (time-window vs. full-history).
 */
fun createModelMetrics(
    metricType: EMConfig.AIClassificationMetrics,
    windowSize: Int = 500 // Window size below 500 may lead to noisy and unstable metrics
): ModelMetrics =
    when (metricType) {
        EMConfig.AIClassificationMetrics.TIME_WINDOW -> ModelMetricsWithTimeWindow(windowSize)
        EMConfig.AIClassificationMetrics.FULL_HISTORY -> ModelMetricsFullHistory()
    }
