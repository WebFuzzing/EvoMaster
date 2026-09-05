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

    private const val ASCII_TYPE = "ascii"
    private const val TEXT_TYPE = "text"
    private const val VARCHAR_TYPE = "varchar"
    private const val TINYINT_TYPE = "tinyint"
    private const val SMALLINT_TYPE = "smallint"
    private const val INT_TYPE = "int"
    private const val BIGINT_TYPE = "bigint"
    private const val VARINT_TYPE = "varint"
    private const val DECIMAL_TYPE = "decimal"
    private const val FLOAT_TYPE = "float"
    private const val DOUBLE_TYPE = "double"
    private const val BOOLEAN_TYPE = "boolean"
    private const val UUID_TYPE = "uuid"
    private const val TIMESTAMP_TYPE = "timestamp"
    private const val DATE_TYPE = "date"
    private const val TIME_TYPE = "time"
    private const val DURATION_TYPE = "duration"

    /**
     * How the gene generating the value of a column is built, for each of the CQL types handled
     * here, keyed by the normalized name of the type. Being the single place where such types are
     * enumerated, it is also what [isSupported] answers from, so that the two cannot disagree.
     */
    private val GENE_BUILDERS: Map<String, (String) -> Gene> = mapOf(
        ASCII_TYPE to { name -> StringGene(name) },
        TEXT_TYPE to { name -> StringGene(name) },
        VARCHAR_TYPE to { name -> StringGene(name) },
        TINYINT_TYPE to { name -> IntegerGene(name, min = Byte.MIN_VALUE.toInt(), max = Byte.MAX_VALUE.toInt()) },
        SMALLINT_TYPE to { name -> IntegerGene(name, min = Short.MIN_VALUE.toInt(), max = Short.MAX_VALUE.toInt()) },
        INT_TYPE to { name -> IntegerGene(name) },
        BIGINT_TYPE to { name -> LongGene(name) },
        VARINT_TYPE to { name -> BigIntegerGene(name) },
        DECIMAL_TYPE to { name -> BigDecimalGene(name) },
        FLOAT_TYPE to { name -> FloatGene(name) },
        DOUBLE_TYPE to { name -> DoubleGene(name) },
        BOOLEAN_TYPE to { name -> BooleanGene(name) },
        UUID_TYPE to { name -> UUIDGene(name) },
        /*
            Only valid values are generated, as these genes are used to set up the state of the
            database, and Cassandra would just reject an insertion carrying an invalid one.
         */
        TIMESTAMP_TYPE to { name -> DateTimeGene(name, onlyValid = true) },
        DATE_TYPE to { name -> DateGene(name, onlyValidDates = true) },
        TIME_TYPE to { name -> TimeGene(name, onlyValidTimes = true) },
        DURATION_TYPE to { name -> CqlDurationGene(name) }
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
