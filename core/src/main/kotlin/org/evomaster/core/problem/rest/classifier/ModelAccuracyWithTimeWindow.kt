package org.evomaster.core.problem.rest.classifier

import com.google.common.collect.EvictingQueue
import org.evomaster.core.problem.rest.data.RestCallResult

class ModelAccuracyWithTimeWindow(
    bufferSize: Int
) : ModelAccuracy {

    private val queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val truePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)
    private val falsePositive400Queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    override fun estimateAccuracy(): Double {
        if(queue.isEmpty()) {
            return 0.0
        }

        val n = queue.size.toDouble()
        val ok = queue.sumOf { if(it) 1.0 else 0.0 }

        return ok / n
    }

    /**
     * Precision for HTTP 400 responses: TP / (TP + FP).
     */
    override fun estimatePrecision400(): Double {
        val tp = truePositive400Queue.count { it }   // count only true entries
        val fp = falsePositive400Queue.count { it }  // count only true entries
        return if ((tp + fp) > 0) tp.toDouble() / (tp + fp) else 0.0
    }

    /**
     * Update performance based on prediction vs actual result.
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int?) {

        val predictionWasCorrect = predictedStatusCode == actualStatusCode

        queue.add(predictionWasCorrect)

        if (actualStatusCode == 400) {
            truePositive400Queue.add(predictionWasCorrect)
        } else {
            falsePositive400Queue.add(!predictionWasCorrect)
        }
    }

}