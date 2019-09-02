package org.evomaster.core.search.service.monitor

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Archive


/**
 * This is used to record a step after executing a selected individual, i.e., evalIndividual
 *      - population is for current archived population in Archive,
 *      - samplingCounter is for current sampling counter in Archive
 *      - added is for whether it is added to Archive
 *      - improved is for whether it helps to improve fitness
 *      - isMutated is for whether it is mutated
 *
 */
open class StepOfSearchProcess<T> where T : Individual{


    /**
     * Key -> id of the target
     *
     * Value -> sorted list of best individuals for that target
     */
    val populations = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()

    /**
     * Key -> id of the target
     *
     * Value -> how often we sampled from the buffer for that target since
     *          last fitness improvement.
     *          Note: such counter will be reset when a fitness improvement
     *          is obtained for that target is obtained.
     *          This means that an infeasible / hard target will not get
     *          its counter reset once the final local optima is reached
     */
    val samplingCounter = mutableMapOf<Int, Int>()

    /**
     * the evaluated individual
     */
    val evalIndividual : EvaluatedIndividual<T>

    var added : Boolean = false
    var improvedArchive : Boolean = false
    val isMutated : Boolean

    /**
     * an index of this evaluation
     */
    val indexOfEvaluation : Int

    /**
     * a time to start this evaluation
     */
    val time : Long


    constructor(_archive:Archive<*>,  _indexOfEvaluation : Int, _i : T, _eval: EvaluatedIndividual<*>, _time:Long, _isMutated : Boolean){

        this.isMutated = _isMutated
        this.time = _time
        this.evalIndividual = _eval as EvaluatedIndividual<T>
        this.indexOfEvaluation = _indexOfEvaluation

        for(entries in _archive.getSnapshotOfBestIndividuals()){
            populations.put(entries.key, mutableListOf())
            for(v in entries.value){
                populations.getValue(entries.key).add((v as EvaluatedIndividual<T>))
            }
        }
        _archive.getSnapshotOfSamplingCounter()
                .forEach { t, u ->  run {
                    samplingCounter.getOrDefault(t, u)
                    }
                }
    }

}