package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual

/**
 * Changing the structure of a test case will heavily depend
 * on the type of addressed problem.
 * And to generate new action, that as well will depend on the
 * addressed problem, and can't really be abstracted away
 */
abstract class StructureMutator {

    @Inject
    protected lateinit var config : EMConfig

    @Inject
    protected lateinit var randomness : Randomness

    /**
     * For example, add new actions, or remove old ones
     */
    abstract fun mutateStructure(individual: Individual)


    /**
     * Before the main "actions" (e.g, HTTP calls for web services and
     * clicks on browsers), there can be a series of initializing actions
     * to control the environment of the SUT, like for example setting
     * up data in a SQL database.
     * What to setup is often depending on what is executed by the test.
     * But once such init actions are added, the behavior of the test
     * might change.
     */
    abstract fun addInitializingActions(individual: EvaluatedIndividual<*>)
}