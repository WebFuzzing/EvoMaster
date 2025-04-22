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
                   /**
                    * constrain class of FlexibleGene
                    * when [valueClasses] is not null,
                    * the FlexibleGene can be replaced only if its class is part of [valueClasses]
                    */
                   val valueClasses : List<Class<*>>?,
                   private var replaceable: Boolean = true
) : CompositeGene(name, mutableListOf(gene)) {

    init {
        geneCheck(gene)
        if (valueClasses != null && valueClasses.isEmpty())
            throw IllegalArgumentException("cannot specify an empty valueClasses")
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(FlexibleGene::class.java)

        fun wrapWithFlexibleGene(gene: Gene, valueClasses : List<Class<*>>?, replaceable: Boolean = true) : FlexibleGene{
            if (gene is FlexibleGene) {
                if (gene.replaceable != replaceable)
                    return FlexibleGene(gene.name, gene.gene, gene.valueClasses, replaceable)
                return gene
            }
            return FlexibleGene(gene.name, gene,valueClasses,replaceable)
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
        if (valueClasses != null && !valueClasses.contains(geneToUpdate::class.java))
            throw IllegalStateException("cannot replace a gene whose type (${geneToUpdate::class.java.name}) is not part of specified valueClasses (${valueClasses.joinToString(",") { it.name }})")

        geneCheck(geneToUpdate)
        Lazy.assert { children.size == 1 }

        killAllChildren()
        geneToUpdate.resetLocalIdRecursively()
        addChild(geneToUpdate)
    }

    @Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
    override fun <T,K> getWrappedGene(klass: Class<K>, strict: Boolean) : T?  where T : Gene, T: K{
        if(matchingClass(klass,strict)){
            return this as T
        }
        return gene.getWrappedGene(klass)
    }

    override fun copyContent(): FlexibleGene {
        return FlexibleGene(name, gene.copy(), valueClasses, replaceable)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        gene.randomize(randomness, tryToForceNewValue)

        //TODO if valueClasses is not null or more than 2 types, might add another
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

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is FlexibleGene)
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        if (replaceable){

            if (!other.isLocallyValid())
                return false

            try {
                geneCheck(other)
            }catch (e: Exception){
                return false
            }

            val replaced = other.gene.copy()
            replaced.resetLocalIdRecursively()
            replaceGeneTo(replaced)

        }else{
            // TODO need to refactor
            log.warn("TOCHECK, attempt to copyValueFrom when it is not replaceable")
        }

        return false

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

    override fun setValueBasedOn(gene: Gene): Boolean {
        return false
    }

    override fun isPrintable(): Boolean {
        return gene.isPrintable()
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }


    override fun possiblySame(gene: Gene): Boolean {
        return gene is FlexibleGene && (
                (valueClasses == null && gene.valueClasses == null)
                        || (valueClasses != null && gene.valueClasses != null && valueClasses.size == gene.valueClasses.size && valueClasses.containsAll(gene.valueClasses))
                )
    }

    private fun geneCheck(geneToBeUpdated : Gene){
        if (geneToBeUpdated is FlexibleGene){
            throw IllegalArgumentException("For a FlexibleGene, its gene to be employed or updated cannot be FlexibleGene")
        }
    }
}