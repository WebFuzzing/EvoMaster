package org.evomaster.core.problem.rest.classifier

import com.google.common.collect.EvictingQueue
import kotlin.math.sqrt

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


    /** Model Accuracy. See [ModelMetrics.estimateAccuracy] */
    override fun estimateAccuracy(): Double {
        if (queue.isEmpty()) return 0.0
        val n = queue.size.toDouble()
        val ok = queue.count { it }.toDouble()
        return ok / n
    }

    /** Precision 400. See [ModelMetrics.estimatePrecision400] */
    override fun estimatePrecision400(): Double {
        val tp = truePositive400Queue.count { it }
        val fp = falsePositive400Queue.count { it }
        return if ((tp + fp) > 0) tp.toDouble() / (tp + fp) else 0.0
    }


    /** Recall 400. See [ModelMetrics.estimateRecall400] */
    override fun estimateRecall400(): Double {
        val tp = truePositive400Queue.count { it }
        val fn = falseNegative400Queue.count { it }
        return if ((tp + fn) > 0) tp.toDouble() / (tp + fn) else 0.0
    }


    /** Matthews Correlation Coefficient (MCC). See [ModelMetrics.estimateMCC400] */
    override fun estimateMCC400(): Double {
        val tp = truePositive400Queue.count { it }.toDouble()
        val tn = trueNegative400Queue.count { it }.toDouble()
        val fp = falsePositive400Queue.count { it }.toDouble()
        val fn = falseNegative400Queue.count { it }.toDouble()

        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) (tp * tn - fp * fn) / denominator else 0.0
    }

    /**
     * Update the rolling window of performance counters
     * after a new prediction and its actual outcome.
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {
        val predictionWasCorrect = predictedStatusCode == actualStatusCode
        queue.add(predictionWasCorrect)


        val isTP = (actualStatusCode == 400 && predictedStatusCode == 400) //True Positive
        val isFN = (actualStatusCode == 400 && predictedStatusCode != 400) //False Negative
        val isFP = (actualStatusCode != 400 && predictedStatusCode == 400) //False Positive
        val isTN = (actualStatusCode != 400 && predictedStatusCode != 400) // True Negative

        truePositive400Queue.add(isTP)
        falseNegative400Queue.add(isFN)
        falsePositive400Queue.add(isFP)
        trueNegative400Queue.add(isTN)

    }
}
