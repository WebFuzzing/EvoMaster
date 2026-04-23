package com.foo.rest.examples.spring.openapi.v3.expectations

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/num/exp"])
class ExpectationsNumRest {

    @GetMapping(path = ["/basicNum/{s}"])
    fun numBehaviour(
            @PathVariable("s") succeeded: Int
    ): ResponseEntity<String> {
        if( succeeded >= 0) return ResponseEntity.ok("Success! $succeeded")
        else return ResponseEntity.status(500).build()
    }
}