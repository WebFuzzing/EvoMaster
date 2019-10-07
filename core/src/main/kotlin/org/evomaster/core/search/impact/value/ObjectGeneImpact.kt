package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext

/**
 * created by manzh on 2019-09-09
 */
class ObjectGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val fields : MutableMap<String, Impact> = mutableMapOf()
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive){

    constructor(id: String, objectGene: ObjectGene) : this (id, fields = objectGene.fields.map { Pair(it.name, ImpactUtils.createGeneImpact(it, it.name)) }.toMap().toMutableMap())

    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, niCounter, positionSensitive, fields.map { Pair(it.key, it.value.copy()) }.toMap().toMutableMap())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)
        if (gc.previous == null && hasImpact) return
        if (gc.current !is ObjectGene)
            throw IllegalArgumentException("gc.current ${gc.current::class.java.simpleName} should be ObjectGene")
        if (gc.previous == null){
            gc.current.fields.forEach {
                val fImpact = fields.getValue(it.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
                val mutatedGeneWithContext = MutatedGeneWithContext(previous = null, current =  it, action = "none", position = -1)
                fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
            }
            return
        }
        if (gc.previous !is ObjectGene)
            throw IllegalArgumentException("gc.previous ${gc.previous::class.java.simpleName} should be ObjectGene")


        gc.current.fields.zip(gc.previous.fields) { cf, pf ->
            Pair(Pair(cf, pf), cf.containsSameValueAs(pf))
        }.filter { !it.second }.map { it.first }.forEach { g->
            val fImpact = fields.getValue(g.first.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = g.second, current =  g.first, action = "none", position = -1)
            fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, hasImpact, noImprovement)
        }

    }

    override fun validate(gene: Gene): Boolean = gene is ObjectGene
}