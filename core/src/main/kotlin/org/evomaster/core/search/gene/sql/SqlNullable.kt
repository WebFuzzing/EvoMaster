package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.sql.SqlNullableImpact
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException


class SqlNullable(name: String,
                  val gene: Gene,
                  var isPresent: Boolean = true,
                  val presentMutationInfo : IntMutationUpdate = IntMutationUpdate(0, 1)
) : SqlWrapperGene(name) {

    init{
        if(gene is SqlWrapperGene && gene.getForeignKey() != null){
            throw IllegalStateException("SqlNullable should not contain a FK, " +
                    "as its nullability is handled directly in SqlForeignKeyGene")
        }

        gene.parent = this
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlNullable::class.java)
        private const val ABSENT = 0.1
    }

    override fun getForeignKey(): SqlForeignKeyGene? {
        return null
    }

    override fun copy(): Gene {
        return SqlNullable(name, gene.copy(), isPresent, presentMutationInfo.copy())
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        isPresent = if (!isPresent && forceNewValue)
            true
        else
            randomness.nextBoolean(ABSENT)

        gene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {

        if (!isPresent) return emptyList()

        if (!enableAdaptiveGeneMutation){
            return if (randomness.nextBoolean(ABSENT)) emptyList() else listOf(gene)
        }
        if (additionalGeneMutationInfo?.impact != null && additionalGeneMutationInfo.impact is SqlNullableImpact){
            //we only set 'active' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            val inactive = additionalGeneMutationInfo.impact.presentImpact.run {
                this.timesToManipulate > 5
                        &&
                        (this.falseValue.timesOfImpact.filter { additionalGeneMutationInfo.targets.contains(it.key) }.map { it.value }.max()?:0) > ((this.trueValue.timesOfImpact.filter { additionalGeneMutationInfo.targets.contains(it.key) }.map { it.value }.max()?:0) * 1.5)
            }
            if (inactive) return emptyList() else listOf(gene)
        }
        throw IllegalArgumentException("impact is null or not OptionalGeneImpact")
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): Map<Gene, AdditionalGeneSelectionInfo?> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlNullableImpact){
            return mapOf(gene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact))
        }
        throw IllegalArgumentException("impact is null or not SqlNullableImpact")
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?) : Boolean{

        isPresent = !isPresent
        if (enableAdaptiveGeneMutation){
            //TODO MAN further check
            //if preferPresent is false, it is not necessary to mutate the gene
//            presentMutationInfo.reached = additionalGeneMutationInfo.archiveMutator.withinNormal()
//            if (presentMutationInfo.reached){
//                presentMutationInfo.preferMin = 0
//                presentMutationInfo.preferMax = 0
//            }

            presentMutationInfo.counter+=1
        }

        return true
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is SqlNullable){
                log.warn("original ({}) should be SqlNullable", original::class.java.simpleName)
                return
            }
            if (mutated !is SqlNullable){
                log.warn("mutated ({}) should be SqlNullable", mutated::class.java.simpleName)
                return
            }
            if (original.isPresent == mutated.isPresent && mutated.isPresent)
                gene.archiveMutationUpdate(original.gene, mutated.gene, doesCurrentBetter, archiveMutator)
            /**
             * may handle Boolean Mutation in the future
             */
        }
    }

    override fun reachOptimal(): Boolean {
        return (presentMutationInfo.reached && presentMutationInfo.preferMin == 0 && presentMutationInfo.preferMax == 0) ||  gene.reachOptimal()
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {

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
}