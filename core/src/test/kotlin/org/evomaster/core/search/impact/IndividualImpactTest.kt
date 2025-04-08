package org.evomaster.core.search.impact

import org.evomaster.core.TestUtils
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.scheduletask.ScheduleTaskAction
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.sql.SqlAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * check init and update impact of individual
 */
class IndividualImpactTest {


    private fun generateFkIndividual() : RestIndividual{
        val twoDbActions = TestUtils.generateTwoFakeDbActions(1001L, 1002L, 12345L, 10L, "Foo", "Bar", 0, 42)

        val scheduleAction = TestUtils.generateFakeScheduleAction()

        val fooAction = TestUtils.generateFakeQueryRestAction("1", "/foo")
        val barAction = TestUtils.generateFakeQueryRestAction("2", "/bar", true)


//        return RestIndividual(mutableListOf(fooAction, barAction), SampleType.RANDOM, twoDbActions.toMutableList())
        return RestIndividual(
            sampleType = SampleType.SEEDED,
            allActions = twoDbActions
                .plus(scheduleAction)
                .plus(RestResourceCalls(actions = listOf(fooAction), sqlActions = mutableListOf()))
                .plus(RestResourceCalls(actions = listOf(barAction), sqlActions = mutableListOf())).toMutableList(),
            mainSize = 2,
            sqlSize = 2,
            scheduleSize = 1
        )
    }

    @Test
    fun testRestIndividualImpactInit(){

        val ind = generateFkIndividual()

        val impactInfo = ImpactsOfIndividual(ind, listOf(SqlAction::class, ScheduleTaskAction::class),false, null)

        impactInfo.fixedMainActionImpacts.apply {
            assertEquals(2, size)
            assertEquals(2, this[0].geneImpacts.size)
            assertEquals(1, this[1].geneImpacts.size)
        }

        impactInfo.initActionImpacts.apply {
            assertEquals(2, size)
            val scheduleImpacts = this[ScheduleTaskAction::class.java.name]
            assertNotNull(scheduleImpacts)
            scheduleImpacts.apply {
                assertEquals(1, this!!.getSize())
                getAll()[0].apply {
                    assertEquals(2, this.geneImpacts.size)
                }
            }
        }
    }
}
