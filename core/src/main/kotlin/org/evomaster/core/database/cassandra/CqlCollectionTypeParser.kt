package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.cassandra.CqlCollectionKind

/**
 * Recognizes the CQL collection types and recovers their parameters, ie the CQL types of what they
 * hold, so that a gene can be built for each of them.
 */
object CqlCollectionTypeParser {

    private const val FROZEN_PREFIX = "frozen"

    private const val TYPE_PARAMETER_SEPARATOR = ','

    /**
     * @param cqlType a normalized, ie trimmed and lower case, CQL type name
     * @return the kind and parameters of [cqlType] if it is a collection type, null if it is not,
     * which covers both the scalar types and the parameterized ones that are not collections, ie
     * the tuples and the vectors
     * @throws IllegalArgumentException if [cqlType] is a collection type carrying the wrong number
     * of parameters, or if its type parameters are not balanced
     */
    fun parse(cqlType: String): CqlCollectionType? {

        val unfrozen = stripFrozen(cqlType)

        if (!unfrozen.endsWith(CqlTypeParameters.END)) {
            return null
        }

        val parametersStart = unfrozen.indexOf(CqlTypeParameters.START)
        if (parametersStart <= 0) {
            return null
        }

        val kind = CqlCollectionKind.entries.find { it.cqlName == unfrozen.substring(0, parametersStart).trim() }
            ?: return null

        val parameters = CqlTypeParameters
            .splitAtTopLevel(unfrozen.substring(parametersStart + 1, unfrozen.length - 1), TYPE_PARAMETER_SEPARATOR)
            .map { it.trim() }

        if (parameters.size != kind.arity) {
            throw IllegalArgumentException("A CQL ${kind.cqlName} is parameterized by ${kind.arity}" +
                    " type(s), but ${parameters.size} were given: $cqlType")
        }

        return CqlCollectionType(kind, parameters)
    }

    /**
     * A collection is written as "frozen<...>" when its value is stored as a single immutable one,
     * which is required of the collections nested inside another one and of the ones composing a
     * primary key. The distinction does not affect how a value of it is written in an insertion,
     * so the marker is just peeled off.
     *
     * Note that the SUT is not expected to report a frozen type in the first place, as the driver
     * metadata the schema is read from is asked for the type without it. This is only here so that
     * a type written by hand is handled the same way.
     */
    private fun stripFrozen(cqlType: String): String {

        var current = cqlType

        while (current.startsWith(FROZEN_PREFIX + CqlTypeParameters.START) && current.endsWith(CqlTypeParameters.END)) {
            current = current.substring(FROZEN_PREFIX.length + 1, current.length - 1).trim()
        }

        return current
    }
}

/**
 * A CQL collection type, ie its kind and the CQL types of what it holds: the type of the elements
 * for a list and a set, and the types of the keys and of the values for a map.
 */
data class CqlCollectionType(

    val kind: CqlCollectionKind,

    /**
     * The CQL types parameterizing the collection, in the order they are written in, ie the type of
     * the elements for a list and a set, and the types of the keys and then of the values for a map.
     * There are exactly as many as the arity of [kind].
     */
    val parameters: List<String>
)