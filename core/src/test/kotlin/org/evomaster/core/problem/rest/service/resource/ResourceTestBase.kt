package org.evomaster.core.problem.rest.service.resource

import com.google.inject.Module
import com.netflix.governator.lifecycle.LifecycleManager
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.extract.h2.ExtractTestBaseH2
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.service.*
import org.evomaster.core.problem.rest.service.resource.model.ResourceBasedTestInterface
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceModule
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceSampler
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

abstract class ResourceTestBase : ExtractTestBaseH2(), ResourceBasedTestInterface {

    private lateinit var config: EMConfig
    private lateinit var sampler: SimpleResourceSampler
    private lateinit var structureMutator : RestResourceStructureMutator
    private lateinit var rm: ResourceManageService
    private lateinit var dm: ResourceDepManageService
    private lateinit var ssc : ResourceSampleMethodController
    private lateinit var lifecycleManager : LifecycleManager
    private lateinit var randomness: Randomness

    @BeforeEach
    fun init() {
        val injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(SimpleResourceModule(), BaseModule()))
                .build().createInjector()

        lifecycleManager = injector.getInstance(LifecycleManager::class.java)
        lifecycleManager.start()

        sampler = injector.getInstance(SimpleResourceSampler::class.java)
        structureMutator = injector.getInstance(RestResourceStructureMutator::class.java)
        config = injector.getInstance(EMConfig::class.java)
        rm = injector.getInstance(ResourceManageService::class.java)
        dm = injector.getInstance(ResourceDepManageService::class.java)
        ssc = injector.getInstance(ResourceSampleMethodController::class.java)
        randomness = injector.getInstance(Randomness::class.java)

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        config.heuristicsForSQL = false
        config.generateSqlDataWithSearch = false
        config.generateSqlDataWithDSE = false
        config.geneMutationStrategy = EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
    }

    @AfterEach
    fun close(){
        lifecycleManager.close()
    }

    abstract fun getSwaggerLocation(): String

    private fun getDatabaseExecutor() : DatabaseExecutor = DirectDatabaseExecutor()

    private class DirectDatabaseExecutor : DatabaseExecutor {

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): Map<Long, Long>? {
            return null
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return SqlScriptRunner.execCommand(connection, dto.command).toDto()
        }

        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return false
        }
    }

    private fun preSteps(skip : List<String> = listOf(), doesInvolveDatabase : Boolean = false, doesAppleNameMatching : Boolean = false, probOfDep : Double = 0.0){
        config.probOfApplySQLActionToCreateResources = if(doesInvolveDatabase) 0.5 else 0.0
        config.doesApplyNameMatching = doesAppleNameMatching

        config.probOfEnablingResourceDependencyHeuristics = probOfDep

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), skip, sqlBuilder)

    }

    fun testSizeResourceCluster(size : Int){
        assertEquals(size, rm.getResourceCluster().size)
    }

    fun testSpecificResourceNode(resource: String, expectedSizeOfTemplates: Int, expectedTemplates: List<String> = listOf(), expectedIsIndependent: Boolean){
        rm.getResourceCluster().getValue(resource)
            .apply {
                assertEquals(expectedSizeOfTemplates, getTemplates().size)
                assert(expectedTemplates.all { getTemplates().containsKey(it) })
                assertEquals(expectedIsIndependent, isIndependent())
            }
    }


    private fun testAllApplicableMethods(){
        ssc.getApplicableMethods().apply {
            forEach { m->
                testApplicableMethodWithoutDependency(m)
            }
        }
    }

    fun testInitialedApplicableMethods(expectedSizeOfMethods : Int, expectedMethods : List<ResourceSamplingMethod> = listOf()){
        ssc.getApplicableMethods().apply {
            assertEquals(expectedSizeOfMethods, size)
            assert(expectedMethods.all { this.contains(it) })
        }
    }

    private fun testApplicableMethodWithoutDependency(method : ResourceSamplingMethod){
        sampler.sampleWithMethodAndDependencyOption(method, false).apply {
            assertNotNull(this)
            when(method){
                ResourceSamplingMethod.S1iR->{
                    assertEquals(1, this!!.getResourceCalls().size)
                    assert(getResourceCalls().first().template!!.independent){
                        "the template of the first call with $method should be independent, but ${getResourceCalls().first().template!!.template}"
                    }
                }
                ResourceSamplingMethod.S1dR->{
                    assertEquals(1, this!!.getResourceCalls().size)
                    getResourceCalls().first().apply {
                        assert(!template!!.independent || dbActions.isNotEmpty()){
                            "the first call with $method should not be independent, but ${template!!.template} with ${dbActions.size} dbActions"
                        }
                    }

                }
                ResourceSamplingMethod.S2dR->{
                    assertEquals(2, this!!.getResourceCalls().size)
                    getResourceCalls().first().apply {
                        assert(!template!!.independent || dbActions.isNotEmpty()){
                            "the first call with $method should not be independent, but ${template!!.template} with ${dbActions.size} dbActions"
                        }
                    }

                }
                ResourceSamplingMethod.SMdR->{
                    assert(2 <= this!!.getResourceCalls().size)
                    getResourceCalls().first().apply {
                        assert(!template!!.independent || dbActions.isNotEmpty()){
                            "the first call with $method should not be independent, but ${template!!.template} with ${dbActions.size} dbActions"
                        }
                    }
                }
            }
        }
    }

    fun testResourceRelatedToTable(resource: String, expectedRelatedTable : List<String>){
        assert(rm.getTableInfo().isNotEmpty())

        rm.getResourceCluster().getValue(resource).getDerivedTables().apply {
            assert(isNotEmpty()){
                "derived tables of resource $resource should not be empty"
            }
            assert(expectedRelatedTable.all { this.any { a->a.equals(it, ignoreCase = true) } }){
                "expected related tables are ${expectedRelatedTable.joinToString(",")}; but actual related tables are ${this.joinToString(",")}."
            }
        }
    }

    private fun testBindingGenesBetweenRestActionAndDbAction(
            paramName : String,
            resourceCalls : RestResourceCalls,
            tableName : String,
            colName : String
    ) : Boolean{

        if(resourceCalls.dbActions.isEmpty()) return false
        if(!resourceCalls.dbActions.any { it.table.name.equals(tableName, ignoreCase = true) }) return false

        val dbGene = resourceCalls.dbActions.find { it.table.name.equals(tableName, ignoreCase = true) }!!.seeGenes().find { it.name.equals(colName, ignoreCase = true) }?: return false

        return resourceCalls.actions.filterIsInstance<RestCallAction>().flatMap { it.parameters.filter { it.name == paramName } }.all { p->
            ParamUtil.compareGenesWithValue(ParamUtil.getValueGene(dbGene!!), ParamUtil.getValueGene(p.gene))
        }
    }

    /**
     * based on inference, relationship between rest action and related tables cannot be determined, so an rest action may bind with different tables.
     * @param tables key is table name and value is a set of column
     */
    fun testBindingGenesBetweenRestActionAndDbAction(resource: String, paramName: String, tables : Map<String, Set<String>>){
        assert(rm.getTableInfo().isNotEmpty())

        val resourceNode = rm.getResourceCluster().getValue(resource)

        assert(resourceNode.resourceToTable.derivedMap.isNotEmpty())

        assert(resourceNode.resourceToTable.derivedMap.any { tables.any { t-> it.key.equals(t.key, ignoreCase = true) } })

        val resourceCalls = mutableListOf<RestResourceCalls>()
        rm.sampleCall(resourceNode.getName(), true, resourceCalls, config.maxTestSize, true)

        assertEquals(1, resourceCalls.size)
        assert(resourceCalls.first().dbActions.isNotEmpty())

        val first = resourceCalls.first()

        assert(tables.any {
            it.value.any { c->
                testBindingGenesBetweenRestActionAndDbAction(paramName, first, it.key, c)
            }
        })
    }

    fun testBindingSimpleGenesAmongRestActions(
            resource: String,
            template: String,
            paramName : String
    ) {
        val resourceNode = rm.getResourceCluster().getValue(resource)

        val call = resourceNode.genCalls(template, randomness, config.maxTestSize, true, true)

        call.apply {
            val paramsRequiredToBind = actions.filterIsInstance<RestCallAction>()
                    .flatMap { it.parameters.filter { it.name == paramName }}
            assert(paramsRequiredToBind.size > 1)
            val base = ParamUtil.getValueGene(paramsRequiredToBind.first().gene)
            (1 until paramsRequiredToBind.size).forEach { g->
                ParamUtil.compareGenesWithValue(base, ParamUtil.getValueGene(paramsRequiredToBind[g].gene))
            }
        }
    }

    fun testDependencyAmongResources(resource: String,  expectedRelatedResources: List<String>){

        assert(rm.getTableInfo().isNotEmpty())
        assert(dm.isDependencyNotEmpty())

        val resourceNode = rm.getResourceCluster().getValue(resource)

        assert(resourceNode.resourceToTable.derivedMap.isNotEmpty())

        dm.getRelatedResource(resource).apply {
            assert(isNotEmpty())
            assert(expectedRelatedResources.all { contains(it)}){
                "expected resources (${expectedRelatedResources.filter { !contains(it) }.joinToString(",")}) are not included."
            }
        }
    }

    override fun testS2dRWithDependency(){

        TestUtils.handleFlaky {
            val related = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.S2dR, true)

            assertNotNull(related)
            related!!.getResourceCalls().apply {
                assertEquals(2, size)
                val first = first()
                val second = last()
                assert(dm.getRelatedResource(first.getResourceNodeKey()).contains(second.getResourceNodeKey()))
            }
        }
    }

    override fun testResourceIndividualWithSampleMethods() {
        TestUtils.handleFlaky {
            testAllApplicableMethods()
        }
    }

    override fun setupWithoutDatabaseAndDependency() {
        preSteps()
    }

    override fun setupWithDatabaseAndDependencyAndNameAnalysis() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true, probOfDep = 0.9)
    }

    override fun setupWithDatabaseAndNameAnalysis() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
    }

    override fun testResourceStructureMutator() {
        val individual = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.S1dR, false)
        assertNotNull(individual)
        assert(individual!!.getResourceCalls().size == 1)
        val addSpec = MutatedGeneSpecification()
        structureMutator.mutateRestResourceCalls(individual, RestResourceStructureMutator.MutationType.ADD, addSpec)
        assert(addSpec.mutatedPosition.size == 1)
        assert(addSpec.addedGenes.isNotEmpty() || individual.getResourceCalls()[addSpec.mutatedPosition.first()].actions.flatMap { it.seeGenes() }.isEmpty())
        assert(individual.getResourceCalls().size == 2)

        val first = individual.getResourceCalls()[0].getResourceNode()
        val second = individual.getResourceCalls()[1].getResourceNode()
        val swapSpec = MutatedGeneSpecification()
        structureMutator.mutateRestResourceCalls(individual, RestResourceStructureMutator.MutationType.SWAP, swapSpec)
        assert(swapSpec.addedGenes.isEmpty())
        assert(swapSpec.removedGene.isEmpty())
        assert(swapSpec.mutatedPosition.size == 2)

        assert(individual.getResourceCalls()[1].getResourceNode().getName() == first.getName())
        assert(individual.getResourceCalls()[0].getResourceNode().getName() == second.getName())

        val previousIndividual = individual.copy() as RestIndividual
        val delSpec = MutatedGeneSpecification()
        structureMutator.mutateRestResourceCalls(individual, RestResourceStructureMutator.MutationType.DELETE, delSpec)
        assert(delSpec.mutatedPosition.size == 1)
        assert(delSpec.removedGene.isNotEmpty() || previousIndividual.getResourceCalls()[delSpec.mutatedPosition.first()].actions.flatMap { it.seeGenes() }.isEmpty())
        assert(individual.getResourceCalls().size == 1)

        val current = individual.getResourceCalls()[0].getResourceNode()
        val replaceSpec = MutatedGeneSpecification()
        structureMutator.mutateRestResourceCalls(individual, RestResourceStructureMutator.MutationType.REPLACE, replaceSpec)
        val replaced = individual.getResourceCalls()[0]
        assert(replaceSpec.removedGene.isNotEmpty() || current.actions.flatMap { it.seeGenes() }.isEmpty())
        assert(replaceSpec.addedGenes.isNotEmpty() || replaced.actions.flatMap { it.seeGenes() }.isEmpty())
        assert(replaceSpec.mutatedPosition.size == 1)
        assert(current.getName() != replaced.getResourceNode().getName())

        if (replaced.getResourceNode().numOfTemplates() > 1){
            val modifySpec = MutatedGeneSpecification()
            structureMutator.mutateRestResourceCalls(individual, RestResourceStructureMutator.MutationType.MODIFY, modifySpec)
            val modified = individual.getResourceCalls()[0]
            assert(modifySpec.removedGene.isNotEmpty() || replaced.actions.flatMap { it.seeGenes() }.isEmpty())
            assert(modifySpec.addedGenes.isNotEmpty() || modified.actions.flatMap { it.seeGenes() }.isEmpty())
            assert(modifySpec.mutatedPosition.size == 1)
            assert(modified.getResourceNode().getName() == replaced.getResourceNode().getName())
            assertNotNull(modified.template)
            assertNotNull(replaced.template)
            assert(modified.template.toString() != replaced.template.toString())
        }
    }

    fun testResourceStructureMutatorWithDependencyWithSimulatedDependency(resourceA: String, resourceB:String, resourceC:String) {
        simulateDerivationOfDependencyRegardingFitness(resourceA, resourceB, resourceC);
        testResourceStructureMutatorWithDependencyWithSpecified(resourceA, resourceC)
    }

    fun testResourceStructureMutatorWithDependencyWithSpecified(resource: String, expectedRelated : String?){
        val callA = rm.getResourceNodeFromCluster(resource).run {
            genCalls(randomness.choose(getTemplates().values).template, randomness, config.maxTestSize)
        }
        val ind = RestIndividual(mutableListOf(callA), SampleType.SMART_RESOURCE)

        TestUtils.handleFlaky {
            structureMutator.mutateRestResourceCalls(ind, RestResourceStructureMutator.MutationType.ADD)
            if (expectedRelated != null){
                assert(ind.getResourceCalls().any {
                    c-> c.getResourceNode().getName() == expectedRelated
                })
            }else{
                assert(ind.getResourceCalls().any {
                    c -> dm.getRelatedResource(resource).contains(c.getResourceNode().getName())
                })
            }
        }
    }

    /**
     * this is used to simulate derivation of dependency based on fitness.
     * precondition: [resourceA] does not depend on [resourceC]
     * Assemble an evaluated individual with an individual (composed of [resourceB] and [resourceA]) and a fitness value, i.e., F1
     * Assemble other evaluated individual with an individual ( composed of [resourceC] and [resourceA]) and a fitness value that is better than F1
     * check whether the relationship (i.e., [resourceA] depends on [resourceC]) is detected
     */
    fun simulateDerivationOfDependencyRegardingFitness(resourceA: String, resourceB:String, resourceC:String) {
        assert(!dm.getRelatedResource(resourceC).contains(resourceA))
        val callA = rm.getResourceNodeFromCluster(resourceA).run {
            genCalls(randomness.choose(getTemplates().values).template, randomness, config.maxTestSize)
        }

        val targetsOfA = callA.actions.mapIndexed { index, _ -> index + 1}

        val callB = rm.getResourceNodeFromCluster(resourceB).run {
            genCalls(randomness.choose(getTemplates().values).template, randomness, config.maxTestSize)
        }

        val targetsOfB = callB.actions.mapIndexed { index, _ -> targetsOfA.last() + 1 + index }

        val callC = rm.getResourceNodeFromCluster(resourceC).run {
            genCalls(randomness.choose(getTemplates().values).template, randomness, config.maxTestSize)
        }

        val targetsOfC = callC.actions.mapIndexed { index, _ -> targetsOfB.last() + 1 + index  }

        val ind1With2Resources = RestIndividual(mutableListOf(callB, callA), SampleType.SMART_RESOURCE)

        val fake1fitnessValue = FitnessValue(ind1With2Resources!!.seeActions().size.toDouble())
        targetsOfB.plus(targetsOfA).forEachIndexed { index, i ->
            fake1fitnessValue.updateTarget(i, 0.2, index)
        }
        val fakeEvalInd1 = EvaluatedIndividual(fake1fitnessValue, ind1With2Resources, mutableListOf())

        val ind2With2Resources = RestIndividual(mutableListOf(callC, callA), SampleType.SMART_RESOURCE)
        val fake2fitnessValue = FitnessValue(ind2With2Resources!!.seeActions().size.toDouble())
        targetsOfC.plus(targetsOfA).forEachIndexed { index, i ->
            fake2fitnessValue.updateTarget(i, 0.3, index)
        }
        val fakeEvalInd2 = EvaluatedIndividual(fake2fitnessValue, ind2With2Resources, mutableListOf())

        dm.detectDependencyAfterStructureMutation(fakeEvalInd1, fakeEvalInd2, 1)
        assert(dm.getRelatedResource(resourceA).contains(resourceC))
    }

}

