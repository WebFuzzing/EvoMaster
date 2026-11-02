package org.evomaster.core.problem.rest.individual

import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallAction.Companion.HTTP_CALL_LOCATION_FOLLOWUP_KEY
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RestIndividualFollowUpTest {

    @Test
    fun testFollowUpUsesBoundSourceDespiteInterveningActions() {
        val source = action("/projects/items")
        val unrelatedCreator = action("/users/items")
        val unrelatedGet = action("/health", HttpVerb.GET)
        val followUp = followUp()
        val individual = individual(source, unrelatedCreator, unrelatedGet, followUp)
        source.saveAndLinkLocationTo(followUp)

        assertEquals(source.path, individual.getSourcePathOfLocationFollowUpAction(3))
    }

    @Test
    fun testFollowUpResolvesNestedLocationBindings() {
        val firstSource = action("/first")
        val secondSource = action("/second")
        val firstFollowUp = followUp(HttpVerb.PUT)
        val secondFollowUp = followUp(HttpVerb.PUT)
        val lastFollowUp = followUp()
        val individual = individual(firstSource, secondSource, firstFollowUp, secondFollowUp, lastFollowUp)
        firstSource.saveAndLinkLocationTo(firstFollowUp)
        secondSource.saveAndLinkLocationTo(secondFollowUp)
        firstFollowUp.saveAndLinkLocationTo(lastFollowUp)

        assertEquals(firstSource.path, individual.getSourcePathOfLocationFollowUpAction(2))
        assertEquals(secondSource.path, individual.getSourcePathOfLocationFollowUpAction(3))
        assertEquals(firstSource.path, individual.getSourcePathOfLocationFollowUpAction(4))
    }

    @Test
    fun testFollowUpStopsAtSchemaActionEvenWhenLocationChained() {
        val parent = action("/projects")
        val source = action("/projects/{id}/items")
        val followUp = followUp()
        val individual = individual(parent, source, followUp)
        parent.saveAndLinkLocationTo(source)
        source.saveAndLinkLocationTo(followUp)

        assertEquals(source.path, individual.getSourcePathOfLocationFollowUpAction(2))
    }

    @Test
    fun testFollowUpRejectsMissingOrDanglingLocationBinding() {
        for (locationId in listOf(null, "missing")) {
            val source = action("/items")
            val followUp = followUp()
            val individual = individual(source, followUp)
            followUp.usePreviousLocationId = locationId

            assertThrows(IllegalStateException::class.java) {
                individual.getSourcePathOfLocationFollowUpAction(1)
            }
        }
    }

    @Test
    fun testFollowUpCannotReferenceAFollowingSource() {
        val unrelated = action("/unrelated")
        val followUp = followUp()
        val source = action("/items")
        val individual = individual(unrelated, followUp, source)
        source.saveAndLinkLocationTo(followUp)

        assertThrows(IllegalStateException::class.java) {
            individual.getSourcePathOfLocationFollowUpAction(1)
        }
    }

    @Test
    fun testFirstActionHasNoPreviousPath() {
        val individual = individual(followUp())

        assertNull(individual.getSourcePathOfLocationFollowUpAction(0))
    }

    private fun action(path: String, verb: HttpVerb = HttpVerb.POST): RestCallAction {
        return RestCallAction("$verb:$path", verb, RestPath(path), mutableListOf())
    }

    private fun followUp(verb: HttpVerb = HttpVerb.GET): RestCallAction {
        return RestCallAction("$verb:$HTTP_CALL_LOCATION_FOLLOWUP_KEY", verb, RestPath("/"), mutableListOf())
    }

    private fun individual(vararg actions: RestCallAction): RestIndividual {
        return RestIndividual(actions = actions.toMutableList(), sampleType = SampleType.RANDOM)
            .apply { doInitializeLocalId() }
    }
}
