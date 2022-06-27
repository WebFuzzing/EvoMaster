package org.evomaster.core.search.impact.impactinfocollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.SeededGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.collection.EnumGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact

/**
 * impact info for SeededGene
 */
class SeededGeneImpact(
    sharedImpactInfo: SharedImpactInfo,
    specificImpactInfo: SpecificImpactInfo,
    val employSeedImpact: BinaryGeneImpact,
    val geneImpact: GeneImpact,
    val seededGeneImpact: EnumGeneImpact,
) : GeneImpact(sharedImpactInfo, specificImpactInfo) {

    constructor(
        id: String,
        employSeedImpact: BinaryGeneImpact = BinaryGeneImpact("employSeeded"),
        geneImpact: GeneImpact,
        seededGeneImpact: EnumGeneImpact
    ) : this(
        SharedImpactInfo(id),
        SpecificImpactInfo(),
        employSeedImpact,
        geneImpact,
        seededGeneImpact
    )

    constructor(id: String, seededGene: SeededGene<*>) : this(
        id,
        geneImpact = ImpactUtils.createGeneImpact(seededGene.gene as Gene, "gene"),
        seededGeneImpact = ImpactUtils.createGeneImpact(seededGene.seeded, "seeded") as EnumGeneImpact
    )

    override fun copy(): SeededGeneImpact {
        return SeededGeneImpact(
            shared.copy(),
            specific.copy(),
            employSeedImpact.copy(),
            geneImpact.copy(),
            seededGeneImpact.copy()
        )
    }

    override fun clone(): SeededGeneImpact {
        return SeededGeneImpact(
            shared,
            specific.copy(),
            employSeedImpact.clone(),
            geneImpact.clone(),
            seededGeneImpact.clone()
        )
    }

    override fun validate(gene: Gene): Boolean {
        return gene is SeededGene<*>
    }

    override fun syncImpact(previous: Gene?, current: Gene) {
        check(previous, current)
        geneImpact.syncImpact((previous as? SeededGene<*>)?.gene as Gene, (current as SeededGene<*>).gene as Gene)
        seededGeneImpact.syncImpact((previous as? SeededGene<*>)?.seeded, (current as SeededGene<*>).seeded)
    }

    override fun flatViewInnerImpact(): Map<String, Impact> {
        return mutableMapOf("${getId()}-${geneImpact.getId()}" to geneImpact)
            .plus("${getId()}-${seededGeneImpact.getId()}" to seededGeneImpact)
            .plus("${getId()}-${employSeedImpact.getId()}" to employSeedImpact)
            .plus(employSeedImpact.flatViewInnerImpact().plus(geneImpact.flatViewInnerImpact()).plus(seededGeneImpact.flatViewInnerImpact()).map { "${getId()}-${it.key}" to it.value })
    }

    override fun innerImpacts(): List<Impact> {
        return listOf(employSeedImpact, geneImpact, seededGeneImpact)
    }
}