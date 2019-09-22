package org.evomaster.core.search.tracer

/**
 * TraceableElement represents an element whose history can be tracked.
 * @property trackOperator is used to attach additional information regarding how it evolved, e.g., mutator.
 * @property tracking presents a history of the [TraceableElement]. In addition, [tracking] is nullable. When tracking is not enabled, the [tracking] is null.
 * @property undoTracking presents a undo history of the [TraceableElement].
 *
 * Note that [undoTracking] is only enabled for evaluated individual, not for individual.
 * Because evaluated individual has a metric (i.e., fitness) to identify whether the "modified" individual is on the list of [undoTracking],
 *      and individual does not have one.
 */

abstract class TraceableElement(
        val trackOperator: TrackOperator? = null,
        private val tracking : MutableList<out TraceableElement>? = null,
        private val undoTracking : MutableList<out TraceableElement>? = null
) {

    open fun isRoot() : Boolean{
        return tracking?.isEmpty()?:false
    }

    /**
     * [tracking] of the [TraceableElement] is not editable, so its getter return list
     */
    open fun getTracking() : List<out TraceableElement>?{
        return tracking?.toList()
    }

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement
     * @return an TraceableElement that is going to be refined by [trackOperator], and its history is [tracking] of [this] plus [this]
     */
    open fun next(trackOperator: TrackOperator, maxLength : Int = -1) : TraceableElement? = null

    /**
     * @param trackOperator presents an operatorTag to refine the TraceElement
     * @param next is an evolved/refined element based on [this],
     *           e.g., EvaluatedIndividual is always created from fitness function based on individual, which does not rely on previous Evaluated individual
     * @param copyFilter indicates how to create new element
     * @return an newly created TraceableElement regarding [next], and its history is [tracking] of [this] plus [this]
     */
    open fun next(trackOperator: TrackOperator, next : TraceableElement, copyFilter: TraceableElementCopyFilter, maxLength : Int = -1) : TraceableElement? = null

    open fun copy(options: TraceableElementCopyFilter) : TraceableElement{
        TODO("NOT IMPLEMENT")
    }

    open fun getUndoTracking() : MutableList<out TraceableElement>? = undoTracking

}