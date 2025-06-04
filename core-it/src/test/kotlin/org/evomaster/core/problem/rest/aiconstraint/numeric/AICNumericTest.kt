package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICNumericController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class AICNumericTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AICNumericController())
        }
    }

    @BeforeEach
    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification","GAUSSIAN"))
    }


    @Test
    fun testBasicInjectorCallModelOnce() {


        // create a request
        val pirTest = getPirToRest()
        // get is a RestCallAction
        val get = pirTest.fromVerbPath("get", "/api/petShop",
            mapOf("category" to "REPTILE",
                "gender" to "FEMALE",
                "birthYear" to "2007",
                "vaccinationYear" to "2020",
                "isAlive" to "true",
                "weight" to "10.5"))!!

        // Calculate the input dimension of the classifier
        // However, this part should be handled based on the schema
        var dimension:Int = 0
        for (gene in get.seeTopGenes()) {
            when (gene) {
                is IntegerGene, is DoubleGene, is BooleanGene, is EnumGene<*> -> {
                    dimension++
                }
            }
        }
        assertTrue(dimension==6)

        // Create a gaussian classifier
        val classifier = injector.getInstance(AIResponseClassifier::class.java)
        classifier.setDimension(dimension)
        classifier.initModel() // initialize after setting the dimension

        // createIndividual send the request and evaluate
        val individual = createIndividual(listOf(get), SampleType.RANDOM)
        val evaluatedAction = individual.evaluatedMainActions()[0]
        val action = evaluatedAction.action as RestCallAction
        val result = evaluatedAction.result as RestCallResult

        // update the model
        classifier.updateModel(action, result)

        // classify an action
        val c = classifier.classify(action)
        // the classification provides two values as the probability of getting 400 and 200
        assertTrue(c.probabilities.values.all { it in 0.0..1.0 }, "All probabilities must be in [0,1]")

    }

}

