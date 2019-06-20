package org.evomaster.core.problem.rest.service.resource

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.EMConfig
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.service.ResourceSamplingMethod
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CatwatchResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/catwatch.sql"
    override fun getSwaggerLocation() = "/swagger/catwatch.json"

    @Test
    fun testResourceCluster(){

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf(), sqlBuilder)

        assertEquals(17, rm.getResourceCluster().size)

        val independentResource = rm.getResourceCluster().getValue("/config")
        independentResource.apply {
            assertEquals(1, getTemplates().size)
            assert(isIndependent())
        }

        val nonIndependentResource = rm.getResourceCluster().getValue("/error")
        nonIndependentResource.apply {
            assertEquals(12, getTemplates().size)
            assert(!isIndependent())
        }
    }

    @Test
    fun testApplicableMethodsForNoneDependentResource(){

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf("/error", "/config/scoring.project","/import"), sqlBuilder)

        //only S1iR is applicable
        ssc.getApplicableMethods().apply {
            assertEquals(1, size)
            assert(this.contains(ResourceSamplingMethod.S1iR))
        }
    }

    @Test
    fun testApplicableMethodsForOneDependentResource(){

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf("/error", "/config/scoring.project"), sqlBuilder)

        ssc.getApplicableMethods().apply {
            assertEquals(2, size)
            assert(this.contains(ResourceSamplingMethod.S1iR))
            assert(this.contains(ResourceSamplingMethod.S1dR))
        }
    }


    @Test
    fun testApplicableMethodsForTwoDependentResource(){

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf("/error"), sqlBuilder)

        ssc.getApplicableMethods().apply {
            assertEquals(3, size)
            assert(this.contains(ResourceSamplingMethod.S1iR))
            assert(this.contains(ResourceSamplingMethod.S1dR))
            assert(this.contains(ResourceSamplingMethod.S2dR))
        }

    }

    @Test
    fun testApplicableMethodsForMoreThanTwoDependentResource(){

        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf(), sqlBuilder)

        //only S1iR is applicable
        assertEquals(4, ssc.getApplicableMethods().size)
    }


    @Test
    fun testSampleMethod(){
        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf(), sqlBuilder)

        assertEquals(4, ssc.getApplicableMethods().size)

        val indByS1iR = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.S1iR, false)
        indByS1iR.apply {
            assertNotNull(this)
            assertEquals(1, this!!.getResourceCalls().size)
            assert(getResourceCalls().first().template!!.independent)
        }

        val indByS1dR = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.S1dR, false)
        indByS1dR.apply {
            assertNotNull(this)
            assertEquals(1, this!!.getResourceCalls().size)
            assert(!getResourceCalls().first().template!!.independent)
        }

        val indByS2dR = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.S2dR, false)
        indByS2dR.apply {
            assertNotNull(this)
            assertEquals(2, this!!.getResourceCalls().size)
            assert(!getResourceCalls().first().template!!.independent)
        }

        val indBySMdR = sampler.sampleWithMethodAndDependencyOption(ResourceSamplingMethod.SMdR, false)
        indBySMdR.apply {
            assertNotNull(this)
            assert(3 <= this!!.getResourceCalls().size)
            assert(!getResourceCalls().first().template!!.independent)
        }
    }

    @Test
    fun testBindingValuesAmongRestActions(){
        //there does not exist the case for binding values among rest actions
    }

    @Test
    fun testBindingValueBetweenRestActionAndDbAction(){
        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability
        config.doesInvolveDatabase = true
        config.doesApplyNameMatching = true

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf(), sqlBuilder)

        assert(rm.getTableInfo().isNotEmpty())

        val resourceNode = rm.getResourceCluster().getValue("/statistics")

        assert(resourceNode.resourceToTable.derivedMap.isNotEmpty())

        val tableName = "statistics"

        assert(resourceNode.resourceToTable.derivedMap.any { it.key.equals(tableName, ignoreCase = true) })

        val resourceCalls = mutableListOf<RestResourceCalls>()
        rm.sampleCall(resourceNode.getName(), true, resourceCalls, config.maxTestSize, true)

        assertEquals(1, resourceCalls.size)
        assert(resourceCalls.first().dbActions.isNotEmpty())

        val paramName = "organizations"
        val colName = "organization_name"

        resourceCalls.first().apply {
            assert(dbActions.isNotEmpty())
            assert(dbActions.any { it.table.name.equals(tableName, ignoreCase = true) })
            val dbGene = dbActions.find { it.table.name.equals(tableName, ignoreCase = true) }!!.seeGenes().find { it.name.equals(colName, ignoreCase = true) }
            assert(dbGene != null)
            assert(dbGene is StringGene)
            actions.filterIsInstance<RestCallAction>().flatMap { it.parameters.filter { it.name == paramName } }.forEach { p->
                val gene = ParamUtil.getValueGene(p.gene)
                assert(gene is StringGene)
                assert(gene.getValueAsRawString().equals(dbGene!!.getValueAsRawString(), ignoreCase = true))
            }
        }
    }

    @Test
    fun testInvolveDependencyWithDatabaseAndTextAnalysis(){
        config.resourceSampleStrategy = EMConfig.ResourceSamplingStrategy.EqualProbability
        config.doesInvolveDatabase = true
        config.doesApplyNameMatching = true

        config.probOfEnablingResourceDependencyHeuristics = 0.5

        val schemaDto = SchemaExtractor.extract(connection)
        val sqlBuilder = SqlInsertBuilder(schemaDto, getDatabaseExecutor())

        sampler.initialize(getSwaggerLocation(), listOf(), sqlBuilder)

        assert(rm.getTableInfo().isNotEmpty())
        assert(dm.isDependencyNotEmpty())

        val resource = "/statistics"
        val resourceNode = rm.getResourceCluster().getValue(resource)

        assert(resourceNode.resourceToTable.derivedMap.isNotEmpty())

        dm.getRelatedResource(resource).apply {
            assert(isNotEmpty())
            assert(contains("/statistics/contributors"))
        }

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