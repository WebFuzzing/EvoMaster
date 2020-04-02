package org.evomaster.core.problem.rest.service.resource

class CatwatchResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/catwatch.sql"
    override fun getSwaggerLocation() = "/swagger/sut/catwatch.json"

    override fun testInitializedNumOfResourceCluster() {
        testSizeResourceCluster(17)
    }

    override fun testInitializedTemplatesForResources() {
        testSpecificResourceNode("/config", 1, listOf("GET"), true)
        testSpecificResourceNode("/error", 12, listOf(), false)
    }

    override fun testApplicableMethods() {
        testInitialedApplicableMethods(4)
    }

    override fun testBindingValuesAmongRestActions() {
        //there does not exist the case for binding values among rest actions
    }

    override fun testResourceRelatedToTable() {
        testResourceRelatedToTable("/statistics", listOf("statistics"))
        testResourceRelatedToTable("/statistics/contributors", listOf("contributor"))
    }

    override fun testBindingValueBetweenRestActionAndDbAction() {
        testBindingGenesBetweenRestActionAndDbAction("/statistics", "organizations", mapOf("statistics" to setOf("organization_name")))
    }

    override fun testDependencyAmongResources() {
        testDependencyAmongResources("/statistics",listOf("/statistics/contributors"))
    }

    override fun testDerivationOfDependency() {
        simulateDerivationOfDependencyRegardingFitness("/statistics","/delete","/export")
    }

    override fun testResourceStructureMutatorWithDependency() {
        testResourceStructureMutatorWithDependencyWithSimulatedDependency("/statistics","/delete","/export")
    }

}