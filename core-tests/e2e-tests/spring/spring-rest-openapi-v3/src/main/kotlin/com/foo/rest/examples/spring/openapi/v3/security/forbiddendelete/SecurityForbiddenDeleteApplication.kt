package com.foo.rest.examples.spring.openapi.v3.security.forbiddendelete

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/forbiddendelete/resources"])
@RestController
open class SecurityForbiddenDeleteApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SecurityForbiddenDeleteApplication::class.java, *args)
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
        val res = ResponseEntity.created(URI.create("/api/forbiddendelete/resources/${counter}")).build<Any>()
        counter.getAndIncrement()
        return res
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

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = data.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
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
            data[id] = auth!!
            return ResponseEntity.status(201).build()
        }

        val source = data.getValue(id)
        //BUG
//        if(source != auth){
//            return ResponseEntity.status(403).build()
//        }
        return ResponseEntity.status(204).build()
    }

    @PatchMapping(path = ["/{id}"])
    open fun patch(
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
        //BUG
//        if(source != auth){
//            return ResponseEntity.status(403).build()
//        }
        return ResponseEntity.status(204).build()
    }


}