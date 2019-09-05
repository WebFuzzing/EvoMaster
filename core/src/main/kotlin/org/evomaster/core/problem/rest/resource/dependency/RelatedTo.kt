package org.evomaster.core.problem.rest.resource.dependency

/**
 * key relies on target
 */
open class RelatedTo(
        private val key: String,
        open val targets : MutableList<out Any>,
        var probability: Double,
        var additionalInfo : String = ""
){
    companion object {
        private const val DIRECT_DEPEND =  "$->$"

        fun generateKeyForMultiple(keys : List<String>) : String = if(keys.isEmpty()) "" else if( keys.size == 1)  keys.first() else "{${keys.joinToString(",")}}"

        fun parseMultipleKey(key : String) : List<String> {
            if(key.startsWith("{") && key.endsWith("}")){
                return key.substring(1, key.length - 1).split(",")
            }
            return listOf(key)
        }
    }
    open fun notateKey() : String = key
    open fun originalKey() : String = key

    open fun getTargetsName () : String = generateKeyForMultiple(targets.map { it.toString() })
    open fun getName() : String = "${notateKey()}$DIRECT_DEPEND${getTargetsName()}"
}