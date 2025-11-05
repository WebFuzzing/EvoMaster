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
        println("[LIPS DEBUG] initPopulation: initial per-target budget set using uncoveredSize=$initUncoveredSize -> budget=${budget.budgetLeftForCurrentTarget}")
    }

    override fun searchOnce() {
        beginGeneration()
        println("[LIPS DEBUG] searchOnce: ENTER")
        // record budget usage for this generation
        val startActions = time.evaluatedActions
        val startSeconds = time.getElapsedSeconds()

        // Compute uncovered goals
        val uncovered = archive.notCoveredTargets()
        println("[LIPS DEBUG] uncovered targets size = ${uncovered.size}")

        // current target is null if covered by previous generation or out of budget
        // Pick target if null, or if previously covered (check coverage directly)
        val needNewTarget = currentTarget == null || archive.isCovered(currentTarget!!)
        println("[LIPS DEBUG] needNewTarget=$needNewTarget currentTarget=${currentTarget}")
        if (needNewTarget) {
            println("[LIPS DEBUG] selecting new target from archive snapshot")
            val target = lastUncoveredBranchTargetId()
            currentTarget = target
            println("[LIPS DEBUG] currentTarget set to $currentTarget")
            // Initialize budget for this NEW target
            println("[LIPS DEBUG] calculating per-target budget with uncoveredSize=${uncovered.size}")
            budget.budgetLeftForCurrentTarget = budget.computePerTargetBudget(uncovered.size)
            println("[LIPS DEBUG] selectTarget: target=$target uncovered=${uncovered.size} budgetLeftForCurrentTarget=${budget.budgetLeftForCurrentTarget}")
        }

        // Focus scoring on the single selected target if present; otherwise use global fitness
        if (currentTarget == null) {
            frozenTargets = emptySet()
            println("[LIPS DEBUG] no currentTarget; using global fitness (frozenTargets empty)")
        } else {
            frozenTargets = setOf(currentTarget!!)
            println("[LIPS DEBUG] frozenTargets=${frozenTargets}")
        }

        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = formTheNextPopulation(population)
        println("[LIPS DEBUG] formed base nextPop with elites size=${nextPop.size} target=$currentTarget")

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
            if ((nextPop.size % 10) == 0 || usedForTarget >= budget.budgetLeftForCurrentTarget) {
                println("[LIPS DEBUG] innerLoop: nextSize=${nextPop.size} usedForTarget=$usedForTarget budgetLeft=${budget.budgetLeftForCurrentTarget}")
            }
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
        println("[LIPS DEBUG] afterGen: budgetLeftForCurrentTarget=${budget.budgetLeftForCurrentTarget}")

        // Switch target if covered or out of budget
        val coveredNow = population.any { score(it) >= 1.0 }
        val switching = budget.shouldSwitchTarget(coveredNow)
        println("[LIPS DEBUG] shouldSwitchTarget=$switching currentTarget=$currentTarget")
        if (switching) currentTarget = null

        endGeneration()
        println("[LIPS DEBUG] searchOnce: EXIT")
    }

    fun lastUncoveredBranchTargetId(): Int? {
        val snapshot = archive.getSnapshotOfBestIndividuals()
        if (snapshot.isEmpty()) return null

        // Iterate targets by numeric id in descending order
        val orderedIds = snapshot.keys.sortedDescending()
        println("[LIPS DEBUG] lastUncoveredBranch: snapshotSize=${snapshot.size} orderedIdsCount=${orderedIds.size}")

        for (targetId in orderedIds) {
            val description = archive.getIdMapper().getDescriptiveId(targetId)
            val isBranch = description.startsWith(ObjectiveNaming.BRANCH)
            val covered = archive.isCovered(targetId)
            println("[LIPS DEBUG] candidate targetId=$targetId desc=$description isBranch=$isBranch covered=$covered")
            if (isBranch) {
                if (!covered) {
                    println("[LIPS DEBUG] lastUncoveredBranch: selected targetId=$targetId")
                    return targetId
                }
            }
        }
        println("[LIPS DEBUG] lastUncoveredBranch: no candidate found")
        return null
    }
}


