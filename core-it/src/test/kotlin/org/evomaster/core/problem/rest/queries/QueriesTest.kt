package org.evomaster.core.problem.rest.queries

import bar.examples.it.spring.queries.QueriesController
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class QueriesTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(QueriesController())
        }
    }

    @Test
    fun testStringOK(){

        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/string", mapOf("x" to "FOO", "y" to "BAR"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that x is OptionalGene, and it is active (since we have provided x)
        val xParam = get.parameters.filter { it.name == "x" }[0]
        Assert.assertTrue(xParam.primaryGene()::class == OptionalGene::class)
        Assert.assertTrue((xParam.primaryGene() as OptionalGene).isActive)


        // check that y is StringGene
        val yParam = get.parameters.filter { it.name == "y" }[0]
        Assert.assertTrue(yParam.primaryGene()::class == StringGene::class)

        // check that value of y is set to BAR
        Assert.assertTrue(yParam.primaryGene().getValueAsRawString() == "BAR")

        assertEquals(200, res.getStatusCode())
    }

    @Test
    fun testStringWrong() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/string", mapOf("x" to "NOT_FOO", "y" to "NOT_BAR"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that x is OptionalGene, and it is active (since we have provided x)
        val xParam = get.parameters.filter { it -> it.name.equals("x") }[0]
        Assert.assertTrue(xParam.gene::class == OptionalGene::class)
        Assert.assertTrue((xParam.gene as OptionalGene).isActive)

        // check that y is StringGene
        val yParam = get.parameters.filter { it -> it.name.equals("y") }[0]
        Assert.assertTrue(yParam.gene::class == StringGene::class)

        // check that value of y is set to NOT_BAR
        Assert.assertTrue(yParam.gene.getValueAsRawString().equals("NOT_BAR"))

        assertEquals(400, res.getStatusCode())
    }

    //TODO write more tests for different combinations
    @Test
    fun testStringOnlyWithOneRequired() {

        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/string", mapOf("y" to "BAR"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult


        // check that x is OptionalGene and it is not active
        val xParam = get.parameters.filter { it -> it.name.equals("x") }[0]
        Assert.assertTrue(xParam.gene::class == OptionalGene::class)
        Assert.assertFalse((xParam.gene as OptionalGene).isActive)

        // check that y is StringGene
        val yParam = get.parameters.filter { it -> it.name.equals("y") }[0]
        Assert.assertTrue(yParam.gene::class == StringGene::class)

        // check that value of y is set to BAR
        Assert.assertTrue(yParam.gene.getValueAsRawString().equals("BAR"))

        assertEquals(400, res.getStatusCode())

    }

    @Test
    fun testStringOnlyWithMissingRequired() {

        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/string", mapOf("x" to "FOO"))!!


        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that x is OptionalGene and it is not active
        val xParam = get.parameters.filter { it -> it.name.equals("x") }[0]
        Assert.assertTrue(xParam.gene::class == OptionalGene::class)
        Assert.assertTrue((xParam.gene as OptionalGene).isActive)

        // check that y is StringGene
        val yParam = get.parameters.filter { it -> it.name.equals("y") }[0]
        Assert.assertTrue(yParam.gene::class == StringGene::class)

        assertEquals(400, res.getStatusCode())
    }

    @Test
    fun testNumericAndBooleanCorrectCase() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/numbers", mapOf("a" to "42", "b" to "-0.2", "c" to "true"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that a is OptionalGene and it is active
        val aParam = get.parameters.filter { it -> it.name.equals("a") }[0]
        Assert.assertTrue(aParam.gene::class == OptionalGene::class)
        Assert.assertTrue((aParam.gene as OptionalGene).isActive)

        // check that b is DoubleGene and its value is -0.2
        val bParam = get.parameters.filter { it -> it.name.equals("b") }[0]
        Assert.assertTrue(bParam.gene::class == DoubleGene::class)
        Assert.assertTrue((bParam.gene as DoubleGene).value == -0.2)

        // check that c is BooleanGene and its value is -0.2
        val cParam = get.parameters.filter { it -> it.name.equals("c") }[0]
        Assert.assertTrue(cParam.gene::class == BooleanGene::class)
        Assert.assertTrue((cParam.gene as BooleanGene).value)

        assertEquals(200, res.getStatusCode())
    }

    @Test
    fun testNumericAndBooleanFalseCase() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/numbers", mapOf("a" to "42", "b" to "0.2", "c" to "false"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that a is OptionalGene and it is active
        val aParam = get.parameters.filter { it -> it.name.equals("a") }[0]
        Assert.assertTrue(aParam.gene::class == OptionalGene::class)
        Assert.assertTrue((aParam.gene as OptionalGene).isActive)

        // check that b is DoubleGene and its value is 0.2
        val bParam = get.parameters.filter { it -> it.name.equals("b") }[0]
        Assert.assertTrue(bParam.gene::class == DoubleGene::class)
        Assert.assertTrue((bParam.gene as DoubleGene).value == 0.2)

        // check that c is BooleanGene and its value is -0.2
        val cParam = get.parameters.filter { it -> it.name.equals("c") }[0]
        Assert.assertTrue(cParam.gene::class == BooleanGene::class)
        Assert.assertFalse((cParam.gene as BooleanGene).value)

        assertEquals(400, res.getStatusCode())
    }

    @Test
    fun testNumericAndBooleanMissingOptional() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/numbers", mapOf("b" to "0.5", "c" to "true"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that a is OptionalGene and it is active
        val aParam = get.parameters.filter { it -> it.name.equals("a") }[0]
        Assert.assertTrue(aParam.gene::class == OptionalGene::class)
        Assert.assertFalse((aParam.gene as OptionalGene).isActive)

        // check that b is DoubleGene and its value is 0.2
        val bParam = get.parameters.filter { it -> it.name.equals("b") }[0]
        Assert.assertTrue(bParam.gene::class == DoubleGene::class)
        Assert.assertTrue((bParam.gene as DoubleGene).value == 0.5)

        // check that c is BooleanGene and its value is -0.2
        val cParam = get.parameters.filter { it -> it.name.equals("c") }[0]
        Assert.assertTrue(cParam.gene::class == BooleanGene::class)
        Assert.assertTrue((cParam.gene as BooleanGene).value)

        assertEquals(400, res.getStatusCode())
    }

    @Test
    fun testNumericAndBooleanMissingOneRequired() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/numbers", mapOf("a" to "55", "c" to "true"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that a is OptionalGene and it is active
        val aParam = get.parameters.filter { it -> it.name.equals("a") }[0]
        Assert.assertTrue(aParam.gene::class == OptionalGene::class)
        Assert.assertTrue((aParam.gene as OptionalGene).isActive)

        // check that b is DoubleGene and its value is 0.2
        val bParam = get.parameters.filter { it -> it.name.equals("b") }[0]
        Assert.assertTrue(bParam.gene::class == DoubleGene::class)

        // the value of b cannot be checked

        // check that c is BooleanGene and its value is -0.2
        val cParam = get.parameters.filter { it -> it.name.equals("c") }[0]
        Assert.assertTrue(cParam.gene::class == BooleanGene::class)
        Assert.assertTrue((cParam.gene as BooleanGene).value)

        assertEquals(400, res.getStatusCode())
    }

    @Test
    fun testNumericAndBooleanMissingTwoRequired() {
        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/numbers", mapOf("a" to "75"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult

        // check that a is OptionalGene and it is active
        val aParam = get.parameters.filter { it -> it.name.equals("a") }[0]
        Assert.assertTrue(aParam.gene::class == OptionalGene::class)
        Assert.assertTrue((aParam.gene as OptionalGene).isActive)

        // check that b is DoubleGene and its value is 0.2
        val bParam = get.parameters.filter { it -> it.name.equals("b") }[0]
        Assert.assertTrue(bParam.gene::class == DoubleGene::class)

        // the value of b cannot be checked

        // check that c is BooleanGene and its value is -0.2
        val cParam = get.parameters.filter { it -> it.name.equals("c") }[0]
        Assert.assertTrue(cParam.gene::class == BooleanGene::class)

        assertEquals(400, res.getStatusCode())
    }
}