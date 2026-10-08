package org.evomaster.core.problem.rest.dynamicpath

import bar.examples.it.spring.body.BodyController
import bar.examples.it.spring.dynamicpath.DynamicPathController
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.builder.DynamicPathUtils
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DynamicPathTest : IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DynamicPathController())
        }
    }


    @Test
    fun testForceSameQueryParams() {

        val pirTest = getPirToRest()

        val put = pirTest.fromVerbPath(
            "put", "/api/dynamicpath/x/42", queryParams = mapOf("foo" to "abc")
        )!!

        val get = pirTest.fromVerbPath(
            "get", "/api/dynamicpath/x/77", queryParams = mapOf("foo" to "wrong","bar" to "true", "k" to "true")
        )!!

        DynamicPathUtils.bindToSamePathResolution(get, put)
        DynamicPathUtils.forceSameQueryParams(get, put)

        val x = put.resolvedPath()
        val y = get.resolvedPath()

        assertEquals("/api/dynamicpath/x/42?foo=abc", x)
        assertEquals("/api/dynamicpath/x/42?foo=abc&k=true", y)
    }

}