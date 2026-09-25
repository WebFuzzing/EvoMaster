package org.evomaster.core.database.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene

/**
 * Represents the HSET(s) needed to create a single document satisfying a failed FT.SEARCH or
 * FT.AGGREGATE command (failed meaning a query that currently has no matching document, or an
 * aggregation whose GROUPBY field is missing from every candidate document).
 *
 * One document, with one field per query filter (plus any field referenced only by GROUPBY), is
 * enough: the calculator's heuristic ANDs every filter together against the same document, so
 * satisfying all of them on a single row is both necessary and sufficient.
 *
 * @param key the new document's key, chosen so it falls under one of the index's declared prefixes.
 * @param fields the fields to HSET on that key.
 */
class RedisQueryAction(
    val key: String,
    val fields: List<RedisQueryField>
) : RedisDbAction() {

    init {
        require(fields.isNotEmpty()) { "RedisQueryAction requires at least one field" }
        addChildren(fields.mapNotNull { it.valueGene })
    }

    override fun getTargetKey() = key

    override fun insertionsCount(): Int = fields.size

    override fun seeTopGenes(): List<Gene> = fields.mapNotNull { it.valueGene }

    override fun copyContent(): Action = RedisQueryAction(key, fields.map { it.copy() })

    override fun getName() = "Redis_QUERY_${key}"
}
