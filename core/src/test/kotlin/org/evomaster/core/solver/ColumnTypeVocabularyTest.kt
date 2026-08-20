package org.evomaster.core.solver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Two vocabularies describe the same column type: [SmtLibGenerator.TYPE_MAP], which decides the SMT-LIB
 * sort a column is encoded as, and [SMTLibZ3DbConstraintSolver], which decides the gene the solved value
 * is turned back into. When they disagree, nothing fails — a value is produced under one reading and
 * consumed under another — so the agreement has to be pinned by a test rather than noticed at runtime.
 */
class ColumnTypeVocabularyTest {

    private val solver = SMTLibZ3DbConstraintSolver()

    /**
     * TYPE_MAP uppercases before looking up, so it accepts a lowercase spelling. The gene side used to
     * match the raw string, which meant a backend reporting `bigint` was encoded as an integer and
     * decoded as a string.
     */
    @Test
    fun `both vocabularies read a type the same way regardless of its spelling`() {
        SmtLibGenerator.TYPE_MAP.keys.forEach { type ->
            val upper = solver.getColumnDataType(type)
            assertEquals(
                upper, solver.getColumnDataType(type.lowercase()),
                "'$type' and '${type.lowercase()}' are the same type to TYPE_MAP, so they must be to the gene side too"
            )
            assertEquals(upper, solver.getColumnDataType(type.replaceFirstChar { it.lowercase() }))
        }
    }

    /**
     * `BOOL` and `BOOLEAN` are one entry apart in TYPE_MAP and encode identically. Treating them as
     * different on the way back costs a column its boolean handling, silently.
     */
    @Test
    fun `the two boolean spellings are equivalent`() {
        assertTrue(solver.typeMatches("BOOL", "BOOLEAN"))
        assertTrue(solver.typeMatches("BOOLEAN", "BOOL"))
        assertTrue(solver.typeMatches("bool", "BOOLEAN"))
        assertTrue(solver.typeMatches("Boolean", "bool"))
    }

    @Test
    fun `a type matches itself whatever its spelling`() {
        assertTrue(solver.typeMatches("BIGINT", "bigint"))
        assertTrue(solver.typeMatches("character varying", "CHARACTER VARYING"))
    }

    /**
     * The equivalence is deliberately narrow: it exists for the boolean spellings and must not quietly
     * make unrelated types interchangeable.
     */
    @Test
    fun `unrelated types do not match`() {
        assertFalse(solver.typeMatches("BIGINT", "BOOLEAN"))
        assertFalse(solver.typeMatches("BOOL", "CHAR"))
        assertFalse(solver.typeMatches("INTEGER", "BIGINT"))
    }
}
