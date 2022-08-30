package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * this class is to collect impacts for an individual including
 * based on a structure of individual, the impacts are classified into
 * - initialization represents impacts of actions in initialization, eg DbAction
 * - mainActions represents impacts of main actions, eg, RestAction (see [Individual.seeMainExecutableActions])
 * Note that the impact collection ignores the structure of action tree, only consider
 * their root actions
 *
 * @property initializationImpacts impacts for initialization actions
 * @property mainActionsImpacts impacts for main actions
 * @property impactsOfStructure impacts for a structure of rest actions
 */
open class ImpactsOfIndividual(
    /**
     * list of impacts per action in initialization of an individual
     */
    val initializationImpacts: InitializationActionImpacts,

    /**
     * list of impacts per action in main actions of an individual
     */
    val mainActionsImpacts: MutableList<ImpactsOfAction>,

    /**
     * a history of structures of [this] with best fitness
     */
    val impactsOfStructure: ActionStructureImpact = ActionStructureImpact("StructureSize")
) {

    constructor(individual: Individual, abstractInitializationGeneToMutate: Boolean,  fitnessValue: FitnessValue?) : this(
            initializationImpacts = InitializationActionImpacts(abstractInitializationGeneToMutate),//individual.seeInitializingActions().map { a -> ImpactsOfAction(a) }.toMutableList(),
            mainActionsImpacts = if (individual.seeActions(ActionFilter.NO_INIT).isEmpty())
//                mutableListOf(ImpactsOfAction(individual, individual.seeGenes()))
                throw IllegalStateException("there is no main actions in this individual")
            else individual.seeActions(ActionFilter.NO_INIT).map { a -> ImpactsOfAction(a) }.toMutableList()
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
                initializationImpacts.copy(),
                mainActionsImpacts.map { it.copy() }.toMutableList(),
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
                initializationImpacts.clone(),
                mainActionsImpacts.map { it.clone() }.toMutableList(),
                impactsOfStructure.clone()
        )
    }

    /**
     * @return pair, key is the position at impacts, and value indicates false is to remove and true is to add
     */
    fun findFirstMismatchedIndex(actions: List<Action>) : Pair<Int, Boolean?>{
        actions.forEachIndexed { index, action ->
            if (index == mainActionsImpacts.size)
                return index to false
            if (action.getName() != mainActionsImpacts[index].actionName){
                val remove = index + 1 < mainActionsImpacts.size && mainActionsImpacts.subList(index+1, mainActionsImpacts.size).any {
                    it.actionName == action.getName()
                }
                return index to remove
            }
        }
        if (mainActionsImpacts.size > actions.size){
            return actions.size to true
        }
        return (-1 to null)
    }


    /**
     * verify action gene impacts based on the given [actions]
     */
    fun verifyActionGeneImpacts(actions : List<Action>){
        if (actions.size != mainActionsImpacts.size)
            throw IllegalStateException("mismatched size of impacts according to actions: ${actions.size} (actions) vs. ${mainActionsImpacts.size} (impacts)")
        actions.forEachIndexed { index, action ->
            if (action.getName() != mainActionsImpacts[index].actionName)
                throw IllegalStateException("mismatched impact info at $index index: actual action is ${action.getName()}, but the impact info is ${mainActionsImpacts[index].actionName}")
        }
    }

    /**
     * @return size of existingData in the initialization
     */
    fun getSQLExistingData() = initializationImpacts.getExistingData()


    /**
     * @return size of action impacts
     * @param fromInitialization specifies whether the actions are in the initialization or not
     */
    fun getSizeOfActionImpacts(fromInitialization: Boolean) = if (fromInitialization) initializationImpacts.getSize() else mainActionsImpacts.size

    /**
     * @param actionIndex is null when there is no action in the individual, then return the first GeneImpact
     */
    @Deprecated("It can be replaced with [getGene(actionName: String?, geneId: String, actionLocalId: String, fromInitialization: Boolean)]")
    fun getGene(actionName: String?, geneId: String, actionIndex: Int?, fromInitialization: Boolean): GeneImpact? {
        if (actionIndex == null || (actionIndex == -1 && noneActionIndividual())) return mainActionsImpacts.first().geneImpacts[geneId]
        val impactsOfAction =
                if (fromInitialization) initializationImpacts.getImpactOfAction(actionName, actionIndex)
                else getImpactsAction(actionName, actionIndex)
        impactsOfAction ?: return null
        return impactsOfAction.get(geneId, actionName)
    }

    /**
     * @param actionName the name of action which contains the gene
     * @param geneId is the id of the gene to get
     * @param actionLocalId local id of the action which contains the gene
     * @param fromInitialization represents whether the action is part of initialization
     */
    fun getGene(actionName: String?, geneId: String, actionLocalId: String, fromInitialization: Boolean): GeneImpact? {
        val impactsOfAction =
            if (fromInitialization) initializationImpacts.getImpactOfAction(actionName, actionLocalId)
            else getImpactsAction(actionName, actionLocalId)
        impactsOfAction ?: return null
        return impactsOfAction.get(geneId, actionName)
    }

    /**
     * @return all genes whose name is [geneId]
     */
    fun getGeneImpact(geneId: String): List<GeneImpact> {
        val list = mutableListOf<GeneImpact>()

        initializationImpacts.getAll().plus(mainActionsImpacts).forEach {
            if (it.geneImpacts.containsKey(geneId))
                list.add(it.geneImpacts[geneId]!!)
        }
        return list
    }


    private fun noneActionIndividual() : Boolean = mainActionsImpacts.size == 1 && mainActionsImpacts.first().actionName == null

    /**
     * synchronize the impacts based on the [individual] and [mutatedGene]
     */
    fun syncBasedOnIndividual(individual: Individual, mutatedGene: MutatedGeneSpecification) {
        // TODO Man fix external services
        val initActions = individual.seeInitializingActions().filterIsInstance<DbAction>()
        //for initialization due to db action fixing
        val diff = initActions.size - initializationImpacts.getOriginalSize()//mutatedGene.addedExistingDataInitialization.size - initializationGeneImpacts.getOriginalSize()
        if (diff < 0) { //truncation
            initializationImpacts.truncation(individual.seeInitializingActions())
        }else if (diff > 0){
            throw IllegalArgumentException("impact is out of sync")
        }
        if (initializationImpacts.getOriginalSize() != initActions.size){
            throw IllegalStateException("inconsistent impact for SQL genes")
        }

        //for action
        if ((individual.seeActions(ActionFilter.NO_INIT).isNotEmpty() && individual.seeActions(ActionFilter.NO_INIT).size != mainActionsImpacts.size) ||
                (individual.seeActions(ActionFilter.NO_INIT).isEmpty() && !noneActionIndividual()))
            throw IllegalArgumentException("inconsistent size of actions and impacts")

        individual.seeActions(ActionFilter.NO_INIT).forEach { action ->
            val actionName = action.getName()
            val index = individual.seeActions(ActionFilter.NO_INIT).indexOf(action)
            //root genes might be changed e.g., additionalInfo, so sync impacts of all genes
            action.seeTopGenes().forEach { g ->
                val id = ImpactUtils.generateGeneId(action, g)
                if (getGene(actionName, id, index, false) == null) {
                    val impact = ImpactUtils.createGeneImpact(g, id)
                    mainActionsImpacts[index].addGeneImpact(actionName, impact)
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
        if (actionIndex.maxOrNull()!! >= mainActionsImpacts.size)
            return false
        actionIndex.sortedDescending().forEach {
            mainActionsImpacts.removeAt(it)
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

        val aImpacts = a.map { mainActionsImpacts[it] }
        val bImpacts = b.map { mainActionsImpacts[it] }

        (a.plus(b)).sorted().reversed().forEach {
            mainActionsImpacts.removeAt(it)
        }

//        actionGeneImpacts.removeAll(aImpacts)
//        actionGeneImpacts.removeAll(bImpacts)


        mainActionsImpacts.addAll(a.first(), bImpacts)
        val bIndex = b.first() + (b.size - a.size)
        mainActionsImpacts.addAll(bIndex, aImpacts)
    }

    /**
     * update impacts for initialization
     */
    fun updateInitializationImpactsAtEnd(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationImpacts.updateInitializationImpactsAtEnd(groupedActions, existingDataSize)
    }

    /**
     * init impacts for initialization
     */
    fun initInitializationImpacts(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationImpacts.initInitializationActions(groupedActions, existingDataSize)
    }

    /**
     * append impacts for initialization
     */
    fun appendInitializationImpacts(groupedActions: List<List<Action>>) {
        initializationImpacts.appendInitialization(groupedActions)
    }

    /**
     * remove impacts for initialization
     */
    fun removeInitializationImpacts(removed : List<Pair<DbAction, Int>>, existingDataSize: Int){
        initializationImpacts.updateSizeOfExistingData(existingDataSize)
        initializationImpacts.removeInitialization(removed)
    }

    /**
     * update impacts for initialization based on the given impacts of individual [other]
     */
    fun updateInitializationGeneImpacts(other: ImpactsOfIndividual) {
        initializationImpacts.initInitializationActions(other.initializationImpacts)
    }

    /**
     * update the size of existing data
     */
    fun updateExistingSQLData(size: Int) {
        initializationImpacts.updateSizeOfExistingData(size)
    }

    /**
     * add/ update gene actions based on
     * @param actionName is the name of the action
     * @param actionIndex specifies the index of the action at the individual
     * @param newAction specifies whether the action is newly added
     * @param impacts specifies the impacts of the actions to be added/updated
     */
    fun addOrUpdateActionGeneImpacts(action: Action, newAction: Boolean, impacts: MutableMap<String, GeneImpact>): Boolean {
        val impactOfAction = mainActionsImpacts.find { it.localId == action.getLocalId() }

        if (newAction) {
            if (impactOfAction != null) {
                log.warn("An impact for the action with id (${action.getLocalId()}) has been added")
                return false
            }
            mainActionsImpacts.add(ImpactsOfAction(action.getLocalId(), action.getName(), impacts))
            return true
        }

        impactOfAction?: return false
        return impactOfAction.addGeneImpact(action.getName(), impacts)
    }

    /**
     * @return whether there exist any collected impact,
     *     e.g., time of manipulation is more than one for any gene/action
     */
    fun anyImpactfulInfo(): Boolean {
        for (a in initializationImpacts.getAll().plus(mainActionsImpacts)) {
            if (a.anyImpactfulInfo()) return true
        }
        return false
    }

    /**
     * @return all flatten gene impacts for the individual
     */
    fun flattenAllGeneImpact(): List<GeneImpact> {
        return initializationImpacts.getAll().plus(mainActionsImpacts).flatMap { it.geneImpacts.values }
    }

    /**
     * @return all gene impacts for each of actions in initialization of the individual
     * Note that for each of the action, we remove a map for all of the genes in the actions:
     *      - the key of the map is the id of genes based on [ImpactUtils.generateGeneId]
     *      - the value of the map is the gene impacts
     */
    fun getInitializationGeneImpact(): List<MutableMap<String, GeneImpact>> {
        return initializationImpacts.getAll().map { it.geneImpacts }
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
        return mainActionsImpacts.map { it.geneImpacts }
    }

    /**
     * @return whether there exist any impact
     */
    fun anyImpactInfo(): Boolean = initializationImpacts.getSize() > 0 || mainActionsImpacts.isNotEmpty()


    fun findImpactsByAction(actionName: String, actionLocalId: String, fromInitialization: Boolean): MutableMap<String, GeneImpact>? {
        val found = findImpactsAction(actionName, actionLocalId, fromInitialization) ?: return null
        return found.geneImpacts
    }

    private fun findImpactsAction(actionName: String, actionLocalId: String, fromInitialization: Boolean): ImpactsOfAction? {
        return try {
            if (fromInitialization) initializationImpacts.getImpactOfAction(actionName, actionLocalId)
            else getImpactsAction(actionName, actionLocalId)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun getImpactsAction(actionName: String?, actionLocalId: String): ImpactsOfAction {

        val actionImpacts = mainActionsImpacts.filter { it.localId == actionLocalId }
        if (actionImpacts.isEmpty())
            throw IllegalStateException("Cannot find any impact with the actionLocalId")
        if (actionImpacts.size > 1)
            throw IllegalStateException("there exist more than one impacts (${actionImpacts.size}) whose local id is $actionLocalId")
        if (actionName != null && actionImpacts.first().actionName != actionName)
            throw IllegalArgumentException("mismatched action name, i.e., current is ${actionImpacts.first().actionName}, but $actionName")
        return actionImpacts.first()
    }

    @Deprecated("It is replaced by [getImpactsAction(actionName: String?, actionLocalId: String)]")
    private fun getImpactsAction(actionName: String?, actionIndex: Int): ImpactsOfAction {
        if (actionIndex >= mainActionsImpacts.size)
            throw IllegalArgumentException("exceed the boundary of impacts regarding actions, i.e., size of actions is ${mainActionsImpacts.size}, but asking index is $actionIndex")
        val actionImpacts = mainActionsImpacts[actionIndex]
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
    @Deprecated("It is replaced by [findImpactsByAction(actionName: String, actionLocalId: String, fromInitialization: Boolean)]")
    fun findImpactsByAction(actionName: String, actionIndex: Int, fromInitialization: Boolean): MutableMap<String, GeneImpact>? {
        val found = findImpactsAction(actionName, actionIndex, fromInitialization) ?: return null
        return found.geneImpacts
    }

    @Deprecated("It is replaced by [findImpactsAction(actionName: String, actionLocalId: String, fromInitialization: Boolean)]")
    private fun findImpactsAction(actionName: String, actionIndex: Int, fromInitialization: Boolean): ImpactsOfAction? {
        return try {
            if (fromInitialization) initializationImpacts.getImpactOfAction(actionName, actionIndex)
            else getImpactsAction(actionName, actionIndex)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}