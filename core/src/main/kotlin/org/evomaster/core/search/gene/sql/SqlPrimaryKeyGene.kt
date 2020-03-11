package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.sql.SqlPrimaryKeyGeneImpact
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A wrapper around a Gene to represent a column in a SQL database
 * where such column is a Primary Key.
 * This is important to check Foreign Keys referencing it.
 */
class SqlPrimaryKeyGene(name: String,
                        val tableName: String,
                        val gene: Gene,
                        /**
                         * Important for the Foreign Keys referencing it.
                         * Cannot be negative
                         */
                        val uniqueId: Long
) : SqlWrapperGene(name) {


    init {
        if (uniqueId < 0) {
            throw IllegalArgumentException("Negative unique id")
        }

        gene.parent = this
    }

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlPrimaryKeyGene::class.java)
    }

    override fun getForeignKey(): SqlForeignKeyGene? {
        if(gene is SqlWrapperGene){
            return gene.getForeignKey()
        }
        return null
    }

    override fun copy() = SqlPrimaryKeyGene(name, tableName, gene.copy(), uniqueId)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        gene.randomize(randomness, false, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        gene.standardMutation(randomness, apc, allGenes)
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if (!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }
        gene.archiveMutation(randomness, allGenes, apc, selection, if(impact != null || impact !is SqlPrimaryKeyGeneImpact) null else (impact as SqlPrimaryKeyGeneImpact).geneImpact, geneReference, archiveMutator, evi, targets)
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is SqlPrimaryKeyGene){
                log.warn("original ({}) should be SqlPrimaryKeyGene", original::class.java.simpleName)
                return
            }
            if (mutated !is SqlPrimaryKeyGene){
                log.warn("mutated ({}) should be SqlPrimaryKeyGene", mutated::class.java.simpleName)
                return
            }
            gene.archiveMutationUpdate(original.gene, mutated.gene, doesCurrentBetter, archiveMutator)
        }
    }

    override fun reachOptimal(): Boolean {
        return gene.reachOptimal()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.gene.copyValueFrom(other.gene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPrimaryKeyGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.gene.containsSameValueAs(other.gene)
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
    }

    override fun getValueAsRawString(): String {
        return gene.getValueAsRawString()
    }

    override fun getVariableName() = gene.getVariableName()

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(gene.flatView(excludePredicate))
    }

    override fun isMutable() = gene.isMutable()

    override fun isPrintable() = gene.isPrintable()


    fun isReferenceToNonPrintable(previousGenes: List<Gene>): Boolean {
        if (gene !is SqlForeignKeyGene) {
            return false
        }

        return gene.isReferenceToNonPrintable(previousGenes)
    }
}