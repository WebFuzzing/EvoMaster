package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlNullableImpact
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException


class SqlNullable(name: String,
                  val gene: Gene,
                  var isPresent: Boolean = true
) : SqlWrapperGene(name, listOf(gene)) {

    init{
        if(gene is SqlWrapperGene && gene.getForeignKey() != null){
            throw IllegalStateException("SqlNullable should not contain a FK, " +
                    "as its nullability is handled directly in SqlForeignKeyGene")
        }
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlNullable::class.java)
        private const val ABSENT = 0.1
    }

    override fun getChildren(): List<Gene> = listOf(gene)


    override fun getForeignKey(): SqlForeignKeyGene? {
        return null
    }

    override fun copyContent(): Gene {
        return SqlNullable(name, gene.copyContent(), isPresent)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        isPresent = if (!isPresent && forceNewValue)
            true
        else
            randomness.nextBoolean(ABSENT)

        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {

        if (!isPresent) return emptyList()

        if (!enableAdaptiveGeneMutation || additionalGeneMutationInfo?.impact == null){
            return if (randomness.nextBoolean(ABSENT)) emptyList() else listOf(gene)
        }
        if (additionalGeneMutationInfo.impact is SqlNullableImpact){
            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            val inactive = additionalGeneMutationInfo.impact.presentImpact.determinateSelect(
                    minManipulatedTimes = 5,
                    times = 1.5,
                    preferTrue = true,
                    targets = additionalGeneMutationInfo.targets,
                    selector = additionalGeneMutationInfo.archiveGeneSelector
            )

            return if (inactive)  emptyList() else listOf(gene)
        }
        throw IllegalArgumentException("impact is not SqlNullableImpact ${additionalGeneMutationInfo.impact::class.java.simpleName}")
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlNullableImpact){
            return listOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, gene))
        }
        throw IllegalArgumentException("impact is null or not SqlNullableImpact")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?) : Boolean{

        isPresent = !isPresent
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        if (!isPresent) {
            return "NULL"
        }

        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlNullable) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.isPresent = other.isPresent
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlNullable) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.isPresent == other.isPresent &&
                this.gene.containsSameValueAs(other.gene)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun mutationWeight(): Double {
        return 1.0 + gene.mutationWeight()
    }

    override fun innerGene(): List<Gene> = listOf(gene)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlNullable) isPresent = gene.isPresent
        return ParamUtil.getValueGene(gene).bindValueBasedOn(ParamUtil.getValueGene(gene))
    }
}