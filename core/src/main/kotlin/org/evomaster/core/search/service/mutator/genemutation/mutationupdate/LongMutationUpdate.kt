package org.evomaster.core.search.service.mutator.genemutation.mutationupdate

import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LongMutationUpdate(direction: Boolean, min: Long, max: Long, updateTimes : Int = 0, counter: Int = 0, reached: Boolean = false, latest : Long? = null, preferMin : Long= min, preferMax: Long = max)
    : MutationBoundaryUpdate<Long>(direction, min, max, counter = counter, updateTimes = updateTimes, reached = reached, latest = latest, preferMin = preferMin, preferMax = preferMax, precision = null, scale = null), Comparable<LongMutationUpdate>{

    constructor(direction: Boolean, min: Int, max: Int) : this(direction, min = min.toLong(), max = max.toLong())

    override fun doReset(current: Long, evaluatedResult: Int): Boolean {
        return (current < preferMin || current > preferMax) && (evaluatedResult > 0)
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(LongMutationUpdate::class.java)
    }

    override fun middle(): Long {
        if (preferMin > preferMax)
            log.warn("min {} should not be more than max {}", preferMin, preferMax)
        return (preferMin/2.0 + preferMax/2.0).toLong()
    }

    override fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: Long, probOfMiddle: Double, start: Int, end: Int, minimalTimeForUpdate: Int): Long {
        var c = current
        if (randomness.nextBoolean(probOfMiddle)) {
            val middle = middle()
            if (middle != current) return middle
            c = middle
        }
        val delta = GeneUtils.getDelta(randomness, apc, range = candidatesBoundary(), start = start, end = end)
        val candidates = listOf(c + delta, c - delta)
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

        return when {
            valid.isNotEmpty() -> randomness.choose(valid)
            candidates.minOrNull()!! > preferMax -> preferMax
            else -> preferMin
        }
    }

    override fun compareTo(other: LongMutationUpdate): Int {
        val result = try{
            Math.subtractExact(other.candidatesBoundary(), candidatesBoundary())
        }catch (e : ArithmeticException) {
            return other.candidatesBoundary().compareTo(candidatesBoundary())
        }
        return if (result > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else if (result < Int.MIN_VALUE.toLong()) Int.MIN_VALUE else result.toInt()
    }

    override fun candidatesBoundary(): Long {
        val range = try {
            if (preferMin != preferMax)
                Math.subtractExact(preferMax, preferMin)
            else
                Math.subtractExact(max, min)
        }catch (e : ArithmeticException){
            return  Long.MAX_VALUE
        }
        return range.also {
            if (it < 0) throw IllegalStateException("preferMax < preferMin: $preferMax, $preferMin")
        }
    }

    override fun updateBoundary(current: Long, evaluatedResult: Int) {
        latest?:return
        if (current == latest || evaluatedResult == 0) return
        val value = latest!!/2.0 + current/2.0
        val isBetter = evaluatedResult>0
        val updateMin = (isBetter && current > latest!!) || (!isBetter && current < latest!!)
        if (updateMin){
            (if (value.toLong()+1L < preferMax) value.toLong()+1L else value.toLong() ).also {
                if (it <= preferMax) preferMin = it
            }
        }else{
            value.toLong().also {
                if (it >= preferMin) preferMax = it
            }
        }
        updateTimes +=1
    }

    override fun direction(latest: Long?, current: Long, evaluatedResult: Int): Int {
        if (latest == null || evaluatedResult == 0) return 0
        return evaluatedResult * current.compareTo(latest)
    }
}