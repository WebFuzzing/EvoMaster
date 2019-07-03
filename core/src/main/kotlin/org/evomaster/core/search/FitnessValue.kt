package org.evomaster.core.search

import org.evomaster.core.database.DatabaseExecution

/**
As the number of targets is unknown, we cannot have
a minimization problem, as new targets could be added
throughout the search
 */
class FitnessValue(
        /** An estimation of the size of the individual that obtained
         * this fitness value. Longer individuals are worse, but only
         * when fitness is not strictly better */
        var size: Double) {

    init {
        if (size < 0.0) {
            throw IllegalArgumentException("Invalid size value: $size")
        }
    }

    companion object {

        const val MAX_VALUE = 1.0

        fun isMaxValue(value: Double) = value == MAX_VALUE
    }

    /**
     *  Key -> target Id
     *
     *  Value -> heuristic distance in [0,1], where 1 is for "covered"
     */
    private val targets: MutableMap<Int, Heuristics> = mutableMapOf()

    /**
     *  Key -> action Id
     *
     * Value -> List of extra heuristics to minimize (min 0).
     * Those are related to the whole test, and not specific target.
     * Covering those extra does not guarantee that it would help in
     * covering target.
     * An example is rewarding SQL Select commands that return non-empty
     *
     * Note: these values are SORTED.
     */
    private val extraToMinimize: MutableMap<Int, List<Double>> = mutableMapOf()


    /**
     * Key -> action Id
     *
     * Value -> info on how the SQL database was accessed
     */
    private val databaseExecutions: MutableMap<Int, DatabaseExecution> = mutableMapOf()

    /**
     * When SUT does SQL commands using WHERE, keep track of when those "fails" (ie evaluate
     * to false), in particular the tables and columns in them involved
     */
    private val aggregatedFailedWhere: MutableMap<String, Set<String>> = mutableMapOf()


    fun copy(): FitnessValue {
        val copy = FitnessValue(size)
        copy.targets.putAll(this.targets)
        copy.extraToMinimize.putAll(this.extraToMinimize)
        copy.databaseExecutions.putAll(this.databaseExecutions) //note: DatabaseExecution supposed to be immutable
        copy.aggregateDatabaseData()
        return copy
    }

    /**
     * We keep track of DB interactions per action.
     * However, there are are cases in which we only care of aggregated data for all actions.
     * Instead of re-computing them each time, we just do it once and save the results
     */
    fun aggregateDatabaseData(){

        aggregatedFailedWhere.clear()
        aggregatedFailedWhere.putAll(DatabaseExecution.mergeData(
                databaseExecutions.values,
                {x ->  x.failedWhere}
        ))
    }

    fun setExtraToMinimize(actionIndex: Int, list: List<Double>) {
        extraToMinimize[actionIndex] = list.sorted()
    }

    fun setDatabaseExecution(actionIndex: Int, databaseExecution: DatabaseExecution){
        databaseExecutions[actionIndex] = databaseExecution
    }

    fun getViewOfData(): Map<Int, Heuristics> {
        return targets
    }

    fun getViewOfAggregatedFailedWhere() = aggregatedFailedWhere

    fun doesCover(target: Int): Boolean {
        return targets[target]?.distance == MAX_VALUE
    }

    fun getHeuristic(target: Int): Double = targets[target]?.distance ?: 0.0


    fun computeFitnessScore(): Double {

        return targets.values.map { h -> h.distance }.sum()
    }

    fun coveredTargets(): Int {

        return targets.values.filter { t -> t.distance == MAX_VALUE }.count()
    }

    fun coverTarget(id: Int) {
        updateTarget(id, MAX_VALUE)
    }

    /**
     * Set the heuristic [value] for the given target [id].
     * If already existing, replace it only if better.
     */
    fun updateTarget(id: Int, value: Double, actionIndex : Int = -1) {

        if (value < 0 || value > MAX_VALUE) {
            throw IllegalArgumentException("Invalid value: $value")
        }

        val current = targets[id]

        if(current == null || value > current.distance) {
            targets[id] = Heuristics(value, actionIndex)
        }
    }

    /**
     * This only merges the target heuristics, and not the extra ones
     */
    fun merge(other: FitnessValue) {

        other.targets.keys.forEach { t ->
            val k = other.getHeuristic(t)
            if (k > this.getHeuristic(t)) {
                this.updateTarget(t, k)
            }
        }
    }

    /**
     * Check if current does subsume [other].
     * This means covering at least the same targets, and at least one better or
     * one more.
     *
     * Recall: during the search, we might not calculate all targets, eg once they
     * are covered.
     *
     * @param other, the one we compare to
     * @param targetSubset, only calculate subsumption on these testing targets
     */
    fun subsumes(other: FitnessValue, targetSubset: Set<Int>): Boolean {

        var atLeastOneBetter = false

        for (k in targetSubset) {

            val v = this.targets[k]?.distance ?: 0.0
            val z = other.targets[k]?.distance ?: 0.0
            if (v < z) {
                return false
            }

            val extra = compareExtraToMinimize(k, other)

            if (v > z ||
                    (v == z && extra > 0) ||
                    (v == z && extra == 0 && this.size < other.size)) {
                atLeastOneBetter = true
            }
        }

        return atLeastOneBetter
    }

    /**
     * Compare the extra heuristics between this and [other].
     *
     * @return 0 if equivalent, 1 if this is better, and -1 otherwise
     */
    fun compareExtraToMinimize(target: Int, other: FitnessValue): Int {

        //TODO parameter to experiment with
        //return compareByRewardMore(other)
        return compareByReduce(target, other)
    }

    fun averageExtraDistancesToMinimize(actionIndex: Int): Double{
        return aggregateDistances(extraToMinimize[actionIndex])
    }

    private fun aggregateDistances(distances: List<Double>?): Double {
        if (distances == null || distances.isEmpty()) {
            return Double.MAX_VALUE
        }

        val sum = distances.map { v -> v / distances.size }.sum()

        return sum
    }

    private fun compareByReduce(target: Int, other: FitnessValue): Int {

        val thisAction = targets[target]?.actionIndex
        val otherAction = other.targets[target]?.actionIndex

        val ts = aggregateDistances(this.extraToMinimize[thisAction])
        val os = aggregateDistances(other.extraToMinimize[otherAction])

        return when {
            ts < os -> +1
            ts > os -> -1
            else -> 0
        }
    }

    /*
        TODO: add back and fix. Use for experiments.
     */

//    private fun compareByRewardMore(other: FitnessValue): Int {
//        val thisLength = this.extraToMinimize.size
//        val otherLength = other.extraToMinimize.size
//        val minLen = Math.min(thisLength, otherLength)
//
//        if (minLen > 0) {
//            for (i in 0..(minLen - 1)) {
//                val te = this.extraToMinimize[i]
//                val oe = other.extraToMinimize[i]
//
//                /*
//                    We prioritize the improvement of lowest
//                    heuristics, as more likely to be covered (ie 0)
//                    first.
//                */
//
//                if (te < oe) {
//                    return +1
//                } else if (te > oe) {
//                    return -1
//                }
//            }
//        }
//
//        if (thisLength == otherLength) {
//            return 0
//        }
//
//        /*
//            up to min size, same values of the heuristics.
//            But one test is doing more stuff, as it has more extra
//            heuristics. And so we reward it.
//
//            However, there is big risk of bloat. So, let's put
//            an arbitrary low limit.
//         */
//        if (minLen >= 3) { //TODO should be a parameter to experiment with
//            return 0
//        }
//
//        if (thisLength > otherLength) {
//            return +1
//        } else {
//            return -1
//        }
//    }
}