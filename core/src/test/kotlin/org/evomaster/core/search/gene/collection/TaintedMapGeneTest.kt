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


    @Test
    fun testAddElement(){

        val gene = TaintedMapGene("foo", TaintInputName.getTaintName(42))
        gene.doInitialize()
        assertEquals(1, gene.getSizeOfElements())

        var json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        var dto = mapper.readValue(json, DTO::class.java)
        assertNull(dto.x)
        assertNull(dto.y)

        gene.addNewKey("y")
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        dto = mapper.readValue(json, DTO::class.java)
        assertNull(dto.x)
        assertNotNull(dto.y)

        gene.addNewKey("x")
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        //default tainted value is String, but x is an int, so should crash when marshalling it
        assertThrows(Exception::class.java){ mapper.readValue(json, DTO::class.java) }

        gene.specifyValueTypeForKey("x", "java/lang/Integer")
        json = gene.getValueAsPrintableString(mode= GeneUtils.EscapeMode.JSON)
        dto = mapper.readValue(json, DTO::class.java)
        assertNotNull(dto.x)
        assertNotNull(dto.y)
    }
}