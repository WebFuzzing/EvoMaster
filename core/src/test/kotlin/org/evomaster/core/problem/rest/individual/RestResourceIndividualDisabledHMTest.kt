package org.evomaster.core.problem.rest.individual

import com.google.inject.*
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.resource.ResourceCluster
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.util.BindingBuilder.isExtraTaintParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.search.service.mutator.StandardMutator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * tests are to check resource-based sampler and mutator which do not employ
 * adaptive hypermutation
 *
 * beside the basic tests for sampler and mutator inherited from [RestIndividualTestBase]
 * there are tests for resource call manipulation and binding genes within a resource call
 */
class RestResourceIndividualDisabledHMTest : RestIndividualTestBase(){

    private lateinit var sampler : ResourceSampler
    private lateinit var mutator : ResourceRestMutator
    private lateinit var ff : RestFitness
    private lateinit var rm : ResourceManageService
    private lateinit var cluster : ResourceCluster


    @BeforeEach
    fun initConfig(){
        /*
            for testing purpose, all fields of ObjectGene should be bound
            such configuration should be done before initializing injector
         */
        BindingBuilder.setProbabilityOfBindingObject(1.0)
    }

    override fun config(): Array<String> {
        return arrayOf(
            "--enableTrackEvaluatedIndividual=false",
            "--weightBasedMutationRate=false",
            "--probOfArchiveMutation=0.0",
            // for testing purpose, force creating insertion for resource call
            "--probOfApplySQLActionToCreateResources=1.0"
        )
    }

    override fun initService(injector: Injector) {

        sampler = injector.getInstance(ResourceSampler::class.java)
        mutator = injector.getInstance(ResourceRestMutator::class.java)
        ff = injector.getInstance(RestFitness::class.java)
        rm = injector.getInstance(ResourceManageService::class.java)
        cluster = rm.cluster
    }

    override fun getProblemModule(): Module = ResourceRestModule(false)

    override fun getSampler() = sampler

    override fun getMutator(): StandardMutator<RestIndividual> = mutator

    override fun getFitnessFunction(): AbstractRestFitness = ff

    private fun sampleDbAction(table : Table) : List<SqlAction>{
        val actions = sqlInsertBuilder!!.createSqlInsertionAction(table.name)
        return actions.map { it.copy() as SqlAction }
    }

    private fun sampleResourceCall(resNode: RestResourceNode? = null): RestResourceCalls {
        val node = resNode?: randomness.choose(cluster.getCluster().values)
        val sampleOption = randomness.nextInt(0, 3)
        return when (sampleOption) {
            0 -> node.sampleOneAction(verb = null, randomness)
            1 -> node.sampleIndResourceCall(randomness, config.maxTestSize)
            2 -> node.sampleAnyRestResourceCalls(randomness, config.maxTestSize)
            3 -> node.sampleRestResourceCalls(randomness.choose(node.getTemplates().values).template, randomness, config.maxTestSize)
            else -> throw IllegalStateException("not support")
        }
    }

    private fun sampleRestIndividual(dbSize : Int, resourceSize: Int): RestIndividual{
        val sqlActions = mutableListOf<SqlAction>()
        val resoureCalls = mutableListOf<RestResourceCalls>()
        do {
            val table = randomness.choose(cluster.getTableInfo().values)
            sqlActions.addAll(sampleDbAction(table))
        }while (sqlActions.size < dbSize)


        do {
            val node = randomness.choose(cluster.getCluster().values)
            resoureCalls.add(sampleResourceCall(node))
        }while (resoureCalls.size < resourceSize)
        return RestIndividual(dbInitialization = sqlActions, resourceCalls = resoureCalls, sampleType = SampleType.RANDOM)
    }

    /**
     * check manipulation of resource calls in RestIndividual
     * such as add, remove, modify
     */
    @ParameterizedTest
    @MethodSource("getBudgetAndNumOfResourceForSampler")
    fun testIndividualResourceManipulation(iteration: Int, numResource: Int){
        initResourceNode(numResource, 5)

        config.maxActionEvaluations = iteration
        config.maxTestSize = 20
        (0 until iteration).forEach { _ ->
            val dbSize = randomness.nextInt(1, 15)
            val resourceSize = randomness.nextInt(2, 4)

            val ind = sampleRestIndividual(dbSize, resourceSize)
            assertEquals(dbSize + resourceSize, ind.getViewOfChildren().size)

            val call = sampleResourceCall()

            // add
            val addedIndex = randomness.nextInt(0, resourceSize-1)
            ind.addResourceCall(addedIndex, call)
            assertEquals(dbSize + addedIndex, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize + resourceSize + 1, ind.getViewOfChildren().size)

            // remove
            ind.removeResourceCall(addedIndex)
            assertEquals(-1, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize + resourceSize, ind.getViewOfChildren().size)

            // replace
            val original = ind.getIndexedResourceCalls()
            val old = randomness.choose(original.keys)
            val oldRes = original[old]!!
            ind.replaceResourceCall(old, call)
            assertEquals(-1, ind.getViewOfChildren().indexOf(oldRes))
            assertEquals(dbSize + old, ind.getViewOfChildren().indexOf(call))
            assertEquals(dbSize + resourceSize, ind.getViewOfChildren().size)

            // swap
            val setToSwap = ind.getIndexedResourceCalls()
            val candidates = randomness.choose(setToSwap.keys, 2)
            assertEquals(2, candidates.size)
            val first = setToSwap[candidates.first()]!!
            val second = setToSwap[candidates.last()]!!
            ind.swapResourceCall(candidates.first(), candidates.last())
            assertEquals(dbSize + candidates.first(), ind.getViewOfChildren().indexOf(second))
            assertEquals(dbSize + candidates.last(), ind.getViewOfChildren().indexOf(first))

        }

    }


    override fun extraSampledIndividualCheck(index: Int, individual: RestIndividual) {
        /*
            check binding gene builder

            with 1.0 of probability on PROBABILITY_BINDING_OBJECT with [initConfig]
            all genes with same name should be bound with each other

         */
        individual.getResourceCalls().forEach { call->
            assertNotNull(call.template)

            // db actions in this resource call
            val dbActions = call.seeActions(ActionFilter.ONLY_SQL)
            /*
                sql genes which can be mutated
                if a sql gene is binding with rest gene,
                then only the rest gene is exposed to be mutable
             */
            val sql = call.seeGenes(Individual.GeneFilter.ONLY_SQL)


            /*
                all exposed sql genes should not be bound with any other gene
             */
            if (sql.isNotEmpty()){
                sql.forEach { s->
                    assertFalse(ParamUtil.getValueGene(s).isBoundGene())
                }
            }

            if (call.status == ResourceStatus.CREATED_SQL){
                assertTrue(dbActions.isNotEmpty())
                dbActions.forEach { a->
                    a.seeTopGenes().forEach { g->
                        val valueGene = ParamUtil.getValueGene(g)
                        if (g.isMutable() && valueGene.isMutable()
                                && !sql.contains(valueGene)
                                && !valueGene.existAnyParent { it is SqlForeignKeyGene || it is SqlPrimaryKeyGene || sql.contains(it)}){
                            /*
                                all sql genes which do not expose for mutation should be bound with any other gene
                             */
                            assertTrue(valueGene.isBoundGene(), "fail at the iteration $index")
                        }
                    }
                }
            }

            val nosql = call.seeGenes(Individual.GeneFilter.NO_SQL).filter {
                !isExtraTaintParam(it.name) && it.isMutable()
            }


            when(call.template!!.template){
                "POST-POST",
                "POST-PUT" -> {
                    assertEquals(2, nosql.size)
                    val postBody1 = ParamUtil.getValueGene(nosql[0])
                    val postBody2 = ParamUtil.getValueGene(nosql[1])

                    assertTrue(postBody1 is ObjectGene)
                    assertTrue(postBody2 is ObjectGene)
                    assertEquals((postBody1 as ObjectGene).refType, (postBody2 as ObjectGene).refType)

                    (postBody1 as ObjectGene).fields.forEachIndexed { index, g->
                        val gene1 = ParamUtil.getValueGene(g)
                        val gene2 = ParamUtil.getValueGene((postBody2 as ObjectGene).fields[index])
                        assertTrue(gene1.isDirectBoundWith(gene2))
                        assertTrue(gene2.isDirectBoundWith(gene1))
                    }

                }

                "POST-GET",
                "POST-DELETE"->{
                    assertEquals(2, nosql.size)
                    val postBody1 = ParamUtil.getValueGene(nosql[0])
                    val queryGene2 = ParamUtil.getValueGene(nosql[1])

                    assertTrue(postBody1 is ObjectGene)
                    val firstField = ParamUtil.getValueGene((postBody1 as ObjectGene).fields[0])
                    assertEquals(queryGene2::class.java.simpleName, firstField::class.java.simpleName)

                    assertTrue(firstField.isDirectBoundWith(queryGene2))
                    assertTrue(queryGene2.isDirectBoundWith(firstField))

                }

                "POST",
                "PUT" ->{
                    /*
                        based on current setting, all sql genes should be bound with rest gene,
                        then none of them should expose for mutation
                     */
                    if (dbActions.isNotEmpty())
                        assertEquals(0, sql.size)
                }
                "GET",
                "DELETE" ->{
                    assertEquals(1, nosql.size)
                    if (dbActions.isNotEmpty()){
                        val g = ParamUtil.getValueGene(nosql[0])
                        if (g !is DateGene
                                || dbActions.any { (it as? SqlAction)?.representExistingData == false } // DateGene cannot bind with ImmutableDataHolderGene now
                        ){
                            // rest gene should be bound with at least one sql gene
                            assertTrue(g.isBoundGene())
                        }
                    }
                }
                else->{
                    fail("the template (${call.template!!.template}) should not exist")
                }
            }
        }


        /*
            with a sampled individual
            all values of genes which have the same name should be same
         */
        sameNameWithSameValue(individual)
    }

    override fun extraMutatedIndividualCheck(evaluated: Int, copyOfImpact: ImpactsOfIndividual?,
                                             original: EvaluatedIndividual<RestIndividual>, mutated: EvaluatedIndividual<RestIndividual>) {
        /*
            with a mutated individual
            all values of genes which have the same name should be same
         */
        sameNameWithSameValue(mutated.individual)
    }

    private fun sameNameWithSameValue(individual: RestIndividual){
        individual.getResourceCalls().forEach { call->
            // collect all mutable&bindingable leaf gene for all actions
            val allGene = call.seeActions(ActionFilter.ALL)
                .flatMap { it.seeTopGenes() }
                .filter { !isExtraTaintParam(it.name) && it.isMutable() && it !is SqlPrimaryKeyGene && it !is SqlForeignKeyGene }
                .flatMap { it.flatView{g: Gene -> g is DateGene || g is DateTimeGene || g is TimeGene} }.filter { it.getViewOfChildren().isEmpty() }

            allGene.groupBy { it.name }.forEach { (t, u) ->
                if (u.size > 1){
                    val printableValue = u.first().getValueAsPrintableString()
                    u.forEach {
                        assertEquals(printableValue, it.getValueAsPrintableString())
                    }
                }
            }
        }
    }
}
