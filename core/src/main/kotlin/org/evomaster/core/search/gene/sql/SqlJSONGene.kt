package org.evomaster.core.search.gene.sql

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.sql.SqlJsonGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlJSONGene(name: String, val objectGene: ObjectGene = ObjectGene(name, fields = listOf())) : Gene(name) {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlJSONGene::class.java)
    }

    init {
        objectGene.parent = this
    }

    override fun copy(): Gene = SqlJSONGene(
            name,
            objectGene = this.objectGene.copy() as ObjectGene)


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        objectGene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun standardMutation(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>) {
        objectGene.standardMutation(randomness, apc, allGenes)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        val rawValue = objectGene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.JSON, targetFormat)
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

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlJSONGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.objectGene.copyValueFrom(other.objectGene)
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlJSONGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.objectGene.containsSameValueAs(other.objectGene)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(objectGene.flatView(excludePredicate))
    }

    override fun archiveMutation(randomness: Randomness, allGenes: List<Gene>, apc: AdaptiveParameterControl, selection: GeneMutationSelectionMethod, impact: GeneImpact?, geneReference: String, archiveMutator: ArchiveMutator, evi: EvaluatedIndividual<*>, targets: Set<Int>) {
        if(!archiveMutator.enableArchiveMutation()){
            standardMutation(randomness, apc, allGenes)
            return
        }

        objectGene.archiveMutation(randomness, allGenes, apc, selection, if(impact == null || impact !is SqlJsonGeneImpact) null else impact.geneImpact, geneReference, archiveMutator, evi, targets)
    }

    override fun archiveMutationUpdate(original: Gene, mutated: Gene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        if (archiveMutator.enableArchiveGeneMutation()){
            if (original !is SqlJSONGene){
                log.warn("original ({}) should be SqlJSONGene", original::class.java.simpleName)
                return
            }
            if (mutated !is SqlJSONGene){
                log.warn("mutated ({}) should be SqlJSONGene", mutated::class.java.simpleName)
                return
            }
            objectGene.archiveMutationUpdate(original.objectGene, mutated.objectGene, doesCurrentBetter, archiveMutator)
        }
    }

    override fun reachOptimal(): Boolean {
        return objectGene.reachOptimal()
    }
}