package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlJsonGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * https://www.postgresql.org/docs/14/datatype-json.html#DATATYPE-JSONPATH
 * Gene for JSON path information.
 * The semantics of SQL/JSON path predicates and operators generally follow SQL.
 * At the same time, to provide a natural way of working with JSON data, SQL/JSON
 * path syntax uses some JavaScript conventions:
 *   - Dot (.) is used for member access.
 *   - Square brackets ([]) are used for array access.
 *   - SQL/JSON arrays are 0-relative, unlike regular SQL arrays that start from 1.
 *
 * The jsonpath type implements support for the SQL/JSON path language in PostgreSQL to
 * efficiently query JSON data.
 *
 * TODO this feels like a string following a specific RegEx
 */
class SqlJSONPathGene(
    name: String,
    val pathExpression: StringGene = StringGene(name)
) : CompositeFixedGene(name, mutableListOf(pathExpression)) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SqlJSONPathGene::class.java)
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlJSONPathGene(
        name,
        pathExpression = this.pathExpression.copy() as StringGene
    )


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        pathExpression.randomize(randomness, tryToForceNewValue)
    }


    override fun adaptiveSelectSubsetToMutate(
        randomness: Randomness,
        internalGenes: List<Gene>,
        mwc: MutationWeightControl,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlJsonGeneImpact) {
            if (internalGenes.size != 1 || !internalGenes.contains(pathExpression))
                throw IllegalStateException("mismatched input: the internalGenes should only contain objectGene")
            return listOf(
                pathExpression to additionalGeneMutationInfo.copyFoInnerGene(
                    additionalGeneMutationInfo.impact.geneImpact,
                    pathExpression
                )
            )
        }
        throw IllegalArgumentException("impact is null or not SqlJsonGeneImpact")
    }


    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val rawValue = pathExpression.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.JSON, targetFormat)
        //val rawValue = objectGene.getValueAsRawString()
        when {
            // TODO: refactor with StringGene.getValueAsPrintableString(()
            (targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                .replace("\\", "\\\\")
                .replace("$", "\\$")
            else -> return "\"$rawValue\""
                .replace("\\", "\\\\")
        }
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlJSONPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.pathExpression.copyValueFrom(other.pathExpression)}, false
        )
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlJSONPathGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.pathExpression.containsSameValueAs(other.pathExpression)
    }



    override fun mutationWeight(): Double {
        return pathExpression.mutationWeight()
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when (gene) {
            is SqlJSONPathGene -> pathExpression.setValueBasedOn(gene.pathExpression)
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind SqlJSONGene with ${gene::class.java.simpleName}")
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