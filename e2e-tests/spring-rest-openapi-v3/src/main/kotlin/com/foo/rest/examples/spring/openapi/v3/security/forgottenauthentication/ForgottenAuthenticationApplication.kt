package com.foo.rest.examples.spring.openapi.v3.security.forgottenauthentication

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class ForgottenAuthenticationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ForgottenAuthenticationApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")

    // this is the wrong check, it should be the same as checkAuth
    private fun checkAuthWrongly(auth: String?) =  auth == null || (auth == "FOO" || auth == "BAR")


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
        if(source != auth){
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.status(204).build()
    }

    @GetMapping(path = ["/{id}"])
    open fun get(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int): ResponseEntity<String> {

        if(!checkAuthWrongly(auth)) {
            // wrong, leaking forgotten authentication. should return 401
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(403).build()
        }

        val source = data.getValue(id)

        if(auth != null && source != auth){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(200).body(source)
    }
}