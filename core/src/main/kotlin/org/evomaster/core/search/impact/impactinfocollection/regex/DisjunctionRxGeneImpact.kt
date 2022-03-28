package org.evomaster.core.search.impact.impactinfocollection.regex

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.DisjunctionRxGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact

/**
 * created by manzh on 2020-07-08
 */
class DisjunctionRxGeneImpact (
        sharedImpactInfo: SharedImpactInfo,
        specificImpactInfo: SpecificImpactInfo,
        val termsImpact : List<GeneImpact>,
        val extraPrefix : BinaryGeneImpact = BinaryGeneImpact("extraPrefix"),
        val extraPostfix : BinaryGeneImpact = BinaryGeneImpact("extraPostfix")
) :  RxAtomImpact(sharedImpactInfo, specificImpactInfo){

    constructor(id : String, gene : DisjunctionRxGene) : this(SharedImpactInfo(id), SpecificImpactInfo(), gene.terms.map { ImpactUtils.createGeneImpact(it, it.name)}.toList())


    override fun copy(): DisjunctionRxGeneImpact {
        return DisjunctionRxGeneImpact(
                shared.copy(),
                specific.copy(),
                termsImpact.map { it.copy() },
                extraPrefix.copy(),
                extraPostfix.copy()
        )
    }

    override fun clone(): DisjunctionRxGeneImpact {
        return DisjunctionRxGeneImpact(
                shared.clone(),
                specific.clone(),
                termsImpact.map { it.clone() },
                extraPrefix.clone(),
                extraPostfix.clone()
        )
    }

    override fun validate(gene: Gene): Boolean = gene is DisjunctionRxGene

    override fun innerImpacts(): List<Impact> {
        return termsImpact.plus(extraPostfix).plus(extraPostfix)
    }

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets, impactTargets, improvedTargets, onlyManipulation, num = gc.numOfMutatedGene)
        check(gc)

        val modifyPost = (gc.current as DisjunctionRxGene).extraPostfix != (gc.previous as? DisjunctionRxGene)?.extraPostfix
        if (modifyPost){
            extraPostfix.countImpactAndPerformance(noImpactTargets, impactTargets, improvedTargets, onlyManipulation, 1)
            return
        }

        val modifyPrefix = gc.current.extraPrefix != (gc.previous as? DisjunctionRxGene)?.extraPrefix
        if (modifyPrefix){
            extraPrefix.countImpactAndPerformance(noImpactTargets, impactTargets, improvedTargets, onlyManipulation, 1)
            return
        }

        val modifiedTerms : List<Pair<Int?, Pair<Gene?, Gene>>> = gc.current.terms.mapIndexed { index, rxTerm ->
            val p = if (gc.previous != null &&  !(gc.previous as DisjunctionRxGene).terms[index].containsSameValueAs(rxTerm)) gc.previous.terms[index] else null
            (if (p != null || gc.previous == null) index else null) to (p to rxTerm)
        }.filter { it.first == null }

        modifiedTerms.forEach {
            if (it.first != null){
                val num = modifiedTerms.size * gc.numOfMutatedGene
                val timpact = termsImpact[it.first!!]
                val tgc = gc.mainPosition(current = it.second.second, previous = it.second.first, numOfMutatedGene = num)
                timpact.countImpactWithMutatedGeneWithContext(
                        gc = tgc,
                        improvedTargets = improvedTargets,
                        noImpactTargets = noImpactTargets,
                        impactTargets = impactTargets,
                        onlyManipulation = onlyManipulation
                )
            }
        }

    }
}