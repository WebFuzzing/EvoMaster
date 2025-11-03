package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.FitnessValue
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
        println("[LIPS DEBUG] initPopulation: enter")
        population.clear()
        println("[LIPS DEBUG] initPopulation: population cleared")

        // 1) Generate Random Individual
        println("[LIPS DEBUG] initPopulation: sampling initial individual")
        val i = sampleSuite()
        println("[LIPS DEBUG] initPopulation: sampled initial individual")

        // 2) Compute UB directly from the sampled individual `i`
        //    Merge suite fitness and consider targets with score < 1.0 as uncovered
        val view = if (i.suite.isNotEmpty()) {
            val fv = i.suite.first().fitness.copy()
            i.suite.forEach { ei -> fv.merge(ei.fitness) }
            fv.getViewOfData()
        } else emptyMap()

        val uncovered = view.filterValues { it.score < FitnessValue.MAX_VALUE }.keys.toList()
        println("[LIPS DEBUG] initPopulation: derived uncoveredSize=${uncovered.size} from initial individual")

        // 3) Select current target b
        if (uncovered.isNotEmpty()) {
            val target = uncovered.last()
            currentTarget = target
            frozenTargets = setOf(target)
            println("[LIPS DEBUG] initPopulation: selected target=$target, frozenTargets set")
        } else {
            currentTarget = null
            frozenTargets = emptySet()
            println("[LIPS DEBUG] initPopulation: no uncovered targets in initial individual; frozenTargets cleared")
        }

        // 4) initialize budget for this target
        budgetLeftForCurrentTarget = calculatePerTargetBudget(maxOf(1, uncovered.size))
        println("[LIPS DEBUG] init: target=${currentTarget} uncovered=${uncovered.size} budgetLeftForCurrentTarget=$budgetLeftForCurrentTarget")

        // 5) P <- RandomPopulation(ps-1) ∪ {i}
        population.add(i)
        println("[LIPS DEBUG] initPopulation: added initial individual, pop=${population.size}")
        while (population.size < config.populationSize) {
            population.add(sampleSuite())
            if (population.size % 5 == 0 || population.size == config.populationSize) {
                println("[LIPS DEBUG] initPopulation: growing pop=${population.size}/${config.populationSize}")
            }
        }
        println("[LIPS DEBUG] initPopulation: exit with pop=${population.size}")
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
            println("[LIPS DEBUG] selectTarget: target=$target uncovered=${uncovered.size} budgetLeftForCurrentTarget=$budgetLeftForCurrentTarget")
        }

        // Focus scoring on the single selected target
        frozenTargets = setOf(currentTarget!!)

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
        println("[LIPS DEBUG] afterGen: target=${currentTarget} used=$usedForTarget budgetLeftForCurrentTarget=$budgetLeftForCurrentTarget")

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


