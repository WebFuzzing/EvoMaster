package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlDurationGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene

/**
 * Builds the gene used to generate the value of a Cassandra column, based on its CQL type.
 *
 * Two different reasons keep a CQL type out of the ones handled here:
 * - the value of a column of that type cannot be generated at all, ie a counter, which is only
 *   writable with an UPDATE, and a timeuuid, which requires a version 1 UUID, whereas [UUIDGene]
 *   generates a random one;
 * - no gene generating a value of that type has been written yet, ie blob, inet, the collections
 *   and the user defined types.
 */
object CassandraColumnGeneBuilder {

    /**
     * How the gene generating the value of a column is built, for each of the CQL types handled
     * here, keyed by the normalized name of the type. Being the single place where such types are
     * enumerated, it is also what [isSupported] answers from, so that the two cannot disagree.
     */
    private val GENE_BUILDERS: Map<String, (String) -> Gene> = mapOf(
        "ascii" to { name -> StringGene(name) },
        "text" to { name -> StringGene(name) },
        "varchar" to { name -> StringGene(name) },
        "tinyint" to { name -> IntegerGene(name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt()) },
        "smallint" to { name -> IntegerGene(name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt()) },
        "int" to { name -> IntegerGene(name) },
        "bigint" to { name -> LongGene(name) },
        "varint" to { name -> BigIntegerGene(name) },
        "decimal" to { name -> BigDecimalGene(name) },
        "float" to { name -> FloatGene(name) },
        "double" to { name -> DoubleGene(name) },
        "boolean" to { name -> BooleanGene(name) },
        "uuid" to { name -> UUIDGene(name) },
        /*
            Only valid values are generated, as these genes are used to set up the state of the
            database, and Cassandra would just reject an insertion carrying an invalid one.
         */
        "timestamp" to { name -> DateTimeGene(name, onlyValid = true) },
        "date" to { name -> DateGene(name, onlyValidDates = true) },
        "time" to { name -> TimeGene(name, onlyValidTimes = true) },
        "duration" to { name -> CqlDurationGene(name) }
    )

    /**
     * @return whether a gene can be built for [column], ie whether its CQL type is one of the
     * scalar types handled here
     */
    fun isSupported(column: CassandraColumn) = normalize(column.cqlType) in GENE_BUILDERS

    /**
     * @throws IllegalArgumentException if the CQL type of [column] is not handled, as verifiable
     * beforehand with [isSupported]
     */
    fun buildGene(column: CassandraColumn): Gene {

        val builder = GENE_BUILDERS[normalize(column.cqlType)]
            ?: throw IllegalArgumentException("Cannot handle the CQL type of column $column")

        return builder(column.name)
    }

    private fun normalize(cqlType: String) = cqlType.trim().lowercase()

}
