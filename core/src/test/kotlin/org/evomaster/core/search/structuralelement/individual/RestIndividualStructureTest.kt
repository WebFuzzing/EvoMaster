package org.evomaster.core.search.structuralelement.individual

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.evomaster.core.search.structuralelement.resourcecall.ResourceNodeCluster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RestIndividualStructureTest : StructuralElementBaseTest(){
    override fun getStructuralElement(): RestIndividual {

        val idColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val foo = Table("Foo", setOf(idColumn), setOf())

        val barIdColumn = Column("Id", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val fkColumn = Column("fooId", ColumnDataType.INTEGER, 10,
            primaryKey = true,
            autoIncrement = false,
            unique = false,
            databaseType = DatabaseType.H2)

        val foreignKey = ForeignKey(sourceColumns = setOf(fkColumn), targetTable = foo.name)

        val bar = Table("Bar", setOf(barIdColumn, fkColumn), setOf(foreignKey))

        val insertId0 = 1001L
        val pkGeneFoo = SqlPrimaryKeyGene("Id", "Foo", IntegerGene("Id", 1, 0, 10), insertId0)
        val action0 = SqlAction(foo, setOf(idColumn), insertId0, listOf(pkGeneFoo))

        val insertId1 = 1002L
        val pkGeneBar = SqlPrimaryKeyGene("Id", "Bar", IntegerGene("Id", 2, 0, 10), insertId0)
        val fkGene = SqlForeignKeyGene("fooId", insertId1, "Foo", false, insertId0)

        val action1 = SqlAction(bar, setOf(barIdColumn, fkColumn), insertId1, listOf(pkGeneBar, fkGene))


        val fooNode = ResourceNodeCluster.cluster.getResourceNode("/v3/api/rfoo/{rfooId}")
        val barNode = ResourceNodeCluster.cluster.getResourceNode("/v3/api/rfoo/{rfooId}/rbar/{rbarId}")

        val call1 = fooNode?.sampleRestResourceCalls("POST-GET", ResourceNodeCluster.randomness, 10) ?: throw IllegalStateException()
        val call2 = barNode?.sampleRestResourceCalls("GET", ResourceNodeCluster.randomness, 10) ?: throw IllegalStateException()


        return RestIndividual(mutableListOf(call1, call2), SampleType.RANDOM, dbInitialization = mutableListOf(action0, action1))

    }

    // 2 db + 2 resource call
    override fun getExpectedChildrenSize(): Int = 2 + 2



    @Test
    fun testTraverseBackIndex(){
        val root = getStructuralElementAndIdentifyAsRoot() as RestIndividual
        assertEquals(root, root.getRoot())

        val barId = root.seeInitializingActions()[1].seeTopGenes()[0]
        val dbpath = listOf(1, 0)
        assertEquals(barId, root.targetWithIndex(dbpath))

        // root.seeMainExecutableActions()[0] is obtained with 2-> 0-> 0, i.e., 2nd children (resourceCall) -> 0th group -> 0th action
        val action = root.seeMainExecutableActions()[0]
        val param = action.parameters.find { it is BodyParam }
        assertNotNull(param)
        val index = action.parameters.indexOf(param)
        val floatValue = (param!!.gene as ObjectGene).fields[3]
        val path = listOf(2, 0, 0, index, 0, 3)
        assertEquals(floatValue, root.targetWithIndex(path))

        val actualPath = mutableListOf<Int>()
        floatValue.traverseBackIndex(actualPath)
        assertEquals(path, actualPath)

    }
}

