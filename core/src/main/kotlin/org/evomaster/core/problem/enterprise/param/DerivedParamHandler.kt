package org.evomaster.core.problem.enterprise.param

import org.evomaster.client.java.controller.api.dto.problem.param.RestDerivedParamDto
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils

class DerivedParamHandler {

    /**
     * ParamName -> info
     *
     */
    private val bodyParams: MutableMap<String,DerivedParamInfo> = mutableMapOf()

    fun initialize(derivedParams: List<RestDerivedParamDto>){

        if(derivedParams.isEmpty()){
            return
        }

        bodyParams.clear()

        for(param in derivedParams){

            // TODO param.context
            val key = param.paramName
            if(bodyParams.containsKey(key)){
                throw SutProblemException("Duplicate derived param name '$key' for the same context")
            }
            val entryPoints = mutableSetOf<String>()
            if(param.endpointPaths != null){
                entryPoints.addAll(param.endpointPaths)
            }
            val info = DerivedParamInfo(key,entryPoints,param.order)
            bodyParams[key] = info
        }
    }

    fun getOrderLevels() : List<Int>{
        return bodyParams.map { it.value.order }
            .toSet()
            .sorted()
    }

    fun prepareRequest(ind: Individual, orderLevel: Int) : List<DerivedParamChangeReq>{

        if(ind !is RestIndividual){
            return emptyList()
        }

        val req = mutableListOf<DerivedParamChangeReq>()

        val actions = ind.seeMainExecutableActions()
        for(i in actions.indices){
            val a = actions[i]
            val body = a.parameters.filterIsInstance<BodyParam>()
            if(body.isEmpty()){
                continue
            }
            val obj = body[0].primaryGene()
                    .getWrappedGene(ObjectGene::class.java)
                    ?: continue
            for(f in obj.fields){
                if(!bodyParams.containsKey(f.name)){
                    continue
                }
                val info = bodyParams[f.name]!!
                if(info.order != orderLevel){
                    continue
                }
                val entryPoints = info.entryPoints
                if(entryPoints.isEmpty() || entryPoints.contains(a.path.toString())){
                    val json = obj.getValueAsPrintableString(targetFormat = null, mode = GeneUtils.EscapeMode.JSON)
                    req.add(DerivedParamChangeReq(f.name,json,a.path.toString(),i))
                }
            }
        }

        return req
    }

    fun modifyParam(ind: Individual, paramName: String, paramValue: String, actionIndex: Int){

        if(ind !is RestIndividual){
            return
        }

        val actions = ind.seeMainExecutableActions()
        if(actionIndex < 0  || actionIndex > actions.size - 1){
            throw IllegalArgumentException("Invalid action index: $actionIndex")
        }

        val error = "Failed applying param derivation for '$paramName'."

        val a = actions[actionIndex]

        //TODO context
        val body = a.parameters.filterIsInstance<BodyParam>()
        if(body.isEmpty()){
            throw IllegalArgumentException("$error No body defined for ${a.getName()}")
        }
        val primary = body[0].primaryGene()
        val obj = primary.getWrappedGene(ObjectGene::class.java)
            ?: throw IllegalArgumentException("$error No object definition for body")
        if(!obj.staticCheckIfImpactPhenotype()){
            //in case body payload is not required
            return
        }
        val target = obj.fields.find { it.name == paramName }
            ?: throw IllegalArgumentException("$error Not found field '$paramName'")

        target.setFromStringValue(paramValue)
    }
}