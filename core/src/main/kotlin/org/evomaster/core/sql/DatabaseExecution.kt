package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto

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
        val executionInfo: List<SqlExecutionInfo>
) {

    companion object {

        fun fromDto(dto: org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto?): DatabaseExecution {

            return DatabaseExecution(
                    cloneData(dto?.queriedData),
                    cloneData(dto?.updatedData),
                    cloneData(dto?.insertedData),
                    cloneData(dto?.failedWhere),
                    dto?.deletedData?.toList() ?: listOf(),
                    dto?.numberOfSqlCommands ?: 0,
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

            return data.map { SqlExecutionInfo(it.command, it.executionTime) }
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