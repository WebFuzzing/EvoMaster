package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestoptimisation

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.wiremock.mockexternal.MockExternalApplication
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.SocketUtils

class HarvestOptimisationController(): SpringController(HarvestOptimisationApplication::class.java) {

    private var mockApplicationContext: ConfigurableApplicationContext? = null

    override fun startSut(): String {
        // If address is not, Spring binds it to localhost. localhost is skipped under
        // method replacement. Due to that, there will be no WM spun. If it's set to
        // 127.0.0.1, the port becomes unavailable for some reason. Most 127.0.0.2
        // is used to spin the default WM, so address set to 127.0.0.2.
        val mockArgs = arrayOf("--server.port=0", "--server.address=127.0.0.2")
        mockApplicationContext = SpringApplication.run(MockExternalApplication::class.java, *mockArgs)

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
        return "http://mock.int:$port"
    }
}