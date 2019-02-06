package org.evomaster.core.search.service.tracer

abstract class Impact(val id : String, protected var value : Double) : WithImpacts {
    abstract fun calculateAnImpact()
}

open class ImpactByTimes<T>(id : String, value : Double = 0.0, var weight : Int = 0, private var changedTimes : Int = 0, private var impactTimes : Int = 0) : Impact(id, value){
    var worst: T? = null
    var best : T? = null

    fun reportImpact(doesFitnessChange : Boolean) {
        if(doesFitnessChange) impactTimes++
    }
    fun reportTimes(doesValueChange : Boolean){
        if(doesValueChange) changedTimes++
    }
    override fun calculateAnImpact(){
        value = (impactTimes * 1.0) / changedTimes
    }
}

interface WithImpacts{
    fun getImpacts() : Map<String, out Impact>? = null
    fun getStructureImpact() : Map<String, out Impact>? = null

    fun update(ei : Any){
        //do nothing
    }
    fun initImpacts(){
        //do nothing
    }

    fun calculate() {
        getImpacts()?.apply {
            this.values.forEach{
                it.calculateAnImpact()
            }
        }
    }
}

