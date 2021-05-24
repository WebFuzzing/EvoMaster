package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator


class SqlAutoIncrementGene(name: String) : Gene(name) {

    override fun copy(): Gene {
        return SqlAutoIncrementGene(name)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        throw IllegalStateException("AutoIncrement fields are not part of the search")
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        throw IllegalStateException("AutoIncrement fields should never be printed")
    }

    /**
     * TODO Shouldn't this method throw an IllegalStateException ?
     */
    override fun copyValueFrom(other: Gene) {
        if (other !is SqlAutoIncrementGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        //do nothing
    }

    /**
     * Since each object instance of SqlAutoIncrementGene represents a different
     * value for that particular column, we check for object identity to check
     * that two genes have the same value
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlAutoIncrementGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this === other
    }

    override fun isMutable() = false

    override fun isPrintable() = false

    override fun mutationWeight(): Double = 0.0

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        // do nothing, cannot bind with others
        return true
    }
}