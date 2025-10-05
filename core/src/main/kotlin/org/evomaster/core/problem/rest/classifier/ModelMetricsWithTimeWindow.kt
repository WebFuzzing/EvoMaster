package org.evomaster.core.problem.rest.classifier

import com.google.common.collect.EvictingQueue
import kotlin.math.sqrt

/**
 * Tracks model performance metrics within a fixed-size sliding time window,
 * also keeps a lifetime counter of total requests as (`totalSentRequests`).
 *
 * Uses Guava's [EvictingQueue] to keep the most recent predictions only,
 * automatically discarding the oldest ones when the buffer is full.
 *
 * Maintains confusion-matrix style queues for the window:
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

    // Sliding window queues
    private val truePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falsePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falseNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val trueNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    // Lifetime counter
    var totalSentRequests: Int = 0
        private set

    // Window-based metrics
    val truePositive400 get() = truePositive400Queue.count { it }
    val falsePositive400 get() = falsePositive400Queue.count { it }
    val falseNegative400 get() = falseNegative400Queue.count { it }
    val trueNegative400 get() = trueNegative400Queue.count { it }

    val correctPredictions get() = truePositive400 + trueNegative400

    /** Model Accuracy in the window. See [ModelMetrics.estimateAccuracy]*/
    override fun estimateAccuracy(): Double {
        return if (totalSentRequests > 0) {
            correctPredictions.toDouble() / (truePositive400 + falsePositive400 + falseNegative400 + trueNegative400)
        } else 0.0
    }

    /** Precision 400. See [ModelMetrics.estimatePrecision400] */
    override fun estimatePrecision400(): Double {
        val denominator = truePositive400 + falsePositive400
        return if (denominator > 0) truePositive400.toDouble() / denominator else 0.0
    }

    /** Recall 400. See [ModelMetrics.estimateRecall400] */
    override fun estimateRecall400(): Double {
        val denominator = truePositive400 + falseNegative400
        return if (denominator > 0) truePositive400.toDouble() / denominator else 0.0
    }

    /** Matthews Correlation Coefficient (MCC). See [ModelMetrics.estimateMCC400] */
    override fun estimateMCC400(): Double {
        val tp = truePositive400.toDouble()
        val tn = trueNegative400.toDouble()
        val fp = falsePositive400.toDouble()
        val fn = falseNegative400.toDouble()

        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) (tp * tn - fp * fn) / denominator else 0.0
    }

    /** Update counters for both the lifetime and the sliding window. */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {
        totalSentRequests++

        val isTP = (actualStatusCode == 400 && predictedStatusCode == 400)
        val isFN = (actualStatusCode == 400 && predictedStatusCode != 400)
        val isFP = (actualStatusCode != 400 && predictedStatusCode == 400)
        val isTN = (actualStatusCode != 400 && predictedStatusCode != 400)

        truePositive400Queue.add(isTP)
        falseNegative400Queue.add(isFN)
        falsePositive400Queue.add(isFP)
        trueNegative400Queue.add(isTN)
    }
}
