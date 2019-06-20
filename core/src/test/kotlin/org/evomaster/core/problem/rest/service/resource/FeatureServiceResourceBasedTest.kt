package org.evomaster.core.problem.rest.service.resource

import org.junit.jupiter.api.Test

class FeatureServiceResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/features_service.sql"
    override fun getSwaggerLocation() = "/swagger/features_service.json"

    @Test
    override fun testResourceCluster() {
        preSteps()
        preCheck()

        testSizeResourceCluster(11)

        testSpecificResourceNode("/products", 1, listOf("GET"), true)

        /*
         In "/products/{productName}/features", there only exists one GET action.
         because its ancestor has POST action, the resource is able to sample an resource call with
         the template "POST-GET".
         */
        testSpecificResourceNode("/products/{productName}/features", 2, listOf("GET", "POST-GET"), false)

        testSpecificResourceNode("/products/{productName}", 6, listOf(), false)
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
        preSteps()
        preCheck()

        testBindingSimpleGenesAmongRestActions("/products/{productName}/configurations/{configurationName}/features/{featureName}", "POST-DELETE", "featureName")
    }

    @Test
    override fun testResourceRelatedToTable() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()
        testResourceRelatedToTable("/products/{productName}", listOf("product"))
    }


    @Test
    override fun testBindingValueBetweenRestActionAndDbAction() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()

        testBindingGenesBetweenRestActionAndDbAction("/products/{productName}", "productName", "product", "name")

    }

    @Test
    override fun testDependencyAmongResources() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true, probOfDep = 0.5)
        preCheck()

        testDependencyAmongResources("/products/{productName}/features", listOf("/products/{productName}/configurations/{configurationName}/features/{featureName}"))
    }

}