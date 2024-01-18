package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.EMConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EndpointFilterTest {

    companion object{

        private val schema = OpenAPIParser().readLocation("/swagger/artificial/filters.yaml", null, null).openAPI

        @JvmStatic
        @BeforeAll
        fun checkInit(){
            assertNotNull(schema)
            assertNotNull(schema.paths)
            assertEquals(4, schema.paths.size)
        }
    }

    @Test
    fun testNone(){
        val config = EMConfig()
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(0, selection.size)
    }

    @Test
    fun testFocus(){
        val config = EMConfig()
        config.endpointFocus = "/api/x"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(3, selection.size)
    }

    @Test
    fun testPrefix(){
        val config = EMConfig()
        config.endpointPrefix = "/api/y"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(4, selection.size)
    }

    @Test
    fun testWrongTag(){
        val config = EMConfig()
        config.endpointTagFilter = "foo"
        assertThrows(Exception::class.java){EndpointFilter.getEndpointsToSkip(config,schema)}
    }

    @Test
    fun testTagsX(){
        val config = EMConfig()
        config.endpointTagFilter = "X"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(3, selection.size)
    }

    @Test
    fun testTagsY(){
        val config = EMConfig()
        config.endpointTagFilter = "Y"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(3, selection.size)
    }

    @Test
    fun testTagsZ(){
        val config = EMConfig()
        config.endpointTagFilter = "Z"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(5, selection.size)
    }

    @Test
    fun testTagsXY(){
        val config = EMConfig()
        config.endpointTagFilter = "X ,   Y"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(1, selection.size)
    }

    @Test
    fun testTagsYZ(){
        val config = EMConfig()
        config.endpointTagFilter = "   Z ,   Y"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(3, selection.size)
    }


    @Test
    fun testTagsXZ(){
        val config = EMConfig()
        config.endpointTagFilter = "   Z ,   X   "
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(2, selection.size)
    }

    @Test
    fun testTagsXYZ(){
        val config = EMConfig()
        config.endpointTagFilter = "   Z ,   X ,Y  "
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(1, selection.size)
    }

    @Test
    fun testMixed(){
        val config = EMConfig()
        config.endpointTagFilter = "X"
        config.endpointPrefix = "/api/y"
        val selection = EndpointFilter.getEndpointsToSkip(config, schema)
        assertEquals(5, selection.size)
    }
}
