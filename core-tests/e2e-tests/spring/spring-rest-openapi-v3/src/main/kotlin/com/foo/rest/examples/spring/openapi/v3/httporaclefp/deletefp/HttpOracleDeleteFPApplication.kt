package com.foo.rest.examples.spring.openapi.v3.httporaclefp.deletefp

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class HttpOracleDeleteFPApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpOracleDeleteFPApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        data[id] = "Data for $id"
        return ResponseEntity.status(200).build()
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        return ResponseEntity.status(200).body(data[id])
    }

    @DeleteMapping(path = ["/{id}"])
    open fun delete(@PathVariable("id") id: Int): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        //not wrong, as not stating we are done
        return ResponseEntity.status(202).build()
    }
}