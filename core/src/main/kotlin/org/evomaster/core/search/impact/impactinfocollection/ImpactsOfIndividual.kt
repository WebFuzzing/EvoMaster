package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.database.DbAction
import org.evomaster.core.search.Action
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * created by manzh on 2019-10-31
 */
class ImpactsOfIndividual private constructor(
        /**
         * list of impacts per action in initialization of an individual
         */
        private val initializationGeneImpacts: InitializationActionImpacts,

        /**
         * list of impacts per action in actions of an individual
         */
        private val actionGeneImpacts: MutableList<ImpactsOfAction>,

        /**
         * a history of structures of [this] with best fitness
         */
        val impactsOfStructure: ActionStructureImpact = ActionStructureImpact("StructureSize"),


        private val maxSqlInitActionsPerMissingData: Int
) {

    constructor(individual: Individual, abstractInitializationGeneToMutate: Boolean, maxSqlInitActionsPerMissingData: Int, fitnessValue: FitnessValue?) : this(
            initializationGeneImpacts = InitializationActionImpacts(abstractInitializationGeneToMutate),//individual.seeInitializingActions().map { a -> ImpactsOfAction(a) }.toMutableList(),
            actionGeneImpacts = if (individual.seeActions().isEmpty()) mutableListOf(ImpactsOfAction(individual, individual.seeGenes())) else individual.seeActions().map { a -> ImpactsOfAction(a) }.toMutableList(),
            maxSqlInitActionsPerMissingData = maxSqlInitActionsPerMissingData
    ) {
        if (fitnessValue != null) {
            impactsOfStructure.updateStructure(individual, fitnessValue)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImpactsOfIndividual::class.java)
    }

    fun copy(): ImpactsOfIndividual {
        return ImpactsOfIndividual(
                initializationGeneImpacts.copy(),
                actionGeneImpacts.map { it.copy() }.toMutableList(),
                impactsOfStructure.copy(),
                maxSqlInitActionsPerMissingData
        )
    }

    fun clone(): ImpactsOfIndividual {
        return ImpactsOfIndividual(
                initializationGeneImpacts.clone(),
                actionGeneImpacts.map { it.clone() }.toMutableList(),
                impactsOfStructure.clone(),
                maxSqlInitActionsPerMissingData
        )
    }

    fun getSQLExistingData() = initializationGeneImpacts.getExistingData()


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

    fun syncBasedOnIndividual(individual: Individual, mutatedGene: MutatedGeneSpecification) {
        //for initialization due to db action fixing
        val diff = individual.seeInitializingActions().size - initializationGeneImpacts.getOriginalSize()//mutatedGene.addedExistingDataInitialization.size - initializationGeneImpacts.getOriginalSize()
        if (diff < 0) { //truncation
            initializationGeneImpacts.truncation(individual.seeInitializingActions())
        }else if (diff > 0){
            throw IllegalArgumentException("impact is out of sync")
        }

        //for action
        if ((individual.seeActions().isNotEmpty() && individual.seeActions().size != actionGeneImpacts.size) ||
                (individual.seeActions().isEmpty() && !noneActionIndividual()))
            throw IllegalArgumentException("inconsistent size of actions and impacts")

        individual.seeActions().forEach { action ->
            val actionName = action.getName()
            val index = individual.seeActions().indexOf(action)
            action.seeGenes().filter { !mutatedGene.allManipulatedGenes().contains(it) }.forEach { g ->
                val id = ImpactUtils.generateGeneId(action, g)
                if (getGene(actionName, id, index, false) == null) {
                    val impact = ImpactUtils.createGeneImpact(g, id)
                    actionGeneImpacts[index].addGeneImpact(actionName, impact)
                }
            }
        }
    }

    fun deleteActionGeneImpacts(actionIndex: Set<Int>): Boolean {
        if (actionIndex.isEmpty()) return false
        if (actionIndex.max()!! >= actionGeneImpacts.size)
            return false
        actionIndex.sortedDescending().forEach {
            actionGeneImpacts.removeAt(it)
        }
        return true
    }

    fun updateInitializationImpactsAtBeginning(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationGeneImpacts.updateInitializationImpactsAtBeginning(groupedActions, existingDataSize)
    }


    fun initInitializationImpacts(groupedActions: List<List<Action>>, existingDataSize: Int) {
        initializationGeneImpacts.initInitializationActions(groupedActions, existingDataSize)
    }

    fun updateInitializationGeneImpacts(other: ImpactsOfIndividual) {
        initializationGeneImpacts.initInitializationActions(other.initializationGeneImpacts)
    }

    fun updateExistingSQLData(size: Int) {
        initializationGeneImpacts.updateSizeOfExistingData(size)
    }

    fun addOrUpdateActionGeneImpacts(actionName: String?, actionIndex: Int, newAction: Boolean, impacts: MutableMap<String, GeneImpact>): Boolean {
        if (newAction) {
            if (actionIndex > actionGeneImpacts.size) return false
            actionGeneImpacts.add(actionIndex, ImpactsOfAction(actionName, impacts))
            return true
        }
        if (actionIndex >= actionGeneImpacts.size) return false
        return actionGeneImpacts[actionIndex].addGeneImpact(actionName, impacts)
    }

    fun anyImpactfulInfo(): Boolean {
        for (a in initializationGeneImpacts.getAll().plus(actionGeneImpacts)) {
            if (a.anyImpactfulInfo()) return true
        }
        return false
    }

    fun flattenAllGeneImpact(): List<GeneImpact> {
        return initializationGeneImpacts.getAll().plus(actionGeneImpacts).flatMap { it.geneImpacts.values }
    }

    fun getInitializationGeneImpact(): List<MutableMap<String, GeneImpact>> {
        return initializationGeneImpacts.getAll().map { it.geneImpacts }
    }

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

    fun getActionGeneImpact(): List<MutableMap<String, GeneImpact>> {
        return actionGeneImpacts.map { it.geneImpacts }
    }

    fun anyImpactInfo(): Boolean = initializationGeneImpacts.getSize() > 0 || actionGeneImpacts.isNotEmpty()

    private fun getImpactsAction(actionName: String?, actionIndex: Int): ImpactsOfAction {
        if (actionIndex >= actionGeneImpacts.size)
            throw IllegalArgumentException("exceed the boundary of impacts regarding actions, i.e., size of actions is ${actionGeneImpacts.size}, but asking index is $actionIndex")
        val actionImpacts = actionGeneImpacts[actionIndex]
        if (actionName != null && actionImpacts.actionName != actionName)
            throw IllegalArgumentException("mismatched action name, i.e., current is ${actionImpacts.actionName}, but $actionName")
        return actionImpacts
    }

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

    /**
     * @property actionName name of action if action exists, versus null
     * @property geneImpacts impact info of genes of the action or the individual (actionName == null)
     */
    private data class ImpactsOfAction(val actionName: String?, val geneImpacts: MutableMap<String, GeneImpact> = mutableMapOf()) {
        fun copy(): ImpactsOfAction {
            return ImpactsOfAction(actionName, geneImpacts.map { it.key to it.value.copy() }.toMap().toMutableMap())
        }

        fun clone(): ImpactsOfAction {
            return ImpactsOfAction(actionName, geneImpacts.map { it.key to it.value.clone() }.toMap().toMutableMap())
        }

        constructor(action: Action) : this(
                actionName = action.getName(),
                geneImpacts = action.seeGenes().map {
                    val id = ImpactUtils.generateGeneId(action, it)
                    id to ImpactUtils.createGeneImpact(it, id)
                }.toMap().toMutableMap())

        constructor(individual: Individual, genes: List<Gene>) : this(
                actionName = null,
                geneImpacts = genes.map {
                    val id = ImpactUtils.generateGeneId(individual, it)
                    id to ImpactUtils.createGeneImpact(it, id)
                }.toMap().toMutableMap()
        )

        constructor(actionName: String?, geneImpacts: List<GeneImpact>) : this(
                actionName = actionName,
                geneImpacts = geneImpacts.map { it.getId() to it }.toMap().toMutableMap()
        )

        /**
         * @return false mismatched action name
         */
        fun addGeneImpact(actionName: String?, geneImpact: GeneImpact, forceUpdate: Boolean = false): Boolean {
            val name = actionName ?: ImpactUtils.extractActionName(geneImpact.getId())
            if (name != actionName) return false

            if (forceUpdate && geneImpacts.containsKey(geneImpact.getId()))
                geneImpacts.replace(geneImpact.getId(), geneImpact)
            else
                geneImpacts.putIfAbsent(geneImpact.getId(), geneImpact)

            return true
        }

        /**
         * @return false mismatched action name
         */
        fun addGeneImpact(actionName: String?, geneImpact: MutableMap<String, GeneImpact>, forceUpdate: Boolean = false): Boolean {
            val mismatched = actionName != this.actionName || geneImpact.any { ImpactUtils.extractActionName(it.key) != this.actionName }
            if (mismatched) return false

            geneImpact.forEach { (t, u) ->
                if (forceUpdate && geneImpacts.containsKey(t))
                    geneImpacts.replace(t, u)
                else
                    geneImpacts.putIfAbsent(t, u)
            }
            return true
        }

        fun exists(geneId: String, actionName: String?): Boolean? {
            val name = actionName ?: ImpactUtils.extractActionName(geneId)
            if (name != actionName) return null
            return geneImpacts.containsKey(geneId)
        }

        fun get(geneId: String, actionName: String?): GeneImpact? {
            val name = actionName ?: ImpactUtils.extractActionName(geneId)
            if (name != actionName) throw IllegalArgumentException("mismatched action name, i.e., current is ${this.actionName}, but $actionName")
            return geneImpacts[geneId]
        }

        fun anyImpactfulInfo(): Boolean = geneImpacts.any { it.value.getTimesOfImpacts().any { i -> i.value > 0 } }

        fun getImpactfulTargets(): Set<Int> = geneImpacts.values.flatMap { it.getTimesOfImpacts().filter { i -> i.value > 0 }.keys }.toSet()

        fun getNoImpactTargets(): Set<Int> = geneImpacts.values.flatMap { it.getTimesOfNoImpactWithTargets().filter { i -> i.value > 0 }.keys }.toSet()

        fun isMissing(actionName: String?, geneId: String): Boolean? {
            val name = actionName ?: ImpactUtils.extractActionName(geneId)
            if (name != actionName) return null
            return !geneImpacts.containsKey(geneId)
        }
    }

    /**
     * impacts of actions for initialization of a test.
     * Currently, the initialization is composed of a sequence of SQL actions, and
     * there exist some duplicated sub-sequence.
     * @property abstract indicates whether extract the actions in order to identify unique sub-sequence.
     * @property enableImpactOnDuplicatedTimes indicates whether collect impacts on duplicated times.
     */
    private class InitializationActionImpacts(val abstract: Boolean, val enableImpactOnDuplicatedTimes: Boolean = false) : ImpactOfDuplicatedArtifact<ImpactsOfAction>() {

        /**
         * index conforms with completeSequence
         * first of pair is name of template
         * second of pair is index of actions in this template
         */
        val indexMap = mutableListOf<Pair<String, Int>>()

        private var existingSQLData = 0

        private fun getGroupedSequence() : List<List<ImpactsOfAction>>{
            if (indexMap.size != completeSequence.size)
                throw IllegalStateException("indexMap is out of sync ${indexMap.size} vs. ${completeSequence.size}")

            return getGroupedSequenceWithIndexMap(completeSequence)
        }

        private fun getGroupedSequenceWithIndexMap(list : List<ImpactsOfAction>) : List<List<ImpactsOfAction>>{
            if (indexMap.size < list.size)
                throw IllegalStateException("list contains more elements than IndexMap")

            val group = list.mapIndexed { index, impactsOfAction ->
                impactsOfAction to indexMap[index].second
            }.groupBy {
                it.second
            }
            val result = mutableListOf<List<ImpactsOfAction>>()

            group.forEach { (t, u) ->
                result.add(t, u.map { it.first })
            }
            return  result
        }


        private fun initPreCheck() {
            if (completeSequence.isNotEmpty() || template.isNotEmpty() || indexMap.isNotEmpty())
                throw IllegalStateException("duplicated initialization")
        }

        fun updateSizeOfExistingData(size : Int){
            existingSQLData = size
        }

        fun getExistingData() = existingSQLData

        /**
         * @param groupedActions should be insertions
         */
        fun initInitializationActions(groupedActions: List<List<Action>>, existingDataSize : Int) {
            initPreCheck()

            updateSizeOfExistingData(existingDataSize)

            /*
            there might exist duplicated db actions with EvoMaster that
                1) ensures that required resources are created; 2) support collection request, e.g., GET Collection
            However in a view of mutation, we only concern unique ones
            therefore we abstract dbactions in Initialization in order to identify unique dbactions,
            then further mutate values with abstracted ones, e.g.,
                a sequence of dbaction is abababc, then its abstraction is ab-c
            */
            groupedActions.forEach { t->
                val group = t.map { a-> ImpactsOfAction(a) }
                val key = generateTemplateKey(group.map { i-> i.actionName!! })
                completeSequence.addAll(group)
                t.forEachIndexed { i, _ ->
                    indexMap.add(Pair(key, i))
                }
                template.putIfAbsent(key, t.map { a-> ImpactsOfAction(a) })
                if (enableImpactOnDuplicatedTimes)
                    templateDuplicateTimes.putIfAbsent(key, Impact(id = key))
            }
        }

        fun updateInitializationImpactsAtBeginning(addedInsertions: List<List<Action>>, existingDataSize : Int){
            updateSizeOfExistingData(existingDataSize)

            val newCompleteSequence =  mutableListOf<ImpactsOfAction>()
            val newIndex = mutableListOf<Pair<String, Int>>()
            addedInsertions.forEach { t->
                val group = t.map { a-> ImpactsOfAction(a) }
                val key = generateTemplateKey(group.map { i-> i.actionName!! })
                newCompleteSequence.addAll(group)
                t.forEachIndexed { i, _ ->
                    newIndex.add(Pair(key, i))
                }
                template.putIfAbsent(key, t.map { a-> ImpactsOfAction(a) })
                if (enableImpactOnDuplicatedTimes)
                    templateDuplicateTimes.putIfAbsent(key, Impact(id = key))
            }
            indexMap.addAll(0, newIndex)
            completeSequence.addAll(0, newCompleteSequence)
        }

        fun initInitializationActions(impact: InitializationActionImpacts) {
            //initPreCheck()
            clone(impact)
        }

        /**
         * @param list actions after truncation
         */
        fun truncation(list: List<Action>) {
            val ignoreExisting = list.filterIsInstance<DbAction>().count{it.representExistingData }
            if (ignoreExisting != getExistingData()){
                log.warn("mismatched existing data")
            }

            val original = completeSequence.size
            val seq = list.filterIsInstance<DbAction>().filter{ !it.representExistingData }
            if (seq.size > original) {
                log.warn("there are more db actions after the truncation")
                return
            }
            if (seq.size == original) return

            val newCompleteSequence = seq.mapIndexed { index, db ->
                val name = db.getName()
                //FIXME Man: further check null case
                getImpactOfAction(name, index + ignoreExisting) ?: ImpactsOfAction(db)
            }

            completeSequence.clear()
            completeSequence.addAll(newCompleteSequence)

            if (!abstract) return


            val middle = indexMap[seq.size - 1].second == indexMap[seq.size].second
            if (middle){
                val starting = indexMap.indexOfFirst {
                    it.second == indexMap[seq.size].second
                }
                val newkey = generateTemplateKey(seq.subList(starting, seq.size).map { it.getName() })
                indexMap.removeAll(indexMap.subList(starting, original))
                (starting until seq.size).forEach {
                    indexMap.add(
                            newkey to it
                    )
                }
                template.putIfAbsent(newkey, newCompleteSequence.subList(starting, original))
                if (enableImpactOnDuplicatedTimes)
                    templateDuplicateTimes.putIfAbsent(newkey, Impact(id = newkey))
            }else{
                indexMap.removeAll(indexMap.subList(seq.size, original))
            }

            val extracted = indexMap.map { it.first }.toSet()
            template.filterKeys { !extracted.contains(it) }.keys.forEach {
                template.remove(it)
                templateDuplicateTimes.remove(it)
            }
        }

        private fun generateTemplateKey(actionNames: List<String>) = actionNames.joinToString("$$")

        fun getTemplateValue(group: List<Action>): List<ImpactsOfAction>? {
            return template[generateTemplateKey(group.map { it.getName() })]
        }

        fun copy(): InitializationActionImpacts {
            val new = InitializationActionImpacts(abstract, enableImpactOnDuplicatedTimes)
            new.completeSequence.addAll(completeSequence.map { it.copy() })
            new.template.putAll(template.mapValues { it.value.map { v -> v.copy() } })
            new.indexMap.addAll(indexMap.map { Pair(it.first, it.second) })
            if (enableImpactOnDuplicatedTimes)
                new.templateDuplicateTimes.putAll(templateDuplicateTimes.mapValues { it.value.copy() })

            new.existingSQLData = existingSQLData
            return new
        }


        fun clone(): InitializationActionImpacts {
            val new = InitializationActionImpacts(abstract, enableImpactOnDuplicatedTimes)
            new.completeSequence.addAll(completeSequence.map { it.clone() })
            new.template.putAll(template.mapValues { it.value.map { v -> v.clone() } })
            new.indexMap.addAll(indexMap.map { Pair(it.first, it.second) })

            if (enableImpactOnDuplicatedTimes)
                new.templateDuplicateTimes.putAll(templateDuplicateTimes.mapValues { it.value.clone() })

            new.existingSQLData = existingSQLData
            return new
        }

        /**
         * clone this based on [other]
         */
        fun clone(other: InitializationActionImpacts) {
            reset()
            existingSQLData = other.getExistingData()
            completeSequence.addAll(other.completeSequence.map { it.clone() })
            template.putAll(other.template.mapValues { it.value.map { v -> v.clone() } })
            indexMap.addAll(other.indexMap.map { Pair(it.first, it.second) })
            if (enableImpactOnDuplicatedTimes != other.enableImpactOnDuplicatedTimes)
                throw IllegalStateException("different setting on enableImpactOnDuplicatedTimes")
            if (enableImpactOnDuplicatedTimes)
                templateDuplicateTimes.putAll(other.templateDuplicateTimes.mapValues { it.value.clone() })
        }

        fun reset() {
            completeSequence.clear()
            template.clear()
            indexMap.clear()
            templateDuplicateTimes.clear()
            existingSQLData = 0
        }

        /**
         * @return impact of action by [actionName] or [actionIndex]
         * @param actionName is a name of action
         * @param actionIndex index of action in a test
         */
        fun getImpactOfAction(actionName: String?, actionIndex: Int): ImpactsOfAction? {
            val mIndex = actionIndex - existingSQLData
            if (mIndex >= completeSequence.size)
                throw IllegalArgumentException("exceed the boundary of impacts regarding actions, i.e., size of actions is ${completeSequence.size}, but asking index is $actionIndex")
            val name = completeSequence[mIndex].actionName
            if (actionName != null && name != actionName)
                throw IllegalArgumentException("mismatched action name, i.e., current is $name, but $actionName")
            if (!abstract) {
                return completeSequence[mIndex]
            }
            val templateInfo = if(indexMap.size > mIndex)
                indexMap[mIndex]
            else
                throw IllegalArgumentException()
            return template[templateInfo.first]?.get(templateInfo.second)
        }

        fun getOriginalSize(includeExistingSQLData : Boolean = true) = completeSequence.size + if (includeExistingSQLData) existingSQLData else 0

        fun getSize(): Int {
            if (abstract) return template.size
            return completeSequence.size
        }

        fun getFullSize() = getSize() + existingSQLData

        fun getAll(): List<ImpactsOfAction> {
            if (abstract) return template.values.flatten()
            return completeSequence
        }
    }

}