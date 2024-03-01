package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.tracer.TrackOperator

/**
 * Changing the structure of a test case will heavily depend
 * on the type of addressed problem.
 * And to generate new action, that as well will depend on the
 * addressed problem, and can't really be abstracted away
 */
abstract class StructureMutator : TrackOperator {

    @Inject
    protected lateinit var config : EMConfig

    @Inject
    protected lateinit var randomness : Randomness

    @Inject
    protected lateinit var time: SearchTimeController

    @Inject
    protected lateinit var apc: AdaptiveParameterControl

    /**
     * For example, add new actions, or remove old ones
     *
     * @param individual is the candidate to be mutated
     * @param evaluatedIndividual contains additional info about the candidate [individual]
     * @param mutatedGenes is used to specify what genes are mutated with this mutation
     * @param targets indicates what targets to be optimized with this mutation
     */
    abstract fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>)


    /**
     *
     * @param individual is the candidate individual whose initialization would be  mutated
     * @param evaluatedIndividual contains additional info about the candidate [individual]
     * @param mutatedGenes is used to specify what genes are mutated with this mutation
     * @param targets indicates what targets to be optimized with this mutation
     */
    abstract fun mutateInitStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>)

    fun getMaxSizeOfMutatingInitAction(): Int{
        Lazy.assert { config.maxSizeOfMutatingInitAction > 0 }
        return apc.getExploratoryValue(config.maxSizeOfMutatingInitAction, 1)
    }

    /**
     * Before the main "actions" (e.g, HTTP calls for web services and
     * clicks on browsers), there can be a series of initializing actions
     * to control the environment of the SUT, like for example setting
     * up data in a SQL database.
     * What to setup is often depending on what is executed by the test.
     * But once such init actions are added, the behavior of the test
     * might change.
     */
    abstract fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?)

    /**
     * Before each main "action" (e.g, HTTP calls for web services and
     * clicks on browsers), there can be a series of external actions
     * to mock responses of external services regarding that main action
     *
     * @return whether any harvest response is added
     */
    abstract fun addAndHarvestExternalServiceActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) : Boolean

    fun canApplyStructureMutator(individual: Individual) : Boolean = canApplyInitStructureMutator() || canApplyActionStructureMutator(individual)

    /**
     * @return whether the init structure mutator is applicable.
     * For instance, regarding rest, the mutator is not applicable if there is no db.
     */
    open fun canApplyInitStructureMutator() : Boolean = false

    /**
     * @return whether the action structure mutator is applicable.
     */
    open fun canApplyActionStructureMutator(individual: Individual) : Boolean = individual.canMutateStructure()

}