package org.evomaster.core.problem.rest.stringvariables

import bar.examples.it.spring.stringvariables.StringVariablesController
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.hibernate.validator.internal.util.Contracts.assertTrue
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StringVariablesDataPoolTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StringVariablesController())
        }
    }

    @BeforeEach
    fun initTests(){
        val config = getEMConfig()
        config.useSuccessDataPool = true
        config.useResponseDataPool = false
        config.useObjectExampleDataPool = false
        config.useDictionaryDataPool = false

        assertEquals(0, getDataPool().keySize())
    }

    @Test
    fun testFailMissingX(){

        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/sva")!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(400, res.getStatusCode())
    }

    @Test
    fun testValidX(){
        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/sva", mapOf("x" to "hello"))!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, res.getStatusCode())

        val dataPool = getDataPool()
        //y and bar are required, so they are added when creating action
        assertEquals(1+2, dataPool.keySize())
        assertTrue(dataPool.hasExactKey("x"))
        assertTrue(dataPool.hasExactKey("y"))
        assertTrue(dataPool.hasExactKey("bar"))
        assertFalse(dataPool.hasExactKey("foo"))

        assertEquals("hello", dataPool.extractValue("x"))
    }

    @Test
    fun testValidXYBar(){
        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/sva",
            mapOf("x" to "abc", "y" to "there"),
            """ 
                {"bar": "hi"}
            """.trimIndent())!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, res.getStatusCode())

        val dataPool = getDataPool()
        assertEquals(3, dataPool.keySize())
        assertTrue(dataPool.hasExactKey("x"))
        assertTrue(dataPool.hasExactKey("y"))
        assertTrue(dataPool.hasExactKey("bar"))
        assertFalse(dataPool.hasExactKey("foo"))

        assertEquals("abc", dataPool.extractValue("x"))
        assertEquals("there", dataPool.extractValue("y"))
        assertEquals("hi", dataPool.extractValue("bar"))
    }


    @Test
    fun testMultipleNested(){
        val pirTest = getPirToRest()

        val post = pirTest.fromVerbPath("post", "/api/sva",
            mapOf("x" to "a", "y" to "b"),
            """ 
                {"bar": "hi", "foo": "how", "nested": {"foo": "Z", "bar": "K"} }
            """.trimIndent())!!

        val x = createIndividual(listOf(post))
        val res = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, res.getStatusCode())

        val dataPool = getDataPool()
        assertEquals(6, dataPool.keySize())
        assertTrue(dataPool.hasExactKey("x"))
        assertTrue(dataPool.hasExactKey("y"))
        assertTrue(dataPool.hasExactKey("bar"))
        assertTrue(dataPool.hasExactKey("foo"))

        assertEquals("a", dataPool.extractValue("x"))
        assertEquals("b", dataPool.extractValue("y"))

        val foo = dataPool.extractAllWithExactKey("foo")
        assertEquals(2, foo.size)
        assertTrue(foo.contains("how"))
        assertTrue(foo.contains("Z"))

        val bar = dataPool.extractAllWithExactKey("bar")
        assertEquals(2, bar.size)
        assertTrue(foo.contains("hi"))
        assertTrue(foo.contains("K"))
    }

}