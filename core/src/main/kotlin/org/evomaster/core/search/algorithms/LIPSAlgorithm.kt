package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * LIPS (Linearly Independent Path-based Search).
 * Single-objective GA that optimizes one target (branch) at a time.
 *
 * - Picks a current target from the uncovered set.
 * - Runs one GA generation to evolve the population towards the current target.
 * - when current target is covered or its budget is spent, select a new current target.
 */
class LIPSAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    private var currentTarget: Int? = null
    private var budgetLeftForCurrentTarget: Int = 0

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.LIPS

    override fun initPopulation() {
        population.clear()

        // 1) Generate Random Individual
        val i = sampleSuite()

        // 2) Get Uncovered Targets
        val uncovered = archive.notCoveredTargets()
        if (uncovered.isEmpty()) {
            return
        }

        // 3) Select current target b
        val target = uncovered.last()
        currentTarget = target
        frozenTargets = setOf(target)

        // 4) initialize budget for this target
        budgetLeftForCurrentTarget = calculatePerTargetBudget(uncovered.size)

        // 5) P <- RandomPopulation(ps-1) ∪ {i}
        population.add(i)
        while (population.size < config.populationSize) {
            population.add(sampleSuite())
        }
    }

    override fun searchOnce() {
        beginGeneration()

        // Compute uncovered goals
        val uncovered = archive.notCoveredTargets()
        if (uncovered.isEmpty()) {
            endGeneration()
            return
        }

        // current target is null if covered by previous generation or out of budget

        // Pick target if null, or if previously covered 
        if (currentTarget == null || !uncovered.contains(currentTarget)) {
            val target = uncovered.last()
            currentTarget = target
            // Initialize budget for this NEW target
            budgetLeftForCurrentTarget = calculatePerTargetBudget(uncovered.size)
        }

        // Focus scoring on the single selected target
        frozenTargets = setOf(currentTarget)

        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = formTheNextPopulation(population)

        // record budget usage for this generation
        val startActions = time.evaluatedActions
        val startSeconds = time.getElapsedSeconds()

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
        val usedForTarget = usedForCurrentTarget(startActions, startSeconds)
        budgetLeftForCurrentTarget -= usedForTarget

        // Check if target is covered or out of budget
        val coveredNow = population.any { score(it) >= 1.0 }
        val outOfBudget = budgetLeftForCurrentTarget <= 0
        
        if (coveredNow || outOfBudget) {
            currentTarget = null
        }

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
}


