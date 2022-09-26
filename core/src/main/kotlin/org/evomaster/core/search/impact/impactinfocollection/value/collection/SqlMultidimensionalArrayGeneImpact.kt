package org.evomaster.core.search.impact.impactinfocollection.value.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.sql.SqlMultidimensionalArrayGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * created by jgaleotti on 2022-04-15
 *
 * TODO need to further extend for elements
 */
class SqlMultidimensionalArrayGeneImpact(sharedImpactInfo: SharedImpactInfo, specificImpactInfo: SpecificImpactInfo,
                                         val sizeImpact: IntegerGeneImpact = IntegerGeneImpact("size")
) : CollectionImpact, GeneImpact(sharedImpactInfo, specificImpactInfo) {

    constructor(
            id: String
    ) : this(SharedImpactInfo(id), SpecificImpactInfo())

    override fun getSizeImpact(): Impact = sizeImpact

    override fun copy(): SqlMultidimensionalArrayGeneImpact {
        return SqlMultidimensionalArrayGeneImpact(
                shared.copy(),
                specific.copy(),
                sizeImpact = sizeImpact.copy())
    }

    override fun clone(): SqlMultidimensionalArrayGeneImpact {
        return SqlMultidimensionalArrayGeneImpact(
                shared.clone(),
                specific.clone(),
                sizeImpact.clone()
        )
    }

    override fun countImpactWithMutatedGeneWithContext(
            gc: MutatedGeneWithContext,
            noImpactTargets: Set<Int>,
            impactTargets: Set<Int>,
            improvedTargets: Set<Int>,
            onlyManipulation: Boolean
    ) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        if (gc.previous == null && impactTargets.isNotEmpty()) return
        if (gc.current !is SqlMultidimensionalArrayGene<*>)
            throw IllegalStateException("gc.current (${gc.current::class.java.simpleName}) should be SqlMultidimensionalArrayGene")
        if ((gc.previous != null && gc.previous !is SqlMultidimensionalArrayGene<*>))
            throw IllegalStateException("gc.previous (${gc.previous::class.java.simpleName}) should be SqlMultidimensionalArrayGene")

        if (gc.previous != null && (gc.previous as SqlMultidimensionalArrayGene<*>).getViewOfChildren().size != gc.current.getViewOfChildren().size)
            sizeImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)

        //TODO for elements
    }

    override fun validate(gene: Gene): Boolean = gene is SqlMultidimensionalArrayGene<*>

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${sizeImpact.getId()}" to sizeImpact)
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(sizeImpact)
    }
}