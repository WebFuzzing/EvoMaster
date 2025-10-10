package org.evomaster.core.problem.rest.callgraphdelete

import bar.examples.it.spring.body.BodyController
import bar.examples.it.spring.callgraphdelete.CallGraphDeleteController
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CallGraphDeleteTest : IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CallGraphDeleteController())
        }
    }

    @Test
    fun testFindDeleteFor() {

        val graph = injector.getInstance(CallGraphService::class.java)
        val pirTest = getPirToRest()

        val a = pirTest.fromVerbPath("post", "/a/users")!!
        var res = graph.findDeleteFor(a)!!
        assertEquals("/a/users/{id}", res.path.toString())

        val bpost = pirTest.fromVerbPath("post", "/b/users/{id}")!!
        res = graph.findDeleteFor(bpost)!!
        assertEquals("/b/users/{id}", res.path.toString())
        val bput = pirTest.fromVerbPath("put", "/b/users/{id}")!!
        res = graph.findDeleteFor(bput)!!
        assertEquals("/b/users/{id}", res.path.toString())

        val c = pirTest.fromVerbPath("put", "/c/users/{x}/{y}")!!
        res = graph.findDeleteFor(c)!!
        assertEquals("/c/users/{x}/{y}", res.path.toString())

        val d = pirTest.fromVerbPath("post", "/d/users/{x}")!!
        res = graph.findDeleteFor(d)!!
        assertEquals("/d/users/{x}/foo", res.path.toString())

        val e = pirTest.fromVerbPath("put", "/e/foo")!!
        res = graph.findDeleteFor(e)!!
        assertEquals("/e/foo", res.path.toString())

        val f = pirTest.fromVerbPath("post", "/f/users/{x}/{y}")!!
        res = graph.findDeleteFor(f)!!
        assertEquals("/f/users/{x}/{y}/{id}", res.path.toString())

        val g = pirTest.fromVerbPath("post", "/g/users")!!
        res = graph.findDeleteFor(g)!!
        assertEquals("/g/users/{x}/{y}/{id}", res.path.toString())

        val h = pirTest.fromVerbPath("post", "/h/users")!!
        res = graph.findDeleteFor(h)!!
        assertEquals("/h/users/delete/{id}", res.path.toString())

        val i = pirTest.fromVerbPath("post", "/i/users")!!
        res = graph.findDeleteFor(i)!!
        assertEquals("/i/user/{id}", res.path.toString())

        val l = pirTest.fromVerbPath("post", "/l/foo")!!
        res = graph.findDeleteFor(l)!!
        assertEquals("/l/bar", res.path.toString())
    }

}