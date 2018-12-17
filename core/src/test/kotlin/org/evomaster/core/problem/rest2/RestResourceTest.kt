package org.evomaster.core.problem.rest2

import io.swagger.parser.SwaggerParser
import org.evomaster.core.problem.rest.RestActionBuilder
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class RestResourceTest{

    private fun loadActions(resourcePath: String, expectedNum : Int)
            : MutableMap<String, RestAResource>  {

        val resourceCluster = mutableMapOf<String, RestAResource>()
        val swagger = SwaggerParser().read(resourcePath)

        val actions: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilder.addActionsFromSwagger(swagger, actions)


        actions.values.forEach { u ->
            if(u is RestCallAction){
                resourceCluster.getOrPut(u.path.toString()){ RestAResource(u.path.copy(), mutableListOf()) }.actions.add(u)
            }

        }
        resourceCluster.forEach { k, ar->
            ar.setAncestors(resourceCluster.values.toList())
            ar.initVerbs()
        }

        assertEquals(expectedNum, resourceCluster.size)
        return resourceCluster
    }

    @Test
    fun testFeaturesService() {
        val resourceCluster = loadActions("/swagger/features_service.json", 11)
        val randomness = Randomness()

        val resourceCall = resourceCluster["/products/{productName}/configurations/{configurationName}/features/{featureName}"]!!.sampleRestResourceCalls("POST-DELETE", randomness, 10)
        assertEquals(4, resourceCall.actions.size)

        var name : String? = null
        val featureNameGenes = resourceCall.actions.flatMap { a -> a.seeGenes() }.flatMap { g -> g.flatView() }.filter { g -> g is StringGene && g.getVariableName() == "featureName" }
        assert(featureNameGenes.isNotEmpty())
        featureNameGenes.forEach { fg->
            if(name == null) name = fg.getValueAsRawString()
            else assertEquals(name, fg.getValueAsRawString())
        }

        name = null
        val configurationNameGenes = resourceCall.actions.flatMap { a -> a.seeGenes() }.flatMap { g -> g.flatView() }.filter { g -> g is StringGene && g.getVariableName() == "configurationName" }
        assert(configurationNameGenes.isNotEmpty())
        configurationNameGenes.forEach { fg->
            if(name == null) name = fg.getValueAsRawString()
            else assertEquals(name, fg.getValueAsRawString())
        }

        name = null
        val productNameGenes = resourceCall.actions.flatMap { a -> a.seeGenes() }.flatMap { g -> g.flatView() }.filter { g -> g is StringGene && g.getVariableName() == "productName" }
        assert(productNameGenes.isNotEmpty())
        productNameGenes.forEach { fg->
            if(name == null) name = fg.getValueAsRawString()
            else assertEquals(name, fg.getValueAsRawString())
        }

        val allGenes = resourceCall.seeGenes().flatMap { it.flatView() }
        val productNameGene = allGenes.find { g -> g is StringGene && g.getVariableName() == "productName" }
        productNameGene?.apply {
            randomize(randomness, true, allGenes)
            name = getValueAsRawString()
        }
        resourceCall.repairGenesAfterMutation(productNameGene)

        resourceCall.actions
                .flatMap { a -> a.seeGenes() }.flatMap { g -> g.flatView() }
                .filter { g -> g is StringGene && g.getVariableName() == "productName" }
                .forEach { fg->
                    assertEquals(name, fg.getValueAsRawString())
                }
    }
}