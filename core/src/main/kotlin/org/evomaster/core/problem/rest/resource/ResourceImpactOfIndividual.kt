package org.evomaster.core.problem.rest.resource

import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.impact.impactinfocollection.ActionStructureImpact
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfAction
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.search.impact.impactinfocollection.InitializationActionImpacts
import org.evomaster.core.search.impact.impactinfocollection.value.numeric.IntegerGeneImpact

/**
 * created by manzhang on 2021/10/21
 */
class ResourceImpactOfIndividual : ImpactsOfIndividual {

    /**
     * impact of changing size of the resource in the individual
     * key - a name of resource, i.e., path
     * value - impact
     */
    val resourceSizeImpact: MutableMap<String, IntegerGeneImpact>

    /**
     * impact of changing size of the table in the initialization of individual.
     *        note that here we do not consider the table in resource handling,
     *        since those could be handled by [resourceSizeImpact]
     * key - a name of table
     * value - impact
     */
    val sqlTableSizeImpact: MutableMap<String, IntegerGeneImpact>

    constructor(
            initializationGeneImpacts: InitializationActionImpacts,
            actionGeneImpacts: MutableList<ImpactsOfAction>,
            impactsOfStructure: ActionStructureImpact = ActionStructureImpact("StructureSize"),
            maxSqlInitActionsPerMissingData: Int,
            resourceSizeImpact: MutableMap<String, IntegerGeneImpact>,
            sqlTableImpact: MutableMap<String, IntegerGeneImpact>
    ) : super(initializationGeneImpacts, actionGeneImpacts, impactsOfStructure, maxSqlInitActionsPerMissingData) {
        this.resourceSizeImpact = resourceSizeImpact
        this.sqlTableSizeImpact = sqlTableImpact
    }

    constructor(individual: RestIndividual, abstractInitializationGeneToMutate: Boolean, maxSqlInitActionsPerMissingData: Int, fitnessValue: FitnessValue?)
            : super(individual, abstractInitializationGeneToMutate, maxSqlInitActionsPerMissingData, fitnessValue) {
        resourceSizeImpact = mutableMapOf<String, IntegerGeneImpact>().apply {
            individual.seeResource(RestIndividual.ResourceFilter.ALL).forEach { r->
                putIfAbsent(r, IntegerGeneImpact("size"))
            }
        }
        sqlTableSizeImpact = mutableMapOf<String, IntegerGeneImpact>().apply {
            individual.seeInitializingActions().filterNot { it.representExistingData }.forEach { d->
                putIfAbsent(d.table.name, IntegerGeneImpact("size"))
            }
        }
    }

    /**
     * @return a copy of [this]
     */
    override fun copy(): ResourceImpactOfIndividual {
        return ResourceImpactOfIndividual(
                initializationGeneImpacts.copy(),
                actionGeneImpacts.map { it.copy() }.toMutableList(),
                impactsOfStructure.copy(),
                maxSqlInitActionsPerMissingData,
                mutableMapOf<String, IntegerGeneImpact>().apply {
                    putAll(resourceSizeImpact.map { it.key to it.value.copy() })
                },
                mutableMapOf<String, IntegerGeneImpact>().apply {
                    putAll(sqlTableSizeImpact.map { it.key to it.value.copy() })
                }
        )
    }

    /**
     * @return a clone of [this]
     */
    override fun clone(): ResourceImpactOfIndividual {
        return ResourceImpactOfIndividual(
                initializationGeneImpacts.clone(),
                actionGeneImpacts.map { it.clone() }.toMutableList(),
                impactsOfStructure.clone(),
                maxSqlInitActionsPerMissingData,
                mutableMapOf<String, IntegerGeneImpact>().apply {
                    putAll(resourceSizeImpact.map { it.key to it.value.clone() })
                },
                mutableMapOf<String, IntegerGeneImpact>().apply {
                    putAll(sqlTableSizeImpact.map { it.key to it.value.clone() })
                }
        )
    }

    /**
     * count an impact of changing resource size
     */
    fun countResourceSizeImpact(previous: RestIndividual, current: RestIndividual, noImpactTargets: Set<Int>, impactTargets: Set<Int>, improvedTargets: Set<Int>, onlyManipulation: Boolean = false) {
        val currentRs = current.seeResource(RestIndividual.ResourceFilter.ALL)
        val previousRs = previous.seeResource(RestIndividual.ResourceFilter.ALL)
        currentRs.toSet().forEach { cr ->
            val rImpact = resourceSizeImpact.getOrPut(cr){IntegerGeneImpact("size")}
            if (currentRs.count { it == cr } != previousRs.count { it == cr }) {
                rImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
            }
        }

        val currentTs = current.seeInitializingActions().filterNot { it.representExistingData }.map { it.table.name }
        val previousTs = previous.seeInitializingActions().filterNot { it.representExistingData }.map { it.table.name }
        currentTs.toSet().forEach { cr ->
            val tImpact = sqlTableSizeImpact.getOrPut(cr){IntegerGeneImpact("size")}
            if (currentTs.count { it == cr } != previousTs.count { it == cr }) {
                tImpact.countImpactAndPerformance(noImpactTargets = noImpactTargets, impactTargets = impactTargets, improvedTargets = improvedTargets, onlyManipulation = onlyManipulation, num = 1)
            }
        }

        // shall I remove impacts of deleted resources or table?
    }
}