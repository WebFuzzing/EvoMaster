package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.gene.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RestPathTest{

    @Test
    fun testResolvePathWithVariable(){

        val id = 5
        val pathParam = PathParam("id", IntegerGene("id", id))

        val path = "/api/foo/{id}"
        val restPath = RestPath(path)

        val resolved = restPath.resolve(listOf(pathParam))
        assertEquals("/api/foo/"+id, resolved)
    }

}