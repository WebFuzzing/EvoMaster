package org.evomaster.core.search.service.mutator.genemutation.mutationupdate

import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import java.math.BigDecimal
import java.math.RoundingMode


class DoubleMutationUpdate(direction: Boolean, min: Double, max: Double, updateTimes : Int = 0, counter: Int = 0, reached: Boolean = false, latest : Double? = null, preferMin: Double = min, preferMax : Double = max)
    : MutationBoundaryUpdate<Double>(direction, min, max, updateTimes = updateTimes, counter = counter, reached = reached, latest = latest, preferMin = preferMin, preferMax = preferMax), Comparable<DoubleMutationUpdate> {

    override fun compareTo(other: DoubleMutationUpdate): Int {
        val r = -candidatesBoundary() + other.candidatesBoundary()
        if (r < Int.MIN_VALUE) return Int.MIN_VALUE
        else if (r > Int.MAX_VALUE) return Int.MAX_VALUE
        return r.toInt()
    }

    override fun doReset(current: Double, evaluatedResult: Int): Boolean {
        return (current < preferMin || current > preferMax) && (evaluatedResult > 0)
    }

    override fun middle(): Double = preferMin/2.0 + preferMax/2.0

    override fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: Double, probOfMiddle: Double, start: Int, end: Int, minimalTimeForUpdate: Int): Double {
        if(randomness.nextBoolean(probOfMiddle)) {
            val m = middle()
            if (m != current) return m
        }
        val delta = randomness.nextGaussian()
        val times = GeneUtils.getDelta(randomness, apc, candidatesBoundary().toLong(), start = start, end = end)
        val candidates = listOf(current + delta, current + times * delta, BigDecimal(current).setScale(randomness.nextInt(15), RoundingMode.HALF_EVEN).toDouble())
        val valid = candidates.filter { it <= preferMax && it >= preferMin }
        if (direction){
            val dir = randomDirection(randomness)
            if (dir != null && dir != 0){
                val values = valid.filter {
                    if(dir > 0) it > current else it < current
                }
                if (values.isNotEmpty()) return randomness.choose(values)
            }
        }
        return when{
            valid.isNotEmpty() -> randomness.choose(valid)
            candidates.minOrNull()!! > preferMax -> preferMax
            else -> preferMin
        }
    }

    override fun copy(): DoubleMutationUpdate = DoubleMutationUpdate(direction, preferMin, preferMax, updateTimes, counter, reached, latest, preferMin, preferMax)

    override fun candidatesBoundary(): Double {
        val result = preferMax - preferMin
        if (Double.NEGATIVE_INFINITY == result || result == Double.NaN || result == Double.NEGATIVE_INFINITY)
            return Long.MAX_VALUE.toDouble()

        return result.also {
            if (it < 0) throw IllegalStateException("preferMax < preferMin: $preferMax, $preferMin")
        }
    }

    override fun updateBoundary(current: Double, evaluatedResult: Int) {
        latest?:return
        if (current == latest || evaluatedResult == 0) return

        val value = latest!!/2.0 + current/2.0
        updateCounter(evaluatedResult)
        val isBetter = evaluatedResult>0
        if ( (isBetter && current > latest!!) || (!isBetter && current < latest!!)){
            value.also {
                if(it <= preferMax) preferMin = it
            }
        }else{
            value.also {
                if (it >= preferMin) preferMax =it
            }
        }
        updateTimes +=1
    }

    override fun direction(latest: Double?, current: Double, evaluatedResult: Int): Int {
        if (latest == null || evaluatedResult == 0) return 0
        return evaluatedResult * current.compareTo(latest)
    }
}