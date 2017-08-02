package org.evomaster.core.casestudy

import io.restassured.RestAssured.given
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class ScoutApiTestNot {

    @Test
    fun test(){

        val ctr = RemoteController("localhost", 40100)
        val info = ctr.getControllerInfo()
        assertNotNull(info)
        assertTrue(info!!.isInstrumentationOn)

        assertTrue(ctr.stopSUT())
        assertTrue(ctr.startSUT())

        var targets = ctr.getTargetCoverage()
        assertTrue(targets!!.targets.isEmpty())

        val baseUrlOfSut = ctr.getSutInfo()!!.baseUrlOfSUT

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/v1/system/roles")
                .then()
                .statusCode(200)

        targets = ctr.getTargetCoverage()
        assertTrue(targets!!.targets.size > 0)

        targets!!.targets.forEach { t ->
            System.out.println(t.descriptiveId)
        }
    }
}