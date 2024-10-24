package org.evomaster.core.search.gene.collection

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.lang.Exception

class TaintedMapGeneTest{

    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private open class DTO(
        val x : Int? = null,
        val y : String? = null
    )

    private open class ArrayDto(
        val x : Array<Int>? = null,
        val y: Array<java.lang.Integer>? = null,
        val b: Array<Boolean>? = null,
        val a: Array<Array<Float>>? = null,
        val m: Array<Map<String,Any>>? = null,
        val l: List<String>? = null,
    )

    @Test
    fun testAddElement(){

        val gene = TaintedMapGene("foo", TaintInputName.getTaintName(42))
        gene.doInitialize()
        assertEquals(1, gene.getSizeOfElements())

        var json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        var dto = mapper.readValue(json, DTO::class.java)
        assertNull(dto.x)
        assertNull(dto.y)

        gene.registerKey("y")
        gene.evolve()
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        dto = mapper.readValue(json, DTO::class.java)
        assertNull(dto.x)
        assertNotNull(dto.y)

        gene.registerKey("x")
        gene.evolve()
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        //default tainted value is String, but x is an int, so should crash when marshalling it
        assertThrows(Exception::class.java){ mapper.readValue(json, DTO::class.java) }

        gene.registerNewType("x", "java/lang/Integer")
        gene.evolve()
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        dto = mapper.readValue(json, DTO::class.java)
        assertNotNull(dto.x)
        assertNotNull(dto.y)
    }


    private fun verifyCollection(key: String, type: String, lambda: (ArrayDto) -> Any?){

        val gene = TaintedMapGene("foo", TaintInputName.getTaintName(42))
        gene.doInitialize()
        gene.registerKey(key)
        gene.evolve()
        gene.registerNewType(key, type)
        gene.evolve()

        var json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        var dto = mapper.readValue(json, ArrayDto::class.java)
        assertNotNull(lambda.invoke(dto))

        val random = Randomness()
        repeat(10){
            gene.randomize(random, true)
            json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
            dto = mapper.readValue(json, ArrayDto::class.java)

            val collection = lambda.invoke(dto)
            assertNotNull(collection)

            if(collection is Array<*> && collection.isNotEmpty()){
                return
            }
            if(collection is Collection<*> && collection.isNotEmpty()){
                return
            }
        }
        fail("Target collection was always empty after randomization")
    }

    @Test
    fun testIntArray(){
        verifyCollection("x", "[I"){it.x}
    }

    @Test
    fun testIntegerArray(){
        verifyCollection("y", "[Ljava/lang/Integer;"){it.y}
    }

    @Test
    fun testBooleanArray(){
        verifyCollection("b", "[Z"){it.b}
    }

    @Test
    fun testArrayArray(){
        verifyCollection("a", "[[F"){it.a}
    }

    @Test
    fun testMapArray(){
        verifyCollection("m", "[Ljava/util/Map;"){it.m}
    }

    @Test
    fun testList(){
        verifyCollection("l", "Ljava/util/List;"){it.l}
    }

}