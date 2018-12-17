package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.gene.Gene

class RestResourceCalls(val resource: RestResource, val actions: MutableList<RestAction>){

    fun copy() : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> a.copy() as RestAction}.toMutableList())
    }

    fun copy(other: MutableList<RestAction>) : RestResourceCalls{
        return RestResourceCalls(resource.copy(), actions.map { a -> other.find { o -> a.getName() == o.getName() }!!}.toMutableList())
    }

    /**
     * return genes that represents this resource, i.e., longest action in this resource call
     */
    fun seeGenes() : List<out Gene>{
        return longestPath().seeGenes()
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

    private fun longestPath() : RestAction{
        val max = actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }.max()!!
        val candidates = actions.filter { a -> a is RestCallAction && a.path.levels() == max }
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
}