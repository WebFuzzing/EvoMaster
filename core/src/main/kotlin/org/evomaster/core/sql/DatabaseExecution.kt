package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionsDto

/**
 * When a test case is executed, and the SUT does access a SQL database,
 * keep track of which SQL commands were executed.
 *
 * In particular, we keep track of which tables and columns were involved.
 *
 * For the Maps:
 * key -> table name
 * value -> column names, where "*" means all
 *
 *
 * This class MUST be immutable
 */
class DatabaseExecution(
        val queriedData: Map<String, Set<String>>,
        val updatedData: Map<String, Set<String>>,
        val insertedData: Map<String, Set<String>>,
        val failedWhere: Map<String, Set<String>>,
        val deletedData: List<String>,
        val numberOfSqlCommands: Int,
        val sqlParseFailureCount: Int,
        val executionInfo: List<SqlExecutionInfo>
) {

    init {
        /*
            The handling of quotes in SQL databases is tricky, see for example for Postgres
            https://stackoverflow.com/questions/20878932/are-postgresql-column-names-case-sensitive
         */
        validateQuotes(queriedData)
        validateQuotes(updatedData)
        validateQuotes(insertedData)
        validateQuotes(failedWhere)
        validateQuotes(deletedData)
    }

    private fun validateQuotes(data: Map<String, Set<String>>) {
        val quoted = data.keys.filter { it.startsWith("\"") }
        if(quoted.isNotEmpty()) {
            throw IllegalArgumentException("Following table names are quoted: ${quoted.joinToString(", ")}")
        }
    }

    private fun validateQuotes(data: List<String>) {
        val quoted = data.filter { it.startsWith("\"") }
        if(quoted.isNotEmpty()) {
            throw IllegalArgumentException("Following table names are quoted: ${quoted.joinToString(", ")}")
        }
    }


    companion object {

        fun fromDto(dto: SqlExecutionsDto?): DatabaseExecution {

            /*
                Dealing with quotes in table names is tricky... could be to handle reserved words or case sensitivity.
                for now, we just remove the quotes, if any.
                TODO this should be handled properly, although unsure how common it is...

                Note: instead of dealing with this in the driver, better here in the core, as what returned in driver
                might depend on library used to analyze the SQL commands.
             */

            return DatabaseExecution(
                    cloneData(dealWithQuotes(dto?.queriedData)),
                    cloneData(dealWithQuotes(dto?.updatedData)),
                    cloneData(dealWithQuotes(dto?.insertedData)),
                    cloneData(dealWithQuotes(dto?.failedWhere)),
                    dealWithQuotes(dto?.deletedData?.toList()) ?: listOf(),
                    dto?.numberOfSqlCommands ?: 0,
                    dto?.sqlParseFailureCount ?: 0,
                    cloneSqlExecutionInfo(dto?.sqlExecutionLogDtoList)
            )
        }

        fun mergeData(
                executions: Collection<DatabaseExecution>,
                extractor: (DatabaseExecution) -> Map<String, Set<String>>
        ): Map<String, Set<String>> {

            val data: MutableMap<String, MutableSet<String>> = mutableMapOf()

            for (ex in executions) {
                merge(data, extractor.invoke(ex))
            }

            return data
        }

        private fun dealWithQuotes(data: Map<String, Set<String>>?): Map<String, Set<String>>? {
            if(data == null) return null

            return data.entries.associate {
                (if(it.key.startsWith("\"")) removeQuotes(it.key) else it.key) to it.value
            }
        }

        private fun dealWithQuotes(data: List<String>?) : List<String>?{
            if(data == null) return null

            return data.map { if(it.startsWith("\"")) removeQuotes(it) else it }
        }

        private fun removeQuotes(s: String) = s.substring(1,s.length-1)

        private fun cloneData(data: List<String>?): List<String> {
            if (data == null) {
                return listOf()
            }

            return data.toList()
        }

        private fun cloneSqlExecutionInfo(data: List<org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto>?): List<SqlExecutionInfo> {
            if (data == null) {
                return listOf()
            }

            return data.map { SqlExecutionInfo(it.sqlCommand, it.threwSqlExeception, it.executionTime) }
        }

        private fun cloneData(data: Map<String, Set<String>>?): Map<String, Set<String>> {
            val clone = mutableMapOf<String, Set<String>>()

            data?.keys?.forEach {
                clone[it] = data[it]!!.toSet()
            }
            return clone
        }

        private fun merge(current: MutableMap<String, MutableSet<String>>,
                          toAdd: Map<String, Set<String>>) {

            for (e in toAdd.entries) {
                val key = e.key
                val values = e.value

                var existing: MutableSet<String>? = current[key]

                if (existing != null && existing.contains("*")) {
                    //nothing to do
                    continue
                }

                if (existing == null) {
                    existing = values.toMutableSet()
                    current[key] = existing
                } else {
                    existing.addAll(values)
                }

                if (existing.size > 1 && existing.contains("*")) {
                    /*
                            remove unnecessary columns, as anyway we take
                            everything with *
                        */
                    existing.clear()
                    existing.add("*")
                }
            }
        }
    }
}
