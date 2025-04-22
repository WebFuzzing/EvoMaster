package org.evomaster.core.solver

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.core.sql.SqlAction

/**
 * The interface for the constraint solver only for Database Constraints.
 * Such as Check, Unique, Primary Key, Foreign Key, etc.
 */
interface DbConstraintSolver : AutoCloseable {

    /**
     * Solves the given constraints and returns the Db Gene to insert in the database
     * @param schemaDto the schema of the database
     * @param sqlQuery the query to solve
     * @param numberOfRows the number of rows to insert in the db per table
     * @return a list of SQLAction with the inserts in the db for the given constraints
     */
    fun solve(schemaDto: DbInfoDto, sqlQuery: String, numberOfRows: Int = 1): List<SqlAction>
}
