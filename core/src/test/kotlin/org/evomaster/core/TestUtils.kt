package org.evomaster.core

import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene


object TestUtils {

    /**
     * handle initialization of individual which are created in unit and integration tests
     */
    fun doInitializeIndividualForTesting(individual: Individual, randomness: Randomness? = null){
        individual.doInitializeLocalId()
        individual.doInitialize(randomness)
    }

    /**
     * Unfortunately JUnit 5 does not handle flaky tests, and Maven is not upgraded yet.
     * See https://github.com/junit-team/junit5/issues/1558#issuecomment-414701182
     *
     * TODO: once that issue is fixed (if it will ever be fixed), then this method
     * will no longer be needed
     *
     * @param lambda
     */
    fun handleFlaky(lambda: () -> Unit) {

        val attempts = 3
        var error: Throwable? = null

        for (i in 0 until attempts) {

            try {
                lambda.invoke()
                return
            } catch (e: OutOfMemoryError) {
                throw e
            } catch (t: Throwable) {
                error = t
            }

        }

        throw error!!
    }

    // for rest
    private fun generateFakeDbAction(pkId : Long, pkGeneUniqueId: Long) : SqlAction {
        val fooId = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, databaseType = DatabaseType.H2)
        val foo = Table("Foo", setOf(fooId), setOf())
        val integerGene = IntegerGene(fooId.name)
        val pkFoo = SqlPrimaryKeyGene(fooId.name, "Foo", integerGene, pkGeneUniqueId)
        return SqlAction(foo, setOf(fooId), pkId, listOf(pkFoo))
    }

    private fun generateFakeRestAction(id: String) : RestCallAction {
        val queryNameParam = QueryParam("name", StringGene("name"))
        val queryIdParam = QueryParam("id", IntegerGene("id"))

        return RestCallAction(id, HttpVerb.GET, RestPath("/foo"), mutableListOf(queryIdParam, queryNameParam))
    }

    fun generateFakeSimpleRestIndividual() : RestIndividual {

        val pkGeneUniqueId = 12345L
        val fooInsertion = generateFakeDbAction(1001L, 12345L)
        val fooId = fooInsertion.table.columns.first()

        val barInsertionId = 1002L
        val integerGene = IntegerGene(fooId.name, 42, 0, 10)
        val pkBar = SqlPrimaryKeyGene(fooId.name, "Bar", integerGene, 10)
        val fkId = Column("fkId", ColumnDataType.INTEGER, 10, primaryKey = false, databaseType = DatabaseType.H2)
        val foreignKeyGene = SqlForeignKeyGene(fkId.name, barInsertionId, "Foo", false, uniqueIdOfPrimaryKey = pkGeneUniqueId)
        val bar = Table("Bar", setOf(fooId, fkId), setOf())
        val barInsertion = SqlAction(bar, setOf(fooId, fkId), barInsertionId, listOf(pkBar, foreignKeyGene))

        val queryIdParam = QueryParam("id", IntegerGene("id"))

        val fooAction = generateFakeRestAction("1")
        val barAction = RestCallAction("2", HttpVerb.GET, RestPath("/bar"), mutableListOf(queryIdParam.copy()))

        return RestIndividual(mutableListOf(fooAction, barAction), SampleType.RANDOM, mutableListOf(fooInsertion, barInsertion))
    }
}