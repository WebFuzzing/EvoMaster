package org.evomaster.core.remote.service

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.ActionDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder
import org.evomaster.client.java.sql.DbSpecification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Asking the driver for the coverage of a large set of targets fails, because the ids are sent
 * as a query parameter and the resulting URI does not fit in the request line.
 *
 * Measured on the "jasper" case study (120s, seed 1): during the recomputation stage of the
 * minimization, [org.evomaster.core.problem.enterprise.service.EnterpriseFitness] refetches
 * descriptive ids for 1554 targets. That is 7302 characters of query string, which together with
 * the path, the other query parameters and the standard headers goes over the 8192 bytes that
 * Jetty allows by default for a request. Jetty answers 414 before the request ever reaches
 * [org.evomaster.client.java.controller.internal.EMController], so the driver code never runs and
 * cannot do anything about it.
 *
 * On the EvoMaster side the 414 is not an exception: [RemoteControllerImplementation.getTestResults]
 * just returns null, the caller logs "Cannot retrieve coverage with full descriptive ids", and the
 * individual is dropped. 22 individuals were lost this way in a single jasper run.
 *
 * The test drives the real client ([RemoteControllerImplementation]) against a real driver, on
 * purpose: it asserts on the outcome of the API call, not on how the ids happen to be transported.
 * That way it keeps passing unchanged once the transport is moved off the URI, and it would catch
 * a regression if it ever moved back.
 */
class RemoteControllerManyTargetsTest {

    companion object {

        /**
         * Number of targets refetched in the jasper run that exposed the problem.
         */
        const val NUMBER_OF_TARGETS = 1554

        /**
         * Realistic magnitude for target ids at the end of a run. Jasper had registered more than
         * 10000 targets by the time minimization started, so the ids being refetched were 5 digits
         * long. Starting from 0 would make the query string shorter than what a real search
         * produces, and the test weaker.
         */
        const val FIRST_ID = 10_000

        const val FAKE_SWAGGER = "/swagger.json"
    }

    private class FakeRestController : EmbeddedSutController() {

        var running: Boolean = false

        override fun startSut(): String? {
            running = true
            return null
        }

        override fun isSutRunning(): Boolean = running

        override fun stopSut() {
            running = false
        }

        override fun getPackagePrefixesToCover(): String? = null

        override fun resetStateOfSUT() {}

        override fun getInfoForAuthentication(): List<AuthenticationDto>? = null

        override fun getDbSpecifications(): MutableList<DbSpecification>? = null

        override fun getProblemInfo(): ProblemInfo = RestProblem(FAKE_SWAGGER, null)

        override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat =
            SutInfoDto.OutputFormat.JAVA_JUNIT_5
    }

    private val driver = FakeRestController()
    private lateinit var remote: RemoteController

    @BeforeEach
    fun init() {
        if (driver.isSutRunning) {
            driver.stopSut()
        }
        driver.controllerPort = 0 //ephemeral
        driver.startTheControllerServer()

        remote = RemoteControllerImplementation("localhost", driver.controllerServerPort, false, false)
    }

    @AfterEach
    fun tearDown() {
        driver.stopSut()
        remote.close()
        driver.stopTheControllerServer()
    }


    @Test
    fun testGetTestResultsForManyTargets() {

        remote.startANewSearch()
        remote.startSUT()
        remote.registerNewAction(ActionDto().apply { index = 0 })

        /*
            The driver refuses to report on an id it never handed out ("Id 'x' is not mapped"),
            so first let it map as many targets as an instrumented SUT would. This is the same
            call the instrumentation makes the first time it sees a target, and it assigns the
            numeric ids sequentially from 0.
         */
        repeat(FIRST_ID + NUMBER_OF_TARGETS) { i ->
            ObjectiveRecorder.getMappedId("Branch_at_Foo_at_line_$i")
        }

        /*
            Control: the very same call, with few ids, works. None of these targets was ever
            covered, so the driver answers with a "not reached" entry for each requested id.
            The only thing that changes below is how many ids are asked for.
         */
        val few = (FIRST_ID until FIRST_ID + 10).toSet()
        val small = remote.getTestResults(ids = few, descriptiveIds = true)
        assertNotNull(small, "Baseline call with ${few.size} ids should work")
        assertEquals(few.size, small!!.targets.size)

        val many = (FIRST_ID until FIRST_ID + NUMBER_OF_TARGETS).toSet()

        val results = remote.getTestResults(ids = many, descriptiveIds = true)

        assertNotNull(results,
            "Failed to fetch the coverage of ${many.size} targets." +
                    " Sent as a query parameter, their ids alone take" +
                    " ${many.joinToString(",").length} characters," +
                    " which does not fit in the ${8 * 1024} bytes Jetty allows for a request.")
        assertEquals(many.size, results!!.targets.size)
    }

    /**
     * Asking for no id at all means "every target".
     *
     * This is easy to break while changing how the ids are transported. An empty list arriving
     * at the driver has to be turned back into null, which is what
     * [org.evomaster.client.java.instrumentation.InstrumentationController.getTargetInfos] reads
     * as "send everything". Passing an empty set instead would answer 200 with an empty, perfectly
     * well formed coverage: no error anywhere, and a search that silently sees nothing.
     */
    @Test
    fun testGetTestResultsForAllTargets() {

        remote.startANewSearch()
        remote.startSUT()
        remote.registerNewAction(ActionDto().apply { index = 0 })

        //cover some real targets, so that "everything" and "nothing" are distinguishable
        val comparisons = 5
        repeat(comparisons) { i ->
            ExecutionTracer.executedNumericComparison("Foo_at_line_$i", 0.1, 0.2, 1.0)
        }

        /*
            Targets seen for the first time are sent back whether they were asked for or not, so
            while that list is populated an empty request looks the same as a correct one. The
            driver empties it at the start of every new test, and that is precisely the state
            during the recomputation stage of the minimization, which only re-runs tests whose
            targets are all already known.
         */
        ObjectiveRecorder.clearFirstTimeEncountered()

        val all = remote.getTestResults()

        assertNotNull(all, "Failed to fetch the coverage of all targets")
        assertTrue(all!!.targets.isNotEmpty(),
            "Asking with no id returned no target at all." +
                    " An empty list of ids must be understood as a request for every target.")
        assertEquals(comparisons * 3, all.targets.size)
    }
}
