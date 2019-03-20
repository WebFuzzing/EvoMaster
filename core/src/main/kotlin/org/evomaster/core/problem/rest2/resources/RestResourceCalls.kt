package org.evomaster.core.problem.rest.serviceII.resources

import ch.qos.logback.classic.db.DBAppender
import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest2.resources.CallsTemplate
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.impact.ImpactOfGene

class RestResourceCalls(
        val template : CallsTemplate,
        val resource: RestResource,
        val actions: MutableList<RestAction>
){

    /**
     * [doesCompareDB] represents whether to compare db data after executing calls
     */
    var doesCompareDB : Boolean = false

    /**
     * [dbActions] are used to initialize data for rest actions, either select from db or insert new data into db
     */
    val dbActions = mutableListOf<DbAction>()


    fun copy() : RestResourceCalls{
        val copy = RestResourceCalls(template, resource.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
        if(dbActions.isNotEmpty()){
            dbActions.forEach { db->
                copy.dbActions.add(db.copy() as DbAction)
            }
        }
        copy.doesCompareDB = doesCompareDB

        return copy
    }

    fun copy(other: MutableList<RestAction>) : RestResourceCalls{
        return RestResourceCalls(template, resource.copy(), actions.map { a -> other.find { o -> a.getName() == o.getName() }!!}.toMutableList())
    }

    /**
     * return genes that represents this resource, i.e., longest action in this resource call
     */
    fun seeGenes() : List<out Gene>{
        return longestPath().seeGenes()
    }

    fun seeGenesIdMap() : Map<Gene, String>{
        longestPath().apply {
            return seeGenes().map { it to ImpactOfGene.generateId(this, it) }.toMap()
        }
    }

    fun repairGenesAfterMutation(gene: Gene? = null){
        val target = longestPath()
        repairGenePerAction(gene, target)
        actions.filter { it is RestCallAction && it != target }
                .forEach{a-> (a as RestCallAction).bindToSamePathResolution(target as RestCallAction)}

    }

    fun repairGenesAfterMutation(){
        repairGenesAfterMutation(null)
    }

    fun longestPath() : RestAction{
        val max = actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.possibleLevels() }.max()!!
        val candidates = actions.filter { a -> a is RestCallAction && a.path.possibleLevels() == max }
        return candidates.first()
    }

    private fun repairGenePerAction(gene: Gene? = null, action : RestAction){
        if(gene != null){
            val genes = action.seeGenes().flatMap { g->g.flatView() }
            if(genes.contains(gene))
                genes.filter { ig-> ig != gene && ig.name == gene.name && ig::class.java.simpleName == gene::class.java.simpleName }.forEach {cg->
                    cg.copyValueFrom(gene)
                }
        }
    }

    fun getVerbs(): Array<HttpVerb>{
        return actions.filter { it is RestCallAction }.map { (it as RestCallAction).verb }.toTypedArray()
    }

}