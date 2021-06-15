package org.evomaster.core.search.structuralelement.individual

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.ForeignKey
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.structuralelement.StructuralElementBaseTest
import org.evomaster.core.search.structuralelement.resourcecall.ResourceNodeCluster
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
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
        val action0 = DbAction(foo, setOf(idColumn), insertId0, listOf(pkGeneFoo))

        val insertId1 = 1002L
        val pkGeneBar = SqlPrimaryKeyGene("Id", "Bar", IntegerGene("Id", 2, 0, 10), insertId0)
        val fkGene = SqlForeignKeyGene("fooId", insertId1, "Foo", false, insertId0)

        val action1 = DbAction(bar, setOf(barIdColumn, fkColumn), insertId1, listOf(pkGeneBar, fkGene))


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
        val root = getStructuralElement()
        assertEquals(root, root.getRoot())

        val barId = root.seeInitializingActions()[1].seeGenes()[0]
        val dbpath = listOf(1, 0)
        assertEquals(barId, root.targetWithIndex(dbpath))

        val floatValue = ((root.seeActions()[0].parameters[0] as BodyParam).gene as ObjectGene).fields[3]
        val path = listOf(2, 0, 0, 0, 3)
        assertEquals(floatValue, root.targetWithIndex(path))

        val actualPath = mutableListOf<Int>()
        floatValue.traverseBackIndex(actualPath)
        assertEquals(path, actualPath)

    }
}

