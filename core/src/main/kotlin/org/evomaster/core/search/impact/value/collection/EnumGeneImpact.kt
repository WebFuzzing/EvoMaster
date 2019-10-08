package org.evomaster.core.search.impact.value.collection

import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.Impact
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.GeneralImpact

/**
 * created by manzh on 2019-09-09
 */
class EnumGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        niCounter : Int = 0,
        positionSensitive: Boolean = false,
        val values : List<GeneralImpact> = listOf()
) : GeneImpact(id = id, degree = degree, timesToManipulate = timesToManipulate, timesOfImpact = timesOfImpact, timesOfNoImpacts = timesOfNoImpacts,counter= counter, niCounter = niCounter, positionSensitive =positionSensitive) {

    constructor(id: String, gene: EnumGene<*>) : this (id, values = gene.values.mapIndexed { index, _ -> GeneralImpact(index.toString()) }.toList())

    override fun copy(): EnumGeneImpact {
        return EnumGeneImpact(id = id, degree = degree,timesToManipulate =  timesToManipulate,timesOfImpact =  timesOfImpact, timesOfNoImpacts = timesOfNoImpacts, counter = counter, niCounter = niCounter, positionSensitive = positionSensitive, values = values)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, hasImpact: Boolean, noImprovement: Boolean) {
        countImpactAndPerformance(hasImpact, noImprovement)

        if (gc.current !is EnumGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be EnumGene")

        values[gc.current.index].countImpactAndPerformance(hasImpact, noImprovement)
    }

    override fun validate(gene: Gene): Boolean = gene is EnumGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return values.map { "$id-${it.id}" to it }.toMap()
    }

    override fun maxTimesOfNoImpact(): Int = values.size * 2
}