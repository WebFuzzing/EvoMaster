package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jExistingDataDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto

/**
 * The properties no two nodes with a label can share, as the controller read them when the search
 * started: the uniqueness constraints of the database plus the ids of the SUT's entities. It also keeps
 * the values the database already held for them, which the reset of the SUT restores before every test.
 */
class Neo4jUniqueKeys(dto: Neo4jExistingDataDto) {

    /**
     * @property label label of the nodes the key applies to
     * @property properties properties that together identify a node with [label]
     */
    data class Key(val label: String, val properties: List<String>)

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
        val usableKeys = dto.uniqueKeys.filter { canBindNodes(it) }
        keys = usableKeys.map { toKey(it) }.distinct()
        existing = usableKeys.flatMap { existingValuesOf(it) }.toSet()
    }

    fun isEmpty() = keys.isEmpty()

    /** The keys that bind a node with these labels. */
    fun keysFor(labels: List<String>): List<Key> = keys.filter { it.label in labels }

    /**
     * Whether [keyDto] names a label and at least one property. Without both, the key cannot bind any
     * node, so it is ignored.
     */
    private fun canBindNodes(keyDto: Neo4jUniqueKeyDto): Boolean =
        !keyDto.label.isNullOrEmpty() && !keyDto.properties.isNullOrEmpty()

    /** The key [keyDto] describes, as the rest of the search compares keys. */
    private fun toKey(keyDto: Neo4jUniqueKeyDto) = Key(keyDto.label, keyDto.properties.toList())

    /**
     * The values of the key of [keyDto] that the database already holds. A value is ignored unless it has
     * a type and a text for every property of the key: missing one, it cannot be compared with the value
     * of an inserted node.
     */
    private fun existingValuesOf(keyDto: Neo4jUniqueKeyDto): List<Value> {
        val key = toKey(keyDto)
        val valuesAsEntries = (keyDto.existingValues ?: emptyList()).map { it.properties ?: emptyList() }
        val completeValues = valuesAsEntries.filter { entries -> coversEveryProperty(key, entries) }
        return completeValues.map { entries -> Value(key, entries.map { it.type to it.value }) }
    }

    /**
     * Whether [entries] give exactly one type and text for each property of [key].
     */
    private fun coversEveryProperty(key: Key, entries: List<Neo4jInsertionEntryDto>): Boolean =
        entries.size == key.properties.size && entries.all { it.type != null && it.value != null }

    companion object {

        /**
         * @return the value [node] holds for [key], or null when the node lacks one of its properties,
         * which leaves it out of the key as Neo4j does
         */
        fun valueOf(key: Key, node: Neo4jNodeTemplate): Value? {
            val values = key.properties.map { name ->
                val property = node.properties.firstOrNull { it.key == name } ?: return null
                property.type to property.valueAsText()
            }
            return Value(key, values)
        }
    }
}
