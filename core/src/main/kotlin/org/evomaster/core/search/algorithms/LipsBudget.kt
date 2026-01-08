package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.SearchTimeController

/**
 * Encapsulates per-target budget accounting for LIPS.
 * It derives fair-share budgets from the global stopping criterion
 * and tracks the remaining budget for the current target.
 */
class LipsBudget(
    private val config: EMConfig,
    private val time: SearchTimeController
) {

    var budgetLeftForCurrentTarget: Int = 0

    fun computePerTargetBudget(uncoveredSize: Int): Int {
        return when (config.stoppingCriterion) {
            EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> {
                val remaining = (config.maxEvaluations - time.evaluatedActions).coerceAtLeast(0)
                if (uncoveredSize <= 0) remaining else remaining / uncoveredSize
            }
            EMConfig.StoppingCriterion.TIME -> {
                val remaining = (config.timeLimitInSeconds() - time.getElapsedSeconds()).coerceAtLeast(0)
                if (uncoveredSize <= 0) remaining else remaining / uncoveredSize
            }
            else -> Int.MAX_VALUE
        }
    }

    fun usedForCurrentTarget(startActions: Int, startSeconds: Int): Int {
        return when (config.stoppingCriterion) {
            EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> time.evaluatedActions - startActions
            EMConfig.StoppingCriterion.TIME -> time.getElapsedSeconds() - startSeconds
            else -> 0
        }
    }

    fun updatePerTargetBudget(actionsAtGenStart: Int, secondsAtGenStart: Int) {
        val used = usedForCurrentTarget(actionsAtGenStart, secondsAtGenStart)
        budgetLeftForCurrentTarget -= used
    }

    fun shouldSwitchTarget(coveredNow: Boolean): Boolean {
        val outOfBudget = budgetLeftForCurrentTarget <= 0
        return coveredNow || outOfBudget
    }
}


