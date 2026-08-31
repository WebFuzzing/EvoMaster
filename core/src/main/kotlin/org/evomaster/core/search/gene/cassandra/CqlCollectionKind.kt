package org.evomaster.core.search.gene.cassandra

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene

/**
 * A Cassandra collection type, with how many CQL types parameterize it and how its literal is
 * delimited, eg a list is written "[1, 2]" whereas a set is written "{1, 2}".
 */
enum class CqlCollectionKind(

    /**
     * The name of the type in CQL, ie how it is written in a schema.
     */
    val cqlName: String,

    /**
     * How many CQL types parameterize this one, ie one for the elements of a list and of a set, two
     * for the keys and the values of a map.
     */
    val arity: Int,

    val opening: String,

    val closing: String
) {

    LIST("list", 1, "[", "]"),

    SET("set", 1, "{", "}"),

    MAP("map", 2, "{", "}");

    /**
     * @return whether [content] is the kind of gene holding the elements of a collection of this
     * type, ie a [FixedMapGene] for a map and an [ArrayGene] for a list and a set
     */
    fun isValidContent(content: Gene) =
        if (this == MAP) content is FixedMapGene<*, *> else content is ArrayGene<*>
}