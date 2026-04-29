package org.evomaster.core.problem.enterprise

import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnterpriseIndividualTest {

    class TestAction : Action(emptyList()) {
        override fun copyContent() = TestAction()
        override fun getName() = "TestAction"
        override fun seeTopGenes() = emptyList<org.evomaster.core.search.gene.Gene>()
        override fun shouldCountForFitnessEvaluations() = true
    }

    class TestIndividual(children: MutableList<out ActionComponent> = mutableListOf()) : EnterpriseIndividual(
        sampleTypeField = SampleType.RANDOM,
        children = children,
        childTypeVerifier = EnterpriseChildTypeVerifier(TestAction::class.java),
        groups = getEnterpriseTopGroups(children, 0, children.size, 0, 0, 0, 0, 0)
    )

    @Test
    fun testGetInsertTableNamesReturnsGroups() {
        val sealedName = "my_sealed_group"
        val openName = "my_open_group"
        val tableName = "my_table"
        
        val tableId = TableId(name = tableName, sealedGroupName = sealedName, openGroupName = openName)
        val table = Table(id = tableId, columns = emptySet(), foreignKeys = emptySet())
        val sqlAction = SqlAction(table = table, selectedColumns = emptySet(), insertionId = 1L)
        
        val individual = TestIndividual()
        individual.addInitializingDbActions(actions = listOf(sqlAction))
        
        // This should not be included as it is existing data
        val existingTableId = TableId(name = "existing_table", sealedGroupName = "s", openGroupName = "o")
        val existingTable = Table(id = existingTableId, columns = emptySet(), foreignKeys = emptySet())
        val existingSqlAction = SqlAction(
            table = existingTable, 
            selectedColumns = emptySet(), 
            insertionId = 2L, 
            representExistingData = true,
            computedGenes = listOf(org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene("id", "1", false))
        )
        individual.addInitializingDbActions(actions = listOf(existingSqlAction))

        val tableNames = individual.getInsertTableNames()
        
        assertEquals(1, tableNames.size)
        val resultTableId = tableNames[0]
        assertEquals(tableName, resultTableId.name)
        assertEquals(sealedName, resultTableId.sealedGroupName)
        assertEquals(openName, resultTableId.openGroupName)
    }
}
