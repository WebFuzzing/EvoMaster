package org.evomaster.core.search.mutationweight.individual

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.mutationweight.GeneWeightTestSchema
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2020-05-07
 */
class IndividualMutationweightTest {

    private fun sumWeight(genes : List<Gene>) : Double = genes.map { it.mutationWeight() }.sum()

    @Test
    fun testOneMaxIndividual(){
        val individual = OneMaxIndividual(2)
        assertEquals(2.0, sumWeight(individual.seeTopGenes()))
    }

    @Test
    fun testDetailInRestAction(){
        val action = GeneWeightTestSchema.getActionFromCluster("POST:/gw/foo")
        assertNotNull(action)
        assertTrue(action is RestCallAction)
        (action as RestCallAction).apply {
            assertEquals(1, parameters.size)
            assertTrue(action.parameters.first() is BodyParam)
            assertTrue((action.parameters.first() as BodyParam).gene is ObjectGene)
            ((action.parameters.first() as BodyParam).gene as ObjectGene).apply {
                assertEquals(3, fields.size, "${action.getName()} \n ${action.getDescription()} \n fields are ${fields.mapIndexed { index, gene ->  "$index:${gene.name}" }.joinToString(",")}")
                fields[0].apply {
                    assertTrue(this is OptionalGene)
                    assertEquals(2.0, mutationWeight())
                    assertTrue((this as OptionalGene).gene is IntegerGene)
                }

                fields[1].apply {
                    assertTrue(this is OptionalGene)
                    assertEquals(2.0, mutationWeight())
                    assertTrue((this as OptionalGene).gene is DateTimeGene)
                }

                fields[2].apply {
                    assertTrue(this is OptionalGene)
                    assertEquals(5.0, mutationWeight())
                    assertTrue((this as OptionalGene).gene is ObjectGene)
                }

            }
        }
    }

    @Test
    fun testRestIndividual(){

        val individual = GeneWeightTestSchema.newRestIndividual()

        val sql = individual.seeTopGenes(ActionFilter.ONLY_SQL)
        assertEquals(3, sql.size)
        //1 sql key , 1 for date, 2 for info obj
        assertEquals(4.0, sumWeight(sql))

        val other = individual.seeTopGenes(ActionFilter.NO_SQL).filter { it.isMutable() }
        assertEquals(1, other.size)
        assertTrue(other.first() is ObjectGene)
        (other.first() as ObjectGene).apply {
            fields.forEachIndexed { index, f->
                assertTrue(f is OptionalGene, "at index $index: ${f::class.java.simpleName}")
            }
            assertEquals(3, fields.size,"fields are ${fields.mapIndexed { index, gene ->  "$index:${gene.name}" }.joinToString(",")}")
            assertEquals(2.0, fields[0].mutationWeight())
            assertEquals(2.0, fields[1].mutationWeight())
            assertEquals(5.0, fields[2].mutationWeight())
        }
        /*
            by default, fields of object genes are OptionalGene that might increase the static weight
         */
        // 2+2+5 for foo type
        assertEquals(9.0, other.first().mutationWeight())


        val all = individual.seeTopGenes().filter { it.isMutable() }
        assertEquals(4, all.size)
        assertEquals(13.0, sumWeight(all))
    }
}