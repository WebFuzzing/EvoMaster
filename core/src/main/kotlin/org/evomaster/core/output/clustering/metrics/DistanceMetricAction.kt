package org.evomaster.core.output.clustering.metrics

import com.google.gson.Gson
import org.evomaster.core.problem.rest.RestCallResult


class DistanceMetricAction : DistanceMetric<RestCallResult>() {
    override fun calculateDistance(val1: RestCallResult, val2: RestCallResult): Double {
        val message1 = Gson().fromJson(val1.getBody(), Map::class.java)?.get("message") ?: ""
        val message2 = Gson().fromJson(val2.getBody(), Map::class.java)?.get("message") ?: ""

        return levenshtein(message1.toString(), message2.toString())
    }

    private fun levenshtein(p0: String, p1: String): Double{
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

        return cost[lhsLength].toDouble()
    }
}