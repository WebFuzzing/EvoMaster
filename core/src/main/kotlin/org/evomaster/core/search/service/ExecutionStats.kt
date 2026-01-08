package org.evomaster.core.search.service

import java.util.concurrent.ConcurrentHashMap


/**
 * Holds statistics for a single action:
 * min, max, sum, count → mean = sum / count
 */
data class ActionStats(
    var min: Long = Long.MAX_VALUE,
    var max: Long = Long.MIN_VALUE,
    var sum: Long = 0,
    var count: Long = 0
) {
    fun mean(): Long = if (count == 0L) 0L else sum / count
}

class ExecutionStats {
    private val stats = ConcurrentHashMap<String, ActionStats>()

    /**
     * Record an execution time for an action.
     */
    @Synchronized
    fun record(actionName: String, durationMs: Long) {
        val s = stats.computeIfAbsent(actionName) { ActionStats() }

        // update stats
        s.min = kotlin.math.min(s.min, durationMs)
        s.max = kotlin.math.max(s.max, durationMs)
        s.sum += durationMs
        s.count += 1
    }

    /**
     * Read computed baseline for an action.
     */
    fun getStats(actionName: String): ActionStats? = stats[actionName]

    /**
     * Clear all stats (e.g. before a new run)
     */
    fun reset() = stats.clear()
}
