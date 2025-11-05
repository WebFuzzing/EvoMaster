package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.search.service.IdMapper

/**
 * Linearly Independent Path-based Search (LIPS).
 *
 * A single-objective GA that optimizes one branch target at a time.
 *
 * - Initializes a random individual i and build the initial population P = random ∪ {i}.
 * - Maintains a current branch target.
 * - Per-target budget is a fair share of the global TIME/ACTIONS budget; switches target when the target is covered or its budget is exhausted.
 */


class LIPSAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    private var currentTarget: Int? = null
    private var budgetLeftForCurrentTarget: Int = 0

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.LIPS

    override fun initPopulation() {
        population.clear()
        // 1) Generate Random Individual
        val i = sampleSuite()
        // 2) P <- RandomPopulation(ps-1) ∪ {i}
        population.add(i)
        while (population.size < config.populationSize) {
            population.add(sampleSuite())
        }
    }

    override fun searchOnce() {
        beginGeneration()
        // record budget usage for this generation
        val startActions = time.evaluatedActions
        val startSeconds = time.getElapsedSeconds()

        // Compute uncovered goals
        val uncovered = archive.notCoveredTargets()

        // current target is null if covered by previous generation or out of budget
        // Pick target if null, or if previously covered (check coverage directly)
        val needNewTarget = currentTarget == null || archive.isCovered(currentTarget!!)
        if (needNewTarget) {
            val target = firstUncoveredBranch()
            currentTarget = target
            // Initialize budget for this NEW target
            budgetLeftForCurrentTarget = calculatePerTargetBudget(uncovered.size)
        }

        // Focus scoring on the single selected target
        frozenTargets = setOf(currentTarget!!)

        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = formTheNextPopulation(population)

        while (nextPop.size < n) {
            beginStep()

            val p1 = tournamentSelection()
            val p2 = tournamentSelection()

            val o1 = p1.copy()
            val o2 = p2.copy()

            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(o1, o2)
            }
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o1)
            }
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o2)
            }

            nextPop.add(o1)
            nextPop.add(o2)

            // Stop if global budget or target budget is up
            val usedForTarget = usedForCurrentTarget(startActions, startSeconds)
            
            if (!time.shouldContinueSearch() || usedForTarget >= budgetLeftForCurrentTarget) {
                endStep()
                break
            }
            endStep()
        }

        population.clear()
        population.addAll(nextPop)

        // Update budget usage for this target
        updatePerTargetBudget(startActions, startSeconds)

        // Switch target if covered or out of budget
        if (shouldSwitchTarget()) currentTarget = null

        endGeneration()
    }

    /**
     * Calculate per-target budget based on the current stopping criterion.
     * Returns the fair share: total budget divided by number of uncovered targets.
     */
    private fun calculatePerTargetBudget(uncoveredSize: Int): Int {
        return when (config.stoppingCriterion) {
            EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> {
                val remaining = (config.maxEvaluations - time.evaluatedActions).coerceAtLeast(0)
                remaining / uncoveredSize
            }
            EMConfig.StoppingCriterion.TIME -> {
                val remaining = (config.timeLimitInSeconds() - time.getElapsedSeconds()).coerceAtLeast(0)
                remaining / uncoveredSize
            }
            else -> Int.MAX_VALUE
        }
    }

    /**
     * Compute the budget spent since the start of the current generation,
     * based on the active stopping criterion.
     */
    private fun usedForCurrentTarget(startActions: Int, startSeconds: Int): Int {
        return when (config.stoppingCriterion) {
            EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> time.evaluatedActions - startActions
            EMConfig.StoppingCriterion.TIME -> time.getElapsedSeconds() - startSeconds
            else -> 0
        }
    }

    private fun updatePerTargetBudget(actionsAtGenStart: Int, secondsAtGenStart: Int) {
        val usedForTarget = usedForCurrentTarget(actionsAtGenStart, secondsAtGenStart)
        budgetLeftForCurrentTarget -= usedForTarget
    }

    private fun shouldSwitchTarget(): Boolean {
        val coveredNow = population.any { score(it) >= 1.0 }
        val outOfBudget = budgetLeftForCurrentTarget <= 0
        return coveredNow || outOfBudget
    }

    fun firstUncoveredBranch(): Int? {
        if (populations.isEmpty()) return null

        // Iterate targets by numeric id in descending order
        val orderedIds = populations.keys.sortedDescending()

        for (targetId in orderedIds) {
            val description = archive.getIdMapper().getDescriptiveId(targetId)
            if (description.startsWith(ObjectiveNaming.BRANCH)) {
                if (!isCovered(targetId)) {
                    return targetId
                }
            }
        }
        return null
    }
}


