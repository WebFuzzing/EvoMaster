package org.evomaster.core.search.gene.sql

import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlPrimaryKeyGeneTest {

    @Test
    fun testConstructorAndProperties() {
        val tableId = TableId("my_table")
        val innerGene = IntegerGene("foo", 42)
        val pk = SqlPrimaryKeyGene("pk", tableId, innerGene, 1L)

        assertEquals("pk", pk.name)
        assertEquals(tableId, pk.tableName)
        assertEquals(innerGene, pk.gene)
        assertEquals(1L, pk.uniqueId)
    }

    @Test
    fun testNegativeUniqueIdThrowsException() {
        val tableId = TableId("my_table")
        val innerGene = IntegerGene("foo", 42)
        assertThrows(IllegalArgumentException::class.java) {
            SqlPrimaryKeyGene("pk", tableId, innerGene, -1L)
        }
    }

    @Test
    fun testCopy() {
        val tableId = TableId("my_table")
        val innerGene = IntegerGene("foo", 42)
        val pk = SqlPrimaryKeyGene("pk", tableId, innerGene, 1L)

        val copy = pk.copy() as SqlPrimaryKeyGene

        assertNotSame(pk, copy)
        assertNotSame(pk.gene, copy.gene)
        assertEquals(pk.name, copy.name)
        assertEquals(pk.tableName, copy.tableName)
        assertEquals(pk.uniqueId, copy.uniqueId)
        assertEquals((pk.gene as IntegerGene).value, (copy.gene as IntegerGene).value)
    }

    @Test
    fun testContainsSameValueAs() {
        val tableId = TableId("table")
        val pk1 = SqlPrimaryKeyGene("pk", tableId, IntegerGene("foo", 1), 1L)
        val pk2 = SqlPrimaryKeyGene("pk", tableId, IntegerGene("foo", 1), 2L) // different uniqueId doesn't matter for value
        val pk3 = SqlPrimaryKeyGene("pk", tableId, IntegerGene("foo", 2), 1L)

        assertTrue(pk1.containsSameValueAs(pk2))
        assertFalse(pk1.containsSameValueAs(pk3))

        assertThrows(IllegalArgumentException::class.java) {
            pk1.containsSameValueAs(IntegerGene("foo", 1))
        }
    }

    @Test
    fun testUnsafeCopyValueFrom() {
        val tableId = TableId("table")
        val pk1 = SqlPrimaryKeyGene("pk", tableId, IntegerGene("foo", 1), 1L)
        val pk2 = SqlPrimaryKeyGene("pk", tableId, IntegerGene("foo", 2), 2L)

        pk1.unsafeCopyValueFrom(pk2)
        assertEquals(2, (pk1.gene as IntegerGene).value)

        assertFalse(pk1.unsafeCopyValueFrom(IntegerGene("foo", 3)))
    }

    @Test
    fun testPrintableAndRawStrings() {
        val tableId = TableId("table")
        val innerGene = IntegerGene("foo", 123)
        val pk = SqlPrimaryKeyGene("pk", tableId, innerGene, 1L)

        assertEquals("123", pk.getValueAsPrintableString())
        assertEquals("123", pk.getValueAsRawString())
        assertEquals(innerGene.getVariableName(), pk.getVariableName())
    }

    @Test
    fun testGetWrappedAndLeafGene() {
        val tableId = TableId("table")
        val innerGene = IntegerGene("foo", 1)
        val pk = SqlPrimaryKeyGene("pk", tableId, innerGene, 1L)

        assertEquals(pk, pk.getWrappedGene(SqlPrimaryKeyGene::class.java))
        assertEquals(innerGene, pk.getWrappedGene(IntegerGene::class.java))
        assertEquals(innerGene, pk.getLeafGene())
    }

    @Test
    fun testCheckForGloballyValid() {
        val table = Table(TableId("t"), emptySet(), emptySet())
        val pk = SqlPrimaryKeyGene("pk", table.id, IntegerGene("foo", 1), 1L)

        // Not mounted

        assertFalse(pk.isGloballyValid())

        val action = SqlAction(table, emptySet(), 1L, listOf(pk))

        // Now it is mounted and insertionId matches uniqueId
        assertTrue(pk.isGloballyValid())
    }

    class TestIndividual(children: MutableList<out ActionComponent>) : EnterpriseIndividual(
        sampleTypeField = SampleType.RANDOM,
        children = children,
        childTypeVerifier = EnterpriseChildTypeVerifier(SqlAction::class.java),
        groups = getEnterpriseTopGroups(children, 0, children.size, 0, 0, 0, 0)
    )

    @Test
    fun testShiftIdByUpdatesFKs() {
        val tableId = TableId("table")
        val pk = SqlPrimaryKeyGene("pk", tableId, IntegerGene("id", 1), 10L)

        val fk1 = SqlForeignKeyGene("fk", 100L, tableId, false, 10L) // bound to pk
        val fk2 = SqlForeignKeyGene("fk2", 101L, tableId, false, 20L) // bound to something else
        val fk3 = SqlForeignKeyGene("fk3", 102L, tableId, false, -1L) // unbound

        val table = Table(tableId, emptySet(), emptySet())
        val action1 = SqlAction(table, emptySet(), 10L, listOf(pk))
        val action2 = SqlAction(table, emptySet(), 11L, listOf(fk1, fk2, fk3))

        TestIndividual(mutableListOf(action1, action2))

        pk.shiftIdBy(5L)

        assertEquals(15L, pk.uniqueId)
        assertEquals(15L, fk1.uniqueIdOfPrimaryKey)
        assertEquals(20L, fk2.uniqueIdOfPrimaryKey)
        assertEquals(-1L, fk3.uniqueIdOfPrimaryKey)
    }
}
