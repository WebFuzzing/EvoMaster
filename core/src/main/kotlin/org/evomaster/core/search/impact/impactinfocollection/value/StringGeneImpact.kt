package org.evomaster.core.search.impact.impactinfocollection.value

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.impactinfocollection.*
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.BinaryGeneImpact
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzh on 2019-09-09
 */
class StringGeneImpact (sharedImpactInfo: SharedImpactInfo,
                        specificImpactInfo: SpecificImpactInfo,
                        val employBinding : BinaryGeneImpact,
                        val employSpecialization : BinaryGeneImpact,
                        /**
                         * impacts on its specific type
                         * it might lead to an issue when the type of gene is dynamic, thus the type of the current might differ from the type of the previous
                         */
                        val specializationGeneImpact : MutableList<Impact>
) : GeneImpact(sharedImpactInfo, specificImpactInfo){

    companion object{
        private val log: Logger = LoggerFactory.getLogger(StringGeneImpact::class.java)
    }

    constructor(
            id : String,
            degree: Double = 0.0,
            timesToManipulate : Int = 0,
            timesOfNoImpacts : Int = 0,
            timesOfNoImpactWithTargets : MutableMap<Int, Double> = mutableMapOf(),
            timesOfImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImpactFromImpact : MutableMap<Int, Double> = mutableMapOf(),
            noImprovement : MutableMap<Int, Double> = mutableMapOf(),
            employBinding: BinaryGeneImpact = BinaryGeneImpact("employBinding"),
            employSpecialization: BinaryGeneImpact = BinaryGeneImpact("employSpecialization"),
            specializationGeneImpact : MutableList<Impact> = mutableListOf()
    ) : this(
            SharedImpactInfo(id, degree, timesToManipulate, timesOfNoImpacts, timesOfNoImpactWithTargets, timesOfImpact),
            SpecificImpactInfo(noImpactFromImpact, noImprovement),
            employBinding,
            employSpecialization,
            specializationGeneImpact
    )

    constructor(id: String, gene : StringGene)
            : this(
            id,
            specializationGeneImpact = gene.specializationGenes.map { ImpactUtils.createGeneImpact(it, it.name) }.toMutableList())

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(
                shared.copy(),
                specific.copy(),
                employBinding.copy(),
                employSpecialization.copy(),
                specializationGeneImpact = specializationGeneImpact.map { it.copy()}.toMutableList())
    }

    override fun clone(): StringGeneImpact {
        return StringGeneImpact(
                shared.clone(),
                specific.clone(),
                employBinding.clone(),
                employSpecialization.clone(),
                specializationGeneImpact = specializationGeneImpact.map { it.clone()}.toMutableList())
    }

    override fun validate(gene: Gene): Boolean = gene is StringGene

    override fun countImpactWithMutatedGeneWithContext(gc: MutatedGeneWithContext, noImpactTargets : Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean) {
        countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)

        //update specialization with current
        if ((gc.current as StringGene).specializationGenes.size > specializationGeneImpact.size){
            val starting = specializationGeneImpact.size
            (starting until gc.current.specializationGenes.size).forEach {
                val gene = gc.current.specializationGenes[it]
                specializationGeneImpact.add(it, ImpactUtils.createGeneImpact(gene, gene.name))
            }
        }else if (gc.current.specializationGenes.size < specializationGeneImpact.size){
            log.warn("some specializations of StringGene are removed")
        }

        if (gc.previous == null && impactTargets.isNotEmpty()) return

        val currentSelect = gc.current.selectedSpecialization
        employSpecialization.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        val taintImpact = if (currentSelect == -1){ employSpecialization.falseValue }else employSpecialization.trueValue
        taintImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)

        if (currentSelect != -1){
            val sImpact = specializationGeneImpact[gc.current.selectedSpecialization]
            val previousSelect = (gc.previous as StringGene).selectedSpecialization

            val mutatedGeneWithContext = MutatedGeneWithContext(previous = if (previousSelect == currentSelect) gc.previous.specializationGenes[previousSelect] else null, current =  gc.current.specializationGenes[currentSelect], action = "none", position = -1, numOfMutatedGene = 1)
            (sImpact as GeneImpact).countImpactWithMutatedGeneWithContext(
                    mutatedGeneWithContext, noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation
            )
        }

        employBinding.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = gc.numOfMutatedGene)
        val ft = if (gc.current.bindingIds.isEmpty()) employBinding.falseValue else employBinding.trueValue
        ft.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)

    }
}