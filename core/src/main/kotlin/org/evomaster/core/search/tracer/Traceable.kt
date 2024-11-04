package org.evomaster.core.search.tracer

import org.evomaster.core.search.service.monitor.ProcessMonitorExcludeField
import org.evomaster.core.search.service.mutator.EvaluatedMutation

/**
 * Traceable represents an element whose history can be tracked.
 * @property trackOperator is used to attach additional information regarding how it evolved, e.g., structure mutator or standard mutator. it is only nullable when tracking is disabled.
 * @property index indicates when the element is created. with mio algorithm, it can be assigned as evaluated actions/individuals
 * @property evaluatedResult represents a result of the element evaluated based on eg Archive when it is created. it is nullable if it is not necessary to collect such info, eg, only tracking individual
 * @property tracking contains a history of the element.
 *
 * Note that [this] element might be not the last one in this history because the evolution is not always towards optimal direction.
 */
interface Traceable {

    var trackOperator: TrackOperator?

    var index : Int

    var evaluatedResult: EvaluatedMutation?

    var tracking: TrackingHistory<out Traceable>?

    fun tagOperator(operator: TrackOperator?, index: Int){
        trackOperator = operator
        this.index = index
    }

    companion object{
        const val DEFAULT_INDEX = -1
    }


    fun <T: Traceable> wrapWithTracking(evaluatedResult: EvaluatedMutation?, maxLength: Int, history: MutableList<T>){
        wrapped()
        this.evaluatedResult = evaluatedResult
        this.tracking = TrackingHistory(maxLength, history)
    }

    fun <T: Traceable> wrapWithTracking(evaluatedResult: EvaluatedMutation?, trackingHistory: TrackingHistory<T>?){
        wrapped()
        if (this.evaluatedResult == null || evaluatedResult!=null)
            this.evaluatedResult = evaluatedResult
        this.tracking = trackingHistory
    }

    fun wrapWithEvaluatedResults(evaluatedResult: EvaluatedMutation?){
        this.evaluatedResult = evaluatedResult
    }

    private fun wrapped(){
        if (evaluatedResult != null || tracking != null) throw IllegalStateException("evaluated result or history has been initialized")
    }

    /**
     * @param next is an evolved/refined element based on [this],
     *           e.g., EvaluatedIndividual is always created from fitness function based on individual, which does not rely on previous Evaluated individual
     *
     * @param copyFilter indicates how to restore element in tracking
     * @param evaluatedResult indicates if the next is effective mutation
     *
     * @return an newly created TraceableElement regarding [next]
     */
    fun next(next: Traceable, copyFilter: TraceableElementCopyFilter, evaluatedResult: EvaluatedMutation) : Traceable?

    /**
     * @param options indicates the option to copy the element in the tracking
     */
    fun copy(options: TraceableElementCopyFilter) : Traceable


    /**
     * push latest element the tracking
     */
    fun <T : Traceable> pushLatest(next: T){
        (tracking as? TrackingHistory<T>)?.update(next)
                ?: throw IllegalStateException("tracking history should not be null")
    }

    fun <T : Traceable> getLast(n : Int, resultRange: IntRange? = null) : List<T>{
        return (tracking as? TrackingHistory<T>)?.history?.filter { resultRange == null ||  (it.evaluatedResult?.run { this.value in resultRange } ?: true)}?.
                run {
            if (size < n)
                this
            else
                subList(size - n, size)
        } ?: throw IllegalStateException("the element is not tracked")
    }

    fun <T: Traceable> getByIndex(index : Int) : T?{
        return ((tracking as? TrackingHistory<T>)?: throw IllegalStateException("tracking should not be null")).history?.find {
            it.index == index
        }
    }

}

data class TrackingHistory<T : Traceable>(
        val maxLength: Int,
        val history : MutableList<T> = mutableListOf()
){

    fun copy(copyFilter: TraceableElementCopyFilter) : TrackingHistory<T> {
        return TrackingHistory(maxLength, history.map { (it.copy(copyFilter) as T)}.toMutableList())
    }

    fun update(next: T){
        if (maxLength == 0) return
        if (history.size == maxLength)
            history.removeAt(0)
        history.add(next)
    }
}

