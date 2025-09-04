package org.evomaster.core.problem.rest.classifier

import com.google.common.collect.EvictingQueue

class ModelAccuracyWithTimeWindow(
    bufferSize: Int
) : ModelAccuracy {

    private val queue: EvictingQueue<Boolean> = EvictingQueue.create(bufferSize)

    override fun estimateAccuracy(): Double {
        if(queue.isEmpty()) {
            return 0.0
        }

        val n = queue.size.toDouble()
        val ok = queue.sumOf { if(it) 1.0 else 0.0 }

        return ok / n
    }

    override fun updatePerformance(predictionWasCorrect: Boolean) {
        queue.add(predictionWasCorrect)
    }
}