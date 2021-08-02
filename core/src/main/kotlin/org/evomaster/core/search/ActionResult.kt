package org.evomaster.core.search


open class ActionResult constructor(
        /** Specify whether the result of this action led to stop the evaluation
         * of the following actions*/
        var stopping: Boolean = false) {

    companion object{
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
    }

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


    fun setErrorMessage(msg: String) = addResultValue(ERROR_MESSAGE, msg)
    fun getErrorMessage(): String? = getResultValue(ERROR_MESSAGE)

    /**
     * @return whether this action result type matches the [action]
     */
    open fun matchedType(action: Action) = true
}