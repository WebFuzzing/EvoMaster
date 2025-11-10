package com.foo.rest.examples.bb.chainedlocation

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.core.MediaType

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/chainedlocation"])
@RestController
open class BBChainedLocationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBChainedLocationApplication::class.java, *args)
        }

        private val data: MutableMap<Int, X> = ConcurrentHashMap()

        private val counter = AtomicInteger(0)
    }


    @RequestMapping(
        path = ["/x/{idx}/y/{idy}/z/{idz}/value"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON]
    )
    fun getZValue(
        @PathVariable("idx") idx: Int,
        @PathVariable("idy") idy: Int,
        @PathVariable("idz") idz: Int
    ): ResponseEntity<*> {
        val x = data[idx] ?: return ResponseEntity.status(404).build<Any>()

        val y = x.map[idy] ?: return ResponseEntity.status(404).build<Any>()

        val z = y.map[idz] ?: return ResponseEntity.status(404).build<Any>()

        CoveredTargets.cover("OK")
        return ResponseEntity.status(200).build<Any>()
    }

    @RequestMapping(path = ["/x"], method = [RequestMethod.POST])
    fun createX(): ResponseEntity<*> {
        val index = counter.incrementAndGet()
        val x = X()
        data[index] = x

        return ResponseEntity.created(URI.create("/api/chainedlocation/x/$index")).build<Any>()
    }

    @RequestMapping(path = ["/x/{idx}/y"], method = [RequestMethod.POST])
    fun createY(@PathVariable("idx") idx: Int): ResponseEntity<*> {
        val x = data[idx] ?: return ResponseEntity.status(404).build<Any>()

        val index = counter.incrementAndGet()
        val y = Y()
        x.map[index] = y

        return ResponseEntity.created(URI.create("/api/chainedlocation/x/$idx/y/$index")).build<Any>()
    }


    @RequestMapping(path = ["/x/{idx}/y/{idy}/z"], method = [RequestMethod.POST])
    fun createZ(
        @PathVariable("idx") idx: Int,
        @PathVariable("idy") idy: Int
    ): ResponseEntity<*> {
        val x = data[idx] ?: return ResponseEntity.status(404).build<Any>()

        val y = x.map[idy] ?: return ResponseEntity.status(404).build<Any>()

        val index = counter.incrementAndGet()
        val z = Z()
        y.map[index] = z

        return ResponseEntity.created(URI.create("/api/chainedlocation/x/$idx/y/$idy/z/$index")).build<Any>()
    }


    private class X {
        val map: MutableMap<Int, Y> = ConcurrentHashMap()
    }

    private class Y {
        val map: MutableMap<Int, Z> = ConcurrentHashMap()
    }

    private class Z {
        var value: String? = null
    }
}