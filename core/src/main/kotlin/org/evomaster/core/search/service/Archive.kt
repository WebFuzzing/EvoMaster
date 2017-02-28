package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy


class Archive<T>() where T : Individual {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var apc: AdaptiveParameterControl

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject
    private lateinit var time: SearchTimeController

    /**
     * Key -> id of the target
     * <br>
     *
     * Value -> sorted list of best individuals for that target
     * <br>
     */
    private val map = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()


    fun extractSolution(): Solution<T> {

        val overall = FitnessValue()
        val uniques = mutableSetOf<EvaluatedIndividual<T>>()

        map.entries.forEach { e ->
            if (isCovered(e.key)) {
                uniques.add(e.value[0])
                overall.coverTarget(e.key)
            }
        }

        return Solution(overall, uniques.toMutableList())
    }


    fun isEmpty() = map.isEmpty()

    fun sampleIndividual(): EvaluatedIndividual<T> {

        if (isEmpty()) {
            throw IllegalStateException("Empty archive")
        }

        var toChooseFrom = map.keys.filter { k -> !isCovered(k) }
        if (toChooseFrom.isEmpty()) {
            //this means all current targets are covered
            toChooseFrom = map.keys.toList()
        }

        val chosenTarget = randomness.choose(toChooseFrom)
        val candidates = map[chosenTarget] ?:
            //should never happen, unless of bug
            throw IllegalStateException("Target $chosenTarget has no candidate individual")

        sortAndShrinkIfNeeded(candidates, chosenTarget)

        val chosen = randomness.choose(candidates)

        return chosen.copy()
    }

    /**
     * Useful for debugging
     */
    fun encounteredTargetDescriptions(): List<String> {

        return map.entries
                .map { e -> "key ${e.key}: ${idMapper.getDescriptiveId(e.key)} , size=${e.value.size}" }
                .sorted()
    }

    /**
     * Get all known targets that are not fully covered
     *
     * @return a list of ids
     */
    fun notCoveredTargets(): Set<Int> {

        return map.keys.filter { k -> !isCovered(k) }.toSet()
    }

    /**
     * @return true if the new individual was added to the archive
     */
    fun addIfNeeded(ei: EvaluatedIndividual<T>): Boolean {

        val copy = ei.copy()
        var added = false

        for ((k, v) in ei.fitness.getViewOfData()) {

            if (v == 0.0) {
                /*
                    No point adding an individual with no impact
                    on a given target
                 */
                continue
            }

            val current = map.getOrPut(k, { mutableListOf() })

            //ind does reach a new target?
            if (current.isEmpty()) {
                current.add(copy)
                added = true

                if (isCovered(k)) {
                    time.newCoveredTarget()
                }

                continue
            }

            val maxed = FitnessValue.isMaxValue(v)

            if (isCovered(k) && maxed) {
                /*
                    Target is already covered. But could it
                    be that new individual covers it as well,
                    and it is better?

                    Recall: during the search, the fitness score could be
                    partial, so this check on collateral coverage likely
                    will not be so effective
                 */
                assert(current.size == 1) //if covered, should keep only one solution in buffer

                val shorter = copy.individual.size() < current[0].individual.size()
                val sameLengthButBetterScore = (copy.individual.size() == current[0].individual.size())
                        && (copy.fitness.computeFitnessScore() > current[0].fitness.computeFitnessScore())

                /*
                 * Once a target is covered, we check if can cover it with a new test that is shorter.
                 * Given two tests covering the same target, both with same length, then we prefer
                 * the one that has most collateral coverage
                 */
                if (shorter || sameLengthButBetterScore) {
                    current[0] = copy
                    added = true
                }
                continue
            }

            if (maxed) {
                current.clear() //remove all existing non-optimal solutions
                current.add(copy)
                added = true
                time.newCoveredTarget()
                continue
            }


            //handle regular case.
            sortAndShrinkIfNeeded(current, k)

            val limit = apc.getArchiveTargetLimit()

            if (current.size < limit) {
                //we have space in the buffer, regardless of fitness
                current.add(copy)
                added = true
                continue
            }

            val currh = current[0].fitness.getHeuristic(k)
            val currsize = current[0].individual.size()
            val copySize = copy.individual.size()

            if (v >= currh || (v == currh && copySize <= currsize)) {
                // replace worst element, if copy is not worse than it (but not necessarily better)
                current[0] = copy
                added = true
            }
        }

        return added
    }

    /*
       Ascending sort based on heuristics and, if same value, on negation of size.
       Worst element will be the first, best the last.
       Resize the list if needed
     */
    private fun sortAndShrinkIfNeeded(list: MutableList<EvaluatedIndividual<T>>, target: Int) {

        list.sortWith(compareBy<EvaluatedIndividual<T>>
        { it.fitness.getHeuristic(target) }.thenBy { -it.individual.size() })

        val limit = apc.getArchiveTargetLimit()
        while (list.size > limit) {
            //remove worst, ie the one with lowest heuristic value
            list.removeAt(0)
        }
    }

    fun isCovered(target: Int): Boolean {

        val current = map[target] ?: return false

        if (current.size != 1) {
            return false
        }

        return current[0].fitness.doesCover(target)
    }
}