package org.evomaster.core.problem.rest.classifier.quantifier

import com.google.common.collect.EvictingQueue

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
    bufferSize: Int = 100
) : ModelMetrics {

    /** Lifetime counter of total evaluated requests. Important for warm-up logic. */
    override var totalSentRequests: Int = 0
        private set

    // Sliding window queues
    private val truePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falsePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falseNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val trueNegative400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    // Window-based confusion-matrix metrics
    override val truePositive400 get() = truePositive400Queue.count { it }
    override val falsePositive400 get() = falsePositive400Queue.count { it }
    override val falseNegative400 get() = falseNegative400Queue.count { it }
    override val trueNegative400 get() = trueNegative400Queue.count { it }

    /**
     * Update both lifetime counter and window-based queues.
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {

        val isTP = (actualStatusCode == 400 && predictedStatusCode == 400)
        val isFN = (actualStatusCode == 400 && predictedStatusCode != 400)
        val isFP = (actualStatusCode != 400 && predictedStatusCode == 400)
        val isTN = (actualStatusCode != 400 && predictedStatusCode != 400)

        truePositive400Queue.add(isTP)
        falseNegative400Queue.add(isFN)
        falsePositive400Queue.add(isFP)
        trueNegative400Queue.add(isTN)

        totalSentRequests++
    }
}