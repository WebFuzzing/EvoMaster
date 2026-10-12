package org.evomaster.core.database.redis

import org.evomaster.core.search.gene.Gene

/**
 * One HSET-able field of a document generated to satisfy a failed FT.SEARCH/FT.AGGREGATE command.
 *
 * Exactly one of [constantValue] or [valueGene] is set. A constant is used whenever the query pins
 * the field to a single exact value that a mutation could never be allowed to move away from (e.g.
 * an exact-match text term); a gene is used whenever the query still leaves some freedom to explore
 * (a numeric range, one of a tag's values, a prefix term, or no constraint at all for a field that
 * is only referenced by GROUPBY).
 */
class RedisQueryField(
    val name: String,
    val constantValue: String? = null,
    val valueGene: Gene? = null
) {

    init {
        require((constantValue == null) != (valueGene == null)) {
            "RedisQueryField must have exactly one of constantValue or valueGene"
        }
    }

    /**
     * The fields of the Redis hash that this field is stored as, as (name, value) pairs. A constant
     * or a simple gene is a single one, named after this field, but a nested object, a collection
     * or a map is stored as many, see [RedisHashFlattener]. Empty if the field is not to be stored,
     * which is the case for an inactive optional gene.
     */
    fun hashEntries(): List<Pair<String, String>> =
        if (constantValue != null) listOf(name to constantValue) else RedisHashFlattener.flatten(name, valueGene!!)

    /**
     * The current value of this field, as a plain string suitable for a Redis HSET, regardless of
     * whether it comes from a constant or from a gene.
     */
    fun rawValue(): String = constantValue ?: valueGene!!.getValueAsRawString()

    fun copy(): RedisQueryField = RedisQueryField(name, constantValue, valueGene?.copy())
}
