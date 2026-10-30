package org.evomaster.core.database.redis

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.MapGene
import org.evomaster.core.search.gene.wrapper.SelectableWrapperGene

/**
 * Turns the gene of an entity's property into the fields of the Redis hash that stores it.
 *
 * A Redis hash is flat, so Spring Data Redis stores a property of a non-simple type under one field
 * per leaf value, with the path to it as the name of the field:
 * - a nested object `address` is stored as `address.city`, `address.zip`
 * - a list, set or array `tags` is stored as `tags.[0]`, `tags.[1]`
 * - a map `attrs` is stored as `attrs.[key]`
 * - the above compose, e.g. `addrs.[0].city` or `byCity.[R].zip`
 *
 * Writing such a property as a single field (e.g. as a JSON string) does not fail on reading: the
 * property is just left null, which is why it is important to get the layout right.
 *
 * A property whose gene is inactive (an optional or nullable one) is not stored at all, which is
 * what a null property looks like. This matters, as an empty string is not equivalent: reading it
 * into a primitive property fails.
 */
object RedisHashFlattener {

    /**
     * @param name the name of the property, ie the first segment of the path of all its fields
     * @return the (field name, value) pairs to store for the property, in order. Empty if the
     * property is not to be stored at all
     */
    fun flatten(name: String, gene: Gene): List<Pair<String, String>> {
        val fields = mutableListOf<Pair<String, String>>()
        visit(name, gene, fields)
        return fields
    }

    private fun visit(path: String, gene: Gene, fields: MutableList<Pair<String, String>>) {
        when (gene) {
            is SelectableWrapperGene -> if (gene.isActive) visit(path, gene.gene, fields)
            is ObjectGene -> gene.fixedFields.forEach { visit("$path.${it.name}", it, fields) }
            is ArrayGene<*> -> gene.getViewOfElements().forEachIndexed { index, element ->
                visit("$path.[$index]", element, fields)
            }
            is MapGene<*, *> -> gene.getAllElements().forEach { pair ->
                visit("$path.[${pair.first.getValueAsRawString()}]", pair.second, fields)
            }
            else -> fields.add(path to gene.getValueAsRawString())
        }
    }
}
