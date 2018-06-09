package org.evomaster.core.database

import org.evomaster.clientJava.controllerApi.dto.database.execution.ReadDbDataDto

/**
 * When a test case is executed, and the SUT does access a SQL database,
 * keep track of which SELECT commands were executed and that ended up
 * in not returning any data.
 *
 * This class is supposed to be immutable
 */
class EmptySelects(
        /**
         * SQL queries executed by the SUTs, but that resulted in empty, no returned data
         */
        val selects: Set<String>,
        /**
         * Map from Table Name to Column Names.
         * The value "*" means all columns in the table.
         */
        val queriedData: Map<String, Set<String>>
) {

    companion object {

        fun fromDtos(dtos: Collection<ReadDbDataDto>): EmptySelects {

            val selects = dtos.flatMap { it.emptySqlSelects }.toSet()
            val data = mutableMapOf<String, MutableSet<String>>()

            for (dto in dtos) {

                for (e in dto.queriedData.entries) {
                    val key = e.key
                    val values = e.value

                    var existing: MutableSet<String>? = data[key]

                    if (existing != null && existing.contains("*")) {
                        //nothing to do
                        continue
                    }

                    if (existing == null) {
                        existing = values.toMutableSet()
                        data[key] = existing
                    } else {
                        existing.addAll(values)
                    }

                    if (existing!!.size > 1 && existing.contains("*")) {
                        /*
                            remove unnecessary columns, as anyway we take
                            everything with *
                        */
                        existing.clear()
                        existing.add("*")
                    }
                }
            }

            return EmptySelects(selects, data)
        }
    }
}