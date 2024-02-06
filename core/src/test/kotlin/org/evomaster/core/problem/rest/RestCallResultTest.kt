package org.evomaster.core.problem.rest

import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestAction
import org.evomaster.client.java.controller.api.dto.database.execution.epa.RestActions
import org.evomaster.core.problem.rest.epa.Enabled
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

internal class RestCallResultTest {

    private val restActionSet = hashSetOf(RestAction("get", "/employees"), RestAction("post", "/employees"))
    private val restActions = RestActions(restActionSet)
    private val restAction = RestAction("post", "/employees")
    private val enabled = Enabled(restAction, restActions)
    @Test
    fun givenAStringIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("", false)
        rc.setBody("{\"id\":\"735\"}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()

        assertEquals("735", res)
    }
    @Test
    fun givenANumericIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("",false)
        rc.setBody("{\"id\":735}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()

        assertEquals("735", res)
    }

    @Test
    fun setAndGetInitialEnabledEndpoints() {
        val rc = RestCallResult("",false)
        rc.setEnabledEndpointsBeforeAction(restActions)

        val res = rc.getEnabledEndpointsBeforeAction()

        Assertions.assertEquals(restActions.toStringForEPA(), res?.toStringForEPA())
    }

    @Test
    fun setAndGetEnabledEndpointsAfterAction() {
        val rc = RestCallResult("",false)
        rc.setEnabledEndpointsAfterAction(enabled)

        val res = rc.getEnabledEndpointsAfterAction()

        Assertions.assertEquals(restAction.toString(), res?.associatedRestAction.toString())
        Assertions.assertEquals(restActions.toStringForEPA(), res?.enabledRestActions?.toStringForEPA())
    }
}
