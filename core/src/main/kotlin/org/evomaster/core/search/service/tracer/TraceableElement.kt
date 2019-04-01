package org.evomaster.core.search.service.tracer

/**
 * @property trackOperator is used to attach additional information regarding how it evolved, e.g., mutator.
 * @property track presents a history of the [TraceableElement]. In addition, [track] is nullable. When tracking is not enabled, the [track] is null.
 * @property undoTrack presents a undo history of the [TraceableElement].
 */

abstract class TraceableElement(
        val trackOperator: TrackOperator? = null,
        private val track : MutableList<out TraceableElement>? = null,
        val undoTrack : MutableList<out TraceableElement>? = null
) {

    open fun isRoot() : Boolean{
        return track?.isEmpty()?:false
    }

    fun getTrack() : List<out TraceableElement>?{
        return track?.toList()
    }

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement, i.e., current TraceElement is the latest history of returned TraceElement
     */
    open fun next(trackOperator: TrackOperator) : TraceableElement? = null

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement, i.e., current TraceElement is the latest history of returned TraceElement
     * @param next presents next element which has been created in an indirectly way,
     *           e.g., EvaluatedIndividual is always created from fitness function based on individual, which does not rely on previous Evaluated individual
     */
    open fun next(trackOperator: TrackOperator, next : TraceableElement) : TraceableElement? = null

    abstract fun copy(withTrack : Boolean) : TraceableElement


}