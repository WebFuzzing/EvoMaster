package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.regex.DisjunctionListRxGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DisjunctionListRxGene(
        val disjunctions: List<DisjunctionRxGene>
) : RxAtom, CompositeFixedGene("disjunction_list", disjunctions) {

    //FIXME refactor with ChoiceGene

    var activeDisjunction: Int = 0

    companion object{
        private const val PROB_NEXT = 0.1
        private val log: Logger = LoggerFactory.getLogger(DisjunctionListRxGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return activeDisjunction >= 0 && activeDisjunction < disjunctions.size
    }
    override fun copyContent(): Gene {
        val copy = DisjunctionListRxGene(disjunctions.map { it.copy() as DisjunctionRxGene })
        copy.activeDisjunction = this.activeDisjunction
        return copy
    }

    override fun isMutable(): Boolean {
        return disjunctions.size > 1 || disjunctions.any { it.isMutable() }
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        /*
            randomize content of all disjunctions
            (since standardMutation can be invoked on another term)
         */
        disjunctions.forEach {  it.randomize(randomness,tryToForceNewValue) }

        /**
         * randomly choose a new disjunction term
         */
        if (disjunctions.size > 1) {
            log.trace("random disjunctions of DisjunctionListRxGene")
            activeDisjunction = randomness.nextInt(0, disjunctions.size-1)
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {

        if(disjunctions.size > 1
            && (!disjunctions[activeDisjunction].isMutable() || randomness.nextBoolean(PROB_NEXT))){
            //activate the next disjunction
            return true
        }
        return false
    }

    override fun mutablePhenotypeChildren(): List<Gene> {
        return listOf(disjunctions[activeDisjunction]).filter { it.isMutable() }
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact == null || additionalGeneMutationInfo.impact !is DisjunctionListRxGeneImpact)
            throw IllegalArgumentException("mismatched gene impact")

        if (!disjunctions.containsAll(internalGenes))
            throw IllegalArgumentException("mismatched internal genes")

        val impacts = internalGenes.map {
            additionalGeneMutationInfo.impact.disjunctions[disjunctions.indexOf(it)]
        }

        val selected = mwc.selectSubGene(
                candidateGenesToMutate = internalGenes,
                impacts = impacts,
                targets = additionalGeneMutationInfo.targets,
                forceNotEmpty = true,
                adaptiveWeight = true
        )
        return selected.map { it to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.disjunctions[disjunctions.indexOf(it)], it) }.toList()
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        // select another disjunction based on impact
        if (enableAdaptiveGeneMutation || selectionStrategy == SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT){
            additionalGeneMutationInfo?:throw IllegalStateException("")
            if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is DisjunctionListRxGeneImpact){
                val candidates = disjunctions.filterIndexed { index, _ -> index != activeDisjunction  }
                val impacts = candidates.map {
                    additionalGeneMutationInfo.impact.disjunctions[disjunctions.indexOf(it)]
                }

                val selected = mwc.selectSubGene(
                        candidateGenesToMutate = candidates,
                        impacts = impacts,
                        targets = additionalGeneMutationInfo.targets,
                        forceNotEmpty = true,
                        adaptiveWeight = true
                )
                activeDisjunction = disjunctions.indexOf(randomness.choose(selected))
                return true
            }
                //throw IllegalArgumentException("mismatched gene impact")
        }

        //activate the next disjunction
        activeDisjunction = (activeDisjunction + 1) % disjunctions.size
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        if (disjunctions.isEmpty()) {
            return ""
        }
        return disjunctions[activeDisjunction]
                .getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        //TODO: Man, shall we check the size of [disjunctions]

        return updateValueOnlyIfValid({
            var ok = true
            for (i in 0 until disjunctions.size) {
                ok = ok && this.disjunctions[i].copyValueFrom(other.disjunctions[i])
            }
            if (ok){
                this.activeDisjunction = other.activeDisjunction
            }
            ok
        }, true)

    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is DisjunctionListRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.activeDisjunction != other.activeDisjunction) {
            return false
        }

        return this.disjunctions[activeDisjunction]
                .containsSameValueAs(other.disjunctions[activeDisjunction])
    }



    override fun mutationWeight(): Double = disjunctions.map { it.mutationWeight() }.sum() + 1


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is DisjunctionListRxGene && gene.disjunctions.size == disjunctions.size){
            var result = true
            disjunctions.indices.forEach { i->
                val r = disjunctions[i].setValueBasedOn(gene.disjunctions[i])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind disjunctions (name: ${disjunctions[i].name}) at index $i")
                result = result && r
            }

            activeDisjunction = gene.activeDisjunction
            return result
        }

        LoggingUtil.uniqueWarn(log, "cannot bind DisjunctionListRxGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun isChildUsed(child: Gene) : Boolean {
        verifyChild(child)
        return child == disjunctions[activeDisjunction]
    }
}