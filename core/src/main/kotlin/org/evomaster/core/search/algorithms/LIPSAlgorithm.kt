package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.EMConfig
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

    @Inject
    private lateinit var idMapper: IdMapper

    private var currentTarget: Int? = null
    private lateinit var budget: LipsBudget

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
        // Initialize budget manager and first per-target budget using current uncovered targets
        budget = LipsBudget(config, time)
        val initUncoveredSize = archive.notCoveredTargets().size
        budget.budgetLeftForCurrentTarget = budget.computePerTargetBudget(initUncoveredSize)
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
            val target = lastUncoveredBranchTargetId()
            currentTarget = target
            // Initialize budget for this NEW target
            budget.budgetLeftForCurrentTarget = budget.computePerTargetBudget(uncovered.size)
        }

        // Focus scoring on the single selected target if present; otherwise use global fitness
        if (currentTarget == null) {
            frozenTargets = emptySet()
        } else {
            frozenTargets = setOf(currentTarget!!)
        }

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
            val usedForTarget = budget.usedForCurrentTarget(startActions, startSeconds)
            if (!time.shouldContinueSearch() || usedForTarget >= budget.budgetLeftForCurrentTarget) {
                endStep()
                break
            }
            endStep()
        }

        population.clear()
        population.addAll(nextPop)

        // Update budget usage for this target
        budget.updatePerTargetBudget(startActions, startSeconds)

        // Switch target if covered or out of budget
        val coveredNow = population.any { score(it) >= 1.0 }
        val switching = budget.shouldSwitchTarget(coveredNow)
        if (switching) currentTarget = null

        endGeneration()
    }

    fun lastUncoveredBranchTargetId(): Int? {
        val snapshot = archive.getSnapshotOfBestIndividuals()
        if (snapshot.isEmpty()) return null

        // Iterate targets by numeric id in descending order
        val orderedIds = snapshot.keys.sortedDescending()

        for (targetId in orderedIds) {
            val description = idMapper.getDescriptiveId(targetId)
            val isBranch = description.startsWith(ObjectiveNaming.BRANCH)
            val covered = archive.isCovered(targetId)
            if (isBranch) {
                if (!covered) {
                    return targetId
                }
            }
        }
        return null
    }
}


