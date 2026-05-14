package org.evomaster.core.parser

import org.evomaster.core.search.gene.Gene

/**
 *  A call on the parser visitor would return either Gene(s) or some data
 */
class VisitResult(
        val genes: MutableList<Gene> = mutableListOf(),
        var data: Any? = null
){
    constructor(gene: Gene) : this() {
        genes.add(gene)
    }

    inline fun <reified T> requireDataList(): List<T> {
        val values = data
            ?: throw IllegalStateException("Expected List<${T::class.simpleName}> in VisitResult, but it was null")

        if (values !is List<*>) {
            throw IllegalStateException(
                "Expected List<${T::class.simpleName}> in VisitResult, but got ${values::class.qualifiedName}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return values.mapIndexed { index, value ->
            value as? T
                ?: throw IllegalStateException(
                    "Expected ${T::class.simpleName} entry at VisitResult[$index], but got " +
                            (value?.let { it::class.qualifiedName } ?: "null"))
        } as List<T>
    }
}