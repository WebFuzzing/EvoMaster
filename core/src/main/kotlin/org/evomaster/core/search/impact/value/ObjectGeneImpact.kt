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
        timesToManipulate : Int = 0,
        timesOfNoImpacts : Int = 0,
        timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
        noImprovement : MutableMap<Int, Int> = mutableMapOf(),
        val fields : MutableMap<String, Impact> = mutableMapOf()
) : GeneImpact(
        id = id,
        degree = degree,
        timesToManipulate = timesToManipulate,
        timesOfNoImpacts = timesOfNoImpacts,
        timesOfImpact= timesOfImpact,
        noImpactFromImpact = noImpactFromImpact,
        noImprovement = noImprovement
) {

    constructor(id: String, objectGene: ObjectGene) : this (id, fields = objectGene.fields.map { Pair(it.name, ImpactUtils.createGeneImpact(it, it.name)) }.toMap().toMutableMap())

    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(id = id,
                degree = degree,
                timesToManipulate = timesToManipulate,
                timesOfNoImpacts = timesOfNoImpacts,
                timesOfImpact= timesOfImpact.toMutableMap(),
                noImpactFromImpact = noImpactFromImpact.toMutableMap(),
                noImprovement = noImprovement.toMutableMap(),
                fields = fields.map { Pair(it.key, it.value.copy()) }.toMap().toMutableMap())
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        countImpactAndPerformance(impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is ObjectGene)
            throw IllegalArgumentException("gc.current ${gc.current::class.java.simpleName} should be ObjectGene")
        if (gc.previous == null){
            gc.current.fields.forEach {
                val fImpact = fields.getValue(it.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
                val mutatedGeneWithContext = MutatedGeneWithContext(previous = null, current =  it, action = "none", position = -1)
                fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
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
            fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }

    }

    override fun validate(gene: Gene): Boolean = gene is ObjectGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        val map = mutableMapOf<String, Impact>()
        fields.forEach { (t, u) ->
            map.putIfAbsent("$id-$t", u)
            if (u is GeneImpact && u.flatViewInnerImpact().isNotEmpty())
                map.putAll(u.flatViewInnerImpact())
        }
        return map
    }
}