package org.evomaster.core.search.gene.mongo

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * In MongoDB, documents stored in a collection require a unique _id field that acts as a primary key.
 * ObjectIds are used as the default value for the _id field
 */
class ObjectIdGene(
    /**
     * The name of this gene
     */
    name: String,

    private val id: RegexGene = RegexHandler.createGeneForJVM("^[0-9a-f]{24}$")

) : CompositeFixedGene(name, mutableListOf(id)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ObjectIdGene::class.java)
    }

    override fun isLocallyValid(): Boolean {
        return id.isLocallyValid()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        id.randomize(randomness, tryToForceNewValue)
    }

    override fun mutablePhenotypeChildren(): List<Gene> {
        return listOf(id)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val hexString = id.getValueAsPrintableString()

        return if (mode == GeneUtils.EscapeMode.EJSON) "{\"\$oid\":$hexString}" else hexString
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is ObjectIdGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            { id.copyValueFrom(other.id) }, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ObjectIdGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return id.containsSameValueAs(other.id)
    }


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is ObjectIdGene) {
            return id.bindValueBasedOn(gene.id)
        }
        LoggingUtil.uniqueWarn(log, "cannot bind SqlBitstringGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun copyContent() = ObjectIdGene(
        name,
        id.copy() as RegexGene
    )

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }
}