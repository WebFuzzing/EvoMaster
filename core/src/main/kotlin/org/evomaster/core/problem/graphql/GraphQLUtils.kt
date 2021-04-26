package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.gene.ObjectGene

object GraphQLUtils {

    //TODO add getPrintableInputGene


    fun repairIndividual(ind: GraphQLIndividual){
        ind.seeActions().forEach { a ->
            a.parameters.filterIsInstance<GQReturnParam>().forEach { p ->
                if(p.gene is ObjectGene){
                    GeneUtils.repairBooleanSelection(p.gene)
                }
            }
        }
    }

}