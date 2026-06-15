package org.evomaster.core.output

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.redis.*
import org.evomaster.core.search.action.EvaluatedRedisDbAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Class used to generate the code in the test dealing with insertion of
 * data into Redis databases.
 */
object RedisWriter {

    private val log: Logger = LoggerFactory.getLogger(RedisWriter::class.java)

    /**
     * Generate Redis SET actions into a test case based on [redisDbInitialization].
     *
     * @param format is the format of tests to be generated
     * @param redisDbInitialization contains the Redis db actions to be generated
     * @param lines is used to save generated textual lines with respect to [redisDbInitialization]
     * @param groupIndex specifies an index of a group of this [redisDbInitialization]
     * @param insertionVars is a list of previous variable names of the db actions (Pair.first)
     *                      and corresponding results (Pair.second)
     * @param skipFailure specifies whether to skip failed insertions
     */
    fun handleRedisDbInitialization(
        format: OutputFormat,
        redisDbInitialization: List<EvaluatedRedisDbAction>,
        lines: Lines,
        groupIndex: String = "",
        insertionVars: MutableList<Pair<String, String>>,
        skipFailure: Boolean
    ) {
        if (redisDbInitialization.isEmpty() ||
            redisDbInitialization.none { !skipFailure || it.redisResult.getInsertExecutionResult() }
        ) {
            return
        }

        val insertionVar = "insertions_redis${groupIndex}"
        val insertionVarResult = "${insertionVar}_result"
        val previousVar = insertionVars.joinToString(", ") { it.first }

        val dslCalls = redisDbInitialization
            .filter { !skipFailure || it.redisResult.getInsertExecutionResult() }
            .flatMap { evaluated -> toDslCalls(evaluated.redisAction, format) }

        dslCalls.forEachIndexed { index, dslCall ->
            lines.add(
                when {
                    index == 0 && format.isJava() ->
                        "List<RedisInsertionDto> $insertionVar = redis($previousVar)"
                    index == 0 && format.isKotlin() ->
                        "val $insertionVar = redis($previousVar)"
                    else -> ".and()"
                } + dslCall
            )
            if (index == 0) lines.indent()
        }

        lines.add(".dtos()")
        lines.appendSemicolon()
        lines.deindent()

        lines.add(
            when {
                format.isJava() -> "RedisInsertionResultsDto "
                format.isKotlin() -> "val "
                else -> throw IllegalStateException(
                    "Redis insertion generation not supported for format $format"
                )
            } + "$insertionVarResult = controller.execInsertionsIntoRedisDatabase($insertionVar)"
        )
        lines.appendSemicolon()

        insertionVars.add(insertionVar to insertionVarResult)
    }

    private fun toDslCalls(action: RedisDbAction, format: OutputFormat): List<String> {
        return when (action) {
            is RedisSetAction -> {
                val key = "\"${escape(action.key, format)}\""
                val value = action.valueGene.getValueAsPrintableString(targetFormat = format)
                listOf(".set($key, $value)")
            }
            is RedisSetFromPatternAction -> {
                val key = action.keyGene.getValueAsPrintableString(targetFormat = format)
                val value = action.valueGene.getValueAsPrintableString(targetFormat = format)
                listOf(".set($key, $value)")
            }
            is RedisHsetAction -> {
                val key = "\"${escape(action.key, format)}\""
                val field = escape(action.field, format)
                val value = action.valueGene.getValueAsPrintableString(targetFormat = format)
                listOf(".hset($key, \"$field\", $value)")
            }
            is RedisSaddAction -> {
                val key = "\"${escape(action.key, format)}\""
                val member = action.memberGene.getValueAsPrintableString(targetFormat = format)
                listOf(".sadd($key, $member)")
            }
            is RedisSaddFromSinterAction -> {
                val member = action.memberGene.getValueAsPrintableString(targetFormat = format)
                action.keys.map { key ->
                    ".sadd(\"${escape(key, format)}\", $member)"
                }
            }
        }
    }

    private fun escape(value: String, format: OutputFormat): String {
        return StringEscapeUtils.escapeJava(value).let {
            if (format.isKotlin()) it.replace("$", "\\$") else it
        }
    }
}