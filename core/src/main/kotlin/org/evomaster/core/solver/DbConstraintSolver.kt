package org.evomaster.core.solver

import org.evomaster.core.sql.SqlAction

/**
 * The interface for the constraint solver only for Database Constraints.
 * Such as Check, Unique, Primary Key, Foreign Key, etc.
 */
interface DbConstraintSolver : AutoCloseable {

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @return a list of SQLAction with the inserts in the db for the given constraints
     */
    fun solve(sqlQuery: String): List<SqlAction>
}
