package org.evomaster.core.search.service.mutator.genemutation.mutationupdate


import org.evomaster.core.search.gene.utils.NumberMutatorUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.utils.NumberCalculationUtil


class DoubleMutationUpdate(direction: Boolean,
                           min: Double,
                           max: Double,
                           precision: Int?,
                           scale: Int?,
                           updateTimes : Int = 0,
                           counter: Int = 0,
                           reached: Boolean = false, latest : Double? = null, preferMin: Double = min, preferMax : Double = max)
    : MutationBoundaryUpdate<Double>(direction, min, max, precision = precision, scale=scale, updateTimes = updateTimes, counter = counter, reached = reached, latest = latest, preferMin = preferMin, preferMax = preferMax), Comparable<DoubleMutationUpdate> {

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
            if (m != current) return NumberMutatorUtils.getFormattedValue(m, scale)
        }

        val sdirection = if (direction) randomDirection(randomness)?.run { this > 0 } else null

        return NumberMutatorUtils.mutateFloatingPointNumber(
            randomness, sdirection, maxRange = candidatesBoundary().toLong(),apc, current, preferMin, preferMax, scale
        )
    }

    override fun candidatesBoundary(): Double {
        val result = if (preferMin != preferMax) NumberCalculationUtil.calculateIncrement(max= preferMax, min=preferMin)
                    else NumberCalculationUtil.calculateIncrement(max =max, min=min)

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