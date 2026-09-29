package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidlocationfp

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/invalid-location-fp"])
@RestController
open class InvalidLocationFPApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(InvalidLocationFPApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }


    @DeleteMapping(path = ["/{id}"])
    open fun delete(@PathVariable("id") id: Int): ResponseEntity<Any> {
        if(! data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }
        data.remove(id)
        return ResponseEntity.status(204).build()
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        val isNew = !data.containsKey(id)
        data[id] = "$id"

        val status = if (isNew) 201 else 200
        return ResponseEntity.status(status)
            .header("Location", "/api/invalid-location-fp/$id")
            .build()
    }
}
