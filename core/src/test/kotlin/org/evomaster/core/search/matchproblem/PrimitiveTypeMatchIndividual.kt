package org.evomaster.core.search.matchproblem

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2020-06-16
 */
open class PrimitiveTypeMatchIndividual (
        val gene : Gene) :  Individual(children = listOf(gene)){

    constructor(value : Any, name : String): this(
            instance(value, name)
    )

    companion object{
        fun name() = "value"

        fun instance(value : Any, name : String) :Gene{
            return when (value) {
                is Int -> IntegerGene(name =  name, value = value)
                is Double -> DoubleGene(name =  name, value = value)
                is Float -> FloatGene(name = name, value = value)
                is Long -> LongGene(name = name, value = value)
                is String -> StringGene(name = name, value = value)
                else -> throw IllegalStateException("NOT SUPPORT")
            }
        }

        fun intTemplate() = PrimitiveTypeMatchIndividual(IntegerGene(name()))
        fun doubleTemplate() = PrimitiveTypeMatchIndividual(DoubleGene(name()))
        fun floatTemplate() = PrimitiveTypeMatchIndividual(FloatGene(name()))
        fun longTemplate() = PrimitiveTypeMatchIndividual(LongGene(name()))
        fun stringTemplate() = PrimitiveTypeMatchIndividual(StringGene(name()))
    }

    override fun seeActions(): List<out Action> = listOf()

    override fun verifyInitializationActions(): Boolean {
        //do nothing
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {
        //do nothing
    }

    override fun size(): Int = 1

    override fun seeGenes(filter: GeneFilter): List<out Gene> = listOf(gene)

    override fun copyContent(): Individual {
        return PrimitiveTypeMatchIndividual(gene.copyContent())
    }

    override fun getChildren(): List<Gene> = listOf(gene)
}
