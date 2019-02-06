package org.evomaster.core.search.service.tracer

/**
 * @property description presents an operator to initialize the TraceElement
 * @property track presents history of the TraceElement
 */

abstract class TraceableElement(
        private var description : String = UNDEFENDED_OPERATOR,
        private val track : MutableList<out TraceableElement>? = null) {

    companion object {
        const val UNDEFENDED_OPERATOR = "undefined"
        var maxlength = -1
    }

    fun getDescription():String{
        return description
    }

    fun appendDescription(append : String){
        description = append + TrackOperator.method_separator + description
    }

    fun getMethod() : String?{
        description.split(TrackOperator.method_separator).apply {
            if(size == 2) return this[0]
            else return null
        }
    }

    protected fun setDescription(description : String){
        this.description = description
    }
    open fun isRoot() : Boolean{
        return track?.isEmpty()?:false
    }

    fun getTrack() : List<out TraceableElement>?{
        return track?.toList()
    }

    /**
     * return true when function next(description) further override with non-null return value
     */
    open fun isCapableOfTracking() : Boolean = false

    /**
     * @param description presents an operator to refine the TraceElement, i.e., current TraceElement is the latest history of returned TraceElement
     */
    open fun next(description: String) : TraceableElement? = null

    /**
     * @param description presents an operator to refine the TraceElement, i.e., current TraceElement is the latest history of returned TraceElement
     * @param next presents next element which has been created in an indirectly way,
     *           e.g., EvaluatedIndividual is always created from fitness function based on individual, which does not rely on previous Evaluated individual
     */
    open fun next(description: String, next : TraceableElement) : TraceableElement? = null

    abstract fun copy(withTrack : Boolean) : TraceableElement


//    Need to further check whether it is required to implement deep next of TraceElement
//    /**
//     * return true when 1) isCapableOfTracking is true,
//     *                  and 2) maxDepthOfHistory() is not -1
//     *                  and 3) every element in [org.evomaster.core.search.service.tracer.TraceElement.track] also records track
//     */
//    open fun doesDeepCopy() : Boolean = false
}