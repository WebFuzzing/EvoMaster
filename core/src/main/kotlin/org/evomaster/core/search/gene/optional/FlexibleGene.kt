package org.evomaster.core.search.gene.optional

import org.evomaster.core.Lazy
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FlexibleGene(name: String,
                   gene: Gene,
                   val replaceable: Boolean = false
) : CompositeGene(name, mutableListOf(gene)) {

    init {
        // gene cannot be flexible gene
        Lazy.assert { gene !is FlexibleGene }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(FlexibleGene::class.java)
    }

    val gene : Gene = children[0]

    fun replaceGeneTo(gene: Gene){
        if (!replaceable)
            throw IllegalStateException("attempt to replace the gene which is not replaceable")
        Lazy.assert { children.size == 1 }
        killChildByIndex(0)
        addChild(gene)
    }

    override fun copyContent(): FlexibleGene {
        return FlexibleGene(name, gene.copy(), replaceable)
    }

    override fun isLocallyValid(): Boolean {
        return gene.isLocallyValid()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        gene.randomize(randomness, tryToForceNewValue)
    }

    override fun isMutable(): Boolean {
        return gene.isMutable()
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        /*
            do not develop shallowMutation yet

            TODO
            we might employ shallowMutation to replace gene with different genes
         */
        return false
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is FlexibleGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        if (replaceable){
            val replaced = other.gene.copy()
            replaced.resetLocalIdRecursively()
            replaceGeneTo(replaced)
        }else{
            // TODO need to refactor
            log.warn("TOCHECK, attempt to copyValueFrom when it is not replaceable")
        }

    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other is FlexibleGene){
            return try {
                gene.containsSameValueAs(other.gene)
            }catch (e : Exception){
                return false
            }
        }
        return false
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return false
    }

    override fun isPrintable(): Boolean {
        return gene.isPrintable()
    }

}