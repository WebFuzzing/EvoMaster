package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.tracer.TrackOperator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


abstract class Sampler<T> : TrackOperator where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Sampler::class.java)
    }

    @Inject
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var config: EMConfig

    @Inject
    protected lateinit var time : SearchTimeController

    @Inject
    protected lateinit var apc: AdaptiveParameterControl

    @Inject
    protected lateinit var searchGlobalState: SearchGlobalState

    /**
     * Set of available actions that can be used to define a test case
     *
     * Key -> action name
     *
     * Value -> an action
     */
    protected val actionCluster: MutableMap<String, Action> = mutableMapOf()

    /**
     * keep a list of seeded individual
     */
    protected val seededIndividuals : MutableList<T> = mutableListOf()

    private var pickingUpLastSeed = false

    fun getActionDefinitions() : List<Action> {
        return actionCluster.values.map { it.copy() }
    }

    /**
     * @return if the last seeded was picked up
     */
    fun isLastSeededIndividual() = pickingUpLastSeed

    private fun resetPickingUpLastSeededIndividual() {
        pickingUpLastSeed = false
    }

    /**
     * Create a new individual. Usually each call to this method
     * will create a new, different individual, but there is no
     * hard guarantee
     */
    fun sample(forceRandomSample: Boolean = false): T {
        if (log.isTraceEnabled){
            log.trace("sampler will be applied")
        }

        resetPickingUpLastSeededIndividual()

        if (config.seedTestCases && seededIndividuals.isNotEmpty()){
            pickingUpLastSeed = seededIndividuals.size == 1
            return seededIndividuals.removeLast()
        }

        val ind = if (forceRandomSample) {
            sampleAtRandom()
        } else if ( config.isEnabledSmartSampling() && (hasSpecialInitForSmartSampler() ||  randomness.nextBoolean(config.probOfSmartSampling))) {
            // If there is still special init set, sample from that, otherwise depend on probability
            smartSample()
        } else {
            sampleAtRandom()
        }

        samplePostProcessing(ind)

        org.evomaster.core.Lazy.assert { ind.verifyValidity(); true }
        return ind
    }

    /**
     * Sample a new individual at random, but still satisfying all given constraints.
     *
     * Note: must guarantee to setup the [searchGlobalState] in this new individual
     */
    protected abstract fun sampleAtRandom(): T

    protected abstract fun initSeededTests(infoDto: SutInfoDto? = null)

    open fun samplePostProcessing(ind: T){

        val state = ind.searchGlobalState ?: return
        val time = state.time.percentageUsedBudget()

        ind.seeAllActions().forEach { a ->
            val allGenes = a.seeTopGenes().flatMap { it.flatView() }

            allGenes.filterIsInstance<OptionalGene>()
                .filter { it.searchPercentageActive < time }
                .forEach { it.forbidSelection() }
        }
    }

    /**
     * Create a new individual, but not fully at random, but rather
     * by using some domain-knowledge.
     *
     * Note: must guarantee to setup the [searchGlobalState] in this new individual
     */
    protected open fun smartSample(): T {
        //unless this method is overridden, just sample at random
        return sampleAtRandom()
    }

    fun numberOfDistinctActions() = actionCluster.size

    /**
     * @return number of seeded individual which are not executed
     */
    fun numberOfNotExecutedSeededIndividuals() = seededIndividuals.size

    /**
     * @return number of seeded individual which are not executed
     */
    fun getNotExecutedSeededIndividuals() = seededIndividuals.toList()

    /**
     * When the search starts, there might be some predefined individuals
     * that we can sample. But we just need to sample each of them just once.
     * The [smartSample] must first pick from this set.
     *
     * @return false if there is not left predefined individual to sample with smart sampler
     */
    open fun hasSpecialInitForSmartSampler() = false

    /**
     * When the search starts, there might be some predefined individuals
     * that we can sample.
     *
     * @return false if there is not left predefined individual to sample
     */
    fun hasSpecialInit() : Boolean = seededIndividuals.isNotEmpty() || hasSpecialInitForSmartSampler()

    open fun resetSpecialInit() {}


    fun seeAvailableActions(): List<Action> {

        return actionCluster.entries
                .asSequence()
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .toList()
    }

    /**
     * this can be used to provide feedback to sampler regarding a fitness of the sampled individual (i.e., [evi]).
     */
    open fun feedback(evi : EvaluatedIndividual<T>){}


    /**
     * get max test size during sampling
     */
    fun getMaxTestSizeDuringSampler() : Int{
        return when(config.maxTestSizeStrategy){
            EMConfig.MaxTestSizeStrategy.SPECIFIED -> config.maxTestSize
            EMConfig.MaxTestSizeStrategy.DPC_INCREASING -> apc.getExploratoryValue(config.dpcTargetTestSize, config.maxTestSize)
            EMConfig.MaxTestSizeStrategy.DPC_DECREASING -> apc.getExploratoryValue(config.maxTestSize, config.dpcTargetTestSize)
        }
    }

    /**
     * extract tables with additional FK tables
     */
    open fun extractFkTables(tables: Set<String>): Set<String>{
        throw IllegalStateException("FK tables have not been not handled yet")
    }

    /**
     * Return a list of pre-written individuals that will be added in the final solution.
     * Those will not be evolved during the search, but still need to compute their fitness,
     * eg to create valid assertions for them.
     */
    open fun getPreDefinedIndividuals() = listOf<T>()
}
