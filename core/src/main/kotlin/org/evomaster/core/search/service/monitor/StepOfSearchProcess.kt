package org.evomaster.exps.monitor

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Archive


/**
 * @author: manzhang
 * @date: 2018/9/5
 */
open class StepOfSearchProcess<T> where T : Individual{

    val populations = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()

    val samplingCounter = mutableMapOf<Int, Int>()

    val evalIndividual : EvaluatedIndividual<T>

    var added : Boolean = false
    var improvedArchive : Boolean = false

    val indexOfEvaluation : Int

    val time : Long

    val isMutated : Boolean

    var timeCost : Double? = null

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