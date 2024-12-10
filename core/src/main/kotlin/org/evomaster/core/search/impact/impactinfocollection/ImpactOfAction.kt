package org.evomaster.core.search.impact.impactinfocollection

import org.evomaster.core.search.action.Action

/**
 * @property localId represent local id of the action
 * @property actionName name of action if action exists, versus null
 * @property geneImpacts impact info of genes of the action or the individual (actionName == null)
 */
data class ImpactsOfAction(
    val localId : String,
    val actionName: String?,
    val geneImpacts: MutableMap<String, GeneImpact> = mutableMapOf()
) {
    fun copy(): ImpactsOfAction {
        return ImpactsOfAction(localId, actionName, geneImpacts.map { it.key to it.value.copy() }.toMap().toMutableMap())
    }

    fun clone(): ImpactsOfAction {
        return ImpactsOfAction(localId, actionName, geneImpacts.map { it.key to it.value.clone() }.toMap().toMutableMap())
    }

    constructor(action: Action) : this(
            localId = action.getLocalId(),
            actionName = action.getName(),
            geneImpacts = action.seeTopGenes().map {
                val id = ImpactUtils.generateGeneId(action, it)
                id to ImpactUtils.createGeneImpact(it, id)
            }.toMap().toMutableMap())

//    constructor(individual: Individual, genes: List<Gene>) : this(
//            actionName = null,
//            geneImpacts = genes.map {
//                val id = ImpactUtils.generateGeneId(individual, it)
//                id to ImpactUtils.createGeneImpact(it, id)
//            }.toMap().toMutableMap()
//    )

    constructor(localId: String, actionName: String?, geneImpacts: List<GeneImpact>) : this(
            localId = localId,
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