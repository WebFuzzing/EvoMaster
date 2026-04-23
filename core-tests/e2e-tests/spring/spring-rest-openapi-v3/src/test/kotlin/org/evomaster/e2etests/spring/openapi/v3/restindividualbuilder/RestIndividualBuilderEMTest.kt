package org.evomaster.e2etests.spring.openapi.v3.restindividualbuilder

import com.foo.rest.examples.spring.openapi.v3.restindividualbuilder.RestIndividiualBuilderController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RestIndividualBuilderEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(RestIndividiualBuilderController())
        }
    }

    /*
        This test is added to reproduce the StackOverflow error that occurs in the RestIndividualBuilder class
        during BlackBox test generation. If there are POST or PUT options for creating a resource, the
        createResourcesFor method calls both resources repeatedly, leading to the ancestor actions being created
        infinitely. Here, we just make sure EM doesn't crash.
     */
    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "RestIndividualBuilderEM",
            100
        ) { args: MutableList<String> ->

            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")

            initAndRun(args)
        }
    }
}