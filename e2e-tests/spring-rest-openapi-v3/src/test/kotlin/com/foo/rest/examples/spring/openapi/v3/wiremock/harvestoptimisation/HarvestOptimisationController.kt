package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.wiremock.mockexternal.MockExternalApplication
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.SocketUtils

class HarvestOptimisationController(): SpringController(HarvestOptimisationApplication::class.java) {

    private var mockApplicationContext: ConfigurableApplicationContext? = null

    override fun startSut(): String {
        // If the port set to 0, mostly it takes a port above 60000
        val port: Int = SocketUtils.findAvailableTcpPort(9000, 10000)

        mockApplicationContext = SpringApplication.run(MockExternalApplication::class.java, "--server.port=9000")

        val args = arrayOf("--server.port=0", "--external=${getMockApplicationURL()}")
        ctx = SpringApplication.run(applicationClass, *args)

        return "http://localhost:$sutPort"
    }

    override fun stopSut() {
        mockApplicationContext?.stop()
        mockApplicationContext?.close()
        super.stopSut()
    }

    private fun getMockApplicationURL() : String{
        val port = (mockApplicationContext!!.environment.propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int
        return "http://imaginary-host.local:$port"
    }
}