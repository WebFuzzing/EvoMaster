package org.evomaster.core.problem.rest.param

import io.swagger.v3.oas.models.parameters.Parameter
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.ArrayGene


class QueryParam(
    name: String,
    gene: Gene,
    /*
        It defines how collections like arrays are going to be represented, if in a singe param,
        or multi-params with same key
        //https://swagger.io/docs/specification/serialization/
     */
    val explode: Boolean = false,
    val style: Parameter.StyleEnum = Parameter.StyleEnum.FORM
    ) : Param(name, gene){


    init {
        //https://swagger.io/docs/specification/serialization/
        /*
            sending x=[1,2,3]  instead of x=1,2,3 is wrong, and can lead to crashes in
            server if desearilazation is not properly handled.
            TODO: But sending such malformatted string should be handled as part of Robustness Testing
         */

        val array = gene.getWrappedGene(ArrayGene::class.java)
        if(array != null){
            val sep : String = when(style){
                Parameter.StyleEnum.SPACEDELIMITED -> " "
                Parameter.StyleEnum.PIPEDELIMITED -> "|"
                else -> ","
            }
            array.modifyPrinting("","", sep)
        }
    }

    fun getGeneForQuery() = genes[0]

    override fun copyContent(): Param {
        return QueryParam(name, gene.copy(), explode, style)
    }
}