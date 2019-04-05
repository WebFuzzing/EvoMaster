package org.evomaster.core.search

import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.tracer.TraceableElement
import org.evomaster.core.search.tracer.TrackOperator

/**
 * EvaluatedIndividual allows to tracking its evolution.
 * Note that tracking EvaluatedIndividual can be enabled by set EMConfig.enableTrackEvaluatedIndividual true.
 */
class EvaluatedIndividual<T>(val fitness: FitnessValue,
                             val individual: T,
                             /**
                              * Note: as the test execution could had been
                              * prematurely stopped, there might be less
                              * results than actions
                              */
                             val results: List<out ActionResult>,
                             trackOperator: TrackOperator? = null,
                             tracking : MutableList<EvaluatedIndividual<T>>? = null,
                             private val undoTracking : MutableList<EvaluatedIndividual<T>>? = null)
    : TraceableElement(trackOperator,  tracking, undoTracking) where T : Individual {

    init{
        if(individual.seeActions().size < results.size){
            throw IllegalArgumentException("Less actions than results")
        }
        if(tracking!=null && tracking.isNotEmpty() && tracking.first().trackOperator !is Sampler<*>){
            throw IllegalArgumentException("First tracking operator must be sampler")
        }
    }


    fun copy(): EvaluatedIndividual<T> {
        return EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                trackOperator
        )
    }

    /**
     * Note: if a test execution was prematurely stopped,
     * the number of evaluated actions would be lower than
     * the total number of actions
     */
    fun evaluatedActions() : List<EvaluatedAction>{

        val list: MutableList<EvaluatedAction> = mutableListOf()

        val actions = individual.seeActions()

        (0 until results.size).forEach { i ->
            list.add(EvaluatedAction(actions[i], results[i]))
        }

        return list
    }

    override fun copy(withTrack: Boolean): EvaluatedIndividual<T> {

        when(withTrack){
            false-> return copy()
            else ->{
                /**
                 * if the [getTrack] is null, which means the tracking option is attached on individual not evaluated individual
                 */
                getTrack()?:return EvaluatedIndividual(
                        fitness.copy(),
                        individual.copy(withTrack) as T,
                        results.map(ActionResult::copy),
                        trackOperator
                )

                return forceCopyWithTrack()
            }
        }
    }

    fun forceCopyWithTrack(): EvaluatedIndividual<T> {
        return EvaluatedIndividual(
                fitness.copy(),
                individual.copy() as T,
                results.map(ActionResult::copy),
                trackOperator?:individual.trackOperator,
                getTrack()?.map { (it as EvaluatedIndividual<T> ).copy() }?.toMutableList()?: mutableListOf(),
                getUndoTrack()?.map { it.copy()}?.toMutableList()?: mutableListOf()
        )
    }


    override fun next(trackOperator: TrackOperator, next: TraceableElement): EvaluatedIndividual<T>? {
        val copyTraces = getTrack()?.map { (it as EvaluatedIndividual<T> ).copy() }?.toMutableList()?: mutableListOf()
        copyTraces.add(this.copy())
        val copyUndoTraces = getUndoTrack()?.map {(it as EvaluatedIndividual<T>).copy()}?.toMutableList()?: mutableListOf()


        return  EvaluatedIndividual(
                (next as EvaluatedIndividual<T>).fitness.copy(),
                next.individual.copy(false) as T,
                next.results.map(ActionResult::copy),
                trackOperator,
                copyTraces,
                copyUndoTraces
        )
    }

    override fun getUndoTrack(): MutableList<EvaluatedIndividual<T>>? {
        undoTracking?: return null
        return undoTracking
    }

}