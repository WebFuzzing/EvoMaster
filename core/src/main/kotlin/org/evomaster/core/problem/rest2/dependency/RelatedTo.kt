package org.evomaster.core.problem.rest2.resources.dependency

import org.evomaster.core.database.schema.Table

open class RelatedTo(
        private val key: String,
        val targets : MutableList<out Any>,
        var probability: Double,
        var additionalInfo : String = ""
){
    companion object {
        private const val separator =  "$->$"
    }

    open fun notateKey() : String = key
    open fun originalKey() : String = key

    open fun getTargetsName () : String = "{${targets.map { it.toString() }.joinToString(",")}}"
    open fun getName() : String = notateKey() + separator + getTargetsName()
}

/**
 * @property key is name of param in a resource path
 * @property targets each element is a name of table
 */
class ParamRelatedToTable (key: String, target: MutableList<String>, probability: Double, info: String="")
    :RelatedTo(key, target, probability, info){

    override fun notateKey() : String = "PARM:${originalKey()}"


}
