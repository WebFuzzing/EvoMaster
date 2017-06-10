package org.evomaster.core.search

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

        @JvmField
        val MAX_VALUE = 1.0

        @JvmStatic
        fun isMaxValue(value: Double) = value == MAX_VALUE
    }

    /**
     *  Key -> target Id
     *  <br/>
     *  Value -> heuristic distance in [0,1], where 1 is for "covered"
     */
    private val targets: MutableMap<Int, Double> = mutableMapOf()

    /**
     * List of extra heuristics to minimize (min 0).
     * Those are related to the whole test, and not specific target.
     * Covering those extra does not guarantee that it would help in
     * covering target.
     * An example is rewarding SQL Select commands that return non-empty
     *
     * Note: this values are sorted.
     */
    private val extraToMinimize: MutableList<Double> = mutableListOf()


    fun copy(): FitnessValue {
        val copy = FitnessValue(size)
        copy.targets.putAll(this.targets)
        copy.extraToMinimize.addAll(this.extraToMinimize)
        return copy
    }

    fun setExtraToMinimize(list: List<Double>) {
        extraToMinimize.clear()
        extraToMinimize.addAll(list)
        extraToMinimize.sort()
    }

    fun getViewOfData(): Map<Int, Double> {
        return targets
    }

    fun doesCover(target: Int): Boolean {
        return targets[target] == MAX_VALUE
    }

    fun getHeuristic(target: Int): Double = targets[target] ?: 0.0


    fun computeFitnessScore(): Double {

        return targets.values.sum()
    }

    fun coveredTargets(): Int {

        return targets.values.filter { t -> t == MAX_VALUE }.count()
    }

    fun coverTarget(id: Int) {
        updateTarget(id, MAX_VALUE)
    }

    fun updateTarget(id: Int, value: Double) {

        if (value < 0 || value > MAX_VALUE) {
            throw IllegalArgumentException("Invalid value: " + value)
        }

        targets[id] = value
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
     * @param targetSubset, only calculate subsumpsion on these testing targets
     */
    fun subsumes(other: FitnessValue, targetSubset: Set<Int>): Boolean {

        var atLeastOneBetter = false

        for (k in targetSubset) {

            val v = this.targets[k] ?: 0.0
            val z = other.targets[k] ?: 0.0
            if (v < z) {
                return false
            }

            val extra = compareExtraToMinimize(other)

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
    fun compareExtraToMinimize(other: FitnessValue): Int {

        //TODO parameter to experiment with
        //return compareByRewardMore(other)
        return compareByReduce(other)
    }

    private fun aggregateDistances(distances : List<Double>) : Double{
        if(distances.isEmpty()){
            return Double.MAX_VALUE
        }
        val max = distances.max()!!
        val sum = distances.sum()
        if(sum >= max){
            return sum
        } else {
            //handle possible overflow
            return Double.MAX_VALUE
        }
    }

    private fun compareByReduce(other: FitnessValue): Int {

        val ts = aggregateDistances(this.extraToMinimize)
        val os = aggregateDistances(other.extraToMinimize)

        if(ts < os){
            return +1
        } else if(ts > os){
            return -1
        } else {
            return 0
        }
    }


    private fun compareByRewardMore(other: FitnessValue): Int {
        val thisLength = this.extraToMinimize.size
        val otherLength = other.extraToMinimize.size
        val minLen = Math.min(thisLength, otherLength)

        if (minLen > 0) {
            for (i in 0..(minLen - 1)) {
                val te = this.extraToMinimize[i]
                val oe = other.extraToMinimize[i]

                /*
                    We prioritize the improvement of lowest
                    heuristics, as more likely to be covered (ie 0)
                    first.
                */

                if (te < oe) {
                    return +1
                } else if (te > oe) {
                    return -1
                }
            }
        }

        if (thisLength == otherLength) {
            return 0
        }

        /*
            up to min size, same values of the heuristics.
            But one test is doing more stuff, as it has more extra
            heuristics. And so we reward it.

            However, there is big risk of bloat. So, let's put
            an arbitrary low limit.
         */
        if (minLen >= 3) { //TODO should be a parameter to experiment with
            return 0
        }

        if (thisLength > otherLength) {
            return +1
        } else {
            return -1
        }
    }
}