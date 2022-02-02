package com.foo.rest.examples.spring.openapi.v3.taintkotlinequal

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/taintkotlinequal"])
class TaintKotlinEqualRest {

    @GetMapping(path = ["/{a}"])
    open fun check(@PathVariable("a") a: String) : ResponseEntity<String> {
        return if (a == "hellotheremyfriend"){
            ResponseEntity.ok("OK")
        } else{
            ResponseEntity.status(500).build()
        }

    }
}