package org.evomaster.core.search.gene.optional

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections

/**
 * A gene that holds many potential genes (genotype) but
 * only one is active at any time (phenotype). The list
 * of gene choices cannot be empty.
 */

class ChoiceGene<T>(
    name: String,
    private val geneChoices: List<T>,
    activeChoice: Int = 0,
    /**
     * Potentially, associate different probabilities for the different choices
     */
    probabilities: List<Double>? = null

) : CompositeFixedGene(name, geneChoices) where T : Gene {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChoiceGene::class.java)
    }

    var activeGeneIndex: Int = activeChoice
        private set

    private val probabilities = probabilities?.toList() //make a copy

    init {
        if (geneChoices.isEmpty()) {
            throw IllegalArgumentException("The list of gene choices cannot be empty")
        }

        if (activeChoice < 0 || activeChoice >= geneChoices.size) {
            throw IllegalArgumentException("Active choice must be between 0 and ${geneChoices.size - 1}")
        }
        if(probabilities != null && probabilities.size != geneChoices.size){
            throw IllegalArgumentException("If probabilities are defined, then they must be same number as the genes")
        }
    }


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        activeGeneIndex = if(probabilities != null){
            randomness.chooseByProbability(probabilities.mapIndexed { index, d -> index to d }.toMap())
        } else {
            randomness.nextInt(geneChoices.size)
        }

        /*
            Even the non-selected genes need to be randomized, otherwise could be let in
            an inconsistent state
         */
        if(!initialized){
        geneChoices
            .filter { it.isMutable() }
            .forEach { it.randomize(randomness, tryToForceNewValue) }
        } else {
            val g = geneChoices[activeGeneIndex]
            if(g.isMutable()){
                g.randomize(randomness, tryToForceNewValue)
            }
        }
    }

    /**
     * TODO This method must be implemented to reflect usage
     * of the selectionStrategy and the additionalGeneMutationInfo
     */
    override fun mutablePhenotypeChildren(): List<Gene> {
        return listOf(geneChoices[activeGeneIndex])
    }

    override fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{
        if(this.javaClass == klass){
            return this as T
        }
        return geneChoices[activeGeneIndex].getWrappedGene(klass)
    }


    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {

      // TODO
        // select another disjunction based on impact
//        if (enableAdaptiveGeneMutation || selectionStrategy == SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT){
//            additionalGeneMutationInfo?:throw IllegalStateException("")
//            if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DisjunctionListRxGeneImpact){
//                val candidates = disjunctions.filterIndexed { index, _ -> index != activeDisjunction  }
//                val impacts = candidates.map {
//                    additionalGeneMutationInfo.impact.disjunctions[disjunctions.indexOf(it)]
//                }
//
//                val selected = mwc.selectSubGene(
//                    candidateGenesToMutate = candidates,
//                    impacts = impacts,
//                    targets = additionalGeneMutationInfo.targets,
//                    forceNotEmpty = true,
//                    adaptiveWeight = true
//                )
//                activeDisjunction = disjunctions.indexOf(randomness.choose(selected))
//                return true
//            }
//            //throw IllegalArgumentException("mismatched gene impact")
//        }

        //activate the next disjunction
        activeGeneIndex = (activeGeneIndex + 1) % geneChoices.size
        return true
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return randomness.nextBoolean(0.1) //TODO check for proper value
    }


    /**
     * Returns the value of the active gene as a printable string
     */
    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return geneChoices[activeGeneIndex]
            .getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    /**
     * Returns the value of the active gene as a raw string
     */
    override fun getValueAsRawString(): String {
        return geneChoices[activeGeneIndex]
            .getValueAsRawString()
    }

    /**
     * Copies the value of the other gene. The other gene
     * has to be a [ChoiceGene] with the same number
     * of gene choices. The value of each gene choice
     * is also copied.
     */
    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        } else if (geneChoices.size != other.geneChoices.size) {
            throw IllegalArgumentException("Cannot copy value from another choice gene with  ${other.geneChoices.size} choices (current gene has ${geneChoices.size} choices)")
        } else {
            return updateValueOnlyIfValid(
                {
                    this.activeGeneIndex = other.activeGeneIndex
                    var ok = true
                    for (i in geneChoices.indices) {
                        // short circuit if ok is false
                        ok =  ok && this.geneChoices[i].copyValueFrom(other.geneChoices[i])
                    }
                    ok
                }, true
            )
        }
    }

    /**
     * Checks that the other gene is another ChoiceGene,
     * the active gene index is the same, and the gene choices are the same.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ChoiceGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.activeGeneIndex != other.activeGeneIndex) {
            return false
        }

        return this.geneChoices[activeGeneIndex]
            .containsSameValueAs(other.geneChoices[activeGeneIndex])
    }

    /**
     * Binds this gene to another [ChoiceGene<T>] with the same number of
     * gene choices, one gene choice to the corresponding gene choice in
     * the other gene.
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is ChoiceGene<*> && gene.geneChoices.size == geneChoices.size) {
            var result = true
            geneChoices.indices.forEach { i ->
                val r = geneChoices[i].bindValueBasedOn(gene.geneChoices[i])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind disjunctions (name: ${geneChoices[i].name}) at index $i")
                result = result && r
            }

            activeGeneIndex = gene.activeGeneIndex
            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind ChoiceGene with ${gene::class.java.simpleName}")
        return false
    }

    /**
     * Returns a copy of this gene choice by copying
     * all gene choices.
     */
    override fun copyContent(): Gene = ChoiceGene(
        name,
        activeChoice = this.activeGeneIndex,
        geneChoices = this.geneChoices.map { it.copy() }.toList(),
        probabilities = probabilities // immutable
    )

    /**
     * Checks that the active gene is the one locally valid
     */
    override fun isLocallyValid() = geneChoices.all { it.isLocallyValid() }

    override fun isPrintable() = this.geneChoices[activeGeneIndex].isPrintable()


}