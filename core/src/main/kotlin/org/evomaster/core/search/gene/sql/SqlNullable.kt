package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.sql.SqlNullableImpact
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
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

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {

        if(! isPresent){
            isPresent = true
        } else if(randomness.nextBoolean(0.1)){
            isPresent = false
        } else {
            gene.standardMutation(randomness, apc, allGenes)
        }
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if(!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }

        val preferPresent = if (!archiveMutator.applyArchiveSelection() || impact == null || impact !is SqlNullableImpact) true
                    else {
            //we only set 'present' false from true when the mutated times is more than 5 and its impact times of a falseValue is more than 1.5 times of a trueValue.
            !impact.presentImpact.run {
                this.timesToManipulate > 5
                        &&
                        (this.falseValue.timesOfImpact.filter { targets.contains(it.key) }.map { it.value }.max()?:0) > ((this.trueValue.timesOfImpact.filter { targets.contains(it.key) }.map { it.value }.max()?:0) * 1.5)
            }
        }

        if (preferPresent){
            if (!isPresent){
                isPresent = true
                presentMutationInfo.counter+=1
                return
            }
            if (randomness.nextBoolean(ABSENT)){
                isPresent = false
                presentMutationInfo.counter+=1
                return
            }
        }else{
            //if preferPresent is false, it is not necessary to mutate the gene
            presentMutationInfo.reached = archiveMutator.withinNormal()
            if (presentMutationInfo.reached){
                presentMutationInfo.preferMin = 0
                presentMutationInfo.preferMax = 0
            }

            if (isPresent){
                isPresent = false
                presentMutationInfo.counter+=1
                return
            }
            if (randomness.nextBoolean(ABSENT)){
                isPresent = true
                presentMutationInfo.counter+=1
                return
            }
        }
        gene.archiveMutation(randomness, allGenes, apc, selection, if (impact == null || impact !is SqlNullableImpact) null else impact.geneImpact, geneReference, archiveMutator, evi, targets)
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
}