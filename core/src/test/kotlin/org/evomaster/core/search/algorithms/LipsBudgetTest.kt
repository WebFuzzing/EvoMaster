package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.service.SearchTimeController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LipsBudgetTest {

    @Test
    fun computePerTargetBudget_Actions_FairShare() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 100
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        // remaining = 100 - 0 = 100; uncovered=5 -> 20 each
        val perTarget = budget.computePerTargetBudget(uncoveredSize = 5)
        assertEquals(20, perTarget)

        // simulate usage: evaluatedActions=30 -> remaining=70 ; uncovered=7 -> 10 each
        time.newActionEvaluation(30)
        val perTargetAfterUsage = budget.computePerTargetBudget(uncoveredSize = 7)
        assertEquals(10, perTargetAfterUsage)
    }

    @Test
    fun computePerTargetBudget_Time_FairShare() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.TIME
            maxTimeInSeconds = 120
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        // remaining seconds = 120; uncovered=4 -> 30 each
        val perTarget = budget.computePerTargetBudget(uncoveredSize = 4)
        assertEquals(30, perTarget)
    }

    @Test
    fun computePerTargetBudget_Actions_ZeroUncovered_ReturnsRemaining() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 50
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        time.newActionEvaluation(5) // remaining = 45
        val perTarget = budget.computePerTargetBudget(uncoveredSize = 0)
        assertEquals(45, perTarget)
    }

    @Test
    fun computePerTargetBudget_Time_ZeroUncovered_UsesRemainingSeconds() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.TIME
            maxTimeInSeconds = 120
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        // If search never started, elapsed=0 -> remaining = maxTimeInSeconds
        val perTarget = budget.computePerTargetBudget(uncoveredSize = 0)
        assertEquals(120, perTarget)
    }

    @Test
    fun updateAndSwitchBudget() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.ACTION_EVALUATIONS
            maxEvaluations = 100
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        budget.budgetLeftForCurrentTarget = 15

        // usedForCurrentTarget = (current - start) -> 10
        val startActions = time.evaluatedActions
        time.newActionEvaluation(10)
        budget.updatePerTargetBudget(startActions, secondsAtGenStart = 0)
        assertEquals(5, budget.budgetLeftForCurrentTarget)

        // should switch once out of budget
        val shouldNotSwitch = budget.shouldSwitchTarget(coveredNow = false)
        assertTrue(!shouldNotSwitch)

        // spend remaining
        val start2 = time.evaluatedActions
        time.newActionEvaluation(5)
        budget.updatePerTargetBudget(start2, secondsAtGenStart = 0)
        assertEquals(0, budget.budgetLeftForCurrentTarget)

        val shouldSwitch = budget.shouldSwitchTarget(coveredNow = false)
        assertTrue(shouldSwitch)
    }

    @Test
    fun updateAndSwitchBudget_Time() {
        val config = EMConfig().apply {
            stoppingCriterion = EMConfig.StoppingCriterion.TIME
            maxTimeInSeconds = 120
        }
        val time = SearchTimeController()
        val budget = LipsBudget(config, time)

        // initialize per-target remaining seconds and start the timer
        budget.budgetLeftForCurrentTarget = 2
        time.startSearch()

        val startSeconds = time.getElapsedSeconds()
        Thread.sleep(1100)
        budget.updatePerTargetBudget(actionsAtGenStart = 0, secondsAtGenStart = startSeconds)
        assertEquals(1, budget.budgetLeftForCurrentTarget)

        val startSeconds2 = time.getElapsedSeconds()
        Thread.sleep(1100)
        budget.updatePerTargetBudget(actionsAtGenStart = 0, secondsAtGenStart = startSeconds2)
        assertEquals(0, budget.budgetLeftForCurrentTarget)

        val shouldSwitch = budget.shouldSwitchTarget(coveredNow = false)
        assertTrue(shouldSwitch)
    }
}


