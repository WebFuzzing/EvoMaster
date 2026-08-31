package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionKind
import org.evomaster.core.search.gene.cassandra.CqlDurationGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.numeric.*
import org.evomaster.core.search.gene.string.StringGene

/**
 * Builds the gene used to generate the value of a Cassandra column, based on its CQL type.
 *
 * The collection types are handled by recursing on the types parameterizing them, so a column is
 * only supported when all of the types composing it are.
 *
 * Two different reasons keep a CQL type out of the ones handled here:
 * - the value of a column of that type cannot be generated at all, ie a counter, which is only
 *   writable with an UPDATE, and a timeuuid, which requires a version 1 UUID, whereas [UUIDGene]
 *   generates a random one;
 * - no gene generating a value of that type has been written yet, ie blob, the tuples, the vectors
 *   and the user defined types.
 */
object CassandraColumnGeneBuilder {

    /**
     * The name given to the genes generating what a collection holds. Such genes are not bound to a
     * column of their own, and the elements of a collection are written with no name in a CQL
     * literal, so the name is only there to identify them while debugging.
     */
    private const val ELEMENT_GENE_NAME = "element"

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
            Only IPv4 addresses are generated for now, although the CQL type also accepts IPv6 ones, as
            that is what InetGene builds. The same restriction already applies to the SQL types.
         */
        "inet" to { name -> InetGene(name) },
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
     * @return whether a gene can be built for [column], ie whether its CQL type is one of the ones
     * handled here, or a collection of such types
     */
    fun isSupported(column: CassandraColumn) = isSupported(normalize(column.cqlType))

    /**
     * @throws IllegalArgumentException if the CQL type of [column] is not handled, as verifiable
     * beforehand with [isSupported]
     */
    fun buildGene(column: CassandraColumn): Gene = buildGene(column.name, normalize(column.cqlType))

    private fun isSupported(cqlType: String): Boolean {

        val collection = CqlCollectionTypeParser.parse(cqlType) ?: return cqlType in GENE_BUILDERS

        return collection.parameters.all { isSupported(normalize(it)) }
    }

    /**
     * @param cqlType a normalized CQL type
     */
    private fun buildGene(name: String, cqlType: String): Gene {

        CqlCollectionTypeParser.parse(cqlType)?.let { return buildCollectionGene(name, it) }

        val builder = GENE_BUILDERS[cqlType]
            ?: throw IllegalArgumentException("Cannot handle the CQL type $cqlType of column $name")

        return builder(name)
    }

    private fun buildCollectionGene(name: String, type: CqlCollectionType): Gene {

        val content = when (type.kind) {
            CqlCollectionKind.LIST -> ArrayGene(name, template = elementGene(type, 0))
            /*
                Cassandra collapses the repeated elements of a set literal into a single one, so
                generating them would just be wasted search effort.
             */
            CqlCollectionKind.SET -> ArrayGene(name, template = elementGene(type, 0), uniqueElements = true)
            CqlCollectionKind.MAP -> FixedMapGene(name, key = elementGene(type, 0), value = elementGene(type, 1))
        }

        return CqlCollectionGene(name, type.kind, content)
    }

    private fun elementGene(type: CqlCollectionType, index: Int) =
        buildGene(ELEMENT_GENE_NAME, normalize(type.parameters[index]))

    private fun normalize(cqlType: String) = cqlType.trim().lowercase()

}
