package org.evomaster.core.database.neo4j

import org.evomaster.core.search.service.Randomness

/**
 * Repairs of the Neo4j insertions of an individual, run before it is executed.
 */
object Neo4jActionUtils {

    /** How many times a node gets one of its key properties randomized before its insertion is given up. */
    const val DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_A_NODE = 5

    /**
     * Makes the nodes of [actions] stop repeating a unique key, either among themselves or with a value
     * already in the database. The actions are visited in the order they are executed: the first node
     * holding a value keeps it, and on a later node that repeats it one of the key's properties is
     * randomized, until the value is free or [maxNumberOfAttempts] tries are spent.
     *
     * Without this, Neo4j rejects the insertion when it enforces the key, and when it does not (eg, an
     * id that only Spring Data assumes unique) the SUT ends up reading two nodes where it expects one
     * and fails in a way no call to its API could reach.
     *
     * @return the actions that still repeat a key after all attempts, which have to be removed
     */
    fun repairUniqueKeys(
        actions: List<Neo4jDbAction>,
        randomness: Randomness,
        maxNumberOfAttempts: Int = DEFAULT_MAX_NUMBER_OF_ATTEMPTS_TO_REPAIR_A_NODE
    ): List<Neo4jDbAction> {

        if (maxNumberOfAttempts < 0) {
            throw IllegalArgumentException("Maximum number of attempts should be non negative but it is: $maxNumberOfAttempts")
        }

        val taken = actions.mapNotNull { it.uniqueKeys }
            .distinct()
            .flatMap { it.existing }
            .toMutableSet()

        val broken = mutableListOf<Neo4jDbAction>()

        for (action in actions) {
            val uniqueKeys = action.uniqueKeys ?: continue
            val added = mutableListOf<Neo4jUniqueKeys.Value>()
            var repaired = true
            for (node in action.nodes) {
                val values = repairNode(node, uniqueKeys.keysFor(node.labels), taken, randomness, maxNumberOfAttempts)
                if (values == null) {
                    repaired = false
                    break
                }
                taken.addAll(values)
                added.addAll(values)
            }
            if (!repaired) {
                // the action is going away, so the values of its earlier nodes are free again
                taken.removeAll(added.toSet())
                broken.add(action)
            }
        }

        return broken
    }

    /**
     * @return the values [node] ends up holding for [keys], or null if one of them is still taken
     */
    private fun repairNode(
        node: Neo4jNodeTemplate,
        keys: List<Neo4jUniqueKeys.Key>,
        taken: Set<Neo4jUniqueKeys.Value>,
        randomness: Randomness,
        maxNumberOfAttempts: Int
    ): List<Neo4jUniqueKeys.Value>? {

        var attempts = 0
        var clash = findFirstTakenValue(node, keys, taken)

        while (clash != null && attempts < maxNumberOfAttempts) {
            val genes = node.properties
                .filter { it.key in clash.key.properties }
                .map { it.gene }
                .filter { it.isMutable() }
            if (genes.isEmpty()) return null

            randomness.choose(genes).randomize(randomness, true)
            attempts++
            // looked up again over every key, since a property shared by two keys changes both
            clash = findFirstTakenValue(node, keys, taken)
        }

        return if (clash == null) valuesOf(node, keys) else null
    }

    private fun valuesOf(node: Neo4jNodeTemplate, keys: List<Neo4jUniqueKeys.Key>): List<Neo4jUniqueKeys.Value> =
        keys.mapNotNull { Neo4jUniqueKeys.valueOf(it, node) }

    private fun findFirstTakenValue(
        node: Neo4jNodeTemplate,
        keys: List<Neo4jUniqueKeys.Key>,
        taken: Set<Neo4jUniqueKeys.Value>
    ): Neo4jUniqueKeys.Value? = valuesOf(node, keys).firstOrNull { it in taken }
}
