package com.foo.rest.examples.spring.openapi.v3.bbexamples

import com.foo.rest.examples.spring.openapi.v3.base.BaseApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbexamples"])
@RestController
open class BBExamplesApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBExamplesApplication::class.java, *args)
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

    @GetMapping(path = ["/{x}/mixed"])
    open fun getMixed(@PathVariable x: Int, @RequestParam data: String?) : ResponseEntity<String>{

        if(x == 12345)
            return ResponseEntity.status(200).body("12345")
        if(x == 456789)
            return ResponseEntity.status(201).body("456789")
        if(x == 778899)
            return ResponseEntity.status(202).body("778899")

        if(data == "Foo")
            return ResponseEntity.status(203).body("Foo")
        if(data == "Bar")
            return ResponseEntity.status(250).body("Bar")
        if(data == "Hello")
            return ResponseEntity.status(251).body("Hello")

        return ResponseEntity.status(400).build()
    }

    //TODO examples in objects?
}