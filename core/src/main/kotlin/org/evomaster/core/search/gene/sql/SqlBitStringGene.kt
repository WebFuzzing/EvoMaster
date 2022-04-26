package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.sql.SqlStrings.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Bit strings are strings of 1's and 0's.
 */
class SqlBitStringGene(
        /**
         * The name of this gene
         */
        name: String,

        val minSize: Int = 0,

        val maxSize: Int = ArrayGene.MAX_SIZE,

        private val booleanArrayGene: ArrayGene<BooleanGene> = ArrayGene(name, template = BooleanGene(name), minSize = minSize, maxSize = maxSize)

) : CollectionGene, Gene(name, booleanArrayGene.getAllElements()) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlBitStringGene::class.java)

        const val TRUE_VALUE = "1"

        const val FALSE_VALUE = "0"

        const val EMPTY_STR = ""
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        booleanArrayGene.randomize(randomness, forceNewValue, allGenes)
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return buildString {
            append("B$SINGLE_APOSTROPHE_PLACEHOLDER")
            append(booleanArrayGene.getChildren().map { g ->
                if (g.value) TRUE_VALUE else FALSE_VALUE
            }.joinToString(EMPTY_STR))
            append(SINGLE_APOSTROPHE_PLACEHOLDER)
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlBitStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        booleanArrayGene.copyValueFrom(other.booleanArrayGene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlBitStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return booleanArrayGene.containsSameValueAs(other.booleanArrayGene)
    }

    override fun innerGene(): List<Gene> {
        return listOf(booleanArrayGene)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlBitStringGene) {
            return booleanArrayGene.bindValueBasedOn(gene.booleanArrayGene)
        }
        LoggingUtil.uniqueWarn(log, "cannot bind SqlBitstringGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun getChildren(): List<out StructuralElement> {
        return booleanArrayGene.getChildren()
    }

    override fun clearElements() {
        return booleanArrayGene.clearElements()
    }

    override fun isEmpty() = booleanArrayGene.isEmpty()

    override fun getMaxSizeOrDefault() = booleanArrayGene.getMaxSizeOrDefault()

    override fun getSpecifiedMaxSize() = booleanArrayGene.getSpecifiedMaxSize()

    override fun getMinSizeOrDefault() = booleanArrayGene.getMinSizeOrDefault()

    override fun getSpecifiedMinSize() = booleanArrayGene.getSpecifiedMinSize()

    override fun getSizeOfElements(filterMutable: Boolean) = booleanArrayGene.getSizeOfElements(filterMutable)

    override fun getGeneName() = name

    override fun getDefaultMaxSize() = booleanArrayGene.getDefaultMaxSize()

    override fun copyContent() = SqlBitStringGene(name, minSize = minSize, maxSize = maxSize, booleanArrayGene.copyContent() as ArrayGene<BooleanGene>)


}