package org.evomaster.core.problem.rest.service.securitytest

import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SecurityRestDeleteTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
           // initClass(PathStatusController())
        }
    }

    @Disabled
    @Test
    fun testDeletePut(){

        //TODO
        val pirTest = getPirToRest()
    }
}