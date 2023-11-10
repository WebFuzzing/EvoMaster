package com.foo.rest.examples.spring.openapi.v3.bbdefault

import com.foo.rest.examples.spring.openapi.v3.base.BaseApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbdefault"])
@RestController
open class BBDefaultApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBDefaultApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get(@RequestParam data: Int?) : ResponseEntity<String> {

        if(data == 42)
            return ResponseEntity.ok("OK")

        return ResponseEntity.status(400).build()
    }


    @GetMapping(path = ["/{x}"])
    open fun getX(@PathVariable x: String) : ResponseEntity<String> {

        if(x == "foo")
            return ResponseEntity.ok("OK")

        return ResponseEntity.status(400).build()
    }

    //TODO default in objects?
}