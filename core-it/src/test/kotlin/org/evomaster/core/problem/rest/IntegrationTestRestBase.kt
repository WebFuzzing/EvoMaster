package org.evomaster.core.problem.rest

import com.google.inject.Injector
import org.evomaster.core.seeding.service.rest.PirToRest
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.BeforeEach

abstract class IntegrationTestRestBase : RestTestBase() {


    protected lateinit var injector: Injector

    @BeforeEach
    fun initInjector(){
        injector = init(listOf())
    }

    fun getPirToRest() = injector.getInstance(PirToRest::class.java)
}