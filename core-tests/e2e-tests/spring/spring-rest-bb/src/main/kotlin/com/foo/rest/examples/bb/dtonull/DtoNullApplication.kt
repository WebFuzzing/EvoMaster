package com.foo.rest.examples.bb.dtonull

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbdtonull"])
@RestController
open class DtoNullApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DtoNullApplication::class.java, *args)
        }
    }


    @PostMapping(path = ["/items"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun post(@RequestBody dto: DtoNullDto) : ResponseEntity<String>{

        val x = dto.x

        if(x == null){
            CoveredTargets.cover("UNDEFINED")
            return ResponseEntity.status(400).body("UNDEFINED")
        }

        if(!x.isPresent){
            CoveredTargets.cover("NULL")
            return ResponseEntity.status(409).body("NULL")
        }

        val n = x.get()
        if(n >=0) {
            CoveredTargets.cover("POSITIVE")
            return ResponseEntity.status(200).body("POSITIVE")
        } else {
            CoveredTargets.cover("NEGATIVE")
            return ResponseEntity.status(201).body("NEGATIVE")
        }
    }
}