package org.evomaster.core.search.tracer

/**
 * TraceableElement represents an element whose history can be tracked.
 * @property trackOperator is used to attach additional information regarding how it evolved, e.g., mutator.
 * @property track presents a history of the [TraceableElement]. In addition, [track] is nullable. When tracking is not enabled, the [track] is null.
 * @property undoTrack presents a undo history of the [TraceableElement].
 *
 * Note that [undoTrack] is only enabled for evaluated individual, not for individual.
 * Because evaluated individual has a metric (i.e., fitness) to identify whether the "modified" individual is on the list of [undoTrack],
 *      and individual does not have one.
 */

abstract class TraceableElement(
        val trackOperator: TrackOperator? = null,
        private val track : MutableList<out TraceableElement>? = null,
        private val undoTrack : MutableList<out TraceableElement>? = null
) {

    open fun isRoot() : Boolean{
        return track?.isEmpty()?:false
    }

    /**
     * [track] of the [TraceableElement] is not editable, so its getter return list
     */
    fun getTrack() : List<out TraceableElement>?{
        return track?.toList()
    }

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement
     * @return an TraceableElement that is going to be refined by [trackOperator], and its history is [track] of [this] plus [this]
     */
    open fun next(trackOperator: TrackOperator) : TraceableElement? = null

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement
     * @param next is an evolved/refined element based on [this],
     *           e.g., EvaluatedIndividual is always created from fitness function based on individual, which does not rely on previous Evaluated individual
     * @return an newly created TraceableElement regarding [next], and its history is [track] of [this] plus [this]
     */
    open fun next(trackOperator: TrackOperator, next : TraceableElement) : TraceableElement? = null

    abstract fun copy(withTrack : Boolean) : TraceableElement

    open fun getUndoTrack() : MutableList<out TraceableElement>? = undoTrack

}