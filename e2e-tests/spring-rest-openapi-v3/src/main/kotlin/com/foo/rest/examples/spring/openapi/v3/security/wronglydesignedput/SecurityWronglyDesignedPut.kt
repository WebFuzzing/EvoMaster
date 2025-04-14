package com.foo.rest.examples.spring.openapi.v3.security.wronglydesignedput

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/wronglydesignedput/resources"])
@RestController
open class SecurityWronglyDesignedPut {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SecurityWronglyDesignedPut::class.java, *args)
        }

        private val data = ConcurrentHashMap<Int, String>()
        private val counter = AtomicInteger(0)

        fun reset(){
            counter.set(0)
            data.clear()
        }
    }


    @PostMapping
    open fun create(
        @RequestHeader("Authorization") auth: String?,
    ) : ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        data[counter.get()] = auth!!
        counter.getAndIncrement()
        return ResponseEntity.status(200).build<Any>()
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")


    @DeleteMapping(path = ["/{id}"])
    open fun delete(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        // This is a security issue, as the user with "BAR" should not be able to delete the resource.
        if(auth == "BAR"){
            return ResponseEntity.status(403).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        data.remove(id)
        return ResponseEntity.status(204).build()
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = data.getValue(id)
        if(source != auth){
            return ResponseEntity.status(401).build()
        }
        data[id] = auth!!
        return ResponseEntity.status(201).build()
    }

}