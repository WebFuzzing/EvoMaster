package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SwaggerParserTest {

    @Test
    fun testSchemeAndHost(){

        val location = "/swagger/sut/ncs.json"
        val schema = OpenAPIParser().readLocation(location, null, null).openAPI

        assertEquals(1, schema.servers.size)
        val url = schema.servers[0].url
        assertEquals("http://localhost:8080/", url)
    }

    @Test
    fun testHostNoScheme(){
        val location = "/swagger/sut/gestaohospital.json"
        val schema = OpenAPIParser().readLocation(location, null, null).openAPI

        assertEquals(1, schema.servers.size)
        val url = schema.servers[0].url
        //technically invalid. to be used, would need to be repaired
        assertEquals("//localhost:8080/", url)
    }

    @Test
    fun testMultiSchemes(){
        val location = "/swagger/sut/disease_sh_api.json"
        val schema = OpenAPIParser().readLocation(location, null, null).openAPI

        assertEquals(2, schema.servers.size)
        assertEquals("https://disease.sh/", schema.servers[0].url)
        assertEquals("http://disease.sh/", schema.servers[1].url)
    }
}