package org.evomaster.core.problem.rest.service.resource

/**
 * check
 * https://trello.com/c/txcQZoRI/200-issue-with-proxyprintresourcebasedtest
 */
class ProxyPrintResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/proxyprint.sql"
    override fun getSwaggerLocation() = "/swagger/sut/proxyprint.json"

    override fun testInitializedNumOfResourceCluster() {
        testSizeResourceCluster(79)
    }

    override fun testInitializedTemplatesForResources() {
        testSpecificResourceNode("/admin/printshops", 1, listOf("GET"), true)

        testSpecificResourceNode("/consumer/{consumerID}/printingschemas/{printingSchemaID}", 4, listOf(), false)

        testSpecificResourceNode("/consumer/subscribe", 12, listOf(), false)
    }

    override fun testApplicableMethods() {
        testInitialedApplicableMethods(4)
    }

    override fun testBindingValuesAmongRestActions() {
        testBindingSimpleGenesAmongRestActions("/consumer/{consumerID}/printingschemas/{printingSchemaID}", "POST-DELETE", "consumerID")
    }

    override fun testResourceRelatedToTable() {
        testResourceRelatedToTable("/consumer/{consumerID}/printingschemas", listOf("consumers", "printing_schemas"))
        testResourceRelatedToTable("/request/register", listOf("printshops", "register_requests"))

    }

    override fun testBindingValueBetweenRestActionAndDbAction() {
        testBindingGenesBetweenRestActionAndDbAction("/consumer/{consumerID}/printingschemas", "consumerID", mapOf("consumers" to  setOf("id")))
    }

    override fun testDependencyAmongResources() {
        testDependencyAmongResources("/request/register", listOf("/printshops/{id}/reviews"))
    }

    override fun testDerivationOfDependency() {
        simulateDerivationOfDependencyRegardingFitness("/env","/admin/register","/autoconfig")
    }

    override fun testResourceStructureMutatorWithDependency() {
        testResourceStructureMutatorWithDependencyWithSimulatedDependency("/env","/admin/register","/autoconfig")
    }

}