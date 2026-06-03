package org.evomaster.e2etests.spring.openapi.v3.base

import com.foo.rest.examples.spring.openapi.v3.base.BaseController
import org.evomaster.core.remote.SutProblemException
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class BaseHttpsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseController())
        }
    }


    @Test
    fun testRunEM() {

            val args = mutableListOf<String>()
            setOption(args, "blackBox", "true")
            setOption(args, "base", baseUrlOfSut.replace("http:","https:"))
            setOption(args, "schema", "$baseUrlOfSut/v3/api-docs")

            val e = assertThrows<SutProblemException> {
                //should recognize HTTPS is not valid
                initAndRun(args)
            }
            assertTrue(e.message!!.contains("SSL"))
    }
}