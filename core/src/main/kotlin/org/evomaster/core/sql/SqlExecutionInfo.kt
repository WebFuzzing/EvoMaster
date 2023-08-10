package org.evomaster.core.sql

/**
 * sql execution info
 * @property command is a sql string command that was executed
 * @property executionTime is a time spent in order to execute the [command]
 */
data class SqlExecutionInfo (val command: String, val executionTime: Long)