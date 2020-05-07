package org.evomaster.core.search.mutationweight.individual

import io.swagger.parser.OpenAPIParser
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.schema.Column
import org.evomaster.core.database.schema.ColumnDataType
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-05-07
 */
class IndividualMutationweightTest {

    private fun sumWeight(genes : List<Gene>) = genes.sumBy { it.mutationWeight() }

    @Test
    fun testOneMaxIndividual(){
        val individual = OneMaxIndividual(2)
        assertEquals(2, sumWeight(individual.seeGenes()))
    }

    @Test
    fun testRestIndividual(){
        val key =  Column("key", ColumnDataType.INTEGER, 10,
                primaryKey = true,
                databaseType = DatabaseType.H2)
        val date =  Column("date", ColumnDataType.DATE, databaseType = DatabaseType.H2)
        val info = Column("info", ColumnDataType.JSON, databaseType = DatabaseType.H2)
        val table = Table("foo", setOf(key, date, info), setOf())

        val gk0 = SqlPrimaryKeyGene(key.name, table.name, IntegerGene(key.name, 1), 1)
        val gd0 = DateGene(date.name)
        val gj0 = ObjectGene(info.name, listOf(DateGene("field1"), IntegerGene("field2", 2)))
        val action0 =  DbAction(table, setOf(key, date, info), 0L, listOf(gk0, gd0, gj0))

        val dbActions = mutableListOf(action0)

        val schema = OpenAPIParser().readLocation("/swagger/artificial/foo.json", null, null).openAPI

        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(schema, actions)

        assertEquals(1, actions.size)

        // 1 dbaction with 3 genes, and 1 restaction with 1 bodyGene and 1 contentType
        val individual = RestIndividual(actions = actions.values.toMutableList(), dbInitialization = dbActions, sampleType = SampleType.RANDOM)

        val sql = individual.seeGenes(Individual.GeneFilter.ONLY_SQL)
        assertEquals(3, sql.size)
        //1 sql key , 3 for date, 2 for info obj
        assertEquals(1+3+2, sumWeight(sql))

        val other = individual.seeGenes(Individual.GeneFilter.NO_SQL)
        assertEquals(1+1, other.size)
        // 3 for foo type, 1 for content type
        assertEquals(3+1, sumWeight(other))

        val all = individual.seeGenes()
        assertEquals(5, all.size)
        assertEquals(10, sumWeight(all))
    }
}