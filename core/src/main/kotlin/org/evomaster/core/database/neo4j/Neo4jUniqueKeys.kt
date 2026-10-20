package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jDbInfoDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto

/**
 * The properties no two nodes with a label can share, as the controller read them when the search
 * started: the uniqueness constraints of the database plus the ids of the SUT's entities. It also keeps
 * the values the database already held for them, which the reset of the SUT restores before every test.
 */
class Neo4jUniqueKeys(dto: Neo4jDbInfoDto) {

    /**
     * @property label label of the nodes the key applies to
     * @property propertyKeys properties that together identify a node with [label]
     */
    data class Key(val label: String, val propertyKeys: List<String>)

    /**
     * The value a node holds for a key, as each property's type and text. The type is part of it
     * because Neo4j sees the integer 1 and the string "1" as different values.
     */
    data class Value(val key: Key, val values: List<Pair<Neo4jPropertyTypeDto, String>>)

    /** Every key, without duplicates. */
    val keys: List<Key>

    /** The values of [keys] already in the database. */
    val existing: Set<Value>

    init {
        val valid = dto.uniqueKeys
            .filter { !it.label.isNullOrEmpty() && it.propertyKeys.isNotEmpty() }
            .map { Key(it.label, it.propertyKeys.toList()) to it.existingValues }
        keys = valid.map { it.first }.distinct()
        existing = valid.flatMap { (key, existingValues) ->
            existingValues
                .filter { entries -> entries.size == key.propertyKeys.size && entries.all { it.type != null && it.value != null } }
                .map { entries -> Value(key, entries.map { it.type to it.value }) }
        }.toSet()
    }

    fun isEmpty() = keys.isEmpty()

    /** The keys that bind a node with these labels. */
    fun keysFor(labels: List<String>): List<Key> = keys.filter { it.label in labels }

    companion object {

        /**
         * @return the value [node] holds for [key], or null when the node lacks one of its properties,
         * which leaves it out of the key as Neo4j does
         */
        fun valueOf(key: Key, node: Neo4jNodeTemplate): Value? {
            val values = key.propertyKeys.map { name ->
                val property = node.properties.firstOrNull { it.key == name } ?: return null
                property.type to property.valueAsText()
            }
            return Value(key, values)
        }
    }
}
