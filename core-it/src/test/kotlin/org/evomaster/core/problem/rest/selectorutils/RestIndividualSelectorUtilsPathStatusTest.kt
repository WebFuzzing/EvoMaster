package org.evomaster.core.problem.rest.selectorutils

import bar.examples.it.spring.pathstatus.PathStatusController
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.RestPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RestIndividualSelectorUtilsPathStatusTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PathStatusController())
        }
    }


    @Test
    fun testPathStatus(){

        val pirTest = getPirToRest()

        val byStatus = RestPath("/api/")
    }

    //TODO
}