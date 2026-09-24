package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlDurationGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.string.StringGene

/**
 * Renders the value of a gene as a CQL literal, ie as it would be written inside a CQL statement.
 *
 * This is needed because such literals are inserted verbatim into the INSERT command built on the
 * client side, and how a value has to be written depends on its type: text, the temporal types and
 * the IP addresses are enclosed in single quotes, whereas numbers, booleans, uuids and durations are
 * not, and the collections are written as a delimited sequence of the literals of what they hold.
 */
object CassandraLiteralRenderer {

    private const val SINGLE_QUOTE = "'"

    /**
     * In CQL, a single quote inside a text literal is escaped by doubling it.
     */
    private const val ESCAPED_SINGLE_QUOTE = "''"

    private const val ELEMENT_SEPARATOR = ", "

    private const val KEY_VALUE_SEPARATOR = ": "

    /**
     * How a list is delimited in a CQL literal, eg "[1, 2]".
     */
    private const val LIST_OPENING = "["
    private const val LIST_CLOSING = "]"

    /**
     * How a set and a map are delimited in a CQL literal, eg "{1, 2}" and "{'a': 1}".
     */
    private const val SET_OR_MAP_OPENING = "{"
    private const val SET_OR_MAP_CLOSING = "}"

    /**
     * @throws IllegalArgumentException if there is no known CQL representation for [gene], which
     * should not happen for the genes built by [CassandraColumnGeneBuilder]
     */
    fun toCqlLiteral(gene: Gene): String {

        return when (gene) {
            is StringGene, is DateGene, is TimeGene, is DateTimeGene, is InetGene -> quote(gene.getValueAsRawString())
            is BooleanGene, is UUIDGene, is NumberGene<*>, is CqlDurationGene -> gene.getValueAsRawString()
            is FixedMapGene<*, *> -> renderMap(gene)
            is ArrayGene<*> -> renderArray(gene)
            else -> throw IllegalArgumentException("Cannot render a CQL literal for a gene of type ${gene.javaClass.simpleName}")
        }
    }

    /**
     * The elements are rendered by recursing, rather than by asking the collection gene to print
     * itself, as each of them has to be written the way a CQL literal of its own type is, eg with a
     * text enclosed in single quotes rather than in the double quotes a gene prints itself with.
     */
    private fun renderMap(gene: FixedMapGene<*, *>): String {

        return gene.getViewOfChildren()
            .map { renderEntry(it as PairGene<*, *>) }
            .joinToString(ELEMENT_SEPARATOR, SET_OR_MAP_OPENING, SET_OR_MAP_CLOSING)
    }

    /**
     * A list and a set are both generated with an [ArrayGene], and are told apart by whether it asks
     * for unique elements, which is how [CassandraColumnGeneBuilder] builds a set: Cassandra keeps a
     * single copy of the repeated elements of a set anyway. The two are written with different
     * delimiters, so the distinction has to be recovered here.
     */
    private fun renderArray(gene: ArrayGene<*>): String {

        val opening = if (gene.uniqueElements) SET_OR_MAP_OPENING else LIST_OPENING
        val closing = if (gene.uniqueElements) SET_OR_MAP_CLOSING else LIST_CLOSING

        return gene.getViewOfChildren()
            .map { toCqlLiteral(it) }
            .joinToString(ELEMENT_SEPARATOR, opening, closing)
    }

    private fun renderEntry(entry: PairGene<*, *>) =
        toCqlLiteral(entry.first) + KEY_VALUE_SEPARATOR + toCqlLiteral(entry.second)

    private fun quote(value: String) =
        SINGLE_QUOTE + value.replace(SINGLE_QUOTE, ESCAPED_SINGLE_QUOTE) + SINGLE_QUOTE
}