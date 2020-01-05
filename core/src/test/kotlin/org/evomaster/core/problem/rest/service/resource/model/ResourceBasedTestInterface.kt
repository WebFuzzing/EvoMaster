package org.evomaster.core.problem.rest.service.resource.model

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

interface ResourceBasedTestInterface {

    /**
     * it is mandatory to test whether resource cluster is initialized correctly
     */
    fun testInitializedNumOfResourceCluster()

    /**
     * it is mandatory to test whether templates are initialized correctly
     */
    fun testInitializedTemplatesForResources()

    /**
     * it is mandatory to test whether applicable resource-based sampling methods are initialized correctly
     */
    fun testApplicableMethods()

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
     * it is mandatory to test whether an individual is sampled correctly using resource-based sampling method
     */
    fun testResourceIndividualWithSampleMethods()

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

    /**
     * it is mandatory to test Resource Rest Structure
     */
    fun testResourceStructureMutator()

    /**
     * it is mandatory to test Resource Rest Structure with dependency
     */
    fun testResourceStructureMutatorWithDependency()

    /**
     * it is mandatory to test derivation of dependency with fitness
     */
    fun testDerivationOfDependency()

    /*************** integrated tests regarding a setting *************************/

    fun setupWithoutDatabaseAndDependency()

    @Test //FIXME move to core-it
    @Disabled("Started to fail since update to OpenApi V3")
    fun testInitializedResourceClusterAndApplicableSampleMethods(){
        setupWithoutDatabaseAndDependency()

        testInitializedNumOfResourceCluster()
        testInitializedTemplatesForResources()
        testApplicableMethods()
        testResourceIndividualWithSampleMethods()
        testBindingValuesAmongRestActions()

        testResourceStructureMutator()
    }

    fun setupWithDatabaseAndNameAnalysis()

    //FIXME move to core-it @Test
    fun testWithDatabaseAndNameAnalysis(){
        setupWithDatabaseAndNameAnalysis()

        testResourceRelatedToTable()
        testBindingValueBetweenRestActionAndDbAction()
    }

    fun setupWithDatabaseAndDependencyAndNameAnalysis()

    //FIXME move to core-it @Test
    fun testWithDependencyAndNameAnalysis(){
        setupWithDatabaseAndDependencyAndNameAnalysis()

        testDependencyAmongResources()
        testS2dRWithDependency()

        //testResourceStructureMutatorWithDependency()
        testDerivationOfDependency()
    }
}