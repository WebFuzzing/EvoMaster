package org.evomaster.core.search.impact.impactInfoCollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.impactInfoCollection.*

/**
 * created by manzh on 2019-09-09
 */
class ObjectGeneImpact  (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val fields : MutableMap<String, Impact> = mutableMapOf()
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Int> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Int> = mutableMapOf(),
            noImprovement : MutableMap<Int, Int> = mutableMapOf(),
            fields : MutableMap<String, Impact> = mutableMapOf()

    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            fields
    )

    constructor(id: String, objectGene: ObjectGene) : this (id, fields = objectGene.fields.map { Pair(it.name, ImpactUtils.createGeneImpact(it, it.name)) }.toMap().toMutableMap())

    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(
                shared.copy(),
                specific.copy(),
                fields = fields.map { Pair(it.key, it.value.copy()) }.toMap().toMutableMap())
    }

    override fun clone(): ObjectGeneImpact {
        return ObjectGeneImpact(
                shared.clone(),
                specific.clone(),
                fields.map { it.key to it.value.clone() }.toMap().toMutableMap()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is ObjectGene)
            throw IllegalArgumentException("gc.current ${gc.current::class.java.simpleName} should be ObjectGene")
        if (gc.previous == null){
            gc.current.fields.forEach {
                val fImpact = fields.getValue(it.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
                val mutatedGeneWithContext = MutatedGeneWithContext(previous = null, current =  it, action = "none", position = -1)
                fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            }
            return
        }
        if (gc.previous !is ObjectGene)
            throw IllegalArgumentException("gc.previous ${gc.previous::class.java.simpleName} should be ObjectGene")


        val mutatedFields = gc.current.fields.zip(gc.previous.fields) { cf, pf ->
            Pair(Pair(cf, pf), cf.containsSameValueAs(pf))
        }.filter { !it.second }.map { it.first }

        val onlyManipulation = mutatedFields.size > 1 && impactTargets.isNotEmpty()

        mutatedFields.forEach {g->
            val fImpact = fields.getValue(g.first.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = g.second, current =  g.first, action = "none", position = -1)
            fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is ObjectGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        val map = mutableMapOf<String, Impact>()
        fields.forEach { (t, u) ->
            map.putIfAbsent("${getId()}-$t", u)
            if (u is GeneImpact && u.flatViewInnerImpact().isNotEmpty())
                map.putAll(u.flatViewInnerImpact())
        }
        return map
    }
}