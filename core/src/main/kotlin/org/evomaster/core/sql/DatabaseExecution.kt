package org.evomaster.core.sql

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionsDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.sql.schema.TableId
import org.slf4j.LoggerFactory

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
        val queriedData: Map<TableId, Set<String>>,
        val updatedData: Map<TableId, Set<String>>,
        val insertedData: Map<TableId, Set<String>>,
        val failedWhere: Map<TableId, Set<String>>,
        val deletedData: List<TableId>,
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

    private fun validateQuotes(data: Map<TableId, Set<String>>) {
        val quoted = data.keys.filter { it.name.startsWith("\"") }
        if(quoted.isNotEmpty()) {
            throw IllegalArgumentException("Following table names are quoted: ${quoted.joinToString(", ")}")
        }
    }

    private fun validateQuotes(data: List<TableId>) {
        val quoted = data.filter { it.name.startsWith("\"") }
        if(quoted.isNotEmpty()) {
            throw IllegalArgumentException("Following table names are quoted: ${quoted.joinToString(", ")}")
        }
    }


    companion object {

        private val log = LoggerFactory.getLogger(DatabaseExecution::class.java)

        /**
         * From [dto] info on what executed on the SQL database, return an internal representation.
         * To achieve this, we need to map it to the existing schema definition from which [tableIds]
         * are inferred.
         * This is done for few reasons, including:
         * 1) collected queries might missing schema info
         * 2) there might be cases we don't handle yet (eg views)
         */
        fun fromDto(dto: SqlExecutionsDto, tableIds: Set<TableId>): DatabaseExecution {

            /*
                Dealing with quotes in table names is tricky... could be to handle reserved words or case sensitivity.
                for now, we just remove the quotes, if any.
                TODO this should be handled properly, although unsure how common it is...

                Note: instead of dealing with this in the driver, better here in the core, as what returned in driver
                might depend on library used to analyze the SQL commands.
             */

            return DatabaseExecution(
                    cloneData(convertData(dto.queriedData, tableIds)),
                    cloneData(convertData(dto.updatedData, tableIds)),
                    cloneData(convertData(dto.insertedData, tableIds)),
                    cloneData(convertData(dto.failedWhere, tableIds)),
                    convertData(dto.deletedData?.toList(), tableIds) ?: listOf(),
                    dto.numberOfSqlCommands,
                    dto.sqlParseFailureCount,
                    cloneSqlExecutionInfo(dto.sqlExecutionLogDtoList)
            )
        }


        private fun convertData(data: List<String>?, tableIds: Set<TableId>) : List<TableId>?{
            if(data == null) return null

            return data.mapNotNull {
                val id = SqlActionUtils.getTableKey(tableIds, removeQuotes(it))
                if(id == null){
                    LoggingUtil.uniqueWarn(log, "Cannot identify table: $it")
                }
                id
            }
        }

        private fun convertData(data: Map<String, Set<String>>?, tableIds: Set<TableId>) : Map<TableId, Set<String>>? {
            if(data == null) return null

            return data.entries
                .mapNotNull { e ->
                    val id =  SqlActionUtils.getTableKey(tableIds, removeQuotes(e.key))
                    if(id == null){
                        LoggingUtil.uniqueWarn(log, "Cannot identify table: ${e.key}")
                    }
                    id?.let { it  to e.value}
                }.toMap()
        }

        fun mergeData(
                executions: Collection<DatabaseExecution>,
                extractor: (DatabaseExecution) -> Map<TableId, Set<String>>
        ): Map<TableId, Set<String>> {

            val data: MutableMap<TableId, MutableSet<String>> = mutableMapOf()

            for (ex in executions) {
                merge(data, extractor.invoke(ex))
            }

            return data
        }


        private fun removeQuotes(s: String) =
            if(s.startsWith("\"") && s.endsWith("\"")) s.substring(1,s.length-1)
            else s

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

        private fun cloneData(data: Map<TableId, Set<String>>?): Map<TableId, Set<String>> {
            val clone = mutableMapOf<TableId, Set<String>>()

            data?.keys?.forEach {
                clone[it] = data[it]!!.toSet()
            }
            return clone
        }

        private fun merge(current: MutableMap<TableId, MutableSet<String>>,
                          toAdd: Map<TableId, Set<String>>) {

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
