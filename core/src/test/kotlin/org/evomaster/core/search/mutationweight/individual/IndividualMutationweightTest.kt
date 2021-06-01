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

    private fun sumWeight(genes : List<Gene>) : Double = genes.map { it.mutationWeight() }.sum()

    @Test
    fun testOneMaxIndividual(){
        val individual = OneMaxIndividual(2)
        assertEquals(2.0, sumWeight(individual.seeGenes()))
    }

    @Test
    fun testRestIndividual(){
        val individual = newRestIndividual()

        val sql = individual.seeGenes(Individual.GeneFilter.ONLY_SQL)
        assertEquals(3, sql.size)
        //1 sql key , 1 for date, 2 for info obj
        assertEquals(4.0, sumWeight(sql))

        val other = individual.seeGenes(Individual.GeneFilter.NO_SQL).filter { it.isMutable() }
        assertEquals(1, other.size)
        /*
            by default, fields of object genes are OptionalGene that might increase the static weight
         */
        // 2+2+5 for foo type
        assertEquals(9.0, sumWeight(other))

        val all = individual.seeGenes().filter { it.isMutable() }
        assertEquals(4, all.size)
        assertEquals(13.0, sumWeight(all))
    }

    companion object{
        /**
         * @return a rest individual which contains 4 genes
         * - three SQL genes, i.e., integerGene(w=1), DateGene(w=1), ObjectGene (w=2)
         * - one REST gene, i.e., ObjectGene(w=9)
         */
        fun newRestIndividual(name : String = "POST:/foo", numSQLAction : Int = 1, numRestAction : Int = 1) : RestIndividual{
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

            val dbActions = (0 until numSQLAction).map { action0.copy() as DbAction}.toMutableList()

            val schema = OpenAPIParser().readLocation("/swagger/artificial/foo.json", null, null).openAPI

            val actions: MutableMap<String, Action> = mutableMapOf()
            RestActionBuilderV3.addActionsFromSwagger(schema, actions)

            val action1 = actions[name]?: throw IllegalArgumentException("$name cannot found in defined schema")

            // 1 dbaction with 3 genes, and 1 restaction with 1 bodyGene and 1 contentType
            return RestIndividual(actions = (0 until numRestAction).map { action1.copy() as RestCallAction}.toMutableList(), dbInitialization = dbActions, sampleType = SampleType.RANDOM)
        }
    }
}