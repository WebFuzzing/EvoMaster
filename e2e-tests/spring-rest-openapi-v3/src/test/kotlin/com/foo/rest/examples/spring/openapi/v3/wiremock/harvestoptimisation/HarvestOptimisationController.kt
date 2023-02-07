package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class HarvestOptimisationController(): SpringController(HarvestOptimisationApplication::class.java) {

    var mockApplicationContext: ConfigurableApplicationContext? = null

    override fun startSut(): String {
        mockApplicationContext = SpringApplication.run(MockExternalApplication::class.java, "--server.port=9001")
        return super.startSut()
    }

    override fun stopSut() {
        super.stopSut()
        mockApplicationContext?.stop()
        mockApplicationContext?.close()
    }

    private fun getFakeApplicationURL() : String{
        val port = (mockApplicationContext!!.environment.propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int
        return "http://localhost:$port"
    }
}