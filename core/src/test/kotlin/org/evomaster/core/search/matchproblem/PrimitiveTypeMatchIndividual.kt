package org.evomaster.core.search.matchproblem

import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2020-06-16
 */
open class PrimitiveTypeMatchIndividual (action: PrimitiveTypeMatchAction) :  Individual(children = mutableListOf(action)){

    constructor(value : Any, name : String): this(
            PrimitiveTypeMatchAction(instance(value, name))
    )

    constructor(gene: Gene): this(
        PrimitiveTypeMatchAction(gene)
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

    val gene : Gene = (children[0] as PrimitiveTypeMatchAction).seeTopGenes()[0]

    override fun verifyInitializationActions(): Boolean {
        //do nothing
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {
        //do nothing
    }

    override fun size(): Int = 1

    override fun seeTopGenes(filter: ActionFilter): List<out Gene> {
        return (children[0] as PrimitiveTypeMatchAction).seeTopGenes()
    }

    override fun copyContent(): Individual {
        return PrimitiveTypeMatchIndividual(children[0].copy() as PrimitiveTypeMatchAction)
    }

}
