package org.evomaster.e2etests.spring.openapi.v3.examples.expectations

import com.foo.rest.examples.spring.openapi.v3.expectations.ExpectationsController
import org.junit.jupiter.api.BeforeAll

class ExpectationTestBase {

    @BeforeAll
    public fun initClass(){
        val controller = ExpectationsController()
    }
}