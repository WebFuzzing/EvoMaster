package org.evomaster.core.problem.rest.service.resource

class ScoutApiResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/scout-api.sql"
    override fun getSwaggerLocation() = "/swagger/sut/scout-api.json"

    override fun testInitializedNumOfResourceCluster() {
        testSizeResourceCluster(21)
    }

    override fun testInitializedTemplatesForResources() {
        testSpecificResourceNode("/api/v1/system/roles", 1, listOf("GET"), true)

        /*
        POST-GET is applicable because its ancestor has POST
         */
        testSpecificResourceNode("/api/v1/users/profile", 2, listOf("GET", "POST-GET"), false)
        testSpecificResourceNode("/api/v2/activities/{id}/rating", 6, listOf(), false)
    }

    override fun testApplicableMethods() {
        testInitialedApplicableMethods(4)
    }

    override fun testBindingValuesAmongRestActions() {
        testBindingSimpleGenesAmongRestActions("/api/v1/activities/{id}/rating", "POST-DELETE", "id")
    }

    override fun testResourceRelatedToTable() {
        testResourceRelatedToTable("/api/v1/activities/{id}/rating", listOf("activity_rating"))
        testResourceRelatedToTable("/api/v1/users/profile", listOf("users"))
    }

    override fun testBindingValueBetweenRestActionAndDbAction() {
        testBindingGenesBetweenRestActionAndDbAction(
                "/api/v1/activities/{id}/rating",
                "id",
                mapOf("activity_rating" to setOf("activity_id"), "activity" to setOf("id")))
    }

    override fun testDependencyAmongResources() {
        testDependencyAmongResources("/api/v1/favourites", listOf("/api/v1/activities/{id}/rating"))
    }

    override fun testDerivationOfDependency() {
        simulateDerivationOfDependencyRegardingFitness("/api/v1/system/roles","/api/v1/system/ping","/api/v1/favourites")
    }

    override fun testResourceStructureMutatorWithDependency() {
        testResourceStructureMutatorWithDependencyWithSimulatedDependency("/api/v1/system/roles","/api/v1/system/ping","/api/v1/favourites")
    }

}