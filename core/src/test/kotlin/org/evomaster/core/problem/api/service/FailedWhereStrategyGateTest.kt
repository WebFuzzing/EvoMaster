package org.evomaster.core.problem.api.service

import org.evomaster.core.problem.api.service.ApiWsStructureMutator.Companion.nothingToDoForFailedWhere
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The two SQL data generation strategies consume different inputs, so what counts as "nothing to do"
 * for a failed WHERE is not the same for both.
 *
 * The case that matters is a search reporting failed WHERE queries where none of the tables involved
 * can be inserted into. The search-based strategy genuinely has nothing to do there; the solver, which
 * reads the query text rather than that map, does.
 */
class FailedWhereStrategyGateTest {

    private fun gate(
        noInsertableTables: Boolean,
        noFailedWhereQueries: Boolean,
        search: Boolean,
        z3: Boolean
    ) = nothingToDoForFailedWhere(noInsertableTables, noFailedWhereQueries, search, z3)

    @Test
    fun `the solver runs on queries whose tables cannot be inserted into`() {
        assertFalse(
            gate(noInsertableTables = true, noFailedWhereQueries = false, search = false, z3 = true),
            "the solver reads the query text, so an empty insertable-table map must not stop it"
        )
    }

    @Test
    fun `the search-based strategy stops when no table can be inserted into`() {
        assertTrue(
            gate(noInsertableTables = true, noFailedWhereQueries = false, search = true, z3 = false),
            "the search builds INSERTs table by table, so with no table it has nothing to do"
        )
    }

    /**
     * Defensive, not reachable: EMConfig rejects a configuration with both strategies enabled. Pinned
     * anyway so the predicate stays total, since it is a pure function that a future caller could
     * reach without that validation in front of it.
     */
    @Test
    fun `with both strategies enabled the search-based precondition is the one that applies`() {
        assertTrue(gate(noInsertableTables = true, noFailedWhereQueries = false, search = true, z3 = true))
        assertFalse(gate(noInsertableTables = false, noFailedWhereQueries = true, search = true, z3 = true))
    }

    /** With no strategy enabled there is nothing to do, whatever the inputs say. */
    @Test
    fun `with neither strategy enabled there is nothing to do`() {
        assertTrue(gate(noInsertableTables = false, noFailedWhereQueries = false, search = false, z3 = false))
        assertTrue(gate(noInsertableTables = true, noFailedWhereQueries = true, search = false, z3 = false))
    }

    @Test
    fun `neither strategy runs without input of its own kind`() {
        assertTrue(gate(noInsertableTables = true, noFailedWhereQueries = true, search = false, z3 = true))
        assertTrue(gate(noInsertableTables = true, noFailedWhereQueries = true, search = true, z3 = false))
    }

    @Test
    fun `each strategy ignores the other's input`() {
        // Queries present but no insertable table: only the solver proceeds.
        assertFalse(gate(noInsertableTables = true, noFailedWhereQueries = false, search = false, z3 = true))
        // Insertable tables present but no query text recorded: only the search proceeds.
        assertFalse(gate(noInsertableTables = false, noFailedWhereQueries = true, search = true, z3 = false))
        assertTrue(gate(noInsertableTables = false, noFailedWhereQueries = true, search = false, z3 = true))
    }
}
