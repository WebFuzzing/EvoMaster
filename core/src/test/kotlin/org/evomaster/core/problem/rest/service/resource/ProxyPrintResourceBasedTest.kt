package org.evomaster.core.problem.rest.service.resource

import org.junit.jupiter.api.Test

class ProxyPrintResourceBasedTest : ResourceTestBase() {

    override fun getSchemaLocation() = "/sql_schema/proxyprint.sql"
    override fun getSwaggerLocation() = "/swagger/proxyprint.json"

    @Test
    override fun testResourceCluster() {
        preSteps()
        preCheck()

        testSizeResourceCluster(79)

        testSpecificResourceNode("/admin/printshops", 1, listOf("GET"), true)

        testSpecificResourceNode("/consumer/{consumerID}/printingschemas/{printingSchemaID}", 4, listOf(), false)

        testSpecificResourceNode("/consumer/subscribe", 12, listOf(), false)
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

        testBindingSimpleGenesAmongRestActions("/consumer/{consumerID}/printingschemas/{printingSchemaID}", "POST-DELETE", "consumerID")
    }

    @Test
    override fun testResourceRelatedToTable() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()
        testResourceRelatedToTable("/consumer/{consumerID}/printingschemas", listOf("consumers", "printing_schemas"))
        testResourceRelatedToTable("/request/register", listOf("printshops", "register_requests"))

    }

    @Test
    override fun testBindingValueBetweenRestActionAndDbAction() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true)
        preCheck()

        testBindingGenesBetweenRestActionAndDbAction("/consumer/{consumerID}/printingschemas", "consumerID", "consumers", "id")

    }

    @Test
    override fun testDependencyAmongResources() {
        preSteps(doesInvolveDatabase = true, doesAppleNameMatching = true, probOfDep = 0.5)
        preCheck()

        testDependencyAmongResources("/request/register", listOf("/printshops/{id}/reviews"))
    }

}