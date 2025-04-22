package com.foo.rest.examples.bb.exampleobject

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbexampleobject"])
@RestController
open class BBExampleObjectApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBExampleObjectApplication::class.java, *args)
        }
    }

    @PostMapping()
    fun post(@RequestBody dto: ExampleObjectDto): ResponseEntity<String>{

        if(dto.id == "Foo" && dto.b == true && dto.x == 42 && dto.y == 12.3
            && dto.other?.name == "Bar" && dto.other?.x == 88){
            CoveredTargets.cover("A")
            return ResponseEntity("OK", HttpStatus.OK)
        }

        return ResponseEntity("FAIL", HttpStatus.BAD_REQUEST)
    }
}