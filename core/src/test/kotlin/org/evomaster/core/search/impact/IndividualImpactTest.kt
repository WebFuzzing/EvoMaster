package org.evomaster.core.search.impact

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * check init and update impact of individual
 */
class IndividualImpactTest {

    private fun generateFakeDbAction(pkId : Long, pkGeneUniqueId: Long) : DbAction{
        val fooId = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, databaseType = DatabaseType.H2)
        val foo = Table("Foo", setOf(fooId), setOf())
        val integerGene = IntegerGene(fooId.name)
        val pkFoo = SqlPrimaryKeyGene(fooId.name, "Foo", integerGene, pkGeneUniqueId)
        return DbAction(foo, setOf(fooId), pkId, listOf(pkFoo))
    }

    private fun generateFakeRestAction(id: String) : RestCallAction{
        val queryNameParam = QueryParam("name", StringGene("name"))
        val queryIdParam = QueryParam("id", IntegerGene("id"))

        return RestCallAction(id, HttpVerb.GET, RestPath("/foo"), mutableListOf(queryIdParam, queryNameParam))
    }

    private fun generateFakeRestIndividual() : RestIndividual{

        val pkGeneUniqueId = 12345L
        val fooInsertion = generateFakeDbAction(1001L, 12345L)
        val fooId = fooInsertion.table.columns.first()

        val barInsertionId = 1002L
        val integerGene = IntegerGene(fooId.name, 42, 0, 10)
        val pkBar = SqlPrimaryKeyGene(fooId.name, "Bar", integerGene, 10)
        val fkId = Column("fkId", ColumnDataType.INTEGER, 10, primaryKey = false, databaseType = DatabaseType.H2)
        val foreignKeyGene = SqlForeignKeyGene(fkId.name, barInsertionId, "Foo", false, uniqueIdOfPrimaryKey = pkGeneUniqueId)
        val bar = Table("Bar", setOf(fooId, fkId), setOf())
        val barInsertion = DbAction(bar, setOf(fooId, fkId), barInsertionId, listOf(pkBar, foreignKeyGene))

        val queryIdParam = QueryParam("id", IntegerGene("id"))

        val fooAction = generateFakeRestAction("1")
        val barAction = RestCallAction("2", HttpVerb.GET, RestPath("/bar"), mutableListOf(queryIdParam.copy()))

        return RestIndividual(mutableListOf(fooAction, barAction), SampleType.RANDOM, mutableListOf(fooInsertion, barInsertion))
    }

    @Test
    fun testImpactInit(){

        val ind = generateFakeRestIndividual()

        val impactInfo = ImpactsOfIndividual(ind, false, null)

        impactInfo.fixedMainActionImpacts.apply {
            assertEquals(2, size)
            assertEquals(2, this[0].geneImpacts.size)
            assertEquals(1, this[1].geneImpacts.size)
        }
    }
}