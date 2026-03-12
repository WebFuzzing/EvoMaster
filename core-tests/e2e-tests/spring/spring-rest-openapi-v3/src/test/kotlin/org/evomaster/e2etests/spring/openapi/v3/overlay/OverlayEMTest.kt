package org.evomaster.e2etests.spring.openapi.v3.overlay

import com.foo.rest.examples.spring.openapi.v3.overlay.OverlayController
import com.foo.rest.examples.spring.openapi.v3.stringlength.StringLengthController
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OverlayEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(OverlayController())
        }
    }

    //these should be kept in sink with what written in the Overlay files
    private val X = "Some X value like 1234"
    private val Y = "Some Y value like 777"

    @Test
    fun testRunEM_Overlay_None() {

        runTestHandlingFlakyAndCompilation(
                "Overlay_None",
                100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", null)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", X)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", Y)
        }
    }


    @Test
    fun testRunEM_Overlay_X() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_X",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay/x.yaml")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", X)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", Y)
        }
    }

    @Test
    fun testRunEM_Overlay_Y_BB() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_Y_BB",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay/subfolder/y.yaml")
            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", X)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", Y)
        }
    }


    @Test
    fun testRunEM_Overlay_Z_fail() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_Z_fail",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay/z.json")
            //default behavior must be non-lenient

            //z does not exist
            assertThrows<ConfigProblemException> { initAndRun(args) }
        }
    }

    @Test
    fun testRunEM_Overlay_Z_lenient() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_Z_lenient",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay/z.json")
            setOption(args, "overlayLenient", "true")

            //z does not exist... but, when lenient, shouldn't fail
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", null)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", X)
            assertNone(solution, HttpVerb.GET, 200, "/api/overlay", Y)
        }
    }

    @Test
    fun testRunEM_Overlay_folder() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_folder",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay")
            //by default, z.json will be picked, and so failed because non-lenient

            //z does not exist
            assertThrows<ConfigProblemException> { initAndRun(args) }
        }
    }


    @Test
    fun testRunEM_Overlay_folder_filtered() {

        runTestHandlingFlakyAndCompilation(
            "Overlay_folder_filtered",
            100
        ) { args: MutableList<String> ->

            setOption(args, "overlay", "src/main/resources/overlay")
            //make sure to not pick-up z.json
            setOption(args, "overlayFileSuffixes", ".yaml")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", X)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/overlay", Y)
        }
    }


}