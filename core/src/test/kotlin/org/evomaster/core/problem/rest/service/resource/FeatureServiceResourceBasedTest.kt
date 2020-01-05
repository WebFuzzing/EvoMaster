package org.evomaster.core.problem.rest.service.resource

class FeatureServiceResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/features_service.sql"
    override fun getSwaggerLocation() = "/swagger/sut/features_service.json"

    override fun testInitializedNumOfResourceCluster() {
        testSizeResourceCluster(11)
    }

    override fun testInitializedTemplatesForResources() {
        testSpecificResourceNode("/products", 1, listOf("GET"), true)

        /*
         In "/products/{productName}/features", there only exists one GET action.
         because its ancestor has POST action, the resource is able to sample an resource call with
         the template "POST-GET".
         */
        testSpecificResourceNode("/products/{productName}/features", 2, listOf("GET", "POST-GET"), false)

        testSpecificResourceNode("/products/{productName}", 6, listOf(), false)

    }
    override fun testBindingValuesAmongRestActions() {

        testBindingSimpleGenesAmongRestActions("/products/{productName}/configurations/{configurationName}/features/{featureName}", "POST-DELETE", "featureName")
    }

    override fun testResourceRelatedToTable() {
        testResourceRelatedToTable("/products/{productName}", listOf("product"))
    }


    override fun testBindingValueBetweenRestActionAndDbAction() {
        testBindingGenesBetweenRestActionAndDbAction("/products/{productName}", "productName", mapOf("product" to setOf("name")))
    }

    override fun testDependencyAmongResources() {
        testDependencyAmongResources("/products/{productName}/features", listOf("/products/{productName}/configurations/{configurationName}/features/{featureName}"))
    }

    override fun testApplicableMethods() {
        testInitialedApplicableMethods(4)
    }

    override fun testDerivationOfDependency() {
        // all are related
    }


    override fun testResourceStructureMutatorWithDependency() {
        testResourceStructureMutatorWithDependencyWithSpecified("/products/{productName}/features", null);
    }
}
