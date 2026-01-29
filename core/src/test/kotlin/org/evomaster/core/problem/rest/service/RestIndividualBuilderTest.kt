package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestIndividualBuilderTest {


    @Test
    fun mergeCombinesMainActionsAndKeepsOrder() {
        // create two simple RestCallAction with no parameters
        val callA = RestCallAction("idA", HttpVerb.GET, RestPath("/a"), mutableListOf())
        val callB = RestCallAction("idB", HttpVerb.GET, RestPath("/b"), mutableListOf())

        // build individuals from single actions
        val first = RestIndividual(actions = mutableListOf(callA), sampleType = SampleType.RANDOM)
        val second = RestIndividual(actions = mutableListOf(callB), sampleType = SampleType.RANDOM)

        // merge
        val builder = RestIndividualBuilder.createIndependentBuilderForTests()
        val merged = builder.merge(first, second)

        // main executable actions should contain both actions in order
        val mains = merged.seeMainExecutableActions()
        assertEquals(2, mains.size)
        assertEquals(callA.getName(), mains[0].getName())
        assertEquals(callB.getName(), mains[1].getName())

        // merged total actions should equal sum of both (no initializing actions present)
        val before = first.seeAllActions().size + second.seeAllActions().size
        assertEquals(before, merged.seeAllActions().size)
    }

    @Test
    fun mergeInitActionsAndKeepsOrder() {
        // create two simple RestCallAction with no parameters
        val callA = RestCallAction("idA", HttpVerb.GET, RestPath("/a"), mutableListOf())
        val callB = RestCallAction("idB", HttpVerb.GET, RestPath("/b"), mutableListOf())

        // create a simple table/column for sql initialization
        val col = Column(
            "ID",
            ColumnDataType.INT,
            primaryKey = true,
            databaseType = org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType.H2
        )
        val table1 = Table(TableId("T1"), setOf(col), emptySet())
        val table2 = Table(TableId("T2"), setOf(col), emptySet())

        val sql1 = SqlAction(table1, setOf(col), 1L, computedGenes = listOf(IntegerGene("ID", 1)))
        val sql2 = SqlAction(table2, setOf(col), 1L, computedGenes = listOf(IntegerGene("ID", 2)))

        // build individuals from single actions with initialization SQL
        val first = RestIndividual(actions = mutableListOf(callA), sampleType = SampleType.RANDOM)
        val second = RestIndividual(actions = mutableListOf(callB), sampleType = SampleType.RANDOM)

        first.addInitializingDbActions(0, listOf(sql1))
        second.addInitializingDbActions(0, listOf(sql2))

        // merge
        val builder = RestIndividualBuilder.createIndependentBuilderForTests()
        val merged = builder.merge(first, second)

        // main executable actions should contain both actions in order
        val mains = merged.seeMainExecutableActions()
        assertEquals(2, mains.size)
        assertEquals(callA.getName(), mains[0].getName())
        assertEquals(callB.getName(), mains[1].getName())

        // initializing SQL actions should contain both sql1 and sql2
        val inits = merged.seeInitializingActions().filterIsInstance<SqlAction>()
        assertEquals(2, inits.size)
        // check their table names preserved and order: first's sql then second's sql
        assertEquals("T1", inits[0].table.name)
        assertEquals("T2", inits[1].table.name)

        // merged total actions should be sum of both initial individuals
        val before = first.seeAllActions().size + second.seeAllActions().size
        assertEquals(before, merged.seeAllActions().size)
    }

    @Test
    fun mergeInitActionsWithExistingDataAndKeepsOrder() {
        // create two simple RestCallAction with no parameters
        val callA = RestCallAction("idA", HttpVerb.GET, RestPath("/a"), mutableListOf())
        val callB = RestCallAction("idB", HttpVerb.GET, RestPath("/b"), mutableListOf())

        // create a simple table/column for sql initialization
        val col = Column(
            "ID",
            ColumnDataType.INT,
            primaryKey = true,
            databaseType = org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType.H2
        )
        val table1 = Table(TableId("T1"), setOf(col), emptySet())
        val table2 = Table(TableId("T2"), setOf(col), emptySet())


        val sql1 = SqlAction(
            table1,
            setOf(col),
            1L,
            computedGenes = listOf(
                SqlPrimaryKeyGene(
                    "ID",
                    TableId("T1"),
                    ImmutableDataHolderGene("ID", "1", inQuotes = false),
                    uniqueId = 1L
                )
            ),
            representExistingData = true
        )
        val sql2 = SqlAction(
            table1,
            setOf(col),
            1L,
            computedGenes = listOf(IntegerGene("ID", 2)),
            representExistingData = false
        )

        val sql3 = SqlAction(
            table2,
            setOf(col),
            1L,
            computedGenes = listOf(
                SqlPrimaryKeyGene(
                    "ID", TableId("T2"),
                    ImmutableDataHolderGene("ID", "3", inQuotes = false),
                    uniqueId = 2L
                )
            ),
            representExistingData = true
        )
        val sql4 = SqlAction(
            table2,
            setOf(col),
            1L,
            computedGenes = listOf(IntegerGene("ID", 4)),
            representExistingData = false
        )

        // build individuals from single actions with initialization SQL
        val first = RestIndividual(actions = mutableListOf(callA), sampleType = SampleType.RANDOM)
        val second = RestIndividual(actions = mutableListOf(callB), sampleType = SampleType.RANDOM)

        first.addInitializingDbActions(0, listOf(sql1, sql2))
        second.addInitializingDbActions(0, listOf(sql3, sql4))

        // merge
        val builder = RestIndividualBuilder.createIndependentBuilderForTests()
        val merged = builder.merge(first, second)

        // main executable actions should contain both actions in order
        val mains = merged.seeMainExecutableActions()
        assertEquals(2, mains.size)
        assertEquals(callA.getName(), mains[0].getName())
        assertEquals(callB.getName(), mains[1].getName())

        // initializing SQL actions should contain both sql1 and sql2
        val inits = merged.seeInitializingActions().filterIsInstance<SqlAction>()
        assertEquals(4, inits.size)
        /*
            check their table names preserved and order: first's sql then second's sql.
            however, existing data is always at the beginning
         */
        assertTrue(inits[0].representExistingData)
        assertTrue(inits[1].representExistingData)
        assertFalse(inits[2].representExistingData)
        assertFalse(inits[3].representExistingData)
        assertEquals("T1", inits[2].table.name)
        assertEquals("T2", inits[3].table.name)

        // merged total actions should be sum of both initial individuals
        val before = first.seeAllActions().size + second.seeAllActions().size
        assertEquals(before, merged.seeAllActions().size)
    }



    @Test
    fun testMergeInitActionsWithDuplicatedExistingData() {
        // create two simple RestCallAction with no parameters
        val callA = RestCallAction("idA", HttpVerb.GET, RestPath("/a"), mutableListOf())
        val callB = RestCallAction("idB", HttpVerb.GET, RestPath("/b"), mutableListOf())

        // create a simple table/column for sql initialization
        val col = Column(
            "ID",
            ColumnDataType.INT,
            primaryKey = true,
            databaseType = org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType.H2,
            unique = true
        )
        val table1 = Table(TableId("T1"), setOf(col), emptySet())
        val table2 = Table(TableId("T2"), setOf(col), emptySet())


        val sql1 = SqlAction(
            table1,
            setOf(col),
            1L,
            computedGenes = listOf(
                SqlPrimaryKeyGene(
                    "ID",
                    TableId("T1"),
                    ImmutableDataHolderGene("ID", "1", inQuotes = false),
                    uniqueId = 1L
                )
            ),
            representExistingData = true
        )
        val sql2 = SqlAction(
            table1,
            setOf(col),
            2L,
            computedGenes = listOf(IntegerGene("ID", 2)),
            representExistingData = false
        )

        val sql3 = SqlAction(
            table2,
            setOf(col),
            3L,
            computedGenes = listOf(
                SqlPrimaryKeyGene(
                    "ID", TableId("T2"),
                    ImmutableDataHolderGene("ID", "3", inQuotes = false),
                    uniqueId = 2L
                )
            ),
            representExistingData = true
        )
        val sql4 = SqlAction(
            table2,
            setOf(col),
            4L,
            computedGenes = listOf(IntegerGene("ID", 4)),
            representExistingData = false
        )
        val sql5 = SqlAction(
            table2,
            setOf(col),
            5L,
            computedGenes = listOf(
                SqlPrimaryKeyGene(
                    "ID", TableId("T2"),
                    ImmutableDataHolderGene("ID", "5", inQuotes = false),
                    uniqueId = 2L
                )
            ),
            representExistingData = true
        )


        // build individuals from single actions with initialization SQL
        val first = RestIndividual(actions = mutableListOf(callA), sampleType = SampleType.RANDOM)
        val second = RestIndividual(actions = mutableListOf(callB), sampleType = SampleType.RANDOM)

        first.addInitializingDbActions(0, listOf(sql5,sql1, sql2))
        second.addInitializingDbActions(0, listOf(sql3,sql5.copy() as SqlAction, sql4))

        // merge
        val builder = RestIndividualBuilder.createIndependentBuilderForTests()
        val merged = builder.merge(first, second)
        merged.verifyValidity()

        // main executable actions should contain both actions in order
        val mains = merged.seeMainExecutableActions()
        assertEquals(2, mains.size)
        assertEquals(callA.getName(), mains[0].getName())
        assertEquals(callB.getName(), mains[1].getName())

        // initializing SQL actions should not contain the duplicated existing sql5
        val inits = merged.seeInitializingActions().filterIsInstance<SqlAction>()
        assertEquals(5, inits.size)
        /*
            check their table names preserved and order: first's sql then second's sql.
            however, existing data is always at the beginning
         */
        assertTrue(inits[0].representExistingData)
        assertTrue(inits[1].representExistingData)
        assertTrue(inits[2].representExistingData)
        assertFalse(inits[3].representExistingData)
        assertFalse(inits[4].representExistingData)
        assertEquals("T1", inits[3].table.name)
        assertEquals("T2", inits[4].table.name)

        // merged total actions should be sum of both initial individuals, minus the 1 duplicate
        val before = first.seeAllActions().size + second.seeAllActions().size
        assertEquals(before-1, merged.seeAllActions().size)
    }

}
