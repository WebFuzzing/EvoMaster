package org.evomaster.core.search

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.search.tracer.TraceableElementCopyFilter
import org.evomaster.core.search.tracer.TrackOperator
import org.evomaster.core.search.tracer.TrackingHistory

/**
 * An individual for the search.
 * Also called "chromosome" in evolutionary algorithms.
 * In our context, most of the time an Individual will represent
 * a single test case, composed by 1 or more "actions" (eg, calls
 * to a RESTful API, SQL operations on a database or WireMock setup)
 *
 * @property trackOperator presents which operator creates the individual, e.g., sampler
 * @property index presents when the individual is created
 * @param children specify the children of the individual with the constructor
 *
 */
abstract class Individual(override var trackOperator: TrackOperator? = null,
                          override var index: Int = Traceable.DEFAULT_INDEX,
                          children: List<out StructuralElement>
) : Traceable, StructuralElement(children){

    /**
     * presents the evaluated results of the individual once the individual is tracked (i.e., [EMConfig.enableTrackIndividual]).
     *
     * Note that if the evalutedIndividual is tracked (i.e., [EMConfig.enableTrackEvaluatedIndividual]),
     * e do not recommend to track the individual
     */
    override var evaluatedResult: EvaluatedMutation? = null

    /**
     * presents the history of the individual once the individual is tracked (i.e., [EMConfig.enableTrackIndividual]).
     *
     * Note that if the evalutedIndividual is tracked (i.e., [EMConfig.enableTrackEvaluatedIndividual]),
     * we do not recommend to track the individual
     */
    override var tracking: TrackingHistory<out Traceable>? = null

    /**
     * Mainly used for debugging. keep track from which MIO population this individual was sampled from.
     * if generated at random, or by any other mean, then it is null
     */
    var populationOrigin : String? = null

    /**
     * Make a deep copy of this individual
     */
    final override fun copy(): Individual{
        val copy = super.copy()
        if (copy !is Individual)
            throw IllegalStateException("mismatched type: the type should be Individual, but it is ${this::class.java.simpleName}")
        copy.populationOrigin = this.populationOrigin
        return copy
    }

    override fun copyContent(): Individual {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }

    enum class GeneFilter { ALL, NO_SQL, ONLY_SQL }

    /**
     * Return a view of all the Genes in this chromosome/individual
     */
    abstract fun seeGenes(filter: GeneFilter = GeneFilter.ALL): List<out Gene>

    /**
     * An estimation of the "size" of this individual.
     * Longer/bigger individuals are usually considered worse,
     * unless they cover more coverage targets
     */
    abstract fun size(): Int



    /**
     * @return actions based on the specified [filter]
     *
     * TODO refactor [seeActions], [seeInitializingActions] and [seeDbActions] based on this fun
     */
    open fun seeActions(filter: ActionFilter) : List<out Action>{
        return seeActions()
    }

    /**
     * Return a view of all the "actions" defined in this individual.
     * Note: each action could be composed by 0 or more genes
     */
    abstract fun seeActions(): List<out Action>

    /**
     * Return a view of all initializing actions done before the main
     * ones. Example: these could set up database before doing HTTP
     * calls toward the SUT.
     * A test does not require to have initializing actions.
     */
    open fun seeInitializingActions(): List<Action> = listOf()

    /**
     * return a list of all db actions in [this] individual
     * that include all initializing actions plus db actions among rest actions.
     *
     * NOTE THAT if EMConfig.probOfApplySQLActionToCreateResources is 0.0, this method
     * would be same with [seeInitializingActions]
     */
    open fun seeDbActions() : List<Action> = seeInitializingActions()

    /**
     * Determine if the structure (ie the actions) of this individual
     * can be mutated (eg, add/remove actions).
     * Note: even if this is false, it would still be possible to
     * mutate the genes in those actions
     */
    open fun canMutateStructure() = false


    /**
     * Returns true if the initialization actions
     * are correct (i.e. all constraints are satisfied)
     */
    abstract fun verifyInitializationActions(): Boolean

    /**
     * Attempts to repair the initialization actions.
     * Initialization actions must pass the verifyInitializationAction()
     * test after this method is invoked.
     */
    abstract fun repairInitializationActions(randomness: Randomness)

    override fun copy(options: TraceableElementCopyFilter): Traceable {
        val copy = copy()
        return when(options){
            TraceableElementCopyFilter.NONE -> copy
            TraceableElementCopyFilter.WITH_TRACK->{
                copy.wrapWithTracking(evaluatedResult, tracking?.copy())
                copy
            }
            TraceableElementCopyFilter.WITH_ONLY_EVALUATED_RESULT ->{
                copy.wrapWithEvaluatedResults(evaluatedResult)
                copy
            }
            else -> throw IllegalArgumentException("NOT support $options")
        }
    }

    override fun next(next: Traceable, copyFilter: TraceableElementCopyFilter, evaluatedResult: EvaluatedMutation): Traceable? {
        tracking?: throw IllegalStateException("cannot create next due to unavailable tracking info")

        val nextInTracking = (next.copy(copyFilter) as Individual).also { this.wrapWithEvaluatedResults(evaluatedResult) }
        pushLatest(nextInTracking)

        val new = (next as Individual).copy()
//        new.wrapWithTracking(
//                evaluatedResult = evaluatedResult,
//                trackingHistory = tracking?.copy(copyFilter)
//        )

        new.wrapWithTracking(
                evaluatedResult = evaluatedResult,
                trackingHistory = tracking
        )

        return new
    }
    /**
     * @return whether this individual has same actions with [other]
     */
    open fun sameActions(other: Individual, excludeInitialization : Boolean = false) : Boolean{
        if (!excludeInitialization || seeInitializingActions().size != other.seeInitializingActions().size)
            return false
        if (seeActions().size != other.seeActions().size)
            return false
        if (!excludeInitialization || (0 until seeInitializingActions().size).any { seeInitializingActions()[it].getName() != other.seeInitializingActions()[it].getName() })
            return false
        if ((0 until seeActions().size).any { seeActions()[it].getName() != other.seeActions()[it].getName() })
            return false
        return true
    }

    /**
     * @return whether there exist any actions in the individual,
     *  e.g., if false, the individual might be composed of a sequence of genes.
     */
    open fun hasAnyAction()  = seeActions().isNotEmpty()


    open fun cleanBrokenBindingReference(){
        val all = seeGenes(GeneFilter.ALL).flatMap { it.flatView() }
        all.filter { it.isBoundGene() }.forEach { b->
            b.cleanBrokenReference(all)
        }
    }


    /**
     * @return a gene in [this] based on the [gene] in [individual]
     */
    open fun findGene(individual: Individual, gene: Gene): Gene?{
        // individuals should be same type
        if (individual::class.java.name != this::class.java.name) return null

        val allgenes = individual.seeGenes().flatMap { it.flatView() }
        val all = seeGenes().flatMap { it.flatView() }

        if (allgenes.size != all.size) return null

        val index = allgenes.indexOf(gene)
        if (index == -1){
            throw IllegalArgumentException("given gene (${gene.name}) does not belong to the individual which contains ${allgenes.joinToString(","){it.name}}")
        }

        val found = all[index]
        if (!gene.possiblySame(found))
            return null

        return found
    }

    /**
     * verify whether all binding genes are in this individual
     */
    fun verifyBindingGenes() : Boolean{
        val all = seeGenes(GeneFilter.ALL).flatMap{it.flatView()}
        all.forEach { g->
            val inside = g.bindingGeneIsSubsetOf(all)
            if (!inside)
                return false
        }
        return true
    }

}
