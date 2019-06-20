package org.evomaster.core.problem.rest.service.resource.model

interface ResourceBasedTestInterface {

    /**
     * it is mandatory to test whether resource cluster is initialized correctly
     */
    fun testResourceCluster()

    /**
     * it is mandatory to test whether an individual is sampled correctly using resource-based sampling method
     */
    fun testSampleMethod()

    /**
     * it is not always required to test whether applicable methods is initialized correctly with specific setting
     */
    fun testApplicableMethodsForNoneDependentResource() {}

    /**
     * it is not always required to test whether applicable methods is initialized correctly with specific setting
     */
    fun testApplicableMethodsForOneDependentResource() {}

    /**
     * it is not always required to test whether applicable methods is initialized correctly with specific setting
     */
    fun testApplicableMethodsForTwoDependentResource() {}

    /**
     * it is not always required to test whether applicable methods is initialized correctly with specific setting
     */
    fun testApplicableMethodsForMoreThanTwoDependentResource() {}

    /**
     * it is mandatory to test the binding functionality among rest actions
     */
    fun testBindingValuesAmongRestActions()

    /**
     * it is mandatory to test relationship between resources and tables
     */
    fun testResourceRelatedToTable()

    /**
     * it is mandatory to test binding functionality between rest action and db action
     */
    fun testBindingValueBetweenRestActionAndDbAction()

    /**
     * it is mandatory to test whether the dependency is initialized properly
     */
    fun testDependencyAmongResources()

    /**
     * it is mandatory to test S2dR sampling method with a consideration of dependency
     */
    fun testS2dRWithDependency()

}