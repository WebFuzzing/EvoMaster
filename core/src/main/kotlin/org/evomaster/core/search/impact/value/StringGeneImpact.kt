package org.evomaster.core.search.impact.value

import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class StringGeneImpact (
        id: String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int =0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        /**
         * which type has more impacts?
         * not sure whether it is required
         */
        val specializationTypes : EnumGeneImpact? = null,
        /**
         * impacts on its specific type
         * it might lead to an issue when the type of gene is dynamic, thus the type of the current might differ from the type of the previous
         */
        var specializationGeneImpact : GeneImpact? = null
): GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive) {

    companion object{
        const val specializationName = "specialization"
    }


    constructor(id: String, gene : StringGene)
            : this(
            id,
            specializationTypes = gene.specializations.isNotEmpty().run { EnumGeneImpact(specializationName, EnumGene(specializationName, StringSpecialization.values().map { it.name })) },
            specializationGeneImpact = gene.specializationGene?.run { ImpactUtils.createGeneImpact(this, this.name) })

    override fun copy(): StringGeneImpact {
        return StringGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, specializationTypes, specializationGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is StringGene

    fun countSpecializationGeneImpact(previous : StringGene, current : StringGene, hasImpact : Boolean, isWorse :Boolean){
        /*
        TODO
         is it possible that the specializationGene of StringGene is not initialized?
         if possible, specializationGeneImpact is required to re-assigned based on whether specializations is empty
         */
        if (specializationGeneImpact == null) return

        if (current.specializationGene != null
                && previous.specializationGene != null
                && current.specializations::class.java.simpleName == previous.specializationGene!!::class.java.simpleName){
            val gcGene = MutatedGeneWithContext(current = current.specializationGene!!, previous = previous.specializationGene!!)
            ImpactUtils.processImpact(specializationGeneImpact!!, gcGene, hasImpact, isWorse = isWorse)
        }

    }
}