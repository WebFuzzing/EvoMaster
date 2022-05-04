package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlCompositeGene(
        // the name of the column
        name: String,
        // the ordered components of the column
        val fields: List<out Gene>,
        // the name of the composite type
        val compositeTypeName: String? = null
) : Gene(name, mutableListOf<StructuralElement>().apply { addAll(fields) }) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SqlCompositeGene::class.java)

    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        fields.filter { it.isMutable() }
                .forEach { it.randomize(randomness, forceNewValue, allGenes) }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "ROW(${
            fields
                    .map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                    .joinToString { SqlStrings.replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder(it) }
        })"
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlCompositeGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in fields.indices) {
            this.fields[i].copyValueFrom(other.fields[i])
        }
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

    override fun innerGene(): List<Gene> = fields


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlCompositeGene && (fields.indices).all { fields[it].possiblySame(gene.fields[it]) }) {
            var result = true
            (fields.indices).forEach {
                val r = fields[it].bindValueBasedOn(gene.fields[it])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the field ${fields[it].name}")
                result = result && r
            }
            if (!result)
                LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} (with the refType ${compositeTypeName ?: "null"}) with the object gene (with the refType ${gene.compositeTypeName ?: "null"})")
            return result
        }
        // might be cycle object genet
        LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} (with the refType ${compositeTypeName ?: "null"}) with ${gene::class.java.simpleName}")
        return false
    }

    override fun getChildren() = fields

    override fun copyContent() = SqlCompositeGene(this.name, fields.map { it.copyContent() }.toList(), this.compositeTypeName)

    /**
     * Dummy mutation for composite genes
     */
    override fun mutate(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            mwc: MutationWeightControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        this.randomize(randomness, true, allGenes)
        return true
    }
}