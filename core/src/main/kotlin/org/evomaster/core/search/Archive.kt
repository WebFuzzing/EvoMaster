package org.evomaster.core.search


class Archive<T>(val randomness: Randomness) where T : Individual {

    /**
     * Key -> id of the target
     * <br>
     * Value -> sorted list of best individuals for that target
     * <br>
     */
    private val map = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()


    fun extractSolution() : Solution<T> {

        val overall = FitnessValue()
        val uniques = mutableSetOf<EvaluatedIndividual<T>>()

        map.entries.forEach { e ->
           if(isCovered(e.key)){
               uniques.add(e.value[0])
               overall.coverTarget(e.key)
           }
        }

        return Solution(overall, uniques.toMutableList())
    }

    fun sampleIndividual() : T {

        if(map.isEmpty()){
            throw IllegalStateException("Empty archive")
        }

        var toChooseFrom = map.keys.filter { k -> ! isCovered(k) }
        if(toChooseFrom.isEmpty()){
            //this means all current targets are covered
            toChooseFrom = map.keys.toList()
        }

        val chosenTarget = randomness.choose(toChooseFrom)
        val candidates = map[chosenTarget] ?: emptyList<EvaluatedIndividual<T>>()
        assert(candidates.size > 0)

        val chosen = randomness.choose(candidates)

        return chosen.individual.copy() as T
    }


    fun addIfNeeded(ei: EvaluatedIndividual<T>): Boolean {

        val copy = ei.copy()
        var added = false

        for ((k, v) in ei.fitness.getViewOfData()) {

            if(v == 0.0){
                /*
                    No point adding an individual with no impact
                    on a given target
                 */
                continue
            }

            val current = map.getOrPut(k, {mutableListOf()})

            if (current.isEmpty()) {
                /*
                    ind covers a new target
                 */
                current.add(copy)
                added = true
                continue
            }

            if (isCovered(k)) {
                /*
                    Target is already covered. But could it
                    be that new individual covers it as well,
                    and it is better?
                 */
                if (FitnessValue.isMaxValue(v) &&
                        copy.fitness.computeFitnessScore() >
                                current[0].fitness.computeFitnessScore()
                ) {
                    current[0] = copy
                    added = true
                }
                continue
            }

            if(FitnessValue.isMaxValue(v)){
                current.clear() //remove all existing non-optimal solutions
                current.add(copy)
                added = true
                continue
            }

            val limit = 10 //TODO config
            if(current.size == limit){
                current.sortBy { c -> c.fitness.getHeuristic(k) }
                current.removeAt(0)
            }
            current.add(copy)
            added = true
        }

        return added
    }

    private fun isCovered(target: Int): Boolean {

        val current = map.getOrPut(target, {mutableListOf()})
        if (current.size != 1) {
            return false
        }

        return current[0].fitness.doesCover(target)
    }
}