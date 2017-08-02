package org.evomaster.core.casestudy

import io.restassured.RestAssured
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test


class OcvnTestNot {

    @Test
    fun test(){

        val ctr = RemoteController("localhost", 40100)
        val info = ctr.getControllerInfo()
        Assertions.assertNotNull(info)
        Assertions.assertTrue(info!!.isInstrumentationOn)

        Assertions.assertTrue(ctr.stopSUT())
        Assertions.assertTrue(ctr.startSUT())

        var targets = ctr.getTargetCoverage()
        Assertions.assertTrue(targets!!.targets.isEmpty())

        val baseUrlOfSut = ctr.getSutInfo()!!.baseUrlOfSUT

        try {
            RestAssured.given().accept("*/*")
                    .get(baseUrlOfSut + "/api/totalProjectsByYear/")
            fail<Any>("Expected exception")
        }catch (e: Exception){

        }

//        targets = ctr.getTargetCoverage()
//        Assertions.assertTrue(targets!!.targets.size > 0)
//
//        targets!!.targets.forEach { t ->
//            System.out.println(t.descriptiveId)
//        }
    }
}