package org.evomaster.core.output.clustering.metrics

import kotlin.math.max

object LevenshteinDistance {
    fun distance(p0: String, p1: String): Double{
        val lhsLength = p0.length
        val rhsLength = p1.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i

            for (j in 1..lhsLength) {
                val editCost= if(p0[j - 1] == p1[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + editCost
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength].toDouble()/ max(lhsLength, rhsLength)

        /*
        NOTE: I am hoping that, by making the distance more dependent on length
        (e.g. as a percentage of difference instead of absolute)
        it will be more robust in terms of analyzing different error messages.

        This could lead to other problems in terms of error messages that have the same
        proportion of difference, but are actually very different messages.

        Until a more elegant solution can be found, I'll try to assess this idea.
         */
    }
}