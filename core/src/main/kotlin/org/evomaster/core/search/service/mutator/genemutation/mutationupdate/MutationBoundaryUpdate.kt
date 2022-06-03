package org.evomaster.core.search.service.mutator.genemutation.mutationupdate

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2019-09-19
 *
 * this is to calculate the available boundary with evolution history
 * @property direction indicates whether collect change directions
 * @property min is minimum value
 * @property max is maximum value
 * @property counter indicates times from the recent improvement
 * @property reached indicates whether the gene has reached its optimal
 * @property updateTimes represents times to be updated
 * @property resetTimes represents how many times of the values are reset, which may be useful to 'gene dependency' analysis (but not implemented)
 * @property latest record the latest value which is used to boundary value
 * @property preferMin is preferred minimum value for this mutation
 * @property preferMax is preferred maximum value for this mutation
 */
abstract class MutationBoundaryUpdate<T> (
        val direction : Boolean,
        val min : T, val max : T,
        val precision: Int?,
        val scale: Int?,
        var counter : Int = 0, var reached : Boolean = false,
        var updateTimes : Int = 0,
        var resetTimes : Int = 0, var latest: T? = null, var preferMin : T = min, var preferMax : T = max) where T : Number{

    private var directionHistory : MutableList<Int> = mutableListOf()

    abstract fun candidatesBoundary() : T

    fun updateCounter(improved : Int){
        if (improved > 0) counter = 0
        else counter += 1
    }

    fun reset(){
        preferMin = min
        preferMax = max
        resetTimes += 1
        reached = false
    }

    fun isReached(current : T) : Boolean = preferMin == preferMax && preferMin == current

    abstract fun middle() : T
    abstract fun random(apc: AdaptiveParameterControl, randomness: Randomness, current: T, probOfMiddle : Double, start: Int, end: Int, minimalTimeForUpdate: Int) : T

    abstract fun doReset(current : T, evaluatedResult: Int) : Boolean
    abstract fun updateBoundary(current: T, evaluatedResult : Int)
    abstract fun direction(latest: T?, current: T, evaluatedResult: Int) : Int

    fun updateOrRestBoundary(index : Int, current: T, evaluatedResult : Int){
        if (direction)
            directionHistory.add(index, direction(latest, current, evaluatedResult))

        if (doReset(current, evaluatedResult)){
            reset()
        }else{
            updateBoundary(current, evaluatedResult)
        }
        updateCounter(evaluatedResult)
        latest = current
    }

    fun updateOrRestBoundary(history : List<Pair<T, Int>>){

        (history.indices).forEach { i->
            updateOrRestBoundary(
                    index = i,
                    current = history[i].first,
                    evaluatedResult = history[i].second
            )
        }
    }

    fun randomDirection(randomness: Randomness) : Int?{
        val changes = directionHistory.filter { it != 0 }
        if (changes.isEmpty()) return null
        if (randomness.nextBoolean(0.8)) return changes.last()
        return randomness.choose(changes)
    }

    fun isUpdatable() : Boolean = max.toDouble() > min.toDouble()
}





