package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.schema.Table
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.resource.dependency.MutualResourcesRelations
import org.evomaster.core.problem.rest.resource.dependency.ResourceRelatedToResources
import org.evomaster.core.problem.rest.resource.dependency.ResourceRelatedToTable
import org.evomaster.core.problem.rest.resource.dependency.SelfResourcesRelation
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.problem.rest.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.problem.rest.util.inference.model.ParamGeneBindMap
import org.evomaster.core.problem.util.StringSimilarityComparator
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Randomness
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.max


/**
 * this class is used to manage dependency among resources
 */
class ResourceDepManageService {

    @Inject
    private lateinit var rm: ResourceManageService

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    companion object{
        private const val DERIVE_RELATED = 1.0
    }
    /**
     * key is either a path of one resource, or a list of paths of resources
     * value is a list of related to resources
     */
    private val dependencies: MutableMap<String, MutableList<ResourceRelatedToResources>> = mutableMapOf()

    /**
     * key is a path of an resource
     * value is a set of resources that is not related to the key, i.e., the key does not rely on
     */
    private val uncorrelated: MutableMap<String, MutableSet<String>> = mutableMapOf()

    private val inference = SimpleDeriveResourceBinding()

    /************************  manage relationship between resource and tables ***********************************/

    /**
     * update relationship between resource and tables.
     * Note that the entry point is on the rest fitness.
     */
    fun updateResourceTables(resourceRestIndividual: RestIndividual, dto: TestResultsDto) {

        val tables = rm.getTableInfo()
        /*
        TODO how to decide to remove relationship between resource and table
         */
        val addedMap = mutableMapOf<String, MutableSet<String>>()
        val removedMap = mutableMapOf<String, MutableSet<String>>()

        resourceRestIndividual.seeActions().forEachIndexed { index, action ->
            if (action is RestAction && config.doesApplyNameMatching) updateParamInfo(action, tables)
            // size of extraHeuristics might be less than size of action due to failure of handling rest action
            if (index < dto.extraHeuristics.size) {
                val dbDto = dto.extraHeuristics[index].databaseExecutionDto
                if (action is RestCallAction && dbDto != null)
                    updateResourceToTable(action, dbDto, tables, addedMap, removedMap)
            }
        }
        if (addedMap.isNotEmpty() || removedMap.isNotEmpty())
            updateDependencyOnceResourceTableUpdate(addedMap, removedMap)

    }

    private fun updateParamInfo(action: RestAction, tables: Map<String, Table>) {
        if (action is RestCallAction) {
            val r = rm.getResourceNodeFromCluster(action.path.toString())
            val additionalInfo = r.updateAdditionalParams(action)
            if (!additionalInfo.isNullOrEmpty()) {
                inference.deriveParamsToTable(additionalInfo, r, allTables = tables)
            }
        }
    }

    /**
     * TODO remove false-derived dependencies based on feedback from evomaster driver
     */
    private fun updateDependencyOnceResourceTableUpdate(addedMap: MutableMap<String, MutableSet<String>>, removedMap: MutableMap<String, MutableSet<String>>) {

        val groupTable = addedMap.flatMap { it.value }.toHashSet()
        groupTable.forEach { table ->
            val newRelatedResource = addedMap.filter { it.value.contains(table) }.keys
            val previousResourcesWithTable = dependencies.values.flatMap { l -> l.filter { d->d is MutualResourcesRelations && d.referredTables.contains(table) }.flatMap { it.targets }}.toHashSet()

            var find = false
            dependencies.values.forEach { rlist ->
                rlist.forEach { mu ->
                    if (mu is MutualResourcesRelations && mu.targets.containsAll(newRelatedResource.plus(previousResourcesWithTable).toHashSet())) {
                        mu.referredTables.add(table)
                        find = true
                    }
                }
            }

            if (!find) {
                //update existing dependency with new related resources if it refers to the table
                val updateToAddNewResource = mutableMapOf<String, MutableList<MutualResourcesRelations>>()
                dependencies.forEach { (k, d) ->
                    d.filter { r -> r is MutualResourcesRelations && r.referredTables.contains(table) }.forEach { r->
                        updateToAddNewResource.getOrPut(k){ mutableListOf()}.add(r as MutualResourcesRelations)
                    }
                }
                updateToAddNewResource.forEach { (t, u) ->
                    dependencies.getValue(t).removeAll(u)
                    u.forEach { m->
                        val newTargets = m.targets.plus(newRelatedResource).toHashSet()
                        val newTables = m.referredTables.plus(table).toHashSet()
                        val newMut = MutualResourcesRelations(newTargets.toList(), DERIVE_RELATED, newTables)
                        dependencies.getValue(t).add(newMut)
                    }
                }

                //add new dependency for new RelatedResources with table
                newRelatedResource.forEach {nr->
                    dependencies.getOrPut(nr) { mutableListOf() }
                    val append = dependencies.getValue(nr).filter { it is MutualResourcesRelations && newRelatedResource.plus(previousResourcesWithTable).toHashSet().containsAll(it.targets) }
                    if (append.isNotEmpty()){
                        append.forEach { a->
                            dependencies.getValue(nr).remove(a)
                            val newTargets = a.targets.plus(newRelatedResource).plus(previousResourcesWithTable).toHashSet()
                            val newTables = (a as MutualResourcesRelations).referredTables.plus(table).toHashSet()
                            val newMut = MutualResourcesRelations(newTargets.toList(), DERIVE_RELATED, newTables)
                            dependencies.getValue(nr).add(newMut)
                        }
                    }else{
                        val newMut = MutualResourcesRelations(newRelatedResource.plus(previousResourcesWithTable).toHashSet().toList(), DERIVE_RELATED, mutableSetOf(table))
                        dependencies.getValue(nr).add(newMut)
                    }
                }
            }
        }
    }

    private fun updateResourceToTable(action: RestCallAction, updated: Map<String, MutableSet<String>>, matchedWithVerb: Boolean, tables: Map<String, Table>,
                                      addedMap: MutableMap<String, MutableSet<String>>, removedMap: MutableMap<String, MutableSet<String>>) {
        val ar = rm.getResourceNodeFromCluster(action.path.toString())
        val rToTable = ar.resourceToTable

        if (updated.isNotEmpty() && matchedWithVerb) {
            val derivedTables = rToTable.getTablesInDerivedMap()

            updated.forEach { (t, u) ->
                if (derivedTables.any { it.equals(t, ignoreCase = true) }) {
                    if (action.parameters.isNotEmpty() && u.isNotEmpty() && u.none { it == "*" }) {
                        action.parameters.forEach { p ->
                            val paramId = ar.getParamId(action.parameters, p)
                            ar.resourceToTable.paramToTable[paramId]?.let { paramToTable ->
                                paramToTable.getRelatedColumn(t)?.apply {
                                    paramToTable.confirmedColumn.addAll(this.intersect(u))
                                }
                            }
                        }
                    }
                } else {
                    val matchedInfo = ResourceRelatedToTable.generateFromDtoMatchedInfo(t.toLowerCase())
                    ar.resourceToTable.derivedMap.put(t, mutableListOf(matchedInfo))
                    action.parameters.forEach { p ->
                        val paramId = ar.getParamId(action.parameters, p)
                        val paramInfo = ar.paramsInfo[paramId].run {
                            if (this == null) ar.updateAdditionalParam(action, p).also {
                                inference.deriveParamsToTable(paramId, it, ar, tables)
                            } else this
                        }
                        // ?:throw IllegalArgumentException("cannot find the param Id $paramId in the rest resource ${referResource.getName()}")
                        val hasMatchedParam = inference.deriveRelatedTable(ar, paramId, paramInfo, mutableSetOf(t), p is BodyParam, -1, alltables = tables)
                        ar.resourceToTable.paramToTable[paramId]?.let { paramToTable ->
                            paramToTable.getRelatedColumn(t)?.apply {
                                paramToTable.confirmedColumn.addAll(this.intersect(u.filter { it != "*" }))
                            }
                        }
                    }

                    addedMap.getOrPut(ar.getName()) { mutableSetOf() }.add(t)

                }

                rToTable.confirmedSet.getOrPut(t) { true }
                rToTable.confirmedSet[t] = true
            }
        } else {
            updated.keys.forEach { t ->
                rToTable.confirmedSet.getOrPut(t) { false }
            }
        }
    }

    private fun updateResourceToTable(action: RestCallAction, dto: ExecutionDto, tables: Map<String, Table>,
                                      addedMap: MutableMap<String, MutableSet<String>>, removedMap: MutableMap<String, MutableSet<String>>) {

        dto.insertedData.filter { u -> tables.any { it.key.toLowerCase() == u.key } }.let { added ->
            updateResourceToTable(action, added, (action.verb == HttpVerb.POST || action.verb == HttpVerb.PUT), tables, addedMap, removedMap)
        }

        dto.updatedData.filter { u -> tables.any { it.key.toLowerCase() == u.key } }.let { updated ->
            updateResourceToTable(action, updated, (action.verb == HttpVerb.PATCH || action.verb == HttpVerb.PUT), tables, addedMap, removedMap)
        }

        dto.deletedData.filter { u -> tables.any { it.key.toLowerCase() == u } }.let { del ->
            updateResourceToTable(action, del.map { Pair(it, mutableSetOf<String>()) }.toMap(), (action.verb == HttpVerb.PATCH || action.verb == HttpVerb.PUT), tables, addedMap, removedMap)

        }
        dto.queriedData.filter { u -> tables.any { it.key.toLowerCase() == u.key } }.let { get ->
            updateResourceToTable(action, get, true, tables, addedMap, removedMap)
        }

        rm.getResourceNodeFromCluster(action.path.toString()).resourceToTable.updateActionRelatedToTable(action.verb.toString(), dto, tables.keys)
    }

    /************************  derive dependency using parser ***********************************/

    fun initDependencyBasedOnDerivedTables(resourceCluster: List<RestResourceNode>, tables: Map<String, Table>) {
        tables.keys.forEach { table ->
            val mutualResources = resourceCluster.filter { r -> r.getDerivedTables().any { e -> e.equals(table, ignoreCase = true) } }.map { it.getName() }.toList()
            if (mutualResources.isNotEmpty() && mutualResources.size > 1) {
                val mutualRelation = MutualResourcesRelations(mutualResources, StringSimilarityComparator.SimilarityThreshold, mutableSetOf(table))

                mutualResources.forEach { res ->
                    val relations = dependencies.getOrPut(res) { mutableListOf() }
                    relations.find { r -> r is MutualResourcesRelations && r.targets.containsAll(mutualRelation.targets) }.let {
                        if (it == null)
                            relations.add(mutualRelation)
                        else
                            (it as MutualResourcesRelations).referredTables.add(table.toLowerCase())
                    }
                }
            }
        }
    }

    /**
     * to derive dependency based on schema, i.e., description of each action if exists.
     *
     * If a description of a Post action includes some tokens (the token must be some "object") that is related to other rest action,
     * we create a "possible dependency" between the actions.
     */
    fun deriveDependencyBasedOnSchema(resourceCluster: List<RestResourceNode>) {
        resourceCluster
                .filter { it.actions.filter { it is RestCallAction && it.verb == HttpVerb.POST }.isNotEmpty() }
                .forEach { r ->
                    /*
                     TODO Man should only apply on POST Action? how about others?
                     */
                    val post = r.actions.first { it is RestCallAction && it.verb == HttpVerb.POST } as RestCallAction
                    post.tokens.forEach { _, u ->
                        resourceCluster.forEach { or ->
                            if (or != r) {
                                or.actions
                                        .filterIsInstance<RestCallAction>()
                                        .flatMap { it.tokens.values.filter { t -> t.fromDefinition && t.isDirect && t.isType } }
                                        .filter { ot ->
                                            StringSimilarityComparator.isSimilar(u.getKey(), ot.getKey())
                                        }.let {
                                            if (it.isNotEmpty()) {
                                                val addInfo = it.map { t -> t.getKey() }.joinToString(";")
                                                updateDependencies(or.getName(), mutableListOf(r.getName()), addInfo, StringSimilarityComparator.SimilarityThreshold)
                                                updateDependencies(r.getName(), mutableListOf(or.getName()), addInfo, StringSimilarityComparator.SimilarityThreshold)
                                            }
                                        }

                            }
                        }
                    }
                }
    }

    /************************  utility ***********************************/

    private fun compare(actionName: String, eviA: EvaluatedIndividual<RestIndividual>, eviB: EvaluatedIndividual<RestIndividual>): Int {
        val actionAs = mutableListOf<Int>()
        val actionBs = mutableListOf<Int>()
        eviA.individual.seeActions().forEachIndexed { index, action ->
            if (action.getName() == actionName)
                actionAs.add(index)
        }

        eviB.individual.seeActions().forEachIndexed { index, action ->
            if (action.getName() == actionName)
                actionBs.add(index)
        }

        return compare(actionAs, eviA, actionBs, eviB)
    }

    /**
     *  is the performance of [actionA] better than the performance [actionB]?
     */
    private fun compare(actionA: Int, eviA: EvaluatedIndividual<RestIndividual>, actionB: Int, eviB: EvaluatedIndividual<RestIndividual>): Int {
        return compare(mutableListOf(actionA), eviA, mutableListOf(actionB), eviB)
    }

    private fun compare(actionA: MutableList<Int>, eviA: EvaluatedIndividual<RestIndividual>, actionB: MutableList<Int>, eviB: EvaluatedIndividual<RestIndividual>): Int {
        val alistHeuristics = eviA.fitness.getViewOfData().filter { actionA.contains(it.value.actionIndex) }
        val blistHeuristics = eviB.fitness.getViewOfData().filter { actionB.contains(it.value.actionIndex) }

        //whether actionA reach more
        if (alistHeuristics.size > blistHeuristics.size) return 1
        else if (alistHeuristics.size < blistHeuristics.size) return -1

        //whether actionA reach new
        if (alistHeuristics.filter { !blistHeuristics.containsKey(it.key) }.isNotEmpty()) return 1
        else if (blistHeuristics.filter { !alistHeuristics.containsKey(it.key) }.isNotEmpty()) return -1

        val targets = alistHeuristics.keys.plus(blistHeuristics.keys).toHashSet()

        targets.forEach { t ->
            val ta = alistHeuristics[t]
            val tb = blistHeuristics[t]

            if (ta != null && tb != null) {
                if (ta.distance > tb.distance)
                    return 1
                else if (ta.distance < tb.distance)
                    return -1
            }
        }

        return 0
    }

    /**
     * update dependencies based on derived info
     * [additionalInfo] is structure mutator in this context
     */
    private fun updateDependencies(key: String, target: MutableList<String>, additionalInfo: String, probability: Double = DERIVE_RELATED) {

        val relation = if (target.size == 1 && target[0] == key) SelfResourcesRelation(key, probability, additionalInfo)
        else ResourceRelatedToResources(listOf(key), target, probability, info = additionalInfo)

        updateDependencies(relation, additionalInfo)
    }

    private fun updateDependencies(relation: ResourceRelatedToResources, additionalInfo: String) {
        val found = dependencies.getOrPut(relation.originalKey()) { mutableListOf() }.find { it.targets.containsAll(relation.targets) }
        if (found == null) dependencies[relation.originalKey()]!!.add(relation)
        else {
            /*
                TODO Man a strategy to manipulate the probability
             */
            found.probability = max(found.probability, relation.probability)
            if (found.additionalInfo.isBlank())
                found.additionalInfo = additionalInfo
            else if (!found.additionalInfo.contains(additionalInfo))
                found.additionalInfo += ";$additionalInfo"
        }
    }

    /**
     * return all calls of [ind] which are related to [call] with a probability that is more than [minProbability] and not more than [maxProbability]
     */
    private fun findDependentResources(ind: RestIndividual, call: RestResourceCalls, minProbability: Double = 0.0, maxProbability: Double = 1.0): MutableList<RestResourceCalls> {
        return ind.getResourceCalls().filter { other ->
            (other != call) && dependencies[call.getResourceNodeKey()]?.any { r -> r.targets.contains(other.getResourceNodeKey()) && r.probability >= minProbability && r.probability <= maxProbability } ?:false
        }.toMutableList()
    }

    private fun findNonDependentResources(ind: RestIndividual, call: RestResourceCalls): MutableList<RestResourceCalls> {
        return ind.getResourceCalls().filter { other ->
            (other != call) && uncorrelated[call.getResourceNodeKey()]?.contains(other.getResourceNodeKey()) ?: false
        }.toMutableList()
    }

    /**
     * [call] is related to any resource which exists in [ind] with a probability that is more than [minProbability] and not more than [maxProbability]
     */
    private fun existsDependentResources(ind: RestIndividual, call: RestResourceCalls, minProbability: Double = 0.0, maxProbability: Double = 1.0): Boolean {
        return ind.getResourceCalls().any { other ->
            (other != call) && dependencies[call.getResourceNodeKey()]?.any { r -> r.targets.contains(other.getResourceNodeKey()) && r.probability >= minProbability && r.probability <= maxProbability }?:false
        }
    }

    private fun isNonDepResources(ind: RestIndividual, call: RestResourceCalls): Boolean {
        return ind.getResourceCalls().find { other ->
            (other != call) && uncorrelated[other.getResourceNodeKey()]?.contains(call.getResourceNodeKey()) ?: false
        } != null
    }

    /************************  detect dependency based on fitness ***********************************/

    /**
     * detect possible dependencies by comparing a mutated (i.e., swap) individual with its previous regarding fitness
     */
    private fun detectAfterSwap(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()
        /*
        For instance, ABCDEFG, if we swap B and F, become AFCDEBG, then check FCDE (do not include B!).
        if F is worse, F may rely on {C, D, E, B}
        if C is worse, C rely on B; else if C is better, C rely on F; else C may not rely on B and F

        there is another case regarding duplicated resources calls (i.e., same resource and same actions) in a test,
        for instance, ABCDB*B**EF, swap B and F, become AFCDB*B**EB, in this case,
        B* probability become better, B** is same, B probability become worse
        */

        /**
         * collect elements is not in the same position
         */
        val swapsloc = mutableListOf<Int>()

        seqCur.forEachIndexed { index, restResourceCalls ->
            if (restResourceCalls.resourceInstance!!.getKey() != seqPre[index].resourceInstance!!.getKey())
                swapsloc.add(index)
        }
        if (swapsloc.size != 2) throw IllegalArgumentException("detect wrong mutator!")

        val swapF = seqCur.getOrNull(swapsloc[0])
                ?: throw IllegalArgumentException("detect wrong mutator!")
        val swapB = seqCur.getOrNull(swapsloc[1])
                ?: throw IllegalArgumentException("detect wrong mutator!")

        if (isBetter != 0) {
            val locOfF = swapsloc[0]
            val distance = swapF.actions.size - swapB.actions.size

            //check F
            val middles = seqCur.subList(swapsloc[0] + 1, swapsloc[1] + 1).map { it.getResourceNodeKey() }
            if (compare(swapsloc[0], current, swapsloc[1], previous) != 0) {
                middles.forEach {
                    updateDependencies(swapF.getResourceNodeKey(), mutableListOf(it), RestResourceStructureMutator.MutationType.SWAP.toString(), (1.0 / middles.size))
                }
            } else {
                uncorrelated.getOrPut(swapF.getResourceNodeKey()) { mutableSetOf() }.apply {
                    addAll(middles.toHashSet())
                }
            }

            //check FCDE
            var actionIndex = seqCur.mapIndexed { index, restResourceCalls ->
                if (index <= locOfF) restResourceCalls.actions.size
                else 0
            }.sum()

            ((locOfF + 1) until swapsloc[1]).forEach { indexOfCalls ->
                var isAnyChange = false
                var changeDegree = 0

                seqCur[indexOfCalls].actions.forEach { curAction ->
                    val actionA = actionIndex - distance

                    val compareResult = swapF.actions.plus(swapB.actions).find { it.getName() == curAction.getName() }.run {
                        if (this == null) compare(actionIndex, current, actionA, previous)
                        else compare(this.getName(), current, previous)
                    }.also { r -> changeDegree += r }

                    isAnyChange = isAnyChange || compareResult != 0
                    actionIndex += 1
                    //isAnyChange = isAnyChange || compare(actionA, current, actionIndex, previous).also { r-> changeDegree += r } !=0
                }

                val seqKey = seqCur[indexOfCalls].getResourceNodeKey()
                if (isAnyChange) {

                    val relyOn = if (changeDegree > 0) {
                        mutableListOf(swapF.getResourceNodeKey())
                    } else if (changeDegree < 0) {
                        mutableListOf(swapB.getResourceNodeKey())
                    } else
                        mutableListOf(swapB.getResourceNodeKey(), swapF.getResourceNodeKey())

                    updateDependencies(seqKey, relyOn, RestResourceStructureMutator.MutationType.SWAP.toString())
                } else {
                    uncorrelated.getOrPut(seqKey) { mutableSetOf() }.apply {
                        add(swapB.getResourceNodeKey())
                        add(swapF.getResourceNodeKey())
                    }
                }
            }

            val before = seqCur.subList(swapsloc[0], swapsloc[1]).map { it.getResourceNodeKey() }
            if (compare(swapsloc[1], current, swapsloc[0], previous) != 0) {
                middles.forEach {
                    updateDependencies(swapB.getResourceNodeKey(), mutableListOf(it), RestResourceStructureMutator.MutationType.SWAP.toString(), (1.0 / before.size))
                }
            } else {
                uncorrelated.getOrPut(swapB.getResourceNodeKey()) { mutableSetOf() }.addAll(before)
            }

            //TODO check G, a bit complicated,

        } else {
            /*
                For instance, ABCDEFG, if we swap B and F, become AFCDEBG.
                if there is no any impact on fitness,
                    1) it probably means {C,D,E} does not rely on B and F
                    2) F does not rely on {C, D, E}
                    3) F does not rely on B
             */
            val middles = seqCur.subList(swapsloc[0] + 1, swapsloc[1] + 1).map { it.getResourceNodeKey() }
            middles.forEach { c ->
                uncorrelated.getOrPut(c) { mutableSetOf() }.apply {
                    add(swapB.getResourceNodeKey())
                    add(swapF.getResourceNodeKey())
                }
                uncorrelated.getOrPut(swapF.getResourceNodeKey()) { mutableSetOf() }.add(c)
            }
            uncorrelated.getOrPut(swapF.getResourceNodeKey()) { mutableSetOf() }.add(swapB.getResourceNodeKey())
        }
    }

    /**
     * detect possible dependencies by comparing a mutated (i.e., modify) individual with its previous regarding fitness
     */
    private fun detectAfterModify(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()

        /*
            For instance, ABCDEFG, if we replace B with another resource instance, then check CDEFG.
            if C is worse/better, C rely on B, else C may not rely on B, i.e., the changes of B cannot affect C.
         */
        if (isBetter != 0) {
            val locOfModified = (0 until seqCur.size).find { seqPre[it].template!!.template != seqCur[it].template!!.template }
                    ?: return
            //throw IllegalArgumentException("mutator does not change anything.")

            val modified = seqCur[locOfModified]
            val distance = seqCur[locOfModified].actions.size - seqPre[locOfModified].actions.size

            var actionIndex = seqCur.mapIndexed { index, restResourceCalls ->
                if (index <= locOfModified) restResourceCalls.actions.size
                else 0
            }.sum()

            ((locOfModified + 1) until seqCur.size).forEach { indexOfCalls ->
                var isAnyChange = false
                seqCur[indexOfCalls].actions.forEach {
                    val actionA = actionIndex - distance
                    isAnyChange = isAnyChange || compare(actionIndex, current, actionA, previous) != 0
                    actionIndex += 1
                }

                if (isAnyChange) {
                    val seqKey = seqCur[indexOfCalls].getResourceNodeKey()
                    updateDependencies(seqKey, mutableListOf(modified.getResourceNodeKey()), RestResourceStructureMutator.MutationType.MODIFY.toString())
                }
            }
        }
    }
    /**
     * detect possible dependencies by comparing a mutated (i.e., replace) individual with its previous regarding fitness
     */
    private fun detectAfterReplace(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()

        /*
            For instance, ABCDEFG, if we replace B with H become AHCDEFG, then check CDEFG.
            if C is worse, C rely on B; else if C is better, C rely on H; else C may not rely on B and H

         */

        val mutatedIndex = (0 until seqCur.size).find { seqCur[it].resourceInstance!!.getKey() != seqPre[it].resourceInstance!!.getKey() }!!

        val replaced = seqCur[mutatedIndex]
        val replace = seqPre[mutatedIndex]

        if (isBetter != 0) {
            val locOfReplaced = seqCur.indexOf(replaced)
            val distance = locOfReplaced - seqPre.indexOf(replace)

            var actionIndex = seqCur.mapIndexed { index, restResourceCalls ->
                if (index <= locOfReplaced) restResourceCalls.actions.size
                else 0
            }.sum()

            ((locOfReplaced + 1) until seqCur.size).forEach { indexOfCalls ->
                var isAnyChange = false
                var changeDegree = 0
                seqCur[indexOfCalls].actions.forEach { curAction ->
                    val actionA = actionIndex - distance

                    val compareResult = replaced.actions.plus(replace.actions).find { it.getName() == curAction.getName() }.run {
                        if (this == null) compare(actionIndex, current, actionA, previous)
                        else compare(this.getName(), current, previous)
                    }.also { r -> changeDegree += r }

                    isAnyChange = isAnyChange || compareResult != 0
                    actionIndex += 1

                    //isAnyChange = isAnyChange || compare(actionA, current, actionIndex, previous).also { r-> changeDegree += r } !=0
                }

                val seqKey = seqCur[indexOfCalls].getResourceNodeKey()
                if (isAnyChange) {

                    val relyOn = if (changeDegree > 0) {
                        mutableListOf(replaced.getResourceNodeKey())
                    } else if (changeDegree < 0) {
                        mutableListOf(replace.getResourceNodeKey())
                    } else
                        mutableListOf(replaced.getResourceNodeKey(), replace.getResourceNodeKey())

                    updateDependencies(seqKey, relyOn, RestResourceStructureMutator.MutationType.REPLACE.toString())
                } else {
                    uncorrelated.getOrPut(seqKey) { mutableSetOf() }.apply {
                        add(replaced.getResourceNodeKey())
                        add(replace.getResourceNodeKey())
                    }
                }
            }

        } else {
            /*
            For instance, ABCDEFG, if we replace B with H become AHCDEFG, then check CDEFG.
            if there is no any impact on fitness, it probably means {C, D, E, F, G} does not rely on B and H
            */
            ((mutatedIndex + 1) until seqCur.size).forEach {
                val non = seqCur[it].getResourceNodeKey()
                uncorrelated.getOrPut(non) { mutableSetOf() }.apply {
                    add(replaced.getResourceNodeKey())
                    add(replace.getResourceNodeKey())
                }
            }
        }
    }
    /**
     * detect possible dependencies by comparing a mutated (i.e., add) individual with its previous regarding fitness
     */
    private fun detectAfterAdd(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()

        /*
             For instance, ABCDEFG, if we add H at 3nd position, become ABHCDEFG, then check CDEFG.
             if C is better, C rely on H; else if C is worse, C rely on H ? ;else C may not rely on H
        */
        val added = seqCur.find { cur -> seqPre.find { pre -> pre.resourceInstance!!.getKey() == cur.resourceInstance!!.getKey() } == null }
                ?: return
        val addedKey = added.getResourceNodeKey()

        val locOfAdded = seqCur.indexOf(added)

        if (isBetter != 0) {
            var actionIndex = seqCur.mapIndexed { index, restResourceCalls ->
                if (index <= locOfAdded) restResourceCalls.actions.size
                else 0
            }.sum()

            val distance = added.actions.size

            (locOfAdded + 1 until seqCur.size).forEach { indexOfCalls ->
                var isAnyChange = false

                seqCur[indexOfCalls].actions.forEach { curAction ->
                    var actionA = actionIndex - distance
                    val compareResult = added.actions.find { it.getName() == curAction.getName() }.run {
                        if (this == null) compare(actionIndex, current, actionA, previous)
                        else compare(this.getName(), current, previous)
                    }

                    isAnyChange = isAnyChange || compareResult != 0
                    actionIndex += 1 //actionB
                }
                val seqKey = seqCur[indexOfCalls].getResourceNodeKey()
                if (isAnyChange) {
                    updateDependencies(seqKey, mutableListOf(addedKey), RestResourceStructureMutator.MutationType.ADD.toString())
                } else {
                    uncorrelated.getOrPut(seqKey) { mutableSetOf() }.add(addedKey)
                }
            }

        } else {
            /*
            For instance, ABCDEFG, if we add H at 3nd position, become ABHCDEFG.
            if there is no any impact on fitness, it probably means {C, D, E, F, G} does not rely on H
             */
            (locOfAdded + 1 until seqCur.size).forEach {
                val non = seqCur[it].getResourceNodeKey()
                uncorrelated.getOrPut(non) { mutableSetOf() }.add(addedKey)
            }
        }
    }

    /**
     * detect possible dependencies by comparing a mutated (i.e., delete) individual with its previous regarding fitness
     */
    private fun detectAfterDelete(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()

        /*
         For instance, ABCDEFG, if B is deleted, become ACDEFG, then check CDEFG.
         if C is worse, C rely on B;
            else if C is better, C rely one B ?;
            else C may not rely on B.

         there is another case regarding duplicated resources calls (i.e., same resource and same actions) in a test, for instance, ABCB* (B* denotes the 2nd B), if B is deleted, become ACB*, then check CB* as before,
         when comparing B*, B* probability achieves better performance by taking target from previous first B, so we need to compare with merged targets, i.e., B and B*.
        */
        val delete = seqPre.find { pre -> seqCur.find { cur -> pre.resourceInstance!!.getKey() == cur.resourceInstance!!.getKey() } == null }
                ?: return
        val deleteKey = delete.getResourceNodeKey()

        val locOfDelete = seqPre.indexOf(delete)

        if (isBetter != 0) {

            var actionIndex = seqPre.mapIndexed { index, restResourceCalls ->
                if (index < locOfDelete) restResourceCalls.actions.size
                else 0
            }.sum()

            val distance = 0 - delete.actions.size

            (locOfDelete until seqCur.size).forEach { indexOfCalls ->
                var isAnyChange = false

                seqCur[indexOfCalls].actions.forEach { curAction ->
                    val actionA = actionIndex - distance

                    val compareResult = delete.actions.find { it.getName() == curAction.getName() }.run {
                        if (this == null) compare(actionIndex, current, actionA, previous)
                        else compare(this.getName(), current, previous)
                    }

                    isAnyChange = isAnyChange || compareResult != 0
                    actionIndex += 1 //actionB
                }

                val seqKey = seqCur[indexOfCalls].getResourceNodeKey()
                if (isAnyChange) {
                    updateDependencies(seqKey, mutableListOf(deleteKey), RestResourceStructureMutator.MutationType.DELETE.toString())
                } else {
                    uncorrelated.getOrPut(seqKey) { mutableSetOf() }.add(deleteKey)
                }
            }
        } else {
            /*
              For instance, ABCDEFG, if B is deleted, become ACDEFG, then check CDEFG.
              if there is no impact on fitness, it probably means {C, D, E, F, G} does not rely on B
             */
            (locOfDelete until seqCur.size).forEach {
                val non = seqCur[it].getResourceNodeKey()
                uncorrelated.getOrPut(non) { mutableSetOf() }.add(deleteKey)
            }

        }
    }

    /**
     * detect possible dependency among resources,
     * the entry is structure mutation
     *
     * [isBetter] 1 means current is better than previous, 0 means that they are equal, and -1 means current is worse than previous
     */
    fun detectDependencyAfterStructureMutation(previous: EvaluatedIndividual<RestIndividual>, current: EvaluatedIndividual<RestIndividual>, isBetter: Int) {
        val seqPre = previous.individual.getResourceCalls()
        val seqCur = current.individual.getResourceCalls()

        when (seqCur.size - seqPre.size) {
            0 -> {
                if (seqPre.map { it.getResourceNodeKey() }.toList() == seqCur.map { it.getResourceNodeKey() }.toList()) {
                    //Modify
                    detectAfterModify(previous, current, isBetter)
                } else if (seqCur.size > 1
                        && seqCur
                                .filterIndexed { index, restResourceCalls -> restResourceCalls.resourceInstance!!.getKey() != seqPre[index].resourceInstance!!.getKey() }.size == 2) {
                    //SWAP
                    detectAfterSwap(previous, current, isBetter)
                } else {
                    //REPLACE
                    detectAfterReplace(previous, current, isBetter)
                }
            }
            1 -> detectAfterAdd(previous, current, isBetter)
            -1 -> detectAfterDelete(previous, current, isBetter)
            else -> {
                throw IllegalArgumentException("apply undefined structure mutator that changed the size of resources from ${seqPre.size} to ${seqCur.size}")
            }
        }

    }

    fun isDependencyNotEmpty(): Boolean {
        return dependencies.isNotEmpty()
    }

    /************************  manage to resource call regarding dependency ***********************************/

    /**
     * handle to select an resource call for adding in [ind], and the resource should (probably) depend on one of resource in [ind].
     * @return pair, first is an existing resource call in [ind], and second is a newly created resource call that is related to the first
     *         null, all resources are checked, and none of resource is related to other
     */
    fun handleAddDepResource(ind: RestIndividual, maxTestSize: Int, candidates: MutableList<RestResourceCalls> = mutableListOf()): Pair<RestResourceCalls?, RestResourceCalls>? {
        val options = mutableListOf(0, 1)
        while (options.isNotEmpty()) {
            val option = randomness.choose(options)
            val pair = when (option) {
                0 -> handleAddNewDepResource(if (candidates.isEmpty()) ind.getResourceCalls().toMutableList() else candidates, maxTestSize)
                1 -> {
                    handleAddNotCheckedDepResource(ind, maxTestSize)?:handleAddNewDepResource(if (candidates.isEmpty()) ind.getResourceCalls().toMutableList() else candidates, maxTestSize)
                }
                else -> null
            }
            if (pair != null) return pair
            options.remove(option)
        }
        return null
    }

    /**
     * @return pair, first is an existing resource call in [sequence], and second is a newly created resource call that is related to the first
     */
    private fun handleAddNewDepResource(sequence: MutableList<RestResourceCalls>, maxTestSize: Int): Pair<RestResourceCalls?, RestResourceCalls>? {

        val existingRs = sequence.map { it.getResourceNodeKey() }

        val candidates = sequence
                .filter {
                    dependencies[it.getResourceNodeKey()] != null &&
                            dependencies[it.getResourceNodeKey()]!!.any { dep ->
                                dep.targets.any { t -> existingRs.none { e -> e == t } } ||
                                        (dep is SelfResourcesRelation && existingRs.count { e -> e == it.getResourceNodeKey() } == 1)
                            }
                }

        if (candidates.isNotEmpty()) {
            val first = randomness.choose(candidates)
            /*
                add self relation with a relative low probability, i.e., 20%
             */
            dependencies[first.getResourceNodeKey()]!!.flatMap { dep ->
                if (dep !is SelfResourcesRelation)
                    dep.getDependentResources(first.getResourceNodeKey(), exclude = existingRs)
                else if (randomness.nextBoolean(0.2)) dep.targets else mutableListOf()
            }.let { templates ->
                if (templates.isNotEmpty()) {
                    rm.getResourceNodeFromCluster(randomness.choose(templates)).sampleAnyRestResourceCalls(randomness, maxTestSize, prioriDependent = true).let { second ->
                        return Pair(first, second)
                    }
                }
            }
        }
        return null
    }

    private fun handleAddNotCheckedDepResource(ind: RestIndividual, maxTestSize: Int): Pair<RestResourceCalls?, RestResourceCalls>? {
        val checked = ind.getResourceCalls().flatMap { cur ->
            findDependentResources(ind, cur).plus(findNonDependentResources(ind, cur))
        }.map { it.getResourceNodeKey() }.toHashSet()

        rm.getResourceCluster().keys.filter { !checked.contains(it) }.let { keys ->
            if (keys.isNotEmpty()) {
                rm.getResourceNodeFromCluster(randomness.choose(keys)).sampleAnyRestResourceCalls(randomness, maxTestSize, prioriDependent = true).let { second ->
                    return Pair(null, second)
                }
            }
        }
        return null
    }

    /**
     * handle to select an non-dependent resource for deletion
     * @return a candidate, if none of a resource is deletable, return null
     */
    fun handleDelNonDepResource(ind: RestIndividual): RestResourceCalls? {
        val candidates = ind.getResourceCalls().filter { cur ->
            !existsDependentResources(ind, cur) && cur.isDeletable
        }
        if (candidates.isEmpty()) return null

        candidates.filter { isNonDepResources(ind, it) }.apply {
            if (isNotEmpty())
                return randomness.choose(this)
            else
                return randomness.choose(candidates)
        }
    }

    /**
     * handle to select two related resources for swap
     * @return a pair of position to swap, if none of a pair resource is movable, return null
     */
    fun handleSwapDepResource(ind: RestIndividual): Pair<Int, Int>? {
        val options = mutableListOf(1, 2, 3)
        while (options.isNotEmpty()) {
            val option = randomness.choose(options)
            val pair = when (option) {
                1 -> adjustDepResource(ind)
                2 -> {
                    swapNotConfirmedDepResource(ind)?:adjustDepResource(ind)
                }
                3 -> {
                    swapNotCheckedResource(ind)?:adjustDepResource(ind)
                }
                else -> null
            }
            if (pair != null) return pair
            options.remove(option)
        }
        return null
    }

    private fun adjustDepResource(ind: RestIndividual): Pair<Int, Int>? {
        val candidates = mutableMapOf<Int, MutableSet<Int>>()
        ind.getResourceCalls().forEachIndexed { index, cur ->
            findDependentResources(ind, cur, minProbability = StringSimilarityComparator.SimilarityThreshold).map { ind.getResourceCalls().indexOf(it) }.filter { second -> index < second }.apply {
                if (isNotEmpty()) candidates.getOrPut(index) { mutableSetOf() }.addAll(this.toHashSet())
            }
        }
        if (candidates.isNotEmpty()) randomness.choose(candidates.keys).let {
            return Pair(it, randomness.choose(candidates.getValue(it)))
        }
        return null
    }

    private fun swapNotConfirmedDepResource(ind: RestIndividual): Pair<Int, Int>? {
        val probCandidates = ind.getResourceCalls().filter { existsDependentResources(ind, it, maxProbability = StringSimilarityComparator.SimilarityThreshold) }
        if (probCandidates.isEmpty()) return null
        val first = randomness.choose(probCandidates)
        val second = randomness.choose(findDependentResources(ind, first, maxProbability = StringSimilarityComparator.SimilarityThreshold))
        return Pair(ind.getResourceCalls().indexOf(first), ind.getResourceCalls().indexOf(second))
    }

    private fun swapNotCheckedResource(ind: RestIndividual): Pair<Int, Int>? {
        val candidates = mutableMapOf<Int, MutableSet<Int>>()
        ind.getResourceCalls().forEachIndexed { index, cur ->
            val checked = findDependentResources(ind, cur).plus(findNonDependentResources(ind, cur))
            ind.getResourceCalls().filter { it != cur && !checked.contains(it) }.map { ind.getResourceCalls().indexOf(it) }.apply {
                if (isNotEmpty()) candidates.getOrPut(index) { mutableSetOf() }.addAll(this)
            }
        }
        if (candidates.isNotEmpty()) randomness.choose(candidates.keys).let {
            return Pair(it, randomness.choose(candidates.getValue(it)))
        }
        return null
    }

    /************************  sample resource individual regarding dependency ***********************************/
    /**
     * sample an individual which contains related resources
     */
    fun sampleRelatedResources(calls: MutableList<RestResourceCalls>, sizeOfResource: Int, maxSize: Int) {
        var start = -calls.sumBy { it.actions.size }

        val first = randomness.choose(dependencies.keys)
        rm.sampleCall(first, true, calls, maxSize)
        var size = calls.sumBy { it.actions.size } + start
        val excluded = mutableListOf<String>()
        val relatedResources = mutableListOf<RestResourceCalls>()
        excluded.add(first)
        relatedResources.add(calls.last())

        while (relatedResources.size < sizeOfResource && size < maxSize) {
            val notRelated = rm.getResourceCluster().keys.filter { r-> (dependencies[first]?.none { t -> t.targets.contains(r)}?:true) && !excluded.contains(r) }
            val candidates = dependencies[first]!!.flatMap { it.getDependentResources(first, exclude = excluded) }
            /*
                if there is no valid candidate, prefer not related resource
             */
            val related = if (candidates.isNotEmpty()) randomness.choose(candidates) else if(notRelated.isNotEmpty()) randomness.choose(notRelated) else break

            excluded.add(related)
            rm.sampleCall(related, true, calls, size, false, if (related.isEmpty()) null else relatedResources)
            relatedResources.add(calls.last())
            size = calls.sumBy { it.actions.size } + start
        }
    }

    /**************************************** apply parser to derive ************************************************************************/

    /**
     * check if the [call] has related tables
     */
    fun checkIfDeriveTable(call: RestResourceCalls): Boolean {
        if (!call.template!!.independent) return false

        call.actions.first().apply {
            if (this is RestCallAction) {
                if (this.parameters.isNotEmpty()) return true
            }
        }
        return false
    }


    /**
     * init related tables for all resources
     */
    fun initRelatedTables(resourceCluster: MutableList<RestResourceNode>, tables: Map<String, Table>) {
        resourceCluster.forEach {
            inference.deriveResourceToTable(it, tables)
        }
    }
    /**
     * @return extracted related tables for [call] regarding [dbActions]
     * if [dbActions] is not empty, return related table from tables in [dbActions]
     * if [dbActions] is empty, return all derived related table
     */
    fun extractRelatedTablesForCall(call: RestResourceCalls, dbActions: MutableList<DbAction> = mutableListOf()) = inference.generateRelatedTables(call, dbActions)

    /**
     * bind values between [call] and [dbActions]
     * [candidates] presents how to map [call] and [dbActions] at Gene-level
     * [forceBindParamBasedOnDB] is an option to bind params of [call] based on [dbActions]
     */
    fun bindCallWithDBAction(
            call: RestResourceCalls,
            dbActions: MutableList<DbAction>,
            candidates: MutableMap<RestAction, MutableList<ParamGeneBindMap>>,
            forceBindParamBasedOnDB: Boolean = false) {

        assert(call.actions.isNotEmpty())

        for (a in call.actions) {
            if (a is RestCallAction) {
                var list = candidates[a]
                if (list == null) list = candidates.filter { a.getName() == it.key.getName() }.values.run {
                    if (this.isEmpty()) null else this.first()
                }
                if (list != null && list.isNotEmpty()) {
                    list.forEach { pToGene ->
                        var dbAction = dbActions.find { it.table.name.toLowerCase() == pToGene.tableName.toLowerCase() }
                                ?: throw IllegalArgumentException("cannot find ${pToGene.tableName} in db actions ${dbActions.map { it.table.name }.joinToString(";")}")
                        var columngene = dbAction.seeGenes().first { g -> g.name.toLowerCase() == pToGene.column.toLowerCase() }
                        val param = a.parameters.find { p -> rm.getResourceCluster()[a.path.toString()]!!.getParamId(a.parameters, p).toLowerCase() == pToGene.paramId.toLowerCase() }
                        param?.let {
                            if (pToGene.isElementOfParam) {
                                if (param is BodyParam && param.gene is ObjectGene) {
                                    param.gene.fields.find { f -> f.name == pToGene.targetToBind }?.let { paramGene ->
                                        ParamUtil.bindParamWithDbAction(columngene, paramGene, forceBindParamBasedOnDB || dbAction.representExistingData)
                                    }
                                }
                            } else {
                                ParamUtil.bindParamWithDbAction(columngene, param.gene, forceBindParamBasedOnDB || dbAction.representExistingData)
                            }
                        }

                    }
                }
            }
        }
    }

    private fun bindCallWithOtherDBAction(call: RestResourceCalls, dbActions: MutableList<DbAction>) {
        val dbRelatedToTables = dbActions.map { it.table.name }.toMutableList()
        val dbTables = call.dbActions.map { it.table.name }.toMutableList()

        if (dbRelatedToTables.containsAll(dbTables)) {
            call.dbActions.clear()
        } else {
            call.dbActions.removeIf { dbRelatedToTables.contains(it.table.name) }
            /*
             TODO Man there may need to add selection in order to ensure the reference pk exists
             */
            //val selections = mutableListOf<DbAction>()
            val previous = mutableListOf<DbAction>()
            val created = mutableListOf<DbAction>()
            call.dbActions.forEach { dbaction ->
                if (dbaction.table.foreignKeys.find { dbRelatedToTables.contains(it.targetTable) } != null) {
                    val refers = DbActionUtils.repairFK(dbaction, dbActions.plus(previous).toMutableList(), created, rm.getSqlBuilder())
                    //selections.addAll( (sampler as ResourceRestSampler).sqlInsertBuilder!!.generateSelect(refers) )
                }
                previous.add(dbaction)
            }
            call.dbActions.addAll(0, created)
            rm.repairDbActions(dbActions.plus(call.dbActions).toMutableList())
            //call.dbActions.addAll(0, selections)
        }

        val dbActions = dbActions.plus(call.dbActions).toMutableList()
        inference.generateRelatedTables(call, dbActions).let {
            bindCallWithDBAction(call, dbActions, it, forceBindParamBasedOnDB = true)
        }


    }

    /**
     * bind a call based on its front including resource call and dbactions
     */
    fun bindCallWithFront(call: RestResourceCalls, front: MutableList<RestResourceCalls>) {

        val targets = front.flatMap { it.actions.filter { a -> a is RestCallAction } }

        /*
        TODO

         e.g., A/{a}, A/{a}/B/{b}, A/{a}/C/{c}
         if there are A/{a} and A/{a}/B/{b} that exists in the test,
         1) when appending A/{a}/C/{c}, A/{a} should not be created again;
         2) But when appending A/{a} in the test, A/{a} with new values should be created.
        */
//        if(call.actions.size > 1){
//            call.actions.removeIf {action->
//                action is RestCallAction &&
//                        //(action.verb == HttpVerb.POST || action.verb == HttpVerb.PUT) &&
//                        action.verb == HttpVerb.POST &&
//                        action != call.actions.last() &&
//                        targets.find {it is RestCallAction && it.getName() == action.getName()}.also {
//                            it?.let {ra->
//                                front.find { call-> call.actions.contains(ra) }?.let { call -> call.isStructureMutable = false }
//                                if(action.saveLocation) (ra as RestCallAction).saveLocation = true
//                                action.locationId?.let {
//                                    (ra as RestCallAction).saveLocation = action.saveLocation
//                                }
//                            }
//                        }!=null
//            }
//        }

        /*
         bind values based front actions,
         */
        call.actions
                .filter { it is RestCallAction }
                .forEach { a ->
                    (a as RestCallAction).parameters.forEach { p ->
                        targets.forEach { ta ->
                            ParamUtil.bindParam(p, a.path, (ta as RestCallAction).path, ta.parameters)
                        }
                    }
                }

        /*
         bind values of dbactions based front dbactions
         */
        front.flatMap { it.dbActions }.apply {
            if (isNotEmpty()) {
                bindCallWithOtherDBAction(call, this.toMutableList())

            }
        }

        val frontTables = front.map { Pair(it, it.dbActions.map { it.table.name }) }.toMap()
        call.dbActions.forEach { db ->
            db.table.foreignKeys.map { it.targetTable }.let { ftables ->
                frontTables.filter { entry ->
                    entry.value.intersect(ftables).isNotEmpty()
                }.forEach { t, _ ->
                    t.isDeletable = false
                    t.shouldBefore.add(call.getResourceNodeKey())
                }
            }
        }
    }


    /**
     * @return whether all resources in SUT are independent
     */
    fun onlyIndependentResource(): Boolean {
        return rm.getResourceCluster().values.filter { r -> !r.isIndependent() }.isEmpty()
    }

    fun getRelatedResource(resource : String) : Set<String> = dependencies[resource]?.flatMap { it.targets }?.toSet()?: setOf()


    fun exportDependencies(){
        val path = Paths.get(config.dependencyFile)
        Files.createDirectories(path.parent)

        if (dependencies.isNotEmpty()){
            val header = mutableListOf("key").plus(dependencies.values.first().first().toCSVHeader()).joinToString(",")
            val content = mutableListOf<String>()
            dependencies.forEach { (t, u) ->
                u.forEachIndexed { index, resourceRelatedToResources ->
                    val row = mutableListOf(if (index == 0) t else "").plus(resourceRelatedToResources.exportCSV())
                    content.add(row.joinToString(","))
                }
            }
            Files.write(path, listOf(header).plus(content))
        }
    }
}