package org.evomaster.core.problem.rest.service.resource

import com.google.inject.Module
import com.netflix.governator.lifecycle.LifecycleManager
import com.netflix.governator.guice.LifecycleInjector
import com.sun.org.apache.regexp.internal.RE
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.database.extract.h2.ExtractTestBaseH2
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.service.ResourceDepManageService
import org.evomaster.core.problem.rest.service.ResourceManageService
import org.evomaster.core.problem.rest.service.ResourceSampleMethodController
import org.evomaster.core.problem.rest.service.ResourceSamplingMethod
import org.evomaster.core.problem.rest.service.resource.model.ResourceBasedTestInterface
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceModule
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceSampler
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import org.jetbrains.kotlin.psi2ir.generators.BodyGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

abstract class ResourceTestBase : ExtractTestBaseH2(), ResourceBasedTestInterface {

    private lateinit var config: EMConfig
    private lateinit var sampler: SimpleResourceSampler
    private lateinit var rm: ResourceManageService
    private lateinit var dm: ResourceDepManageService
    private lateinit var ssc : ResourceSampleMethodController
    private lateinit var lifecycleManager : LifecycleManager
    private lateinit var randomness: Randomness
    private var doesSamplerInitialed : Boolean = false

    @BeforeEach
    fun init() {
        assert(!doesSamplerInitialed)

        val injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(SimpleResourceModule(), BaseModule()))
                .build().createInjector()

        lifecycleManager = injector.getInstance(LifecycleManager::class.java)
        lifecycleManager.start()

        sampler = injector.getInstance(SimpleResourceSampler::class.java)
        config = injector.getInstance(EMConfig::class.java)
        rm = injector.getInstance(ResourceManageService::class.java)
        dm = injector.getInstance(ResourceDepManageService::class.java)
        ssc = injector.getInstance(ResourceSampleMethodController::class.java)
        randomness = injector.getInstance(Randomness::class.java)

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

    }

    @AfterEach
    fun close(){
        lifecycleManager.close()
        doesSamplerInitialed = false
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

    fun preSteps(skip : List<String> = listOf(), doesInvolveDatabase : Boolean = false, doesAppleNameMatching : Boolean = false, probOfDep : Double = 0.0){
        config.doesInvolveDatabase = doesInvolveDatabase
        config.doesApplyNameMatching = doesAppleNameMatching

        config.probOfEnablingResourceDependencyHeuristics = probOfDep

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), skip, sqlBuilder)

        doesSamplerInitialed = true
    }

    fun preCheck(){
        assert(doesSamplerInitialed)
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


    fun testAllApplicableMethods(){
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
                    assert(getResourceCalls().first().template!!.independent)
                }
                ResourceSamplingMethod.S1dR->{
                    assertEquals(1, this!!.getResourceCalls().size)
                    assert(!getResourceCalls().first().template!!.independent)
                }
                ResourceSamplingMethod.S2dR->{
                    assertEquals(2, this!!.getResourceCalls().size)
                    assert(!getResourceCalls().first().template!!.independent)
                }
                ResourceSamplingMethod.SMdR->{
                    assert(3 <= this!!.getResourceCalls().size)
                    assert(!getResourceCalls().first().template!!.independent)
                }
            }
        }

    }

    fun testResourceRelatedToTable(resource: String, expectedRelatedTable : List<String>){

        assert(rm.getTableInfo().isNotEmpty())

        rm.getResourceCluster().getValue(resource).getDerivedTables().apply {
            assert(isNotEmpty())
            assert(expectedRelatedTable.all { this.any { a->a.equals(it, ignoreCase = true) } })
        }


    }

    fun testBindingGenesBetweenRestActionAndDbAction(
            resource: String,
            paramName : String,
            tableName : String,
            colName : String
    ) {

        assert(rm.getTableInfo().isNotEmpty())

        val resourceNode = rm.getResourceCluster().getValue(resource)

        assert(resourceNode.resourceToTable.derivedMap.isNotEmpty())

        assert(resourceNode.resourceToTable.derivedMap.any { it.key.equals(tableName, ignoreCase = true) })

        val resourceCalls = mutableListOf<RestResourceCalls>()
        rm.sampleCall(resourceNode.getName(), true, resourceCalls, config.maxTestSize, true)

        assertEquals(1, resourceCalls.size)
        assert(resourceCalls.first().dbActions.isNotEmpty())

        resourceCalls.first().apply {
            assert(dbActions.isNotEmpty())
            assert(dbActions.any { it.table.name.equals(tableName, ignoreCase = true) })
            val dbGene = dbActions.find { it.table.name.equals(tableName, ignoreCase = true) }!!.seeGenes().find { it.name.equals(colName, ignoreCase = true) }
            assertNotNull(dbGene)
            actions.filterIsInstance<RestCallAction>().flatMap { it.parameters.filter { it.name == paramName } }.forEach { p->
                val gene = ParamUtil.getValueGene(p.gene)
                assert(ParamUtil.compareGenesWithValue(ParamUtil.getValueGene(dbGene!!), gene))
            }
        }
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
            assert(expectedRelatedResources.all { contains(it)})
        }
    }

    @Test
    override fun testS2dRWithDependency(){

        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true, probOfDep = 0.5)
        preCheck()

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

