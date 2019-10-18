package org.evomaster.core.search.service.mutator.geneMutation

/**
 * created by manzh on 2019-09-19
 */
abstract class MutationBoundaryUpdate<T> (var preferMin : T, var preferMax : T, var counter : Int = 0, var reached : Boolean = false) where T : Number{

    abstract fun copy() : MutationBoundaryUpdate<T>

    fun updateCounter(improved : Boolean){
        if (improved) counter = 0
        else counter++
    }
}

interface UpdateBoundary<T> where T : Number{
    fun updateBoundary(previous : T, current: T, doesCurrentBetter : Boolean)
}


class IntMutationUpdate(preferMin: Int, preferMax: Int, counter: Int = 0, reached: Boolean = false) : MutationBoundaryUpdate<Int>(preferMin, preferMax, counter, reached), UpdateBoundary<Int>{

    override fun copy(): IntMutationUpdate = IntMutationUpdate(preferMin, preferMax, counter, reached)

    override fun updateBoundary(previous: Int, current: Int, doesCurrentBetter: Boolean) {
        val value = (previous + current) / 2.0
        updateCounter(doesCurrentBetter)
        if ( (doesCurrentBetter && current > previous) || (!doesCurrentBetter && current < previous)){
            preferMin = if (value > value.toInt()) value.toInt()+1 else value.toInt()
        }else
            preferMax = value.toInt()
    }
}

class DoubleMutationUpdate(preferMin: Double, preferMax: Double, counter: Int = 0, reached: Boolean = false) : MutationBoundaryUpdate<Double>(preferMin, preferMax, counter,reached),UpdateBoundary<Double>{
    override fun copy(): DoubleMutationUpdate = DoubleMutationUpdate(preferMin,preferMax,counter,reached)

    override fun updateBoundary(previous: Double, current: Double, doesCurrentBetter: Boolean) {
        val value = (previous + current) / 2.0
        updateCounter(doesCurrentBetter)
        if ( (doesCurrentBetter && current > previous) || (!doesCurrentBetter && current < previous)){
            preferMin = value
        }else
            preferMax = value
    }
}
