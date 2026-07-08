package com.foo.rest.examples.spring.openapi.v3.dictionarybase

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/dictionarybase"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class DictionaryBaseApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DictionaryBaseApplication::class.java, *args)
        }
    }

    @GetMapping(path = ["/{aggregatortype}"])
    fun get(@PathVariable aggregatortype: String) : ResponseEntity<String> {

        /*
            based on actual entry in core/src/main/resources/llm_dictionary.jsonl
            {"aggregatortype":["sum","avg","count","min","max","first","last","median","mode","stddev","variance","group_concat","array_agg","string_agg","bit_and","bit_or","bool_and","bool_or","every","some"]}
            if change dictionary, we might need to update this test
         */

        if(listOf("sum","avg","count","min","max","first","last","median","mode","stddev","variance").contains(aggregatortype)){
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}