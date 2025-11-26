package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.GeneUtils.replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlCompositeGene(
        // the name of the column
        name: String,
        // the ordered components of the column
        val fields: List<out Gene>,
        // the name of the composite type
        val compositeTypeName: String? = null
) : CompositeFixedGene(name, fields.toMutableList()) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SqlCompositeGene::class.java)

        const val SINGLE_APOSTROPHE_PLACEHOLDER = "SINGLE_APOSTROPHE_PLACEHOLDER"
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        fields.filter { it.isMutable() }
                .forEach { it.randomize(randomness, tryToForceNewValue) }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }


    private val QUOTATION_MARK = "\""

    private fun replaceEnclosedQuotationMarks(str: String): String {
        if (str.startsWith(QUOTATION_MARK) && str.endsWith(QUOTATION_MARK)) {
            return SINGLE_APOSTROPHE_PLACEHOLDER + str.subSequence(1, str.length - 1) + SINGLE_APOSTROPHE_PLACEHOLDER
        } else {
            return str
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "ROW(${
            fields.filter { it.isPrintable() }
                    .map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                    .joinToString { replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder(it) }
        })"
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlCompositeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return ((this.fields.size == other.fields.size)
                && this.fields.zip(other.fields)
        { thisField, otherField ->
            thisField.containsSameValueAs(otherField)
        }.all { it })
    }


    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is SqlCompositeGene) {
            return false
        }

        var ok = true
        for (i in fields.indices) {
            ok = ok && this.fields[i].unsafeCopyValueFrom(other.fields[i])
        }
        return ok
    }

    override fun copyContent() = SqlCompositeGene(this.name, fields.map { it.copy() }.toList(), this.compositeTypeName)


}