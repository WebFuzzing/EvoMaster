package org.evomaster.core.database.redis

import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFilterDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Transforms a failed Redis read command into the corresponding insert action.
 *
 * Each supported command type maps to a specific [RedisDbAction] subclass:
 * - GET key                       -> [RedisSetAction]
 * - KEYS pattern                  -> [RedisSetFromPatternAction]
 * - HGET key field                -> [RedisHsetAction]
 * - HGETALL key                   -> [RedisHsetAction]
 * - SMEMBERS key                  -> [RedisSaddAction]
 * - SINTER key [key ...]          -> [RedisSaddFromSinterAction]
 * - FT.SEARCH/FT.AGGREGATE        -> [RedisQueryAction]
 */
object RedisInsertBuilder {

    private val log: Logger = LoggerFactory.getLogger(RedisInsertBuilder::class.java)

    fun buildInsertActions(
        failedCommands: List<RedisFailedCommand>,
        existingKeys: Set<String>
    ): List<RedisDbAction> {
        return failedCommands.flatMap { cmd ->
            buildActionsForCommand(cmd, existingKeys)
        }
    }

    private fun buildActionsForCommand(
        cmd: RedisFailedCommand,
        existingKeys: Set<String>
    ): List<RedisDbAction> {
        val keys = cmd.keys ?: emptyList()

        return when (cmd.command) {
            "GET" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(
                    RedisSetAction(
                        key = key,
                        valueGene = StringGene("value")
                    )
                )
            }
            "HGET" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(
                    RedisHsetAction(
                        key = key,
                        field = cmd.field ?: "field",
                        valueGene = StringGene("value")
                    )
                )
            }
            "HGETALL" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(
                    RedisHsetAction(
                        key = key,
                        field = "field",
                        valueGene = StringGene("value")
                    )
                )
            }
            "KEYS" -> {
                val pattern = cmd.pattern ?: return emptyList()
                val keyGene = RegexHandler.createGeneForJVM(pattern)
                listOf(RedisSetFromPatternAction(
                    keyGene = keyGene,
                    valueGene = StringGene("value")
                ))
            }
            "SMEMBERS" -> {
                val key = keys.firstOrNull() ?: return emptyList()
                if (key in existingKeys) return emptyList()
                listOf(RedisSaddAction(
                    key = key,
                    memberGene = StringGene("member")
                ))
            }
            "SINTER" -> {
                if (keys.isEmpty()) return emptyList()
                listOf(RedisSaddFromSinterAction(
                    keys = keys,
                    memberGene = StringGene("member")
                ))
            }
            "FT_SEARCH", "FT_AGGREGATE" -> buildFtActions(cmd, existingKeys)
            else -> {
                LoggingUtil.uniqueWarn(log, "Unsupported Redis command for insert action: ${cmd.command}")
                assert(false) { "Unsupported Redis command: ${cmd.command}" }
                emptyList()
            }
        }
    }

    /**
     * A single document is enough to satisfy a failed FT.SEARCH/FT.AGGREGATE: the calculator ANDs
     * every filter of the query together against the same candidate document, so a document with
     * one field per filter (plus any field referenced only by GROUPBY) satisfies all of them at once.
     */
    private fun buildFtActions(
        cmd: RedisFailedCommand,
        existingKeys: Set<String>
    ): List<RedisDbAction> {
        val indexName = cmd.indexName ?: return emptyList()
        val prefixes = cmd.indexPrefixes
        if (prefixes.isNullOrEmpty()) return emptyList()

        val key = buildDeterministicKey(indexName, prefixes[0], cmd)
        if (key in existingKeys) return emptyList()

        val fields = buildFieldsForFtCommand(cmd) ?: return emptyList()

        return listOf(RedisQueryAction(key = key, fields = fields))
    }

    /**
     * The same failed command (same index, filters and GROUPBY fields) always maps to the same key,
     * so a query that keeps failing across generations reuses its own previously-inserted document
     * instead of piling up duplicates (the caller already filters out keys in [existingKeys]).
     */
    private fun buildDeterministicKey(indexName: String, prefix: String, cmd: RedisFailedCommand): String {
        // String templates stringify nullable fields (min/max are null for TAG/TEXT filters) via
        // toString() semantics; StringBuilder.append(Double!) would instead risk resolving to the
        // primitive append(double) overload and unboxing-NPE on those nulls.
        val filterSignatures = (cmd.filters ?: emptyList()).joinToString(";") { f ->
            "${f.type}:${f.field}:${f.values}:${f.min}:${f.max}:${f.term}"
        }
        val groupBySignature = (cmd.groupByFields ?: emptyList()).joinToString(",")
        val signature = "$indexName|$filterSignatures|$groupBySignature"
        return "${prefix}evomaster_ft_${Math.abs(signature.hashCode())}"
    }

    private fun buildFieldsForFtCommand(cmd: RedisFailedCommand): List<RedisQueryField>? {
        val attributes = cmd.indexAttributes ?: emptyMap()
        val fields = LinkedHashMap<String, RedisQueryField>()

        for (filter in cmd.filters ?: emptyList()) {
            val field = buildFieldForFilter(filter, attributes) ?: return null
            fields[field.name] = field
        }

        for (groupByField in cmd.groupByFields ?: emptyList()) {
            if (!fields.containsKey(groupByField)) {
                fields[groupByField] = buildFreeField(groupByField, attributes[groupByField])
            }
        }

        if (fields.isEmpty()) {
            // query was "*" with no GROUPBY: any document under the index's prefix is a match,
            // so a single placeholder field is enough to make it a candidate.
            fields["value"] = RedisQueryField("value", valueGene = StringGene("value"))
        }

        return fields.values.toList()
    }

    /**
     * @return null when the filter cannot be turned into a concrete field (e.g. a field-less text
     * term with no TEXT attribute anywhere in the index schema to place it into).
     */
    private fun buildFieldForFilter(filter: RedisSearchFilterDto, attributes: Map<String, String>): RedisQueryField? {
        return when (filter.type) {
            RedisSearchFilterDto.TAG -> {
                val field = filter.field ?: return null
                val values = filter.values
                if (values.isNullOrEmpty()) return null
                RedisQueryField(field, valueGene = EnumGene(field, values))
            }
            RedisSearchFilterDto.NUMERIC -> {
                val field = filter.field ?: return null
                val min = filter.min ?: return null
                val max = filter.max ?: return null
                RedisQueryField(field, valueGene = DoubleGene(field, value = (min + max) / 2.0, min = min, max = max))
            }
            RedisSearchFilterDto.TEXT -> {
                val term = filter.term ?: return null
                val field = filter.field ?: attributes.entries.firstOrNull { it.value == RedisSearchFilterDto.TEXT }?.key
                ?: return null
                if (term.endsWith("*")) {
                    val word = term.removeSuffix("*")
                    val regex = "^" + escapeRegexLiteral(word) + ".*$"
                    RedisQueryField(field, valueGene = RegexHandler.createGeneForJVM(regex))
                } else {
                    RedisQueryField(field, constantValue = term)
                }
            }
            else -> null
        }
    }

    private fun buildFreeField(field: String, attributeType: String?): RedisQueryField {
        val gene: Gene = if (attributeType == RedisSearchFilterDto.NUMERIC) DoubleGene(field) else StringGene(field)
        return RedisQueryField(field, valueGene = gene)
    }

    private fun escapeRegexLiteral(word: String): String {
        val metachars = ".+*?()[]{}|^$\\"
        val escaped = StringBuilder()
        for (c in word) {
            if (metachars.indexOf(c) >= 0) escaped.append('\\')
            escaped.append(c)
        }
        return escaped.toString()
    }
}