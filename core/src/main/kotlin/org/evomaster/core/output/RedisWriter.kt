package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.redis.*
import org.evomaster.core.search.action.EvaluatedRedisDbAction

/**
 * Class used to generate the code in the test dealing with insertion of
 * data into Redis databases.
 */
object RedisWriter {

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

        redisDbInitialization
            .filter { !skipFailure || it.redisResult.getInsertExecutionResult() }
            .forEachIndexed { index, evaluated ->

                val action = evaluated.redisAction

                val dslCall = when (action) {
                    is RedisSetAction -> {
                        val key = escape(action.keyGene.getValueAsRawString(), format)
                        val value = escape(action.valueGene.getValueAsRawString(), format)
                        ".set(\"$key\", \"$value\")"
                    }
                    is RedisHsetAction -> {
                        val key = escape(action.keyGene.getValueAsRawString(), format)
                        val field = escape(action.field, format)
                        val value = escape(action.valueGene.getValueAsRawString(), format)
                        ".hset(\"$key\", \"$field\", \"$value\")"
                    }
                }

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

    private fun escape(value: String, format: OutputFormat): String {
        return StringEscapeUtils.escapeJava(value).let {
            if (format.isKotlin()) it.replace("$", "\\$") else it
        }
    }
}
