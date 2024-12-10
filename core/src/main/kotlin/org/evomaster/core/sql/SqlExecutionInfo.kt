package org.evomaster.core.sql

/**
 * sql execution info
 * @property sqlCommand is a sql string command that was executed
 * @property threwSqlException Indicates whether an SQL exception was thrown during the execution of [sqlCommand].
 *                             It is `true` if an exception was thrown, and `false` otherwise.
 * @property executionTime is a time spent in order to execute the [sqlCommand]
 */
data class SqlExecutionInfo (
    val sqlCommand: String,
    val threwSqlException: Boolean,
    val executionTime: Long
)
