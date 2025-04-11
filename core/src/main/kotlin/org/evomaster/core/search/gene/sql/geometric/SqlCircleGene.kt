package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlCircleGene(
        name: String,
        val c: SqlPointGene = SqlPointGene(name = "c"),
        // radius cannot be negative
        val r: FloatGene = FloatGene(name = "r", min = 0f, minInclusive = true)
) : CompositeFixedGene(name, mutableListOf(c, r)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlCircleGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }


    override fun copyContent(): Gene = SqlCircleGene(
            name,
            c.copy() as SqlPointGene,
            r.copy() as FloatGene
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        c.randomize(randomness, tryToForceNewValue)
        r.randomize(randomness, tryToForceNewValue)
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return "(${c.getValueAsRawString()}, ${r.getValueAsRawString()})"
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlCircleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {this.c.copyValueFrom(other.c) && this.r.copyValueFrom(other.r)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlCircleGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.c.containsSameValueAs(other.c)
                && this.r.containsSameValueAs(other.r)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlCircleGene -> {
                c.setValueBasedOn(gene.c) &&
                        r.setValueBasedOn(gene.r)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind CircleGene with ${gene::class.java.simpleName}")
                false
            }
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