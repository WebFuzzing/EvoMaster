package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.RestIndividual

/**
 * created by manzh on 2019-10-31
 *
 * this class is to collect impacts for an individual including
 * @property initializationGeneImpacts impacts for initialization actions
 * @property actionGeneImpacts impacts for rest actions
 * @property impactsOfStructure impacts for a structure of rest actions
 */
open class ImpactsOfIndividual(
        /**
         * list of impacts per action in initialization of an individual
         */
        val initializationGeneImpacts: InitializationActionImpacts,

        /**
         * list of impacts per action in actions of an individual
         */
        val actionGeneImpacts: MutableList<ImpactsOfAction>,

        /**
         * a history of structures of [this] with best fitness
         */
        val impactsOfStructure: ActionStructureImpact = ActionStructureImpact("StructureSize")
) {

    constructor(individual: Individual, abstractInitializationGeneToMutate: Boolean,  fitnessValue: FitnessValue?) : this(
            initializationGeneImpacts = InitializationActionImpacts(abstractInitializationGeneToMutate),//individual.seeInitializingActions().map { a -> ImpactsOfAction(a) }.toMutableList(),
            actionGeneImpacts = if (individual.seeActions(ActionFilter.NO_INIT).isEmpty()) mutableListOf(ImpactsOfAction(individual, individual.seeGenes())) else individual.seeActions(ActionFilter.NO_INIT).map { a -> ImpactsOfAction(a) }.toMutableList()
    ) {
        if (fitnessValue != null) {
            impactsOfStructure.updateStructure(individual, fitnessValue)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImpactsOfIndividual::class.java)
    }

    /**
     * @return a copy of [this]
     */
    open fun copy(): ImpactsOfIndividual {
        return ImpactsOfIndividual(
                initializationGeneImpacts.copy(),
                actionGeneImpacts.map { it.copy() }.toMutableList(),
                impactsOfStructure.copy()
        )
    }

    /**
     * @return a clone of this.
     *
     * Note the with the clone, for the
     */
    open fun clone(): ImpactsOfIndividual {
        return ImpactsOfIndividual(
                initializationGeneImpacts.clone(),
                actionGeneImpacts.map { it.clone() }.toMutableList(),
                impactsOfStructure.clone()
        )
    }

    /**
     * @return pair, key is the position at impacts, and value indicates false is to remove and true is to add
     */
    fun findFirstMismatchedIndex(actions: List<Action>) : Pair<Int, Boolean?>{
        actions.forEachIndexed { index, action ->
            if (index == actionGeneImpacts.size)
                return index to false
            if (action.getName() != actionGeneImpacts[index].actionName){
                val remove = index + 1 < actionGeneImpacts.size && actionGeneImpacts.subList(index+1, actionGeneImpacts.size).any {
                    it.actionName == action.getName()
                }
                return index to remove
            }
        }
        if (actionGeneImpacts.size > actions.size){
            return actions.size to true
        }
        return (-1 to null)
    }


    /**
     * verify action gene impacts based on the given [actions]
     */
    fun verifyActionGeneImpacts(actions : List<Action>){
        if (actions.size != actionGeneImpacts.size)
            throw IllegalStateException("mismatched size of impacts according to actions: ${actions.size} (actions) vs. ${actionGeneImpacts.size} (impacts)")
        actions.forEachIndexed { index, action ->
            if (action.getName() != actionGeneImpacts[index].actionName)
                throw IllegalStateException("mismatched impact info at $index index: actual action is ${action.getName()}, but the impact info is ${actionGeneImpacts[index].actionName}")
        }
    }

    /**
     * @return size of existingData in the initialization
     */
    fun getSQLExistingData() = initializationGeneImpacts.getExistingData()


    /**
     * @return size of action impacts
     * @param fromInitialization specifies whether the actions are in the initialization or not
     */
    fun getSizeOfActionImpacts(fromInitialization: Boolean) = if (fromInitialization) initializationGeneImpacts.getSize() else actionGeneImpacts.size

    /**
     * @param actionIndex is null when there is no action in the individual, then return the first GeneImpact
     */
    fun getGene(actionName: String?, geneId: String, actionIndex: Int?, fromInitialization: Boolean): GeneImpact? {
        if (actionIndex == null || (actionIndex == -1 && noneActionIndividual())) return actionGeneImpacts.first().geneImpacts[geneId]
        val impactsOfAction =
                if (fromInitialization) initializationGeneImpacts.getImpactOfAction(actionName, actionIndex)
                else getImpactsAction(actionName, actionIndex)
        impactsOfAction ?: return null
        return impactsOfAction.get(geneId, actionName)
    }

    /**
     * @return all genes whose name is [geneId]
     */
    fun getGeneImpact(geneId: String): List<GeneImpact> {
        val list = mutableListOf<GeneImpact>()

        initializationGeneImpacts.getAll().plus(actionGeneImpacts).forEach {
            if (it.geneImpacts.containsKey(geneId))
                list.add(it.geneImpacts[geneId]!!)
        }
        return list
    }

    private fun noneActionIndividual() : Boolean = actionGeneImpacts.size == 1 && actionGeneImpacts.first().actionName == null

    /**
     * synchronize the impacts based on the [individual] and [mutatedGene]
     */
    fun syncBasedOnIndividual(individual: Individual, mutatedGene: MutatedGeneSpecification) {
        //for initialization due to db action fixing
        val diff = individual.seeInitializingActions().size - initializationGeneImpacts.getOriginalSize()//mutatedGene.addedExistingDataInitialization.size - initializationGeneImpacts.getOriginalSize()
        if (diff < 0) { //truncation
            initializationGeneImpacts.truncation(individual.seeInitializingActions())
        }else if (diff > 0){
            throw IllegalArgumentException("impact is out of sync")
        }
        if (initializationGeneImpacts.getOriginalSize() != individual.seeInitializingActions().size){
            throw IllegalStateException("inconsistent impact for SQL genes")
        }

        //for action
        if ((individual.seeActions(ActionFilter.NO_INIT).isNotEmpty() && individual.seeActions(ActionFilter.NO_INIT).size != actionGeneImpacts.size) ||
                (individual.seeActions(ActionFilter.NO_INIT).isEmpty() && !noneActionIndividual()))
            throw IllegalArgumentException("inconsistent size of actions and impacts")

        individual.seeActions(ActionFilter.NO_INIT).forEach { action ->
            val actionName = action.getName()
            val index = individual.seeActions(ActionFilter.NO_INIT).indexOf(action)
            //root genes might be changed e.g., additionalInfo, so sync impacts of all genes
            action.seeGenes().forEach { g ->
                val id = ImpactUtils.generateGeneId(action, g)
                if (getGene(actionName, id, index, false) == null) {
                    val impact = ImpactUtils.createGeneImpact(g, id)
                    actionGeneImpacts[index].addGeneImpact(actionName, impact)
                }
            }
        }
    }

    /**
     * remove impacts for actions based on their index [actionIndex]
     * @return whether the removal performs successfully
     */
    fun deleteActionGeneImpacts(actionIndex: Set<Int>): Boolean {
        if (actionIndex.isEmpty()) return false
        if (actionIndex.maxOrNull()!! >= actionGeneImpacts.size)
            return false
        actionIndex.sortedDescending().forEach {
            actionGeneImpacts.removeAt(it)
        }
        return true
    }


    /**
     * swap gene impacts based on their index
     */
    fun swapActionGeneImpact(actionIndex: List<Int>, swapTo: List<Int>){
        var a = actionIndex
        var b = swapTo

        if (swapTo.first() < actionIndex.first()){
            a = swapTo
            b = actionIndex
        }

        val aImpacts = a.map { actionGeneImpacts[it] }
        val bImpacts = b.map { actionGeneImpacts[it] }

        (a.plus(b)).sorted().reversed().forEach {
            actionGeneImpacts.removeAt(it)
        }

//        actionGeneImpacts.removeAll(aImpacts)
//        actionGeneImpacts.removeAll(bImpacts)


        actionGeneImpacts.addAll(a.first(), bImpacts)
        val bIndex = b.first() + (b.size - a.size)
        actionGeneImpacts.addAll(bIndex, aImpacts)
    }

    /**
     * update impacts for initialization
     */
    fun updateInitializationImpactsAtBeginning(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationGeneImpacts.updateInitializationImpactsAtBeginning(groupedActions, existingDataSize)
    }

    /**
     * init impacts for initialization
     */
    fun initInitializationImpacts(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationGeneImpacts.initInitializationActions(groupedActions, existingDataSize)
    }

    /**
     * append impacts for initialization
     */
    fun appendInitializationImpacts(groupedActions: List<List<Action>>) {
        initializationGeneImpacts.appendInitialization(groupedActions)
    }

    /**
     * remove impacts for initialization
     */
    fun removeInitializationImpacts(removed : List<Pair<DbAction, Int>>, existingDataSize: Int){
        initializationGeneImpacts.updateSizeOfExistingData(existingDataSize)
        initializationGeneImpacts.removeInitialization(removed)
    }

    /**
     * update impacts for initialization based on the given impacts of individual [other]
     */
    fun updateInitializationGeneImpacts(other: ImpactsOfIndividual) {
        initializationGeneImpacts.initInitializationActions(other.initializationGeneImpacts)
    }

    /**
     * update the size of existing data
     */
    fun updateExistingSQLData(size: Int) {
        initializationGeneImpacts.updateSizeOfExistingData(size)
    }

    /**
     * add/ update gene actions based on
     * @param actionName is the name of the action
     * @param actionIndex specifies the index of the action at the individual
     * @param newAction specifies whether the action is newly added
     * @param impacts specifies the impacts of the actions to be added/updated
     */
    fun addOrUpdateActionGeneImpacts(actionName: String?, actionIndex: Int, newAction: Boolean, impacts: MutableMap<String, GeneImpact>): Boolean {
        if (newAction) {
            if (actionIndex > actionGeneImpacts.size) return false
            actionGeneImpacts.add(actionIndex, ImpactsOfAction(actionName, impacts))
            return true
        }
        if (actionIndex >= actionGeneImpacts.size) return false
        return actionGeneImpacts[actionIndex].addGeneImpact(actionName, impacts)
    }

    /**
     * @return whether there exist any collected impact,
     *     e.g., time of manipulation is more than one for any gene/action
     */
    fun anyImpactfulInfo(): Boolean {
        for (a in initializationGeneImpacts.getAll().plus(actionGeneImpacts)) {
            if (a.anyImpactfulInfo()) return true
        }
        return false
    }

    /**
     * @return all flatten gene impacts for the individual
     */
    fun flattenAllGeneImpact(): List<GeneImpact> {
        return initializationGeneImpacts.getAll().plus(actionGeneImpacts).flatMap { it.geneImpacts.values }
    }

    /**
     * @return all gene impacts for each of actions in initialization of the individual
     * Note that for each of the action, we remove a map for all of the genes in the actions:
     *      - the key of the map is the id of genes based on [ImpactUtils.generateGeneId]
     *      - the value of the map is the gene impacts
     */
    fun getInitializationGeneImpact(): List<MutableMap<String, GeneImpact>> {
        return initializationGeneImpacts.getAll().map { it.geneImpacts }
    }

    /**
     * export impacts info to [content] for the given targets [targets]
     * @param areInitializationGeneImpact specifies whether the impacts are in the initialization
     */
    fun exportImpactInfo(areInitializationGeneImpact: Boolean, content : MutableList<String>, targets : Set<Int>? = null){
        val impacts = if (areInitializationGeneImpact) getInitializationGeneImpact() else getActionGeneImpact()
        val prefix = if (areInitializationGeneImpact) "Initialization" else "Action"
        impacts.forEachIndexed { aindex, mutableMap ->
            mutableMap.forEach { (t, geneImpact) ->
                content.add(mutableListOf("$prefix$aindex", t).plus(geneImpact.toCSVCell(targets)).joinToString(","))
                geneImpact.flatViewInnerImpact().forEach { (name, impact) ->
                    content.add(mutableListOf("$prefix$aindex", "$t-$name").plus(impact.toCSVCell(targets)).joinToString(","))
                }
            }
        }
    }

    /**
     * @return all genes of the actions in the indiviudal
     */
    fun getActionGeneImpact(): List<MutableMap<String, GeneImpact>> {
        return actionGeneImpacts.map { it.geneImpacts }
    }

    /**
     * @return whether there exist any impact
     */
    fun anyImpactInfo(): Boolean = initializationGeneImpacts.getSize() > 0 || actionGeneImpacts.isNotEmpty()

    private fun getImpactsAction(actionName: String?, actionIndex: Int): ImpactsOfAction {
        if (actionIndex >= actionGeneImpacts.size)
            throw IllegalArgumentException("exceed the boundary of impacts regarding actions, i.e., size of actions is ${actionGeneImpacts.size}, but asking index is $actionIndex")
        val actionImpacts = actionGeneImpacts[actionIndex]
        if (actionName != null && actionImpacts.actionName != actionName)
            throw IllegalArgumentException("mismatched action name, i.e., current is ${actionImpacts.actionName}, but $actionName")
        return actionImpacts
    }

    /**
     * @return impact of action based on
     * @param actionName specifies the name of the action
     * @param actionIndex specifies the index of the actions in the initialization or not from the individual
     * @param fromInitialization specifies whether the actions are in the initialization
     */
    fun findImpactsByAction(actionName: String, actionIndex: Int, fromInitialization: Boolean): MutableMap<String, GeneImpact>? {
        val found = findImpactsAction(actionName, actionIndex, fromInitialization) ?: return null
        return found.geneImpacts
    }

    private fun findImpactsAction(actionName: String, actionIndex: Int, fromInitialization: Boolean): ImpactsOfAction? {
        return try {
            if (fromInitialization) initializationGeneImpacts.getImpactOfAction(actionName, actionIndex)
            else getImpactsAction(actionName, actionIndex)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}