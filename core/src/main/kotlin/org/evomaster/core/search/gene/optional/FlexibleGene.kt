package org.evomaster.core.search.gene.optional

import org.evomaster.core.Lazy
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * This represents a flexible gene which does not stick to a specific type
 * the type can be dynamically updated,
 * ie, [gene] can be replaced with [replaceGeneTo] method
 *
 * this gene is now mainly used to support array and map whose template
 * might not follow a fixed typed. FlexibleMap is added, might need it for
 * array gene
 */
class FlexibleGene(name: String,
                   gene: Gene,
                   private var replaceable: Boolean = true
) : CompositeGene(name, mutableListOf(gene)) {

    init {
        geneCheck(gene)
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(FlexibleGene::class.java)

        fun wrapWithFlexibleGene(gene: Gene, replaceable: Boolean = true) : FlexibleGene{
            if (gene is FlexibleGene) {
                if (gene.replaceable != replaceable)
                    return FlexibleGene(gene.name, gene.gene, replaceable)
                return gene
            }
            return FlexibleGene(gene.name, gene, replaceable)
        }
    }

    val gene: Gene
        get() {return children.first()}

    /**
     * forbid replacing gene to other
     */
    fun forbidReplace(){
        replaceable = false
    }

    fun replaceGeneTo(geneToUpdate: Gene){
        if (!replaceable)
            throw IllegalStateException("attempt to replace the gene which is not replaceable")
        geneCheck(geneToUpdate)
        Lazy.assert { children.size == 1 }

        killAllChildren()
        geneToUpdate.resetLocalIdRecursively()
        addChild(geneToUpdate)
    }

    override fun <T> getWrappedGene(klass: Class<T>) : T?  where T : Gene{
        if(this.javaClass == klass){
            return this as T
        }
        return gene.getWrappedGene(klass)
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

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    private fun geneCheck(geneToBeUpdated : Gene){
        if (geneToBeUpdated is FlexibleGene){
            throw IllegalArgumentException("For a FlexibleGene, its gene to be employed or updated cannot be FlexibleGene")
        }
    }
}