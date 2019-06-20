package org.evomaster.core.problem.rest.service.resource

import org.evomaster.core.problem.rest.service.ResourceSamplingMethod
import org.junit.jupiter.api.Test

class CatwatchResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/catwatch.sql"
    override fun getSwaggerLocation() = "/swagger/catwatch.json"

    @Test
    override fun testResourceCluster() {

        preSteps()
        preCheck()

        testSizeResourceCluster(17)

        testSpecificResourceNode("/config", 1, listOf("GET"), true)
        testSpecificResourceNode("/error", 12, listOf(), false)
    }

    @Test
    override fun testApplicableMethodsForNoneDependentResource() {

        preSteps(skip = listOf("/error", "/config/scoring.project", "/import"))
        preCheck()

        testInitialedApplicableMethods(1, listOf(ResourceSamplingMethod.S1iR))

    }

    @Test
    override fun testApplicableMethodsForOneDependentResource() {

        preSteps(skip = listOf("/error", "/config/scoring.project"))
        preCheck()

        testInitialedApplicableMethods(2, listOf(ResourceSamplingMethod.S1iR, ResourceSamplingMethod.S1dR))
    }

    @Test
    override fun testApplicableMethodsForTwoDependentResource() {

        preSteps(skip = listOf("/error"))
        preCheck()

        testInitialedApplicableMethods(3, listOf(ResourceSamplingMethod.S1iR, ResourceSamplingMethod.S1dR, ResourceSamplingMethod.S2dR))
    }


    @Test
    override fun testApplicableMethodsForMoreThanTwoDependentResource() {

        preSteps()
        preCheck()

        testInitialedApplicableMethods(4)
    }

    @Test
    override fun testSampleMethod() {

        preSteps()
        preCheck()

        testInitialedApplicableMethods(4)
        testAllApplicableMethods()
    }

    @Test
    override fun testBindingValuesAmongRestActions() {
        //there does not exist the case for binding values among rest actions
    }

    @Test
    override fun testResourceRelatedToTable() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()
        testResourceRelatedToTable("/statistics", listOf("statistics"))
    }

    @Test
    override fun testBindingValueBetweenRestActionAndDbAction() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()
        testBindingGenesBetweenRestActionAndDbAction("/statistics", "organizations", "statistics", "organization_name")
    }

    @Test
    override fun testDependencyAmongResources() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true, probOfDep = 0.5)
        preCheck()
        testDependencyAmongResources("/statistics",listOf("/statistics/contributions"))

    }

}