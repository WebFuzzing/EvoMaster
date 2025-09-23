package org.evomaster.core.problem.rest.classifier

import com.google.common.collect.EvictingQueue

/**
 * Tracks model performance metrics within a fixed-size sliding time window.
 *
 * Uses Guava's [EvictingQueue] to keep the most recent predictions only,
 * automatically discarding the oldest ones when the buffer is full.
 *
 * Maintains confusion-matrix style queues:
 * - TP (True Positives): predicted 400 and actual 400
 * - FP (False Positives): predicted 400 but actual not-400
 * - FN (False Negatives): predicted not-400 but actual 400
 * - TN (True Negatives): predicted not-400 and actual not-400
 *
 * Metrics are estimated over this rolling buffer, providing a
 * short-term view of performance rather than lifetime history.
 */
class ModelMetricsWithTimeWindow(
    bufferSize: Int
) : ModelMetrics {

    private val queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    private val truePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falsePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falseNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val trueNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    /**
     * Accuracy = (TP + TN) / (TP + TN + FP + FN)
     */
    override fun estimateAccuracy(): Double {
        if (queue.isEmpty()) return 0.0
        val n = queue.size.toDouble()
        val ok = queue.count { it }.toDouble()
        return ok / n
    }

    /**
     * Precision(400) = TP / (TP + FP)
     */
    override fun estimatePrecision400(): Double {
        val tp = truePositive400Queue.count { it }
        val fp = falsePositive400Queue.count { it }
        return if ((tp + fp) > 0) tp.toDouble() / (tp + fp) else 0.0
    }

    /**
     * Recall(400) = TP / (TP + FN)
     */
    override fun estimateRecall400(): Double {
        val tp = truePositive400Queue.count { it }
        val fn = falseNegative400Queue.count { it }
        return if ((tp + fn) > 0) tp.toDouble() / (tp + fn) else 0.0
    }

    /**
     * F1(400) = 2 * (Precision * Recall) / (Precision + Recall)
     */
    override fun estimateF1Score400(): Double {
        val p = estimatePrecision400()
        val r = estimateRecall400()
        return if (p + r > 0.0) 2 * (p * r) / (p + r) else 0.0
    }

    /**
     * MCC(400) = (TP*TN - FP*FN) /
     *            sqrt((TP+FP)(TP+FN)(TN+FP)(TN+FN))
     */
    override fun estimateMCC400(): Double {
        val tp = truePositive400Queue.count { it }.toDouble()
        val tn = trueNegative400Queue.count { it }.toDouble()
        val fp = falsePositive400Queue.count { it }.toDouble()
        val fn = falseNegative400Queue.count { it }.toDouble()

        val denominator = Math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) (tp * tn - fp * fn) / denominator else 0.0
    }

    /**
     * Unified metrics estimate packaged into a [ModelEvaluation].
     */
    override fun estimateMetrics(): ModelEvaluation {
        return ModelEvaluation(
            accuracy = estimateAccuracy(),
            precision400 = estimatePrecision400(),
            recall400 = estimateRecall400(),
            f1Score400 = estimateF1Score400(),
            mcc = estimateMCC400()
        )
    }

    /**
     * Update the rolling window of performance counters
     * after a new prediction and its actual outcome.
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {
        val predictionWasCorrect = predictedStatusCode == actualStatusCode
        queue.add(predictionWasCorrect)

        when {
            actualStatusCode == 400 && predictedStatusCode == 400 -> truePositive400Queue.add(true)
            actualStatusCode == 400 && predictedStatusCode != 400 -> falseNegative400Queue.add(true)
            actualStatusCode != 400 && predictedStatusCode == 400 -> falsePositive400Queue.add(true)
            actualStatusCode != 400 && predictedStatusCode != 400 -> trueNegative400Queue.add(true)
        }
    }
}
