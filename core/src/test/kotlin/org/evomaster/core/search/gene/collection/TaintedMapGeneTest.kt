package org.evomaster.core.search.gene.collection

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Exception

class TaintedMapGeneTest{

    private val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private open class DTO(
        val x : Int? = null,
        val y : String? = null
    )

    private open class ArrayDto(
        val x : Array<Int>? = null
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


    @Test
    fun testIntArray(){

        val gene = TaintedMapGene("foo", TaintInputName.getTaintName(42))
        gene.doInitialize()
        gene.registerKey("x")
        gene.evolve()

        gene.registerNewType("x", "[I")
        gene.evolve()

        val json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        val dto = mapper.readValue(json, ArrayDto::class.java)
        assertNotNull(dto.x)
    }


    /*
        TODO
        testIntegerArray
        testBooleanArray
        testArrayArray
        testMapArray
        testDoubleList
        testMapList
     */
}