package org.evomaster.core.search.gene.string

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class Base64StringGene(
        name: String,
        val data: StringGene = StringGene("data")
) : CompositeFixedGene(name, data) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(Base64StringGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = Base64StringGene(name, data.copy() as StringGene)

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        data.randomize(randomness, tryToForceNewValue)
    }



    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return Base64.getEncoder().encodeToString(data.value.toByteArray())
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.data.containsSameValueAs(other.data)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        return when(gene){
            is Base64StringGene -> data.unsafeCopyValueFrom(gene.data)
            is StringGene -> data.unsafeCopyValueFrom(gene)
            else->{
                LoggingUtil.uniqueWarn(log, "cannot bind the Base64StringGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    @Deprecated("Do not call directly outside this package. Call setFromStringValue")
    override fun unsafeSetFromStringValue(value: String): Boolean {
        return try {
            // Charset is set to UTF_8 to ensure decoding works properly
            val value = Base64
                .getDecoder()
                .decode(value)
                .toString(Charsets.UTF_8)

            data.setFromStringValue(value)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}
