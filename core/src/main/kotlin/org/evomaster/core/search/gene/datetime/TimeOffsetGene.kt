package org.evomaster.core.search.gene.datetime

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.ChoiceGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.CompositeFixedGeneImpact
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.date.TimeOffsetGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Using RFC3339
 *
 * https://datatracker.ietf.org/doc/html/rfc3339#section-5.6
 *
 *    time-hour       = 2DIGIT  ; 00-23
 *    time-minute     = 2DIGIT  ; 00-59
 *    time-numoffset  = ("+" / "-") time-hour ":" time-minute
 *    time-offset     = "Z" / time-numoffset
 */
class TimeOffsetGene(
    name: String,
    val type: ChoiceGene<*> = ChoiceGene(
        "type",
        listOf(
            EnumGene("Z", listOf("Z"), treatAsNotString = true),
            TimeNumOffsetGene("numoffset")
        )
    )
)
 : CompositeFixedGene(name, listOf(type)){

     fun selectZ(){
         type.selectActiveGene(0)
     }


    override fun copyContent(): Gene {
        return TimeOffsetGene(name, type.copy() as ChoiceGene<*>)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        type.randomize(randomness, tryToForceNewValue)
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return type.getValueAsPrintableString(previousGenes,mode,targetFormat,extraCheck)
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is TimeOffsetGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid({type.copyValueFrom(other.type)}, true)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TimeOffsetGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return type.containsSameValueAs(other.type)
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        //TODO
        return false
    }

    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is TimeOffsetGeneImpact) {
            val maps = mapOf<Gene, GeneImpact>(
                type to additionalGeneMutationInfo.impact.typeImpact
            )
            return mwc.selectSubGene(
                internalGenes,
                adaptiveWeight = true,
                targets = additionalGeneMutationInfo.targets,
                impacts = internalGenes.map { i -> maps.getValue(i) },
                individual = null,
                evi = additionalGeneMutationInfo.evi
            )
                .map { it to additionalGeneMutationInfo.copyFoInnerGene(maps.getValue(it), it) }
        }
        throw IllegalArgumentException("impact is null or not TimeOffsetGeneImpact")
    }
}