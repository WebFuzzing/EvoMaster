package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
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

    abstract fun mutateStructure(individual: Individual)

}