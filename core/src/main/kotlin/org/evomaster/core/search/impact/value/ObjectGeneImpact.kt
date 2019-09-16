package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.GeneImpact
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
        positionSensitive: Boolean = false,
        val fields : MutableMap<String, GeneImpact> = mutableMapOf()
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive){

    constructor(id: String, objectGene: ObjectGene) : this (id, fields = objectGene.fields.map { Pair(it.name, ImpactUtils.createGeneImpact(it, it.name)) }.toMap().toMutableMap())

    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, fields)
    }


    fun countFieldImpact(previous: ObjectGene, current : ObjectGene, hasImpact: Boolean, countDeepImpact : Boolean){
        current.fields.zip(previous.fields) { cf, pf ->
            Pair(Pair(cf, pf), cf.containsSameValueAs(pf))
        }.filter { !it.second }.map { it.first }.forEach { g->
            val fImpact = fields.getValue(g.first.name)
            if (countDeepImpact){
                val mutatedGeneWithContext = MutatedGeneWithContext(previous = g.second, current =  g.first, action = "none", position = -1)
                ImpactUtils.processImpact(fImpact, mutatedGeneWithContext,hasImpact, countDeepImpact)
            }else{
                fImpact.countImpact(hasImpact)
            }
        }
    }

    override fun validate(gene: Gene): Boolean = gene is ObjectGene
}