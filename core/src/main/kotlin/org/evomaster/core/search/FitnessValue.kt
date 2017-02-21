package org.evomaster.core.search

/**
As the number of targets is unknown, we cannot have
a minimization problem, as new targets could be added
throughout the search
 */
class FitnessValue {

    companion object{

        @JvmField
        val MAX_VALUE = 1.0

        @JvmStatic
        fun isMaxValue(value : Double) = value == MAX_VALUE
    }

    /**
     *  Key -> target Id
     *  <br/>
     *  Value -> heuristic distance in [0,1], where 1 is for "covered"
     */
    private val targets : MutableMap<Int,Double> = mutableMapOf()


    fun copy() : FitnessValue {
        val copy = FitnessValue()
        copy.targets.putAll(this.targets)
        return copy
    }

    fun getViewOfData() : Map<Int, Double>{
        return targets
    }

    fun doesCover(target: Int) : Boolean{
        return targets[target] == MAX_VALUE
    }

    fun getHeuristic(target: Int) : Double = targets[target] ?: 0.0


    fun computeFitnessScore() : Double{

        return targets.values.sum()
    }

    fun coveredTargets() : Int {

        return targets.values.filter { t -> t == MAX_VALUE }.count()
    }

    fun coverTarget(id: Int){
        updateTarget(id, MAX_VALUE)
    }

    fun updateTarget(id: Int, value: Double){

        if(value < 0 || value > MAX_VALUE){
            throw IllegalArgumentException("Invalid value: "+value)
        }

        targets[id] = value
    }


    fun merge(other: FitnessValue){

        other.targets.keys.forEach { t ->
            val k = other.getHeuristic(t)
            if(k > this.getHeuristic(t)){
                this.updateTarget(t, k)
            }
        }
    }

    /**
     * Check if current does subsume other.
     * This means covering at least the same targets, and at least one better or
     * one more.
     *
     * Note: during the search, we might not calculate all targets, eg once they
     * are covered.
     * So this check might always be false when comparing existing against a new one.
     * With "strict" false, the check will ignore covered targets in the current, as
     * likely the info for those will be missing in new individuals
     */
    fun subsumes(other: FitnessValue, strict: Boolean = true) : Boolean{

        if(this.targets.size < other.targets.size){
            //if less targets, cannot subsumes
            return false
        }

        var atLeastOneBetter = false

        for((k,v) in this.targets){

            if(!strict && v==1.0){
                continue
            }

            val z = other.targets[k] ?: 0.0
            if(v < z){
                return false
            }
            if(v > z){
                atLeastOneBetter = true
            }
        }

        if(! atLeastOneBetter){
            return false
        }

        val missing = other.targets.keys
                .filter { k -> ! this.targets.containsKey(k) }
                .size

        return missing == 0
    }
}