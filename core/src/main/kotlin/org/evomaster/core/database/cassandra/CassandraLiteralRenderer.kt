package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Renders the value of a gene as a CQL literal, ie as it would be written inside a CQL statement.
 *
 * This is needed because such literals are inserted verbatim into the INSERT command built on the
 * client side, and how a value has to be written depends on its type: text and the temporal types
 * are enclosed in single quotes, whereas numbers, booleans and uuids are not.
 */
object CassandraLiteralRenderer {

    private const val SINGLE_QUOTE = "'"

    /**
     * In CQL, a single quote inside a text literal is escaped by doubling it.
     */
    private const val ESCAPED_SINGLE_QUOTE = "''"

    /**
     * @throws IllegalArgumentException if there is no known CQL representation for [gene], which
     * should not happen for the genes built by [CassandraColumnGeneBuilder]
     */
    fun toCqlLiteral(gene: Gene): String {

        val value = gene.getValueAsRawString()

        return when (gene) {
            is StringGene, is DateGene, is TimeGene, is DateTimeGene -> quote(value)
            is BooleanGene, is UUIDGene, is NumberGene<*> -> value
            else -> throw IllegalArgumentException("Cannot render a CQL literal for a gene of type ${gene.javaClass.simpleName}")
        }
    }

    private fun quote(value: String) =
        SINGLE_QUOTE + value.replace(SINGLE_QUOTE, ESCAPED_SINGLE_QUOTE) + SINGLE_QUOTE
}
