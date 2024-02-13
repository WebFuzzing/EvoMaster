package org.evomaster.core

import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.Randomness

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.problem.api.param.Param
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

    // for rest problem
    fun generateFakeDbAction(pkId : Long, pkGeneUniqueId: Long, tableName : String = "Foo", intValue : Int =0, fkColumn : Column?=null, fkGene: SqlForeignKeyGene? = null) : SqlAction {
        val fooId = Column("Id", ColumnDataType.INTEGER, 10, primaryKey = true, databaseType = DatabaseType.H2)
        val foo = Table(tableName, setOf(fooId), setOf())
        val integerGene = IntegerGene(fooId.name, intValue)
        val pkFoo = SqlPrimaryKeyGene(fooId.name, "Foo", integerGene, pkGeneUniqueId)
        if(fkColumn != null && fkGene != null)
            return SqlAction(foo, setOf(fooId, fkColumn), pkId, listOf(pkFoo, fkGene))
        return SqlAction(foo, setOf(fooId), pkId, listOf(pkFoo))
    }

    fun generateTwoFakeDbActions(aUniqueId : Long, bUniqueId : Long, aId: Long, bId: Long, aTable: String, bTable: String, aValue : Int = 0, bValue : Int = 42) : List<SqlAction>{
        val fooInsertion = generateFakeDbAction(aId, aUniqueId, aTable, aValue)

        val fkColumName = "fkId"
        val fkId = Column(fkColumName, ColumnDataType.INTEGER, 10, primaryKey = false, databaseType = DatabaseType.H2)
        val foreignKeyGene = SqlForeignKeyGene(fkColumName, bId, aTable, false, uniqueIdOfPrimaryKey = aUniqueId)

        val barInsertion = generateFakeDbAction(bId, bUniqueId,  bTable, bValue, fkId, foreignKeyGene)

        return listOf(fooInsertion, barInsertion)
    }

    fun generateFakeQueryRestAction(id: String, pathString: String, onlyId : Boolean = false) : RestCallAction {
        val queryNameParam = QueryParam("name", StringGene("name"))
        val queryIdParam = QueryParam("id", IntegerGene("id"))
        val actions : MutableList<Param> = if (onlyId) mutableListOf(queryIdParam) else  mutableListOf(queryIdParam, queryNameParam)
        return RestCallAction(id, HttpVerb.GET, RestPath(pathString), actions)
    }


}