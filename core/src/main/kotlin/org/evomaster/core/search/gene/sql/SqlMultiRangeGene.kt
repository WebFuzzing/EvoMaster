package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.SqlStrings.removeEnclosedQuotationMarks
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This gene is used for Postgres SQL multigrange types such as int4multirange,
 * int8multirange, nummultirange, tsmultirange, tstzmultirange and
 * datemultirange.
 *
 * A multirange is a sequence (potentially empty) of range types. For example,
 * a int4multirange is a sequence of int4range values.
 * Some examples are:
 *   {} an empty multirange
 *   { empty } a multirange with a single, empty, range
 *   {[ 0.0 , 0.23751964 ], [ 0.0 , 0.0 ], [ 0.0 , 0.82279074 ], empty} a nummultirange
 *   with 4 different numranges.
 */
class SqlMultiRangeGene<T>(
        name: String,
        val template: SqlRangeGene<T>,
        val rangeGenes: ArrayGene<SqlRangeGene<T>> = ArrayGene(name, template)
) : Gene(name, mutableListOf(rangeGenes)) where T : ComparableGene {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlMultiRangeGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(rangeGenes)

    override fun copyContent(): Gene {
        val copyOfRangeGenes = rangeGenes.copyContent() as ArrayGene<SqlRangeGene<T>>
        return SqlMultiRangeGene(
                name,
                template = copyOfRangeGenes.template,
                rangeGenes = copyOfRangeGenes
        )
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        rangeGenes.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(rangeGenes)
    }

    override fun getValueAsRawString(): String {
        return "{ ${
            rangeGenes.getAllElements()
                    .map { it.getValueAsRawString() }
                    .joinToString(" , ")
        } } "
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultiRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.rangeGenes.copyValueFrom(other.rangeGenes)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultiRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.rangeGenes.containsSameValueAs(other.rangeGenes)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(rangeGenes.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(rangeGenes)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlMultiRangeGene<*> -> {
                rangeGenes.bindValueBasedOn(gene.rangeGenes)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind ${this::class.java.simpleName} with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return "\"{" +
                rangeGenes.elements.map { g ->
                    removeEnclosedQuotationMarks(g.getValueAsPrintableString(previousGenes, mode, targetFormat))
                }.joinToString(", ") +
                "}\""
    }

}