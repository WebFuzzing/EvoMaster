package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.gene.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact

class CompositeFixedGeneImpact(
    sharedImpactInfo: SharedImpactInfo,
    specificImpactInfo: SpecificImpactInfo,
    // key is index_name of the gene
    val childrenImpacts : MutableMap<String, Impact> = mutableMapOf()
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    companion object{
        private fun getKey(index: Int, name: String) = "${index}_$name"
    }

    constructor(
        id : String,
        gene: CompositeFixedGene,
    ) : this(
        SharedImpactInfo(id),
        SpecificImpactInfo(),
        childrenImpacts= gene.getViewOfChildren().mapIndexed { index, g ->  Pair(getKey(index, g.name), ImpactUtils.createGeneImpact(g, g.name)) }.toMap().toMutableMap())


    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(
            shared.copy(),
            specific.copy(),
            fields = childrenImpacts.map { Pair(it.key, it.value.copy()) }.toMap().toMutableMap())
    }

    override fun clone(): ObjectGeneImpact {
        return ObjectGeneImpact(
            shared.clone(),
            specific.clone(),
            childrenImpacts.map { it.key to it.value.clone() }.toMap().toMutableMap()
        )
    }


    fun getChildImpact(index: Int, name: String): Impact? = childrenImpacts[getKey(index, name)]

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {

        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is CompositeFixedGene)
            throw IllegalArgumentException("gc.current ${gc.current::class.java.simpleName} should be CompositeFixedGene")
        if (gc.previous == null){
            gc.current.getViewOfChildren().forEachIndexed { index, i->
                val fImpact = childrenImpacts[getKey(index, i.name)] as? GeneImpact
                    ?:throw IllegalArgumentException("impact should be gene impact")
                val mutatedGeneWithContext = MutatedGeneWithContext(previous = null, current =  i, action = "none", position = -1, numOfMutatedGene = gc.current.getViewOfChildren().size)
                fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
            }
            return
        }
        if (gc.previous !is CompositeFixedGene)
            throw IllegalArgumentException("gc.previous ${gc.previous::class.java.simpleName} should be ObjectGene")


        val mutatedFields = gc.current.getViewOfChildren().zip(gc.previous.getViewOfChildren()) { cf, pf ->
            Pair(Pair(gc.current.getViewOfChildren().indexOf(cf), Pair(cf, pf)), cf.containsSameValueAs(pf))
        }.filter { !it.second }.map { it.first }

        val onlyManipulation = mutatedFields.size > 1 && impactTargets.isNotEmpty()

        mutatedFields.forEach {p->
            val g = p.second
            val fImpact = getChildImpact(p.first, g.first.name) as? GeneImpact?:throw IllegalArgumentException("impact should be gene impact")
            val mutatedGeneWithContext = MutatedGeneWithContext(previous = g.second, current =  g.first, action = "none", position = -1, numOfMutatedGene = gc.numOfMutatedGene * mutatedFields.size)
            fImpact.countImpactWithMutatedGeneWithContext(mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation)
        }
    }

    override fun validate(gene: Gene): Boolean = gene is CompositeFixedGene

    override fun flatViewInnerImpact(): Map<String, Impact> {
        val map = mutableMapOf<String, Impact>()
        childrenImpacts.forEach { (t, u) ->
            map.putIfAbsent("${getId()}-$t", u)
            if (u is GeneImpact && u.flatViewInnerImpact().isNotEmpty())
                map.putAll(u.flatViewInnerImpact())
        }
        return map
    }

    override fun innerImpacts(): List<Impact> {
        return childrenImpacts.values.toList()
    }
}