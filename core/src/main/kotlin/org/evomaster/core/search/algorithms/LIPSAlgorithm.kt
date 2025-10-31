package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * LIPS (Linearly Independent Path-based Search).
 * Single-objective GA that optimizes one target (branch) at a time.
 *
 * Simplified adaptation:
 * - Picks a current target from the uncovered set.
 * - Freezes scoring on that target only.
 * - Runs one GA generation to evolve the population towards the current target.
 * - Adds discovered individuals to archive; when targets are covered, picks a new target.
 */
class LIPSAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    private var currentTarget: Int? = null
    private val budgetLeftPerTarget: MutableMap<Int, Int> = mutableMapOf()

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.LIPS

    override fun initPopulation() {
        population.clear()

        // 1) Generate i
        val i = sampleSuite()

        // 2) Compute uncovered 
        val uncovered = archive.notCoveredTargets()
        if (uncovered.isEmpty()) {
            return
        }

        // 3) Select current target b
        val target = uncovered.last()
        currentTarget = target
        frozenTargets = setOf(target)

        // 4) initialize per-target budget
        val perTarget = calculatePerTargetBudget(uncovered.size)
        budgetLeftPerTarget.putIfAbsent(target, perTarget)

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

        // Pick target if none, or if previously covered
        if (currentTarget == null || !uncovered.contains(currentTarget)) {
            currentTarget = uncovered.last()
        }

        // Focus scoring on the single selected target
        frozenTargets = setOf(currentTarget)

        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = formTheNextPopulation(population)

        // record budget usage for this generation
        val startActions = time.evaluatedActions
        val startSeconds = time.getElapsedSeconds()
        
        val t = currentTarget!!
        val targetBudgetLeft = budgetLeftPerTarget.getOrPut(t) {
            calculatePerTargetBudget(uncovered.size)
        }

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

            // Keep best wrt frozen target
            val a = if (score(o1) >= score(o2)) o1 else o2
            nextPop.add(a)

            // Stop if global budget or target budget is up
            val usedForTarget = when (config.stoppingCriterion) {
                EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> time.evaluatedActions - startActions
                EMConfig.StoppingCriterion.TIME -> time.getElapsedSeconds() - startSeconds
                else -> 0
            }
            
            if (!time.shouldContinueSearch() || usedForTarget >= targetBudgetLeft) {
                endStep()
                break
            }
            endStep()
        }

        population.clear()
        population.addAll(nextPop)

        // Update budget usage for this target
        currentTarget?.let { target ->
            val usedForTarget = when (config.stoppingCriterion) {
                EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> time.evaluatedActions - startActions
                EMConfig.StoppingCriterion.TIME -> time.getElapsedSeconds() - startSeconds
                else -> 0
            }
            budgetLeftPerTarget[target] = (budgetLeftPerTarget[target] ?: 0) - usedForTarget
        }

        // Check if target is covered or out of budget
        val coveredNow = population.any { score(it) >= 1.0 }
        val outOfBudget = currentTarget?.let { (budgetLeftPerTarget[it] ?: 1) <= 0 } ?: false
        
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
            EMConfig.StoppingCriterion.ACTION_EVALUATIONS -> 
                config.maxEvaluations / uncoveredSize
            EMConfig.StoppingCriterion.TIME -> 
                config.timeLimitInSeconds() / uncoveredSize
            else -> Int.MAX_VALUE
        }
    }
}


