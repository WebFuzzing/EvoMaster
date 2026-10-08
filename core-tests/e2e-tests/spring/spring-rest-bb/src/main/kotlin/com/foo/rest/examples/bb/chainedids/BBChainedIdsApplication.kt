package com.foo.rest.examples.bb.chainedids

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/chainedids"])
@RestController
open class BBChainedIdsApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBChainedIdsApplication::class.java, *args)
        }

        private val dataX: MutableMap<String, X> = ConcurrentHashMap()
        private val dataY: MutableMap<String, Y> = ConcurrentHashMap()

        fun size() = dataX.size + dataY.size
    }

    class X (var id: String? = null)
    class Y (var id: String? = null)


    @PostMapping(path = ["/x"])
    fun createX(): ResponseEntity<X> {
        val id = UUID.randomUUID().toString()
        val x = X(id)
        dataX[id] = x
        return ResponseEntity.status(201).body(x)
    }

    @GetMapping(path = ["/x/{id}"])
    fun getX(@PathVariable("id") id: String): ResponseEntity<X> {
        val x = dataX[id]
        return if(x == null) {
            ResponseEntity.notFound().build()
        } else {
            CoveredTargets.cover("OKX")
            ResponseEntity.ok(x)
        }
    }

    @DeleteMapping(path = ["/x/{id}"])
    fun deleteX(@PathVariable("id") id: String): ResponseEntity<Any> {
        val x = dataX[id]
        return if(x == null) {
            ResponseEntity.notFound().build()
        } else {
            dataX.remove(id)
            ResponseEntity.status(204).build()
        }
    }

    @PostMapping(path = ["/y/"])
    fun createY(): ResponseEntity<Y> {
        val id = UUID.randomUUID().toString()
        val y = Y(id)
        dataY[id] = y
        return ResponseEntity.status(201).body(y)
    }

    @GetMapping(path = ["/y/{id}"])
    fun getY(@PathVariable("id") id: String): ResponseEntity<Y> {
        val y = dataY[id]
        return if(y == null) {
            ResponseEntity.notFound().build()
        } else {
            CoveredTargets.cover("OKY")
            ResponseEntity.ok(y)
        }
    }

    @DeleteMapping(path = ["/y/{id}"])
    fun deleteY(@PathVariable("id") id: String): ResponseEntity<Any> {
        val y = dataY[id]
        return if(y == null) {
            ResponseEntity.notFound().build()
        } else {
            dataY.remove(id)
            ResponseEntity.status(204).build()
        }
    }


}