package com.foo.rest.examples.spring.openapi.v3.security.notrecognized

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class NotRecognizedApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(NotRecognizedApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")


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

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(403).build()
        }

        val source = data.getValue(id)
        if(source != auth){
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.status(200).body(source)
    }

    @PostMapping(path = ["/"])
    open fun onlyBar(@RequestHeader("Authorization") auth: String?): ResponseEntity<String> {

        if(auth == null || auth != "BAR"){ //because of the schema definition, auth can be random string value.
            // wrong, as FOO should be recognized, and possibly return 403 if no access
            return ResponseEntity.status(401).build()
        }
        //we return 204 only if the auth is "BAR"
        return ResponseEntity.status(204).build()
    }
}
