package org.evomaster.e2etests.spring.openapi.v3.stringlength

import com.foo.rest.examples.spring.openapi.v3.stringlength.StringLengthController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StringLengthEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StringLengthController())
        }
    }


    @Test
    fun testRunEM_tooShort() {

        runTestHandlingFlakyAndCompilation(
                "StringLengthEM_tooShort",
                "org.foo.StringLengthEM_tooShort",
                100
        ) { args: MutableList<String> ->

            args.add("--maxLengthForStrings")
            args.add("2")
            args.add("--maxLengthForStringsAtSamplingTime")
            args.add("2")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
        }
    }


    @Test
    fun testRunEM_canMutate() {

        runTestHandlingFlakyAndCompilation(
            "StringLengthEM_canMutate",
            "org.foo.StringLengthEM_canMutate",
            1000
        ) { args: MutableList<String> ->

            args.add("--maxLengthForStrings")
            args.add("20")
            args.add("--maxLengthForStringsAtSamplingTime")
            args.add("1") // do not cover it on sampling, but mutation should add over the limit

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/stringlength/long", "OK")
        }
    }


    @Test
    fun testRunEM_taint() {

        runTestHandlingFlakyAndCompilation(
            "StringLengthEM_taint",
            "org.foo.StringLengthEM_taint",
            100
        ) { args: MutableList<String> ->

            args.add("--maxLengthForStrings")
            args.add("500")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/stringlength/taint", "OK")
        }
    }

}