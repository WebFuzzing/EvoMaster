package org.evomaster.core.search


open class ActionResult constructor(
        /** Specify whether the result of this action led to stop the evaluation
         * of the following actions*/
        var stopping: Boolean = false) {

    private val results : MutableMap<String, String> = mutableMapOf()

    protected constructor(other: ActionResult) : this(other.stopping){
        results.putAll(other.results)
    }


    open fun copy() : ActionResult{
        return ActionResult(this)
    }

    fun addResultValue(name: String, value: String){
        results[name] = value
    }

    fun getResultValue(name: String) : String? {
        return results[name]
    }

    fun isEmpty() = results.isEmpty()
}