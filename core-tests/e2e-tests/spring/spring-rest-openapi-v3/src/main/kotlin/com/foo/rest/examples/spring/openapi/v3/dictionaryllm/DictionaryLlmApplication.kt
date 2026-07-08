package com.foo.rest.examples.spring.openapi.v3.dictionaryllm

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/dictionaryllm"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class DictionaryLlmApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DictionaryLlmApplication::class.java, *args)
        }
    }

    @PostMapping
    fun post(@RequestBody dto: DictionaryLlmDto) : ResponseEntity<String> {


        if(dto.giganotosaurus == "triceratops") {
            return ResponseEntity.ok("OK")
        }

        return ResponseEntity.status(400).build()
    }
}